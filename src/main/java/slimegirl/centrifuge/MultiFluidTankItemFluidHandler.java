package slimegirl.centrifuge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import slimeknights.tconstruct.library.utils.NBTTags;
import slimeknights.tconstruct.smeltery.item.TankItem;
import slimeknights.tconstruct.smeltery.item.TankItemFluidHandler;

import java.util.ArrayList;
import java.util.List;

public class MultiFluidTankItemFluidHandler extends TankItemFluidHandler {
    private final TankItem TANKITEM;
    private final ItemStack STACK;

    public MultiFluidTankItemFluidHandler(TankItem tankItem, ItemStack container) {
        super(tankItem, container);
        TANKITEM = tankItem;
        STACK = container;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        // TODO: 抽象调用，更多检测，或者直接删除这个类
        CompoundTag nbt = STACK.getOrCreateTag();
        List<FluidStack> fluids = new ArrayList<>();
        if (nbt.contains("tanks", Tag.TAG_LIST)) {
            ListTag tankList = nbt.getList("tanks", Tag.TAG_COMPOUND);
            for (int i = 0; i < tankList.size(); i++) {
                CompoundTag tankTag = tankList.getCompound(i);
                fluids.add(FluidStack.loadFluidStackFromNBT(tankTag));
            }
        }
        if (nbt.contains(NBTTags.TANK, Tag.TAG_COMPOUND)) {
            fluids.add(0, FluidStack.loadFluidStackFromNBT(nbt.getCompound(NBTTags.TANK)));
        }
        int space = AlloyTankBlock.CAPACITY - fluids.stream().mapToInt(FluidStack::getAmount).sum();
        if (space > 0) {
            return super.fill(resource, action);
        }
        return 0;
    }

    @Override
    public @NonNull FluidStack drain(int maxDrain, FluidAction action) {
        // TODO: 搬迁mixin的逻辑到这里
        return super.drain(maxDrain, action);
    }
}
