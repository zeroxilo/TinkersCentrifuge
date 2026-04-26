package slimegirl.centrifuge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock;

public class AlloyTankBlock extends SearedTankBlock {
    public AlloyTankBlock(Properties properties, int capacity, PushReaction reaction) {
        super(properties, capacity, reaction);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TinkersCentrifuge.TANK_BLOCK_ENTITY.get().create(pos, state);
    }
}
