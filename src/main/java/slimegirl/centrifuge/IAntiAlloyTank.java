package slimegirl.centrifuge;

import net.minecraftforge.fluids.FluidStack;
import slimeknights.mantle.recipe.container.IEmptyContainer;

public interface IAntiAlloyTank extends IEmptyContainer {
   int getTemperature();

   int getTanks();

   FluidStack getFluidInTank(int var1);

   boolean canFit(FluidStack var1, int var2);
}
