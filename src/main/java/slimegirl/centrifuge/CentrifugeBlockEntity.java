package slimegirl.centrifuge;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.library.client.model.ModelProperties;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;
import slimeknights.tconstruct.shared.block.entity.TableBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.ITankBlockEntity;
import slimeknights.tconstruct.library.client.SafeClient;

import javax.annotation.Nullable;

public class CentrifugeBlockEntity extends TableBlockEntity implements ITankBlockEntity {
    public static final int DEFAULT_CAPACITY = FluidType.BUCKET_VOLUME * 8;
    public static final int TANK_CAPACITY = FluidType.BUCKET_VOLUME * 1;
    public static final int TANK_NUM = 8;
    public static final int PROCESS_TIME = 10;

    private static final Component NAME = Component.translatable("gui.centrifuge");

    protected final AntiAlloyModule antiAlloyModule;
    protected AntiAlloyRecipe currentRecipe;
    protected int timer = 0;
    protected final MultiFluidTank tanks;
    private final LazyOptional<IFluidHandler> fluidHolder;
    /** Last redstone state of the block */
    private boolean lastRedstone = false;
    /** Last comparator strength to reduce block updates */
    private int lastStrength = -1;

    public static final BlockEntityTicker<CentrifugeBlockEntity> SERVER_TICKER = (level, pos, state, self) -> self.serverTick(level, pos);
    /** Handles ticking on the clientside */
    public static final BlockEntityTicker<CentrifugeBlockEntity> CLIENT_TICKER = (level, pos, state, self) -> self.clientTick(level, pos);

    
    private static final String TAG_REDSTONE = "redstone";
    

    public CentrifugeBlockEntity(BlockPos pos, BlockState state) {
        this(TinkersCentrifuge.CENTRIFUGE_ENTITY.get(), pos, state);
    }

