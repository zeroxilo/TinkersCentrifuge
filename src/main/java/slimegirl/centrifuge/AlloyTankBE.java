package slimegirl.centrifuge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity;

public class AlloyTankBE extends TankBlockEntity {
    public AlloyTankBE(BlockPos pos, BlockState state) {
        super(TinkersCentrifuge.TANK_BLOCK_ENTITY.get(), pos, state, (ITankBlock)state.getBlock());
    }
}
