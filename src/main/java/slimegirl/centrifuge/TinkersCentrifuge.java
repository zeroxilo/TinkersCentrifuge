package slimegirl.centrifuge;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.registration.deferred.BlockEntityTypeDeferredRegister;
import slimeknights.mantle.registration.deferred.SynchronizedDeferredRegister;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(TinkersCentrifuge.MODID)
public class TinkersCentrifuge{
    public static final String MODID = "tinkerscentrifuge";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final BlockEntityTypeDeferredRegister BLOCK_ENTITIES = new BlockEntityTypeDeferredRegister(MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, MODID);
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, MODID);
    public static final SynchronizedDeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = SynchronizedDeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MODID);
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, MODID);
    public static final DeferredRegister<LootItemFunctionType> LOOT_FUNCTIONS =
            DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, MODID);

    public static final RegistryObject<LootItemFunctionType> CENTRIFUGE_NBT_FUNCTION =
            LOOT_FUNCTIONS.register("split_centrifuge_tanks",
                    () -> new LootItemFunctionType(new CentrifugeNbtFunction.CentrifugeSerializer()));


    protected static final Item.Properties ITEM_PROPS = new Item.Properties();
    


    //离心机注册
    public static final RegistryObject<CentrifugeBlock> CENTRIFUGE_BLOCK = BLOCKS.register(
        "centrifuge",
        () -> new CentrifugeBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK).noOcclusion().strength(5.0f, 6.0f)
            .requiresCorrectToolForDrops()
        )
    );
    public static final RegistryObject<MultiFluidTankItem> CENTRIFUGE_BLOCK_ITEM = ITEMS.register(
        "centrifuge",
            () -> new MultiFluidTankItem(CENTRIFUGE_BLOCK.get(), new Item.Properties(), true)
    );
    public static final RegistryObject<BlockEntityType<CentrifugeBlockEntity>> CENTRIFUGE_ENTITY = BLOCK_ENTITIES.register(
            "centrifuge_entity", CentrifugeBlockEntity::new, CENTRIFUGE_BLOCK
    );
    //离心配方注册
    public static final RegistryObject<RecipeSerializer<AntiAlloyRecipe>> ANTI_ALLOYING_SERIALIZER = RECIPE_SERIALIZERS.register("anti_alloy", () -> LoadableRecipeSerializer.of(AntiAlloyRecipe.LOADER));
    public static final RegistryObject<RecipeType<AntiAlloyRecipe>> ANTI_ALLOYING = RECIPE_TYPES.register(
        "anti_alloy",
        () -> new RecipeType<AntiAlloyRecipe>() {
            @Override
            public String toString() {
                return MODID + ":anti_alloy";
            }
        }
    );

    //合金储罐注册
    public static final RegistryObject<AlloyTankBlock> ALLOY_TANK_BLOCK = BLOCKS.register(
        "alloy_tank",
            () -> new AlloyTankBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).noOcclusion().strength(5.0f, 6.0f))
    );
    public static final RegistryObject<MultiFluidTankItem> ALLOY_TANK_BLOCK_ITEM = ITEMS.register(
        "alloy_tank",
            () -> new MultiFluidTankItem(ALLOY_TANK_BLOCK.get(), ITEM_PROPS, true)
    );

    //合金量器注册
    public static final RegistryObject<AlloyTankBlock> ALLOY_GAUGE_BLOCK = BLOCKS.register(
        "alloy_gauge",
            () -> new AlloyTankBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).noOcclusion().strength(5.0f, 6.0f))
    );
    public static final RegistryObject<MultiFluidTankItem> ALLOY_GAUGE_BLOCK_ITEM = ITEMS.register(
        "alloy_gauge",
            () -> new MultiFluidTankItem(ALLOY_GAUGE_BLOCK.get(), ITEM_PROPS, true)
    );


    public static final RegistryObject<BlockEntityType<AlloyTankBlockEntity>> TANK_BLOCK_ENTITY = BLOCK_ENTITIES.register("tank", AlloyTankBlockEntity::new, set -> {
        set.add(ALLOY_TANK_BLOCK.get());
        set.add(ALLOY_GAUGE_BLOCK.get());
    });
    //月季铁注册
    public static final RegistryObject<Block> ROSA_IRON_BLOCK = BLOCKS.register(
        "rosa_iron_block",
        () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE).strength(5.0f, 6.0f)
            .requiresCorrectToolForDrops()
        )
    );
    public static final RegistryObject<BlockItem> ROSA_IRON_BLOCK_ITEM = ITEMS.register(
        "rosa_iron_block",
        () -> new BlockItem(ROSA_IRON_BLOCK.get(), new Item.Properties())
    );
    public static final RegistryObject<Item> ROSA_IRON_INGOT = ITEMS.register(
        "rosa_iron_ingot",
        () -> new Item(new Item.Properties())
    );
    public static final RegistryObject<Item> ROSA_IRON_NUGGET = ITEMS.register(
        "rosa_iron_nugget",
        () -> new Item(new Item.Properties())
    );
    //熔融月季铁注册
    public static FluidType.Properties MOLTEN_FLUID_TYPE_PROPERTIES = FluidType.Properties.create()
        .density(2000)
        .viscosity(10000)
        .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
        .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA);

    public static final RegistryObject<FluidType> MOLTEN_ROSA_IRON_TYPE = FLUID_TYPES.register(
        "molten_rosa_iron",
        () -> new MoltenFluidType("molten_rosa_iron", TinkersCentrifuge.MOLTEN_FLUID_TYPE_PROPERTIES.temperature(993))
    );
        
    public static final ForgeFlowingFluid.Properties MOLTEN_ROSA_IRON_PROPERTIES = new ForgeFlowingFluid.Properties(
        () -> TinkersCentrifuge.MOLTEN_ROSA_IRON_TYPE.get(),
        () -> TinkersCentrifuge.MOLTEN_ROSA_IRON.get(),
        () -> TinkersCentrifuge.MOLTEN_ROSA_IRON_FLOWING.get()
    ).block(() -> TinkersCentrifuge.MOLTEN_ROSA_IRON_BLOCK.get())
    .bucket(() -> TinkersCentrifuge.MOLTEN_ROSA_IRON_BUCKET.get())
    .slopeFindDistance(3).explosionResistance(100F);
    private static final RegistryObject<FlowingFluid> MOLTEN_ROSA_IRON = FLUIDS.register(
        "molten_rosa_iron", 
        () -> new ForgeFlowingFluid.Source(MOLTEN_ROSA_IRON_PROPERTIES)
    );
    private static final RegistryObject<FlowingFluid> MOLTEN_ROSA_IRON_FLOWING = FLUIDS.register(
        "molten_rosa_iron_flowing", 
        () -> new ForgeFlowingFluid.Flowing(MOLTEN_ROSA_IRON_PROPERTIES)
    );
    public static final RegistryObject<BucketItem> MOLTEN_ROSA_IRON_BUCKET = ITEMS.register(
        "molten_rosa_iron_bucket",
        () -> new BucketItem(MOLTEN_ROSA_IRON, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1))
    );
    public static final RegistryObject<LiquidBlock> MOLTEN_ROSA_IRON_BLOCK = BLOCKS.register(
        "molten_rosa_iron",
        () -> new LiquidBlock(MOLTEN_ROSA_IRON, BlockBehaviour.Properties.of().noLootTable())
    );

    //创造模式物品栏
    public static final RegistryObject<CreativeModeTab> CREATIVE_TAB = CREATIVE_MODE_TABS.register("tinkerscentrifuge_tab", () ->
            CreativeModeTab.builder()
                    .icon(() -> new ItemStack(TinkersCentrifuge.CENTRIFUGE_BLOCK_ITEM.get()))
                    .title(Component.translatable("tab.tinkerscentrifuge"))
                    .displayItems((parameters, output) -> {
                        output.accept(ROSA_IRON_NUGGET.get());
                        output.accept(ROSA_IRON_INGOT.get());
                        output.accept(ROSA_IRON_BLOCK_ITEM.get());
                        output.accept(MOLTEN_ROSA_IRON_BUCKET.get());
                        output.accept(CENTRIFUGE_BLOCK_ITEM.get());
                        output.accept(ALLOY_TANK_BLOCK_ITEM.get());
                        output.accept(ALLOY_GAUGE_BLOCK_ITEM.get());
                    })
                    .build()
    );

    public TinkersCentrifuge(FMLJavaModLoadingContext context){
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::commonSetup);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        FLUIDS.register(modEventBus);
        FLUID_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
        LOOT_FUNCTIONS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        //LOGGER.info("HELLO FROM COMMON SETUP");
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event){
        //LOGGER.info("HELLO from server starting");
    }
}
