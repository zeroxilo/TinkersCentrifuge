package slimegirl.centrifuge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.library.recipe.FluidValues;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CentrifugeBlockEntity extends MultiFluidTankBlockEntify {
    public static final int CAPACITY = FluidValues.INGOT * 48;
    protected final AntiAlloyModule antiAlloyModule;
    protected AntiAlloyRecipe currentRecipe;
    protected int timer = 0;
    protected Util.GraduallyTimer pushOutTimer = new Util.GraduallyTimer();


    /** Last redstone state of the block */
    private boolean lastRedstone = false;
    /** Last comparator strength to reduce block updates */
    private int lastStrength = -1;

    public static final BlockEntityTicker<CentrifugeBlockEntity> SERVER_TICKER = (level, pos, state, self) -> self.serverTick(level, pos);
    /** Handles ticking on the clientside */
    public static final BlockEntityTicker<CentrifugeBlockEntity> CLIENT_TICKER = (level, pos, state, self) -> self.clientTick(level, pos);

    
    private static final String TAG_REDSTONE = "redstone";
    

    public CentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(TinkersCentrifuge.CENTRIFUGE_ENTITY.get(), pos, state, CAPACITY, false);
        antiAlloyModule = new AntiAlloyModule(this.getLevel());
    }



    @Nullable
    public static <CAST extends CentrifugeBlockEntity, RET extends BlockEntity> BlockEntityTicker<RET> getTicker(Level level, BlockEntityType<RET> check, BlockEntityType<CAST> casting) {
        return BlockEntityHelper.castTicker(check, casting, level.isClientSide ? CLIENT_TICKER : SERVER_TICKER);
    }

    /* 客户端每帧事件 */
    private void clientTick(Level level, BlockPos pos) {
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
            int filled = tanks.fill2(output, FluidAction.SIMULATE);
            if (filled > 0) {
                tanks.fill2(output, FluidAction.EXECUTE);
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


    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        lastRedstone = tag.getBoolean(TAG_REDSTONE);
    }

    @Override
    public void saveAdditional(CompoundTag tags) {
        super.saveAdditional(tags);
        tags.putBoolean(TAG_REDSTONE, lastRedstone);
    }


}
