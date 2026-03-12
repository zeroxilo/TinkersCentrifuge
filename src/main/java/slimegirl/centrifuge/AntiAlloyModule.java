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
            this.input = alloyRecipe.getOutput();
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
                    List<FluidIngredient> fluids = JsonHelper.parseList(json, "inputs", FluidIngredient::deserialize);
                    //构建输出流体
                    if (!fluids.isEmpty()) {
                        for(FluidIngredient fluid : fluids){
                            if (fluid.getFluids().isEmpty()) {
                                continue;
                            }
                            FluidStack firstOne = Objects.requireNonNull(fluid.getFluids().get(0));
                            if(firstOne != null){
                                this.outputs.add(firstOne);
                            }
                        }
                        TinkersCentrifuge.LOGGER.info("[AntiAlloy] Loaded recipe: " + alloyRecipe.getId() + " with inputs: " + fluids);
                    } else {
                        TinkersCentrifuge.LOGGER.info("[AntiAlloy] Alloy recipe has an empty input: " + alloyRecipe.getId());
                    }
                } catch (Exception e) {
                    TinkersCentrifuge.LOGGER.error("[AntiAlloy] Failed to load recipe from " + alloyRecipe.getId(), e);
                }
            } else {
                TinkersCentrifuge.LOGGER.warn("[AntiAlloy] Resource manager is not available. Cannot load recipe: " + alloyRecipe.getId());
            }
        }
    
        public FluidStack getInput() {
            return input;
        }
        public List<FluidStack> getOutputs() {
            return outputs;
        }
    }

    private List<AlloyRecipe> alloyRecipes;
    private List<AntiAlloyRecipe> antiAlloyRecipes;
    private List<Fluid> allAlloys;

    //获取所有合金配方
    public void initRecipes(Level level) {
        if(this.level == null){
            this.level = level;
        }
        initRecipes();
    }
    //获取所有合金配方
    public void initRecipes() {
        if(level != null){
            this.server = level.getServer();
            if (server != null && resourceManager == null) {
                resourceManager = server.getResourceManager();
            }
        }
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

    //返回首个符合的配方
    public AntiAlloyRecipe getRecipe(FluidStack alloyStack) {
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
