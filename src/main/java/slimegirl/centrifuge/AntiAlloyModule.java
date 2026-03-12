package slimegirl.centrifuge;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import slimeknights.mantle.recipe.helper.RecipeHelper;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipe;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

public class AntiAlloyModule {

    @Nullable
    private static List<ResourceLocation> allLocations;
    private static List<AlloyRecipe> allRecipes;
    private static List<Fluid> allAlloys;

    //获取所有合金配方
    public static void initAllRecipes() {
        if (allRecipes == null) {
            Level level = Minecraft.getInstance().level;
            if (level == null){
                return;
            }
            RegistryAccess access = level.registryAccess();
            RecipeManager manager = level.getRecipeManager();
            ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
            List<AlloyRecipe> alloyRecipes = manager.getAllRecipesFor(TinkerRecipeTypes.ALLOYING.get());
            //获取所有合金配方的路径
            allLocations = alloyRecipes.stream().map(recipe -> recipe.getId()).toList();

            allRecipes = new ArrayList<>();
            //getLevel().getRecipeManager().getAllRecipesFor(TinkerRecipeTypes.ALLOYING.get());
            TinkersCentrifuge.LOGGER.info("[AntiAlloy] Loaded "+alloyRecipes.size()+" alloy recipes.");
            for(ResourceLocation location : allLocations){
                location.getPath();
                resourceManager.getResource(location).ifPresent(resource -> {
                    try {
                        String anti_recipe = GsonHelper.parse(resource.openAsReader()).toString();
                        TinkersCentrifuge.LOGGER.info("[AntiAlloy] - "+anti_recipe);
                    } catch (Exception e) {
                        TinkersCentrifuge.LOGGER.error("[AntiAlloy] Failed to load recipe from " + location, e);
                    }
                });
                //TinkersCentrifuge.LOGGER.info("[AntiAlloy] - "+serializer.fromJson(location.toString()).toString());
                //TinkersCentrifuge.LOGGER.info("[AntiAlloy] => "+recipe.getOutput().getDisplayName().getString());
            }
            allAlloys = new ArrayList<>();
            //检查配方对象，并列入可用合金列表
            for (AlloyRecipe recipe : allRecipes) {
                if (recipe != null && recipe.getOutput() != null) {
                    allAlloys.add(recipe.getOutput().getFluid());
                }
            }
        }
    }

    //检查是否存在配方
    public static boolean hasRecipe(Fluid alloy) {
        if (allRecipes == null) {
            initAllRecipes();
        }
        if(allRecipes != null && allAlloys.contains(alloy)){
            return true;
        }
        return false;
    }

    //返回首个符合的配方
    public static AlloyRecipe getRecipe(FluidStack alloyStack) {
        Fluid alloy = alloyStack.getFluid();
        if(!hasRecipe(alloy)){
            return null;
        }
        int amount = alloyStack.getAmount();
        for (AlloyRecipe recipe : allRecipes) {
            if (recipe != null && recipe.getOutput().getFluid().equals(alloy)) {
                if (recipe.getOutput().getAmount() >= amount) {
                    return recipe;
                }
            }
        }
        return null;
    }
}
