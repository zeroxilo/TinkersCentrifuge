package slimegirl.centrifuge.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimegirl.centrifuge.MultiFluidTankItem;
import slimeknights.tconstruct.smeltery.item.TankItem;

@Mixin(TankItem.class)
public class TankItemMixin {
    @Inject(method = "isFilled", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onIsFilled(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.getItem() instanceof MultiFluidTankItem) {
            boolean filled = false;
            CompoundTag nbt = stack.getTag();
            if (nbt != null) {
                if (nbt.contains("tank", Tag.TAG_COMPOUND)) {
                    filled = true;
                } else {
                    if (nbt.contains("tanks", Tag.TAG_LIST)) {
                        ListTag listTag = nbt.getList("tanks", Tag.TAG_COMPOUND);
                        if (!listTag.isEmpty()) {
                            filled = true;
                            nbt.put("tank", listTag.get(0));
                            listTag.remove(0);
                            if (listTag.isEmpty()) {
                                nbt.remove("tanks");
                            } else {
                                nbt.put("tanks", listTag);
                            }
                            if (!nbt.isEmpty()) {
                                stack.setTag(nbt);
                            }
                        }
                    }
                }
            }
            cir.setReturnValue(filled); // 强制返回结果并拦截原方法
        }
    }
}
