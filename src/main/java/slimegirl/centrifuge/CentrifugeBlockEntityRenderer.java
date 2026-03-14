package slimegirl.centrifuge;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.mantle.client.render.MantleRenderTypes;

public class CentrifugeBlockEntityRenderer implements BlockEntityRenderer<CentrifugeBlockEntity> {
   public CentrifugeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
   }

   public void render(CentrifugeBlockEntity tile, float pPartialTick, PoseStack matrices, MultiBufferSource buffer, int light, int pPackedOverlay) {
      List<FluidCuboid> fluids = (List)FluidCuboid.REGISTRY.get(tile.getBlockState(), List.of());
      if (!fluids.isEmpty()) {
         IFluidHandler tank = tile.tanks;
         if (tank.getTanks() > 0) {
            FluidStack fluid = tank.getFluidInTank(0);
            if (!fluids.isEmpty()) {
               FluidRenderer.renderCuboids(matrices, buffer.getBuffer(MantleRenderTypes.FLUID), fluids, fluid, light);
            }
         }
      }

   }
}
