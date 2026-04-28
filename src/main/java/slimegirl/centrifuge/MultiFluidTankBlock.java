package slimegirl.centrifuge;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock;

import javax.annotation.Nullable;
import java.util.List;

public class MultiFluidTankBlock extends SearedTankBlock {
    private final int CAPACITY;

    public MultiFluidTankBlock(Properties properties, int capacity) {
        super(properties, capacity, PushReaction.BLOCK);
        CAPACITY = capacity;
    }

    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        CompoundTag nbt = stack.getTag();
        if (nbt != null && worldIn.getBlockEntity(pos) instanceof MultiFluidTankBlockEntify tank) {
            tank.updateTank(nbt);
        }
        super.setPlacedBy(worldIn, pos, state, placer, stack);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter world, BlockPos pos, Player player) {
        ItemStack stack = new ItemStack(this);
        BlockEntityHelper.get(MultiFluidTankBlockEntify.class, world, pos).ifPresent(
                te -> stack.setTag(te.tanks.writeToNBT(new CompoundTag()))
        );
        return stack;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return super.getDrops(state, params);
    }

    @Override
    public int getCapacity() {
        return CAPACITY;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        throw new RuntimeException("No Overrideed BlockEntity");
    }
}
