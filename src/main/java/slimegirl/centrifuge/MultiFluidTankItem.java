package slimegirl.centrifuge;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.checkerframework.checker.nullness.qual.Nullable;
import slimeknights.tconstruct.smeltery.item.TankItem;

import java.util.List;

public class MultiFluidTankItem extends TankItem {
    public MultiFluidTankItem(Block blockIn, Properties builder, boolean limitStackSize) {
        super(blockIn, builder, limitStackSize);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, worldIn, tooltip, flag);
        if (stack.hasTag()) {
            if (stack.getTag().contains("tanks", Tag.TAG_LIST)) {
                tooltip.add(Component.translatable("tooltip.tinkerscentrifuge.more_fluids", stack.getTag().getList("tanks", Tag.TAG_COMPOUND).size())
                        .withStyle(ChatFormatting.GRAY));
            }
        }

    }

    @Nullable
    public ICapabilityProvider initCapabilities(ItemStack stack, @javax.annotation.Nullable CompoundTag nbt) {
        return new MultiFluidTankItemFluidHandler(this, stack);
    }
}