    /* 内容初始化 */
    @SuppressWarnings("WeakerAccess")
    protected CentrifugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, NAME, 2, 1);
        antiAlloyModule = new AntiAlloyModule(this.getLevel());
        tanks = new MultiFluidTank(TANK_NUM, TANK_CAPACITY, this);
        fluidHolder = LazyOptional.of(() -> tanks);
    }

    @Nullable
    public static <CAST extends CentrifugeBlockEntity, RET extends BlockEntity> BlockEntityTicker<RET> getTicker(Level level, BlockEntityType<RET> check, BlockEntityType<CAST> casting) {
        return BlockEntityHelper.castTicker(check, casting, level.isClientSide ? CLIENT_TICKER : SERVER_TICKER);
    }

    /* 客户端每帧事件 */
    private void clientTick(Level level, BlockPos pos) {
    }
    

    /* 服务器每帧事件 - 核心处理逻辑 */
    private void serverTick(Level level, BlockPos pos) {
        //配方获取逻辑
        if (currentRecipe == null) {
            findFluidHandler(Direction.UP).ifPresent(transferFrom -> {
                for(int i = 0;i < transferFrom.getTanks();i++){
                    FluidStack currentFluid = transferFrom.getFluidInTank(i);
                    antiAlloyModule.setLevel(this.level);
                    if (!currentFluid.isEmpty() && antiAlloyModule.hasRecipe(currentFluid)) {
                        AntiAlloyRecipe recipe = antiAlloyModule.getRecipe(currentFluid);
                        //检查配方产物的空间是否充足
                        if (recipe != null && this.tanks.fitAll(recipe.getOutputs())){
                            //汲取合金，开始处理
                            transferFrom.drain(recipe.getInput(), FluidAction.EXECUTE);
                            //TinkersCentrifuge.LOGGER.info("[AntiAlloy] "+recipe.getInput().getDisplayName().getString()+" Recipe Start.");
                            //TinkersCentrifuge.LOGGER.info("[AntiAlloy] Drained"+recipe.getInput().getAmount()+"mb "+recipe.getInput().getDisplayName().getString()+".");
                            currentRecipe = recipe;
                            timer = PROCESS_TIME;//处理时间
                            break;
                        }
                    }
                }
            });
        }
        //计时器
        if(timer > 0){
            timer--;
        }
        //配方处理完成
        if(currentRecipe != null && timer <= 0){
            //TinkersCentrifuge.LOGGER.info("[AntiAlloy] "+currentRecipe.input.getDisplayName().getString()+" Recipe Complete.");
            //返还配方产物，即合金原料
            List<FluidStack> outputs = currentRecipe.getOutputs();
            for(FluidStack output : outputs){
                if(output.isEmpty()) continue;
                int filled = tanks.fill(output, FluidAction.SIMULATE);
                if(filled > 0){
                    tanks.fill(output, FluidAction.EXECUTE);
                }
                //TinkersCentrifuge.LOGGER.info("[AntiAlloy] Filled "+filled+"mb "+output.getDisplayName().getString());
                //已经做足了容量检查，如果还是溢出了，那真没办法了，你浪费吧。
                //W.I.P. 做溢出保护
            }
            currentRecipe = null;
        }
        //尝试将输出槽的液体依次输出到下方的容器中
        findFluidHandler(Direction.DOWN).ifPresent(transferTarget -> {
            if(!tanks.isEmpty()){
                FluidStack output = tanks.getFluid();
                int filled = transferTarget.fill(output, FluidAction.SIMULATE);
                if(filled == output.getAmount()){
                    transferTarget.fill(output, FluidAction.EXECUTE);
                    tanks.drain(output, FluidAction.EXECUTE);
                }else if(filled > 0){
                    transferTarget.fill(new FluidStack(output,filled), FluidAction.EXECUTE);
                    tanks.drain(new FluidStack(output,filled), FluidAction.EXECUTE);
                }
            }
        });
    }

    //比较器相关配置
    public void setLastStrength(int strength) {
        this.lastStrength = strength;
    }
    public int getLastStrength() {
        return lastStrength;
    }
    @Override
    public int comparatorStrength() {
        int total_power = 0;
        for(FluidStack fluid : tanks.fluids){
            if(!fluid.isEmpty()){
                total_power += 2;
            }
        }
        return Math.max(total_power,15);
    }


    public static int getCapacity(Block block) {
        return DEFAULT_CAPACITY;
    }
    
    @Override
    public void updateFluidTo(FluidStack fluid) {
        // update tank fluid
        int oldAmount = tanks.getFluidAmount();
        int newAmount = fluid.getAmount();
        tanks.setFluid(0,fluid);

        // update the tank render offset from the change
        tanks.setRenderOffset(tanks.getRenderOffset() + newAmount - oldAmount);

        // update the block model
        if (isFluidInModel()) {
            SafeClient.updateFluidModel(getTE(), tanks, oldAmount, newAmount);
        }
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) {
            return fluidHolder.cast();
        }
        return super.getCapability(capability, facing);
    }

    //寻找流体容器
    private LazyOptional<IFluidHandler> findFluidHandler(Direction side) {
        assert level != null;
        BlockEntity te = level.getBlockEntity(worldPosition.relative(side));
        if (te != null) {
            LazyOptional<IFluidHandler> handler = te.getCapability(ForgeCapabilities.FLUID_HANDLER, side.getOpposite());
            if (handler.isPresent()) {
                return handler;
            }
        }
        return LazyOptional.empty();
    }

    //从nbt数据中获取流体信息并更新
    public void updateTank(CompoundTag nbt) {
        if (nbt.isEmpty()) {
            tanks.setFluid(FluidStack.EMPTY);
        } else {
            tanks.readFromNBT(nbt);
            //CentrifugeBlockEntity.updateLight(this, tank);
        }
    }

    //保存同步数据，客户端加载时会调用load方法
    @Override
    public void saveSynced(CompoundTag tag) {
        super.saveSynced(tag);
        tanks.writeToNBT(tag);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidHolder.invalidate();
    }

    @Nonnull
    @Override
    public ModelData getModelData() {
        return ModelData.builder()
        .with(ModelProperties.FLUID_STACK, tanks.getFluid())
        .with(ModelProperties.TANK_CAPACITY, tanks.getCapacity())
        .build();
    }

    @Override
    public void onTankContentsChanged() {
        ITankBlockEntity.super.onTankContentsChanged();
        if (this.level != null) {
            //CentrifugeBlockEntity.updateLight(this, tank);
            this.requestModelDataUpdate();
        }
    }

    @Override
    public void load(CompoundTag tag) {
        updateTank(tag);
        lastRedstone = tag.getBoolean(TAG_REDSTONE);
        super.load(tag);
    }

    @Override
    public void saveAdditional(CompoundTag tags) {
        super.saveAdditional(tags);
        tags.putBoolean(TAG_REDSTONE, lastRedstone);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return null;
    }

    @Override
    public FluidTankAnimated getTank() {
        return tanks;
    }
}
