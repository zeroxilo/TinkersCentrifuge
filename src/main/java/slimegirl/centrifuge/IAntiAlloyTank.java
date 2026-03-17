package slimegirl.centrifuge;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import slimeknights.mantle.recipe.container.IEmptyContainer;
import slimeknights.mantle.recipe.container.IRecipeContainer;

public interface IAntiAlloyTank extends IEmptyContainer {
   int getTemperature();

   int getTanks();

   FluidStack getFluidInTank(int var1);

   boolean canFit(FluidStack var1, int var2);
}
