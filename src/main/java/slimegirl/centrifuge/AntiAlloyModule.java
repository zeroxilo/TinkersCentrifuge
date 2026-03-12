package slimegirl.centrifuge;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipe;
import slimeknights.tconstruct.library.recipe.alloying.IAlloyTank;

public class AntiAlloyModule {
    @Nullable
    private final MantleBlockEntity parent = null;

    @Nullable
    private List<AlloyRecipe> allRecipes;
    private List<Fluid> allAlloys;
    private Level getLevel() {
        return Objects.requireNonNull(parent.getLevel(), "Parent tile entity has null world");
    }
    //获取所有合金配方
    public List<AlloyRecipe> getAllRecipes() {
        if (allRecipes == null) {
            allRecipes = getLevel().getRecipeManager().getAllRecipesFor(TinkerRecipeTypes.ALLOYING.get());
            //检查配方对象，并列入可用合金列表
            for (AlloyRecipe recipe : getAllRecipes()) {
                if (recipe != null && recipe.getOutput() != null) {
                    allAlloys.add(recipe.getOutput().getFluid());
                }
            }
        }
        return allRecipes;
    }

    //检查是否存在配方
    public boolean hasRecipe(Fluid alloy) {
        if(allAlloys.contains(alloy)){
            return true;
        }
        return false;
    }

    //返回首个符合的配方
    public AlloyRecipe getRecipe(FluidStack alloyStack) {
        Fluid alloy = alloyStack.getFluid();
        if(!hasRecipe(alloy)){
            return null;
        }
        int amount = alloyStack.getAmount();
        for (AlloyRecipe recipe : getAllRecipes()) {
            if (recipe != null && recipe.getOutput().getFluid() == alloy) {
                if (recipe.getOutput().getAmount() >= amount) {
                    return recipe;
                }
            }
        }
        return null;
    }
}
