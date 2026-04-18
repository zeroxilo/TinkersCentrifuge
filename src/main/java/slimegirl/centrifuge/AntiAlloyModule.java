package slimegirl.centrifuge;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipe;

import java.util.ArrayList;
import java.util.List;

public class AntiAlloyModule {
    public static final List<Fluid> ALL_ALLOYS;
    public static final List<Fluid> BLACKLIST_ALLOYS;
    public static final List<AntiAlloyRecipe> RECIPES;
    public static ResourceManager resourceManager;
    private static boolean isLoaded = false;
    public Level level;
    public MinecraftServer server;
    
    static {
        ALL_ALLOYS = new ArrayList<Fluid>();
        BLACKLIST_ALLOYS = new ArrayList<Fluid>();
        RECIPES = new ArrayList<AntiAlloyRecipe>();
        resourceManager = null;
    }

    public static void clearCache() {
        ALL_ALLOYS.clear();
        BLACKLIST_ALLOYS.clear();
        RECIPES.clear();
        isLoaded = false;
        resourceManager = null;
    }

    public AntiAlloyModule(Level level) {
        this.level = level;
        if(level != null){
            this.server = level.getServer();
            if (server != null && resourceManager == null) {
                resourceManager = server.getResourceManager();
            }
        }
    }


    //获取所有合金配方
    public void setLevel(Level level) {
        this.level = level;
        if(level != null){
            this.server = level.getServer();
            //资源管理器是静态类，但必须得靠这个方法才能在服务端环境里正确获取
            if (server != null && resourceManager == null) {
                resourceManager = server.getResourceManager();
            }
        }
    }
    //自动制作对应的反合金配方
    //每种合金在已有专门的反合金配方的情况下不会再注册一个
    public void initRecipes() {
        if (level != null && resourceManager != null) {
            // 从配方管理器那边获取所有反合金配方
            RecipeManager recipeManager = level.getRecipeManager();
            List<AntiAlloyRecipe> antiAlloyRecipes = recipeManager.getAllRecipesFor(TinkersCentrifuge.ANTI_ALLOYING.get());
            for (AntiAlloyRecipe recipe : antiAlloyRecipes) {
                Fluid recipeFluid = recipe.getInput().getFluid();
                RECIPES.add(recipe);
                if(!ALL_ALLOYS.contains(recipeFluid)){
                    ALL_ALLOYS.add(recipeFluid);
                }
            }
            // 从配方管理器那边获取所有合金配方
            List<AlloyRecipe> alloyRecipes = recipeManager.getAllRecipesFor(TinkerRecipeTypes.ALLOYING.get());
            if (alloyRecipes.isEmpty()){return;}
            // 获取所有配方
            for (AlloyRecipe recipe : alloyRecipes) {
                Fluid recipeFluid = recipe.getOutput().getFluid();
                if(!ALL_ALLOYS.contains(recipeFluid)){
                    AntiAlloyRecipe new_recipe = new AntiAlloyRecipe(recipe,resourceManager);
                    RECIPES.add(new_recipe); //暂时先这么写，未来有机会改成配方注册制
                    ALL_ALLOYS.add(recipeFluid);
                }
            }
            isLoaded = true;
        }
    }

    //检查是否存在配方
    public boolean hasRecipe(Fluid alloy) {
        if (!isLoaded) {initRecipes();}
        if(ALL_ALLOYS.contains(alloy)){
            return true;
        }
        return false;
    }
    
    public boolean hasRecipe(FluidStack alloy) {
        return hasRecipe(alloy.getFluid());
    }

    //返回首个符合的配方
    public AntiAlloyRecipe getRecipe(FluidStack alloyStack) {
        if (!isLoaded) {initRecipes();}
        Fluid alloy = alloyStack.getFluid(); //目前已有合金
        if(!hasRecipe(alloy)){return null;}
        int amount = alloyStack.getAmount(); //目前已有流体量
        for (AntiAlloyRecipe recipe : RECIPES) {
            if (recipe != null && recipe.getInput().getFluid().equals(alloy)) {
                if (recipe.getInput().getAmount() <= amount) {
                    return recipe;
                }
            }
        }
        return null;
    }
}
