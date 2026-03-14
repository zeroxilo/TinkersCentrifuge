package slimegirl.centrifuge;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipe;

public class AntiAlloyModule {
    public boolean isLoaded = false;
    public Level level;
    public MinecraftServer server;
    public static ResourceManager resourceManager;

    public AntiAlloyModule(Level level) {
        this.level = level;
        if(level != null){
            this.server = level.getServer();
            if (server != null && resourceManager == null) {
                resourceManager = server.getResourceManager();
            }
        }
    }

    //内部自用的反合金配方逻辑
    public class AntiAlloyRecipe{
        public final AlloyRecipe alloyRecipe;
        public final FluidStack input;
        public final List<FluidStack> outputs;

        public AntiAlloyRecipe(AlloyRecipe alloyRecipe) {
            this.alloyRecipe = alloyRecipe;
            this.input = new FluidStack (alloyRecipe.getOutput().getFluid(), alloyRecipe.getOutput().getAmount() / 10);
            this.input.setAmount(alloyRecipe.getOutput().getAmount() / 10);
            this.outputs = new ArrayList<>();
            //尝试根据原有配方构造输入输出
            if (AntiAlloyModule.resourceManager != null) {
                try {
                    ResourceLocation recipePath = new ResourceLocation(
                        alloyRecipe.getId().getNamespace(),
                        "recipes/" + alloyRecipe.getId().getPath() + ".json"
                    );
                    //获取Json
                    Resource resource = AntiAlloyModule.resourceManager.getResource(recipePath).orElse(null);
                    if (resource == null){
                        TinkersCentrifuge.LOGGER.error("[AntiAlloy] Failed to find resource for recipe: " + alloyRecipe.getId());
                        return;
                    }
                    //解析配方
                    Reader reader = new InputStreamReader(resource.open());
                    JsonObject json = GsonHelper.parse(reader).getAsJsonObject();
                    reader.close();
                    if (json == null) {
                        TinkersCentrifuge.LOGGER.error("[AntiAlloy] Failed to parse JSON for recipe: " + alloyRecipe.getId());
                        return;
                    }
                    
                    //构建输出流体
                    String recipe_type = GsonHelper.getAsString(json, "type", "unknown");
                    if(recipe_type.equals("tconstruct:alloy")){ //普通合金配方
                        fitToOutputs(JsonHelper.parseList(json, "inputs", FluidIngredient::deserialize));
                        TinkersCentrifuge.LOGGER.info("[AntiAlloy] Processing alloy recipe: " + alloyRecipe.getId());
                    } else if(recipe_type.equals("forge:conditional")) { //下界合金小巧思配方，目前应该没有别的用
                        if(alloyRecipe.getId().toString().equals("tconstruct:smeltery/alloys/molten_netherite")){
                            fitToOutputs(JsonHelper.parseList(json.get("recipes").getAsJsonArray().get(0).getAsJsonObject().get("recipe").getAsJsonObject(), "inputs", FluidIngredient::deserialize));
                            TinkersCentrifuge.LOGGER.info("[AntiAlloy] Processing alloy recipe: " + alloyRecipe.getId());
                        }else{
                            TinkersCentrifuge.LOGGER.info("[AntiAlloy] Unknown Conditional Recipe: " + alloyRecipe.getId());
                        }
                    }else{
                        TinkersCentrifuge.LOGGER.warn("[AntiAlloy] Unsupported Type Recipe: " + alloyRecipe.getId());
                    }
                    /* */
                } catch (Exception e) {
                    TinkersCentrifuge.LOGGER.error("[AntiAlloy] Failed to load recipe from " + alloyRecipe.getId(), e);
                }
            } else {
                TinkersCentrifuge.LOGGER.warn("[AntiAlloy] Resource manager is not available. Cannot load recipe: " + alloyRecipe.getId());
            }
        }
        //将配方条件转化为流体列表
        public void fitToOutputs(List<FluidIngredient> fluid_ingredients){
            if (!fluid_ingredients.isEmpty()) {
                for(FluidIngredient ingredient : fluid_ingredients){
                    if (ingredient.getFluids().isEmpty()) {
                        continue;
                    }
                    /*if (ingredient.catalyst) { //催化剂材料，不消耗自然也不该产出，直接跳过
                        continue;
                    }*/
                    FluidStack firstOne = Objects.requireNonNull(ingredient.getFluids().get(0));
                    if(firstOne != null){
                        this.outputs.add(new FluidStack(firstOne.getFluid(), firstOne.getAmount() / 10));
                    }
                }
            }
        }
    
        //获取输入
        public FluidStack getInput() {
            return input;
        }
        //获取输出
        public List<FluidStack> getOutputs() {
            return outputs;
        }
    }

    private List<AlloyRecipe> alloyRecipes;
    private List<AntiAlloyRecipe> antiAlloyRecipes;
    private List<Fluid> allAlloys;

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
    //获取所有合金配方
    public void initRecipes() {
        if (level != null && resourceManager != null) {
            // 从配方管理器那边获取所有合金配方
            RecipeManager recipeManager = level.getRecipeManager();
            alloyRecipes = recipeManager.getAllRecipesFor(TinkerRecipeTypes.ALLOYING.get());
            if (alloyRecipes.isEmpty()){return;}
            // 获取所有配方
            antiAlloyRecipes = new ArrayList<>();
            for (AlloyRecipe recipe : alloyRecipes) {
                antiAlloyRecipes.add(new AntiAlloyRecipe(recipe));
            }
            //检查配方对象，并列入可用合金列表
            allAlloys = new ArrayList<>();
            for (AntiAlloyRecipe recipe : antiAlloyRecipes) {
                if (recipe != null && recipe.input != null) {
                    allAlloys.add(recipe.input.getFluid());
                }
            }
            isLoaded = true;
        }
    }

    //检查是否存在配方
    public boolean hasRecipe(Fluid alloy) {
        if (!isLoaded) {initRecipes();}
        if(antiAlloyRecipes != null && allAlloys.contains(alloy)){
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
        for (AntiAlloyRecipe recipe : antiAlloyRecipes) {
            if (recipe != null && recipe.input.getFluid().equals(alloy)) {
                if (recipe.input.getAmount() <= amount) {
                    return recipe;
                }
            }
        }
        return null;
    }
}
