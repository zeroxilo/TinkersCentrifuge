package slimegirl.centrifuge;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.client.RenderUtils;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;

public class CentrifugeBlockEntityRenderer implements BlockEntityRenderer<CentrifugeBlockEntity> {
   public CentrifugeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
   }

    public void render(CentrifugeBlockEntity tile, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int combinedLightIn, int combinedOverlayIn) {
        if (!(Boolean) Config.CLIENT.tankFluidModel.get()) {
            FluidTankAnimated tank = tile.getTank();
            FluidCuboid fluid = FluidCuboid.builder().from(0.11f, 0.11f, 0.11f).to(15.89f, 15.89f, 15.89f).build();
            RenderUtils.renderFluidTank(matrixStack, buffer, fluid, tank, combinedLightIn, partialTicks, true);
      }

   }
}
