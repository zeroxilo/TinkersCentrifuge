package slimegirl.centrifuge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.library.client.SafeClient;
import slimeknights.tconstruct.library.client.model.ModelProperties;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;
import slimeknights.tconstruct.library.utils.NBTTags;
import slimeknights.tconstruct.smeltery.block.entity.ITankBlockEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MultiFluidTankBlockEntify extends MantleBlockEntity implements ITankBlockEntity {
    protected final int CAPACITY;
    protected final MultiFluidTank tanks;
    protected final LazyOptional<IFluidHandler> fluidHolder;

    /* 内容初始化 */
    @SuppressWarnings("WeakerAccess")
    protected MultiFluidTankBlockEntify(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, boolean noFill) {
        super(type, pos, state);
        CAPACITY = capacity;
        tanks = noFill ? new MultiFluidTank(capacity, this) : new MultiFluidTank2(capacity, this);
        fluidHolder = LazyOptional.of(() -> tanks);
    }

    @SuppressWarnings("WeakerAccess")
    protected MultiFluidTankBlockEntify(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        this(type, pos, state, capacity, false);
    }

    @Override
    public int getLastStrength() {
        return 0;
    }

    @Override
    public void setLastStrength(int i) {

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
        super.load(tag);
    }

    @Override
    public FluidTankAnimated getTank() {
        return tanks;
    }

    private static class MultiFluidTank2 extends MultiFluidTank {
        public MultiFluidTank2(int capacity, MantleBlockEntity parent) {
            super(capacity, parent);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }
    }
}
