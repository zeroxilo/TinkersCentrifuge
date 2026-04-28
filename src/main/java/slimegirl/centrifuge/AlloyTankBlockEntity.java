package slimegirl.centrifuge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;


public class AlloyTankBlockEntity extends MultiFluidTankBlockEntify {
    public AlloyTankBlockEntity(BlockPos pos, BlockState state) {
        super(TinkersCentrifuge.TANK_BLOCK_ENTITY.get(), pos, state, AlloyTankBlock.CAPACITY);
    }
}
