package slimegirl.centrifuge;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraftforge.fluids.FluidStack;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.client.RenderUtils;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;

import java.util.List;

public class MultiFluidTankBlockEntityRenderer implements BlockEntityRenderer<MultiFluidTankBlockEntify> {
    public MultiFluidTankBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    public void render(MultiFluidTankBlockEntify tile, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int combinedLightIn, int combinedOverlayIn) {
        if (!(Boolean) Config.CLIENT.tankFluidModel.get()) {
            // 1. 获取所有流体
            List<FluidStack> fluids = tile.tanks.getFluids();
            if (fluids.isEmpty()) return;

            int totalCapacity = tile.tanks.getCapacity();

            // 2. 设定离心机内容器的最大/最小渲染边界 (假设范围是 0.11f 到 15.89f，也就是 1.76 到 15.89 像素)
            float minX = 0.11f, minZ = 0.11f;
            float maxX = 15.89f, maxZ = 15.89f;

            float minY = 0.11f;
            float maxY = 15.89f;
            float totalRenderHeight = maxY - minY; // 整个流体柱能达到的最大视觉高度

            // 维护当前渲染底部的Y坐标
            float curY = minY;

            // 3. 循环遍历每种流体，一层一层堆叠上去
            for (FluidStack fluid : fluids) {
                if (fluid == null || fluid.isEmpty()) continue;

                // 计算当前这层流体应该占用的高度： 总渲染高度 * (流体量 / 总容量)
                float h = totalRenderHeight * ((float) fluid.getAmount() / totalCapacity);

                if (h <= 0.001f) continue; // 忽略过小的浮点误差

                // 创建这层流体专用的边界框
                FluidCuboid layerCuboid = FluidCuboid.builder()
                        .from(minX, curY, minZ)
                        .to(maxX, curY + h, maxZ)
                        .build();

                // 【关键技巧】：为了防止 RenderUtils 再次根据容量缩小高度，
                // 我们创建一个刚好 100% 满的“虚拟单层容器”传给它。
                FluidTankAnimated dummyTank = new FluidTankAnimated(fluid.getAmount(), tile);
                dummyTank.setFluid(fluid.copy()); // 容器容量=流体量，即视作 100% 满

                // 渲染这一层
                RenderUtils.renderFluidTank(matrixStack, buffer, layerCuboid, dummyTank, combinedLightIn, partialTicks, true);

                // 抬高底部Y坐标，为下一层流体做准备
                curY += h;
            }
        }
    }
}
