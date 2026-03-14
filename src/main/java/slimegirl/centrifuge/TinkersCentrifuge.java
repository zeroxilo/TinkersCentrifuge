package slimegirl.centrifuge;

import com.mojang.logging.LogUtils;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock;
import slimeknights.tconstruct.smeltery.client.render.GaugeBlockEntityRenderer;
import slimeknights.tconstruct.smeltery.client.render.TankBlockEntityRenderer;
import slimeknights.tconstruct.smeltery.item.TankItem;

import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(TinkersCentrifuge.MODID)
public class TinkersCentrifuge{
    public static final String MODID = "tinkerscentrifuge";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, MODID);
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, MODID);
    
    protected static final Item.Properties ITEM_PROPS = new Item.Properties();
    
    //创造模式物品栏
    public static final RegistryObject<CreativeModeTab> CREATIVE_TAB = CREATIVE_MODE_TABS.register("tinkerscentrifuge_tab", () ->
        CreativeModeTab.builder()
            .icon(() -> new ItemStack(TinkersCentrifuge.CENTRIFUGE_BLOCK_ITEM.get()))
            .title(Component.translatable("tab.tinkerscentrifuge"))
            .build()
    );

    //离心机注册
    public static final RegistryObject<CentrifugeBlock> CENTRIFUGE_BLOCK = BLOCKS.register(
        "centrifuge",
        () -> new CentrifugeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).noOcclusion().strength(5.0f, 6.0f))
    );
    public static final RegistryObject<BlockItem> CENTRIFUGE_BLOCK_ITEM = ITEMS.register(
        "centrifuge",
        () -> new TankItem(CENTRIFUGE_BLOCK.get(), new Item.Properties(),true)
    );
    public static final RegistryObject<BlockEntityType<CentrifugeBlockEntity>> CENTRIFUGE_ENTITY = BLOCK_ENTITIES.register(
        "centrifuge_entity",
        () -> BlockEntityType.Builder.of(CentrifugeBlockEntity::new, CENTRIFUGE_BLOCK.get()).build(null)
    );
    //合金储罐注册
    public static final RegistryObject<Block> ALLOY_TANK_BLOCK = BLOCKS.register(
        "alloy_tank",
        () -> new SearedTankBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).noOcclusion().strength(5.0f, 6.0f),7290,PushReaction.DESTROY)
    );
    public static final RegistryObject<Item> ALLOY_TANK_BLOCK_ITEM = ITEMS.register(
        "alloy_tank",
        () -> new TankItem(ALLOY_TANK_BLOCK.get(), ITEM_PROPS, true)
    );
    //合金量器注册
    public static final RegistryObject<Block> ALLOY_GAUGE_BLOCK = BLOCKS.register(
        "alloy_gauge",
        () -> new SearedTankBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).noOcclusion().strength(5.0f, 6.0f),7290,PushReaction.DESTROY)
    );
    public static final RegistryObject<Item> ALLOY_GAUGE_BLOCK_ITEM = ITEMS.register(
        "alloy_gauge",
        () -> new TankItem(ALLOY_GAUGE_BLOCK.get(), ITEM_PROPS, true)
    );
    //月季铁注册
    public static final RegistryObject<Block> ROSA_IRON_BLOCK = BLOCKS.register(
        "rosa_iron_block",
        () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(5.0f, 6.0f))
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

    public TinkersCentrifuge(FMLJavaModLoadingContext context){
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::commonSetup);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        FLUIDS.register(modEventBus);
        FLUID_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::addCreative);
        //context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event){
        //LOGGER.info("HELLO FROM COMMON SETUP");
    }

    private void clientSetup(final FMLClientSetupEvent event){
        ItemBlockRenderTypes.setRenderLayer(CENTRIFUGE_BLOCK.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ALLOY_TANK_BLOCK.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ALLOY_GAUGE_BLOCK.get(), RenderType.cutout());
    }
    
    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CENTRIFUGE_ENTITY.get(), CentrifugeBlockEntityRenderer::new);
    }

    // 将离心机添加进创造模式物品栏
    private void addCreative(BuildCreativeModeTabContentsEvent event){
        if (event.getTabKey() == CREATIVE_TAB.getKey() ) {
            event.accept(ROSA_IRON_NUGGET);
            event.accept(ROSA_IRON_INGOT);
            event.accept(ROSA_IRON_BLOCK_ITEM);
            event.accept(MOLTEN_ROSA_IRON_BUCKET);
            event.accept(CENTRIFUGE_BLOCK_ITEM);
            event.accept(ALLOY_TANK_BLOCK_ITEM);
            event.accept(ALLOY_GAUGE_BLOCK_ITEM);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event){
        //LOGGER.info("HELLO from server starting");
    }
}
