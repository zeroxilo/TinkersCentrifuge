package slimegirl.centrifuge;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
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
import slimegirl.centrifuge.AntiAlloyModule.AntiAlloyRecipe;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.mantle.fluid.FluidTransferHelper;
import slimeknights.mantle.fluid.transfer.FluidContainerTransferManager;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferResult;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.model.ModelProperties;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipe;
import slimeknights.tconstruct.library.utils.NBTTags;
import slimeknights.tconstruct.shared.block.entity.TableBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.ITankBlockEntity;
import slimeknights.tconstruct.library.client.SafeClient;

import javax.annotation.Nullable;

public class CentrifugeBlockEntity extends TableBlockEntity implements ITankBlockEntity {
    public static final int DEFAULT_CAPACITY = FluidType.BUCKET_VOLUME * 4;
    public static final int TANK_CAPACITY = FluidType.BUCKET_VOLUME;

    private static final Component NAME = TConstruct.makeTranslation("gui", "centrifuge");

    protected final AntiAlloyModule antiAlloyModule;
    protected AntiAlloyRecipe currentRecipe;
    protected int timer = 0;
    protected final List<FluidTankAnimated> tanks;
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
        tanks = List.of(new FluidTankAnimated(TANK_CAPACITY, this),
            new FluidTankAnimated(TANK_CAPACITY, this),
            new FluidTankAnimated(TANK_CAPACITY, this),
            new FluidTankAnimated(TANK_CAPACITY, this));
        fluidHolder = LazyOptional.of(() -> tanks.get(0));
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
            IFluidHandler transferFrom = findFluidHandler(Direction.UP).orElse(null);
            if (transferFrom != null) {
                for(int i = 0;i < transferFrom.getTanks();i++){
                    FluidStack currentFluid = transferFrom.getFluidInTank(i);
                    antiAlloyModule.setLevel(this.level);
                    if (!currentFluid.isEmpty()){
                        if (antiAlloyModule.hasRecipe(currentFluid)) {
                            AntiAlloyRecipe recipe = antiAlloyModule.getRecipe(currentFluid);
                            if (recipe != null) {
                                //汲取合金，开始处理
                                transferFrom.drain(recipe.getInput(), IFluidHandler.FluidAction.EXECUTE);
                                currentRecipe = recipe;
                                timer = 10;//处理时间
                            }
                        }
                    }
                }
            }
        }
        //配方处理逻辑
        if(currentRecipe != null && timer > 0){
            timer--;
            //处理完成
            if (timer == 0) {
                //返还原料
                List<FluidStack> outputs = currentRecipe.getOutputs();
                for(FluidStack output : outputs){
                    if(output.isEmpty()){
                        continue;
                    }
                    TinkersCentrifuge.LOGGER.info("[AntiAlloy]Try to output "+output.getDisplayName().getString());
                    FluidStack remainder = output.copy();
                    for(FluidTankAnimated tank : tanks){
                        int filled = tank.fill(remainder, IFluidHandler.FluidAction.SIMULATE);
                        if(filled == output.getAmount()){
                            tank.fill(remainder, IFluidHandler.FluidAction.EXECUTE);
                            break;
                        }else if(filled > 0){
                            remainder.shrink(filled);
                            tank.fill(new FluidStack(output.getFluid(), filled), IFluidHandler.FluidAction.EXECUTE);
                        }
                    }
                }
                currentRecipe = null;
            }
        }
        //尝试将输出槽的液体依次输出到下方的容器中
        IFluidHandler transferTarget = findFluidHandler(Direction.DOWN).orElse(null);
        if (transferTarget != null){
            for(FluidTankAnimated tank : tanks){
                if(!tank.isEmpty()){
                    FluidStack output = tank.getFluid();
                    int filled = transferTarget.fill(output, IFluidHandler.FluidAction.EXECUTE);
                    if(filled > 0){
                        tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                    }
                    if (!tank.isEmpty()){
                        break;
                    }
                }
            }
        }
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
        for(FluidTankAnimated tank : tanks){
            if(!tank.isEmpty()){
                total_power += 4;
            }
        }
        return total_power;
    }


    public static int getCapacity(Block block) {
        return DEFAULT_CAPACITY;
    }
    public FluidTankAnimated getTank() {
        return tanks.get(0);
    }
    
    @Override
    public void updateFluidTo(FluidStack fluid) {
        // update tank fluid
        FluidTankAnimated tank = getTank();
        int oldAmount = tank.getFluidAmount();
        int newAmount = fluid.getAmount();
        tank.setFluid(fluid);

        // update the tank render offset from the change
        tank.setRenderOffset(tank.getRenderOffset() + newAmount - oldAmount);

        // update the block model
        if (isFluidInModel()) {
            SafeClient.updateFluidModel(getTE(), tank, oldAmount, newAmount);
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
            for (FluidTankAnimated tank : tanks) {
                tank.setFluid(FluidStack.EMPTY);
            }
        } else {
            for(int i = 0; i < tanks.size(); i++){
                if(nbt.contains("tank" + i) && !nbt.getCompound("tank" + i).isEmpty()){
                    CompoundTag outputTag = nbt.getCompound("tank" + i);
                    tanks.get(i).readFromNBT(outputTag);
                }else{
                    tanks.get(i).setFluid(FluidStack.EMPTY);
                }
            }
            //CentrifugeBlockEntity.updateLight(this, tank);
        }
    }

    //保存同步数据，客户端加载时会调用load方法
    @Override
    public void saveSynced(CompoundTag tag) {
        super.saveSynced(tag);
        // want tank on the client on world load
        for(int i = 0; i < tanks.size(); i++){
            if(!tanks.get(i).isEmpty()){
                tag.put("tank" + i,tanks.get(i).writeToNBT(new CompoundTag()));
                break;
            }
        }
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
        .with(ModelProperties.FLUID_STACK, tanks.get(0).getFluid())
        .with(ModelProperties.TANK_CAPACITY, tanks.get(0).getCapacity())
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
        updateTank(tag.getCompound("tank"));
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
}
