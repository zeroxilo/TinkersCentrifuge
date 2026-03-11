package slimegirl.centrifuge;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.mantle.fluid.FluidTransferHelper;
import slimeknights.mantle.fluid.transfer.FluidContainerTransferManager;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferResult;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.library.client.model.ModelProperties;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;
import slimeknights.tconstruct.library.utils.NBTTags;
import slimeknights.tconstruct.shared.block.entity.TableBlockEntity;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity.ITankBlock;
import slimeknights.tconstruct.smeltery.block.entity.ITankBlockEntity;
import slimeknights.tconstruct.smeltery.item.TankItem;
import slimegirl.centrifuge.TinkersCentrifuge;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CentrifugeBlockEntity extends TableBlockEntity implements ITankBlockEntity, WorldlyContainer {
    public static final int INPUT = 0;
    public static final int OUTPUT = 1;

    public static final int DEFAULT_CAPACITY = FluidType.BUCKET_VOLUME * 4;
    public static final int TANK_CAPACITY = FluidType.BUCKET_VOLUME * 4;

    private static final Component NAME = TConstruct.makeTranslation("gui", "casting");

    /** Internal fluid tank instance */
    protected final FluidTankAnimated tank;
    /** Capability holder for the tank */
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
        this(pos, state, state.getBlock() instanceof ITankBlock tankBlock ? tankBlock : null);
    }

    /** Main constructor */
    public CentrifugeBlockEntity(BlockPos pos, BlockState state, ITankBlock block) {
        this(TinkersCentrifuge.CENTRIFUGE_ENTITY.get(), pos, state, block);
    }

    /** Extendable constructor */
    @SuppressWarnings("WeakerAccess")
    protected CentrifugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, ITankBlock block) {
        super(type, pos, state, NAME, 2, 1);
        tank = new FluidTankAnimated(block.getCapacity(), this);
        fluidHolder = LazyOptional.of(() -> tank);
        itemHandler = new SidedInvWrapper(this, Direction.DOWN);
    }

    @Nullable
    public static <CAST extends CentrifugeBlockEntity, RET extends BlockEntity> BlockEntityTicker<RET> getTicker(Level level, BlockEntityType<RET> check, BlockEntityType<CAST> casting) {
        return BlockEntityHelper.castTicker(check, casting, level.isClientSide ? CLIENT_TICKER : SERVER_TICKER);
    }

    public static int getCapacity(Item item) {
        return DEFAULT_CAPACITY;
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
    public void setItem(int slot, ItemStack newStack) {
    }
    private void setInputItem(ItemStack stack) {
        super.setItem(INPUT, stack);
    }

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
        return index == INPUT && !isStackInSlot(OUTPUT);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return index == OUTPUT;
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) {
            return fluidHolder.cast();
        }
        return super.getCapability(capability, facing);
    }

    public void updateTank(CompoundTag nbt) {
        if (nbt.isEmpty()) {
            tank.setFluid(FluidStack.EMPTY);
        } else {
            tank.readFromNBT(nbt);
            //CentrifugeBlockEntity.updateLight(this, tank);
        }
    }

    /** Handles cooling the casting recipe */
    private void serverTick(Level level, BlockPos pos) {
        /*// no recipe
        // TODO: should consider the case where the tank has fluid, but there is no current recipe
        // would like to avoid doing a recipe lookup every tick, so need some way to handle the case of no recipe found, ideally without fluid voiding
        if (currentRecipe == null) {
            return;
        }
        // fully filled
        FluidStack currentFluid = tank.getFluid();
        if (coolingTime >= 0) {
            timer++;
            if (timer >= coolingTime) {
                if (!currentRecipe.matches(castingInventory, level)) {
                // if lost our recipe or the recipe needs more fluid then we have, we are done
                // will come around later for the proper fluid amount
                currentRecipe = findCastingRecipe();
                recipeName = null;
                if (currentRecipe == null || currentRecipe.getFluidAmount(castingInventory) > currentFluid.getAmount()) {
                    timer = 0;
                    updateAnalogSignal();
                    // TODO: client does not get updated if this happens
                    return;
                }
            }

            // actual recipe result
            boolean consumed = currentRecipe.isConsumed(castingInventory);
            ItemStack output = currentRecipe.assemble(castingInventory, level.registryAccess());
            if (currentRecipe.switchSlots() != lastRedstone) {
                if (!consumed) {
                    setItem(OUTPUT, getItem(INPUT));
                }
                    setItem(INPUT, output);
                } else {
                    if (consumed) {
                        setItem(INPUT, ItemStack.EMPTY);
                    }
                    setItem(OUTPUT, output);
                }
                // if redstone swapped behavior, add a click sound
                if (lastRedstone) {
                    level.playSound(null, getBlockPos(), Sounds.CASTING_CLICKS.getSound(), SoundSource.BLOCKS, 1.0f, 1.0f);
                }
                level.playSound(null, pos, Sounds.CASTING_COOLS.getSound(), SoundSource.BLOCKS, 0.5f, 4f);
                reset();
            } else {
                updateAnalogSignal();
            }
        }*/
    }

    /** Handles animating the recipe */
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
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidHolder.invalidate();
    }

    @Nonnull
    @Override
    public ModelData getModelData() {
        return null;
        //return ModelData.builder();
        //.with(ModelProperties.FLUID_STACK, tank.getFluid())
        //.with(ModelProperties.TANK_CAPACITY, tank.getCapacity()).build();
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
        tank.setCapacity(getCapacity(getBlockState().getBlock()));
        updateTank(tag.getCompound(NBTTags.TANK));
        lastRedstone = tag.getBoolean(TAG_REDSTONE);
        super.load(tag);
    }

    @Override
    public void saveAdditional(CompoundTag tags) {
        super.saveAdditional(tags);
        tags.putBoolean(TAG_REDSTONE, lastRedstone);
    }

    @Override
    public void saveSynced(CompoundTag tag) {
        super.saveSynced(tag);
        // want tank on the client on world load
        if (!tank.isEmpty()) {
            tag.put(NBTTags.TANK, tank.writeToNBT(new CompoundTag()));
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return null;
    }
}
