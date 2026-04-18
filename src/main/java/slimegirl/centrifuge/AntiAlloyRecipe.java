package slimegirl.centrifuge;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.checkerframework.checker.units.qual.C;

import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.extensions.IForgeFluid;
import net.minecraftforge.common.extensions.IForgeItem;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.ICustomOutputRecipe;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.mantle.recipe.helper.FluidOutput.Loadable;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipe;

//反合金配方
//虽然写了很多交互内容，但是实际上是纯服务器事件，写这么多方便注册/调用/制作JEI显示
public class AntiAlloyRecipe implements ICustomOutputRecipe<IAntiAlloyTank>{
    public static final RecordLoadable<AntiAlloyRecipe> LOADER;
    public final ResourceLocation id;
    public final AntiAlloyIngredient input;
    public final List<FluidOutput> outputs;
    public final int temperature;

    static {
        LOADER = RecordLoadable.create(
            ContextKey.ID.requiredField(),
            AntiAlloyRecipe.AntiAlloyIngredient.LOADABLE.requiredField("input", (r) -> r.input),
            Loadable.REQUIRED.list().requiredField("result", (r) -> r.outputs),
            IntLoadable.FROM_ONE.requiredField("temperature", (r) -> r.temperature),
            AntiAlloyRecipe::new);
    }

    //正常方式创建反合金配方
    public AntiAlloyRecipe(ResourceLocation id, AntiAlloyIngredient input, List<FluidOutput> outputs,int temperature){
        this.id = id;
        this.input = input;
        this.outputs = outputs;
        this.temperature = temperature;
    }

    //从已有的合金配方创建反合金配方
    public AntiAlloyRecipe(AlloyRecipe alloyRecipe,ResourceManager resourceManager) {
        //解析原配方
        Fluid alloyFluid = alloyRecipe.getOutput().getFluid();
        int alloyAmount = alloyRecipe.getOutput().getAmount() / 10;
        this.input = new AntiAlloyIngredient(FluidIngredient.of(new FluidStack(alloyFluid,alloyAmount)),false);
        this.id = ResourceLocation.fromNamespaceAndPath("tinkerscentrifuge",alloyRecipe.getId().getPath()+"_reversed");
        this.temperature = 0;
        this.outputs = new ArrayList<FluidOutput>();
        try {
            List<FluidIngredient> fluidIngredientList = alloyRecipe
                    .getInputs()
                    .stream()
                    .filter(Predicate.not(AlloyRecipe.AlloyIngredient::catalyst))
                    .map(AlloyRecipe.AlloyIngredient::fluid)
                    .toList();
            fitToOutputs(fluidIngredientList);
        } catch (Exception e) {
            TinkersCentrifuge.LOGGER.error("[AntiAlloy] Failed to load recipe from " + alloyRecipe.getId(), e);
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
                //遍历可行流体，尽可能寻找原版、匠魂本家的材料（毕竟是匠魂的附属...）
                FluidStack pickedFluid = FluidStack.EMPTY;
                boolean picked = false;
                for(FluidStack fluid : ingredient.getFluids()){
                    if(!fluid.isEmpty() && fluid.getFluid().getBucket() != null){
                        FluidType fluidType = fluid.getFluid().getFluidType();
                        try{ // 尝试获取注册ID
                            String nameSpace = ((IForgeRegistry)ForgeRegistries.FLUID_TYPES.get()).getKey(fluidType).getNamespace().toString();
                            if(fluidType.isVanilla() || nameSpace == "tconstruct" || nameSpace == "tinkerscentrifuge" || nameSpace == "minecraft"){
                                pickedFluid = fluid;
                                picked = true;
                            }
                        }finally{}
                    }
                }
                //还没有？那只能取第一个用了
                if(!picked && ingredient.getFluids().size() > 0){
                    pickedFluid = ingredient.getFluids().get(0);
                }
                //将取得的流体作为输出加入到配方中
                if(pickedFluid != null){
                    Fluid mat_fluid = pickedFluid.getFluid();
                    int mat_amount = pickedFluid.getAmount() / 10;
                    this.outputs.add(FluidOutput.fromFluid(mat_fluid,mat_amount));
                }
            }
        }
    }

    

    //获取输入
    public FluidStack getInput() {
        List<FluidStack> fluid_list = this.input.fluid.getFluids();
        if(!fluid_list.isEmpty()){
            return fluid_list.get(0);
        }else{
            return FluidStack.EMPTY;
        }
    }
    
    //获取输出
    public List<FluidStack> getOutputs() {
        List<FluidStack> results = new ArrayList<>();
        for(FluidOutput output : this.outputs){
            results.add(output.get());
        }
        return results;
    }
    
    //获取配方ID
    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    //配方解析器关联
    @Override
    public RecipeSerializer<?> getSerializer() {
        return TinkersCentrifuge.antiAlloyingSerializer.get();
    }

    //获取配方类型
    @Override
    public RecipeType<?> getType() {
        return TinkersCentrifuge.ANTI_ALLOYING.get();
    }

    //检查数据是否可以制造配方
    @Override
    public boolean matches(IAntiAlloyTank tanks, Level level) {
        for(int i = 0;i < tanks.getTanks();i++){
            FluidStack alloyStack = tanks.getFluidInTank(i); //目前已有合金
            if(!alloyStack.isEmpty() && this.input.fluid.test(alloyStack)){
                return true;
            }
        }
        return false;
    }
    
    // 反合金配方条件
    // 实际上用不上，纯服务器处理...
    public static record AntiAlloyIngredient(FluidIngredient fluid,boolean catalyst) {
        public static final RecordLoadable<AntiAlloyIngredient> LOADABLE;

        static {
            LOADABLE = RecordLoadable.create(
                FluidIngredient.LOADABLE.tryDirectField("match", AntiAlloyIngredient::fluid, new String[0]),
                BooleanLoadable.INSTANCE.defaultField("catalyst", false, false, AntiAlloyIngredient::catalyst),
                AntiAlloyIngredient::new
            ).compact(FluidIngredient.LOADABLE.flatXmap(
                (fluid) -> new AntiAlloyIngredient(fluid,false), AntiAlloyIngredient::fluid),
                (alloy) -> !alloy.catalyst()
            );
        }
    }
}