package slimegirl.centrifuge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.tconstruct.library.recipe.FluidValues;

public class AlloyTankBlock extends MultiFluidTankBlock {
    public static final int CAPACITY = FluidValues.INGOT * 81;

    public AlloyTankBlock(Properties properties) {
        super(properties, CAPACITY);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TinkersCentrifuge.TANK_BLOCK_ENTITY.get().create(pos, state);
    }
}
