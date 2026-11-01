package ic2.core.block.machine.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import ic2.core.block.machine.tileentity.TileEntityAirCompressor;
import ic2.core.ref.Ic2BlockEntities;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Live receiver needle over the original-texture baked dial. */
@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class AirCompressorGaugeRenderer
    implements BlockEntityRenderer<TileEntityAirCompressor> {
  public AirCompressorGaugeRenderer(BlockEntityRendererProvider.Context context) {}

  @SubscribeEvent
  public static void register(EntityRenderersEvent.RegisterRenderers event) {
    event.registerBlockEntityRenderer(
        Ic2BlockEntities.AIR_COMPRESSOR, AirCompressorGaugeRenderer::new);
  }

  @Override
  public void render(
      TileEntityAirCompressor tile,
      float partial,
      PoseStack pose,
      MultiBufferSource buffers,
      int light,
      int overlay) {
    pose.pushPose();
    pose.translate(.5, .5, .5);
    pose.mulPose(Axis.YP.rotationDegrees(-tile.getFacing().toYRot() + 180));
    pose.translate(-.5, -.5, -.5);
    pose.translate(5.25 / 16, 11.25 / 16, -.003);
    double fill =
        Math.clamp((double) tile.receiver.getFluidAmount() / tile.receiver.getCapacity(), 0, 1);
    pose.mulPose(Axis.ZP.rotationDegrees((float) (-135 + fill * 270)));
    if (tile.getLevel() != null)
      light =
          LevelRenderer.getLightColor(
              tile.getLevel(), tile.getBlockPos().relative(tile.getFacing()));
    var consumer =
        buffers.getBuffer(
            RenderType.entityCutoutNoCull(
                ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png")));
    float[] xs = {-.012F, .012F, .012F, -.012F};
    float[] ys = {-.025F, -.025F, .095F, .095F};
    for (int i = 0; i < 4; i++)
      consumer
          .addVertex(pose.last(), xs[i], ys[i], 0)
          .setColor(0xFFB82720)
          .setUv(.5F, .5F)
          .setOverlay(overlay)
          .setLight(light)
          .setNormal(pose.last(), 0, 0, -1);
    pose.popPose();
  }
}
