package slimegirl.centrifuge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.library.client.SafeClient;
import slimeknights.tconstruct.library.client.model.ModelProperties;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.utils.NBTTags;
import slimeknights.tconstruct.smeltery.block.entity.ITankBlockEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

public class CentrifugeBlockEntity extends MantleBlockEntity implements ITankBlockEntity {
    public static final int CAPACITY = FluidValues.INGOT * 48;

    protected final AntiAlloyModule antiAlloyModule;
    protected AntiAlloyRecipe currentRecipe;
    protected int timer = 0;
    protected GraduallyTimer pushOutTimer = new GraduallyTimer();
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
        super(type, pos, state);
        antiAlloyModule = new AntiAlloyModule(this.getLevel());
        tanks = new MultiFluidTank(CAPACITY, this);
        fluidHolder = LazyOptional.of(() -> tanks);
    }

    @Nullable
    public static <CAST extends CentrifugeBlockEntity, RET extends BlockEntity> BlockEntityTicker<RET> getTicker(Level level, BlockEntityType<RET> check, BlockEntityType<CAST> casting) {
        return BlockEntityHelper.castTicker(check, casting, level.isClientSide ? CLIENT_TICKER : SERVER_TICKER);
    }

    /* 客户端每帧事件 */
    private void clientTick(Level level, BlockPos pos) {
    }

    public static int getCapacity(Block block) {
        return CAPACITY;
    }

    @Nullable
    private IFluidHandler getHandler(Direction d) {
        LazyOptional<IFluidHandler> fluidHandler = findFluidHandler(d);
        if (fluidHandler.isPresent() && fluidHandler.resolve().isPresent()) {
            IFluidHandler transferFrom = fluidHandler.resolve().get();
            for (int i = 0; i < transferFrom.getTanks(); i++) {
                FluidStack currentFluid = transferFrom.getFluidInTank(i);
                if (!currentFluid.isEmpty()) {
                    return transferFrom;
                }
            }
        }
        return null;
    }

    private void processRecipe_getValidRecipe() {

        IFluidHandler transferFrom = getHandler(Direction.UP);
        if (transferFrom != null) {
            for (int i = 0; i < transferFrom.getTanks(); i++) {
                FluidStack currentFluid = transferFrom.getFluidInTank(i);
                antiAlloyModule.setLevel(this.level);
                if (!currentFluid.isEmpty() && antiAlloyModule.hasRecipe(currentFluid)) {
                    AntiAlloyRecipe recipe = antiAlloyModule.getRecipe(currentFluid);
                    //检查配方产物的空间是否充足
                    if (recipe != null && this.tanks.fitAll(recipe.getOutputs())) {
                        //汲取合金，开始处理
                        transferFrom.drain(recipe.getInput(), FluidAction.SIMULATE);
                        //TinkersCentrifuge.LOGGER.info("[AntiAlloy] "+recipe.getInput().getDisplayName().getString()+" Recipe Start.");
                        //TinkersCentrifuge.LOGGER.info("[AntiAlloy] Drained"+recipe.getInput().getAmount()+"mb "+recipe.getInput().getDisplayName().getString()+".");
                        currentRecipe = recipe;
                        timer = (recipe.getInput().getAmount() * 20 + Config.DETACH_SPEED.get() - 1) / Config.DETACH_SPEED.get();
                        return;
                    }
                }
            }
        }
    }

    private void clean() {
        currentRecipe = null;
        timer = 0;
    }

    private void processRecipe_finish() {
        //TinkersCentrifuge.LOGGER.info("[AntiAlloy] "+currentRecipe.input.getDisplayName().getString()+" Recipe Complete.");
        //返还配方产物，即合金原料
        IFluidHandler transferFrom = getHandler(Direction.UP);
        FluidStack input = currentRecipe.getInput();
        if (transferFrom == null ||
                !transferFrom.drain(input, FluidAction.SIMULATE).equals(input) ||
                !this.tanks.fitAll(currentRecipe.getOutputs())
        ) {
            clean();
            return;
        }
        transferFrom.drain(input, FluidAction.EXECUTE);
        List<FluidStack> outputs = currentRecipe.getOutputs();
        for (FluidStack output : outputs) {
            if (output.isEmpty()) continue;
            int filled = tanks.fill(output, FluidAction.SIMULATE);
            if (filled > 0) {
                tanks.fill(output, FluidAction.EXECUTE);
            }
            //TinkersCentrifuge.LOGGER.info("[AntiAlloy] Filled "+filled+"mb "+output.getDisplayName().getString());
            //已经做足了容量检查，如果还是溢出了，那真没办法了，你浪费吧。
            //W.I.P. 做溢出保护
        }
        clean();
    }

    //比较器相关配置
    public void setLastStrength(int strength) {
        this.lastStrength = strength;
    }
    public int getLastStrength() {
        return lastStrength;
    }

    /* 服务器每帧事件 - 核心处理逻辑 */
    private void serverTick(Level level, BlockPos pos) {
        //配方获取逻辑
        if (currentRecipe == null) {
            processRecipe_getValidRecipe();
        }
        //计时器
        if (timer > 0) {
            timer--;
        }
        //配方处理完成
        if (currentRecipe != null && timer <= 0) {
            processRecipe_finish();
        }
        //尝试将输出槽的液体依次输出到下方的容器中
        if (pushOutTimer.tick()) { // don't do it every tick
            pushOutTimer.reset(() -> {
                AtomicBoolean status = new AtomicBoolean(false);
                findFluidHandler(Direction.DOWN).ifPresent(transferTarget -> {
                    if (!tanks.isEmpty()) {
                        for (int index = 0; index < tanks.getSize(); index++) {
                            FluidStack output = tanks.getFluidInTank(index);
                            if (output.isEmpty()) continue;
                            int filled = transferTarget.fill(output, FluidAction.SIMULATE);
                            if (filled > 0) {
                                transferTarget.fill(new FluidStack(output, filled), FluidAction.EXECUTE);
                                tanks.drain(new FluidStack(output, filled), FluidAction.EXECUTE);
                                status.set(true);
                            }
                        }
                    }
                });
                return status.get();
            });
        }
    }

    @Override
    public int comparatorStrength() {
        int total_power = 0;
        for (FluidStack fluid : tanks.getFluids()) {
            if(!fluid.isEmpty()){
                total_power += 2;
            }
        }
        return Math.max(total_power,15);
    }
    
    @Override
    public void updateFluidTo(FluidStack fluid) {
        // update tank fluid
        int oldAmount = tanks.getFluidAmount();
        int index = tanks.findTankIndex(fluid.getFluid());

        // 修复：不要清空整个容器，只增加流体或修改指定槽位
        if (index == -1) {
            if (!fluid.isEmpty()) {
                tanks.addFluid(fluid);
            }
        } else {
            tanks.setFluid(index, fluid);
        }
        tanks.sort();

        int newAmount = tanks.getFluidAmount();

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
        if (nbt.contains("tanks", Tag.TAG_LIST)) {
            tanks.readFromNBT(nbt);
        } else if (nbt.contains(NBTTags.TANK, Tag.TAG_COMPOUND)) {
            updateFluidTo(FluidStack.loadFluidStackFromNBT(nbt.getCompound(NBTTags.TANK)));
        } else {
            // Old format for backwards compatibility
            int i = 0;
            if (nbt.contains("tank" + i, Tag.TAG_COMPOUND)) {
                while (nbt.contains("tank" + i, Tag.TAG_COMPOUND)) {
                    CompoundTag tankTag = nbt.getCompound("tank" + i);
                    tanks.addFluid(FluidStack.loadFluidStackFromNBT(tankTag));
                    i++;
                }
            } else {
                tanks.setFluid(FluidStack.EMPTY);
            }
        }
        //CentrifugeBlockEntity.updateLight(this, tank);
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
                .with(ModelProperties.FLUID_STACK, new FluidStack(tanks.getFluid(), tanks.getFluidAmount()))
                .with(ModelProperties.TANK_CAPACITY, CAPACITY)
        .build();
    }

    @Override
    public void onTankContentsChanged() {
        ITankBlockEntity.super.onTankContentsChanged();
        this.setChanged();
        // 放弃原版匠魂的单流体同步策略，改为更新完整 NBT 发往客户端。
        if (this.level != null) {
            if (!this.level.isClientSide) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
            }
            this.requestModelDataUpdate();
        }
    }

    @Override
    public boolean shouldSyncOnUpdate() {
        return true;
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

    @Override
    public FluidTankAnimated getTank() {
        return tanks;
    }


    public class GraduallyTimer {
        private final int maxTicks;
        private final int perTicksOnFail;
        private int ticks = 0;
        private int currentMaxTicks = 0;

        public GraduallyTimer(int maxTicks, int perTicksOnFail) {
            this.maxTicks = maxTicks;
            this.perTicksOnFail = perTicksOnFail;
        }

        public GraduallyTimer(int maxTicks) {
            this.maxTicks = maxTicks;
            this.perTicksOnFail = 5;
        }

        public GraduallyTimer() {
            this.maxTicks = 20;
            this.perTicksOnFail = 5;
        }

        public boolean tick() {
            if (ticks < currentMaxTicks) {
                ticks++;
            } else {
                return true;
            }
            return false;
        }

        public void reset(boolean success) {
            ticks = 0;
            if (success) {
                currentMaxTicks = 0;
            } else {
                if (currentMaxTicks != maxTicks) {
                    currentMaxTicks += perTicksOnFail;
                    if (currentMaxTicks > maxTicks) {
                        currentMaxTicks = maxTicks;
                    }
                }
            }
        }

        public void reset(Callable<Boolean> task) {
            try {
                reset(task.call());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
