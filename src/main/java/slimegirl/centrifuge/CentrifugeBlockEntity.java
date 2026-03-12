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
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.mantle.fluid.FluidTransferHelper;
import slimeknights.mantle.fluid.transfer.FluidContainerTransferManager;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferResult;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipe;
import slimeknights.tconstruct.library.utils.NBTTags;
import slimeknights.tconstruct.shared.block.entity.TableBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.ITankBlockEntity;

import javax.annotation.Nullable;

public class CentrifugeBlockEntity extends TableBlockEntity implements ITankBlockEntity, WorldlyContainer {
    public static final int INPUT = 0;
    public static final int OUTPUT = 1;

    public static final int DEFAULT_CAPACITY = FluidType.BUCKET_VOLUME * 4;
    public static final int TANK_CAPACITY = FluidType.BUCKET_VOLUME * 4;

    private static final Component NAME = TConstruct.makeTranslation("gui", "casting");

    protected final AntiAlloyModule alloyModule;
    protected AlloyRecipe currentRecipe;
    protected int timer = 0;
    protected final FluidTankAnimated tank;
    protected final List<FluidTankAnimated> outputTanks;
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
        alloyModule = new AntiAlloyModule();
        tank = new FluidTankAnimated(DEFAULT_CAPACITY, this);
        outputTanks = List.of(new FluidTankAnimated(DEFAULT_CAPACITY, this),
            new FluidTankAnimated(DEFAULT_CAPACITY, this),
            new FluidTankAnimated(DEFAULT_CAPACITY, this),
            new FluidTankAnimated(DEFAULT_CAPACITY, this));
        fluidHolder = LazyOptional.of(() -> tank);
        //itemHandler = new SidedInvWrapper(this, Direction.DOWN);
    }

    @Nullable
    public static <CAST extends CentrifugeBlockEntity, RET extends BlockEntity> BlockEntityTicker<RET> getTicker(Level level, BlockEntityType<RET> check, BlockEntityType<CAST> casting) {
        return BlockEntityHelper.castTicker(check, casting, level.isClientSide ? CLIENT_TICKER : SERVER_TICKER);
    }

    /* 客户端每帧事件 */
    private void clientTick(Level level, BlockPos pos) {
        /*if (currentRecipe == null) {
            return;
        }
        // fully filled
        FluidStack currentFluid = tank.getFluid();
        if (currentFluid.getAmount() >= tank.getCapacity() && !currentFluid.isEmpty()) {
            timer++;
            if (level.random.nextFloat() > 0.9f) {
                level.addParticle(ParticleTypes.SMOKE, pos.getX() + level.random.nextDouble(), pos.getY() + 1.1d, pos.getZ() + level.random.nextDouble(), 0.0D, 0.0D, 0.0D);
            }
        }*/
    }
    

    /* 服务器每帧事件 - 核心处理逻辑 */
    private void serverTick(Level level, BlockPos pos) {
        if (currentRecipe == null) {
            FluidStack currentFluid = tank.getFluid();
            //TinkersCentrifuge.LOGGER.info("[AntiAlloy]Check Recipe of "+currentFluid.getDisplayName().getString());
            if (!currentFluid.isEmpty()) {
                AlloyRecipe recipe = alloyModule.getRecipe(currentFluid);
                if (recipe != null) {
                    TinkersCentrifuge.LOGGER.info("[AntiAlloy]Found Recipe of "+currentFluid.getDisplayName().getString());
                    currentRecipe = recipe;
                    timer = 19;//20帧
                }
            }
        }else if(timer > 0){
            timer--;
            if (timer == 0) {
                //处理完成，输出合金原材料
                FluidStack alloy = currentRecipe.getOutput();
                tank.drain(alloy, IFluidHandler.FluidAction.EXECUTE);
                List<Ingredient> outputss = currentRecipe.getIngredients();
                for(Ingredient output : outputss){
                    TinkersCentrifuge.LOGGER.info("[AntiAlloy]Try to output "+output.toString());
                }
                /*
                FluidStack output = null;
                for(List<FluidStack> outputs : outputss){
                    output = outputs.get(0);
                    LOGGER.info("[AntiAlloy]Try to output "+output.getDisplayName().getString());
                    if(output.isEmpty()){
                        continue;
                    }
                    for(FluidTankAnimated outputTank : outputTanks){
                        if(outputTank.fill(output, IFluidHandler.FluidAction.SIMULATE) == output.getAmount()){
                            outputTank.fill(output, IFluidHandler.FluidAction.EXECUTE);
                            break;
                        }
                    }
                }*/
                currentRecipe = null;
            }
        }
        //尝试将输出槽的液体依次输出到下方的容器中
        for(FluidTankAnimated outputTank : outputTanks){
            if(!outputTank.isEmpty()){
                FluidStack output = outputTank.getFluid();
                TinkersCentrifuge.LOGGER.info("[AntiAlloy]Try to transfer "+output.getDisplayName().getString());
                BlockPos targetPos = pos.below();
                if(level.getBlockEntity(targetPos) instanceof IFluidHandler targetHandler){
                    int filled = targetHandler.fill(output, IFluidHandler.FluidAction.EXECUTE);
                    if(filled > 0){
                        outputTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                    }
                }
            }
        }
    }

    public static int getCapacity(Block block) {
        return DEFAULT_CAPACITY;
    }
    public void setLastStrength(int strength) {
        this.lastStrength = strength;
    }
    public int getLastStrength() {
        return lastStrength;
    }
    public FluidTankAnimated getTank() {
        return tank;
    }

    @Override
    public void setItem(int slot, ItemStack newStack) {}
    @Override
    public boolean canPlaceItem(int pIndex, ItemStack pStack) {
        return false;
    }
    
    @Override
    @Nonnull
    public int[] getSlotsForFace(Direction side) {
        return new int[]{INPUT, OUTPUT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack itemStackIn, @Nullable Direction direction) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return false;
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) {
            return fluidHolder.cast();
        }
        return super.getCapability(capability, facing);
    }

    //从nbt数据中获取流体信息并更新
    public void updateTank(CompoundTag nbt) {
        if (nbt.isEmpty()) {
            tank.setFluid(FluidStack.EMPTY);
            for (FluidTankAnimated outputTank : outputTanks) {
                outputTank.setFluid(FluidStack.EMPTY);
            }
        } else {
            tank.readFromNBT(nbt);
            if(!nbt.contains("outputs") || nbt.getCompound("outputs").isEmpty()){
                for (FluidTankAnimated outputTank : outputTanks) {
                    outputTank.setFluid(FluidStack.EMPTY);
                }
            }else{
                for(int i = 0; i < outputTanks.size(); i++){
                    if(nbt.getCompound("outputs").contains("output" + i) && !nbt.getCompound("outputs").getCompound("output" + i).isEmpty()){
                        CompoundTag outputTag = nbt.getCompound("outputs").getCompound("output" + i);
                        outputTanks.get(i).readFromNBT(outputTag);
                    }else{
                        outputTanks.get(i).setFluid(FluidStack.EMPTY);
                    }
                }
            }
            //CentrifugeBlockEntity.updateLight(this, tank);
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
        //.with(ModelProperties.FLUID_STACK, tank.getFluid())
        //.with(ModelProperties.TANK_CAPACITY, tank.getCapacity())
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
        updateTank(tag.getCompound(NBTTags.TANK));
        lastRedstone = tag.getBoolean(TAG_REDSTONE);
        super.load(tag);
    }

    @Override
    public void saveAdditional(CompoundTag tags) {
        super.saveAdditional(tags);
        tags.putBoolean(TAG_REDSTONE, lastRedstone);
    }

    //保存同步数据，客户端加载时会调用load方法
    @Override
    public void saveSynced(CompoundTag tag) {
        super.saveSynced(tag);
        // want tank on the client on world load
        if (!tank.isEmpty()) {
            tag.put(NBTTags.TANK, tank.writeToNBT(new CompoundTag()));
        }
        boolean hasOutput = false;
        for(FluidTankAnimated outputTank : outputTanks){
            if(!outputTank.isEmpty()){
                if(!hasOutput){
                    tag.put(NBTTags.TANK + ".outputs", new CompoundTag());
                }
                hasOutput = true;
                tag.put(NBTTags.TANK + ".outputs.output" + outputTanks.indexOf(outputTank),outputTank.writeToNBT(new CompoundTag()));
                break;
            }
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return null;
    }
}
