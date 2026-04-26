package slimegirl.centrifuge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock;

public class AlloyTankBlock extends SearedTankBlock {
    public static final int CAPACITY = FluidValues.INGOT * 81;
    public AlloyTankBlock(Properties properties, PushReaction reaction) {
        super(properties, CAPACITY, reaction);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TinkersCentrifuge.TANK_BLOCK_ENTITY.get().create(pos, state);
    }
}
