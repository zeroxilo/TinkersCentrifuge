package slimegirl.centrifuge;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = TinkersCentrifuge.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TinkersCentrifugeClient {

    @SuppressWarnings("removal")
    @SubscribeEvent
    public static void clientSetup(final FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(TinkersCentrifuge.CENTRIFUGE_BLOCK.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(TinkersCentrifuge.ALLOY_TANK_BLOCK.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(TinkersCentrifuge.ALLOY_GAUGE_BLOCK.get(), RenderType.cutout());
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TinkersCentrifuge.CENTRIFUGE_ENTITY.get(), MultiFluidTankBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(TinkersCentrifuge.TANK_BLOCK_ENTITY.get(), MultiFluidTankBlockEntityRenderer::new);
    }
}
