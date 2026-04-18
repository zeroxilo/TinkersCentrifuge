package slimegirl.centrifuge;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import slimeknights.mantle.block.InventoryBlock;
import slimeknights.tconstruct.library.utils.NBTTags;
import slimeknights.tconstruct.smeltery.block.entity.ITankBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity.ITankBlock;

import javax.annotation.Nullable;

public class CentrifugeBlock extends InventoryBlock implements ITankBlock, EntityBlock {
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);

    private static final VoxelShape SHAPE = Shapes.block();
    /*Shapes.join(
        Shapes.block(),
        Shapes.or(
            Block.box(0.0D, 0.0D, 5.0D, 16.0D, 2.0D, 11.0D),
            Block.box(5.0D, 0.0D, 0.0D, 11.0D, 2.0D, 16.0D),
            Block.box(2.0D, 4.0D, 2.0D, 14.0D, 16.0D, 14.0D)),
        BooleanOp.ONLY_FIRST);*/

    public CentrifugeBlock(Properties builder) {
        super(builder);
        registerDefaultState(defaultBlockState().setValue(LIGHT, 0));
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIGHT);
    }

    @Deprecated
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new CentrifugeBlockEntity(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> check) {
        return CentrifugeBlockEntity.getTicker(pLevel, check, TinkersCentrifuge.CENTRIFUGE_ENTITY.get());
    }

    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        CompoundTag nbt = stack.getTag();
        if (nbt != null && worldIn.getBlockEntity(pos) instanceof CentrifugeBlockEntity tank) {
            tank.updateTank(nbt.getCompound(NBTTags.TANK));
        }
        super.setPlacedBy(worldIn, pos, state, placer, stack);
    }

    @SuppressWarnings("deprecation")
    @Deprecated
    @Override
    public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        /*if (!worldIn.isClientSide() && worldIn.getBlockEntity(pos) instanceof CastingTankBlockEntity tank) {
            tank.handleRedstone(worldIn.hasNeighborSignal(pos));
        }*/
    }

    @SuppressWarnings("deprecation")
    @Deprecated
    @Override
    public void tick(BlockState state, ServerLevel worldIn, BlockPos pos, RandomSource rand) {
        /*if (!worldIn.isClientSide() && worldIn.getBlockEntity(pos) instanceof CastingTankBlockEntity tank) {
            tank.swap();
        }*/
    }
    
    /* Comparator support */

    @Deprecated
    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Deprecated
    @Override
    public int getAnalogOutputSignal(BlockState blockState, Level worldIn, BlockPos pos) {
        return ITankBlockEntity.getComparatorInputOverride(worldIn, pos);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter world, BlockPos pos, Player player) {
        ItemStack stack = new ItemStack(this);
        /*BlockEntityHelper.get(CentrifugeBlockEntity.class, world, pos).ifPresent(te -> te.setTankTag(stack));*/
        return stack;
    }

    @Override
    public int getCapacity() {
        return CentrifugeBlockEntity.DEFAULT_CAPACITY;
    }
}