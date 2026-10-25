package ic2.core.experimental;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import ic2.core.IC2;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/** Original cover background/arrow artwork, rendered without expanding pipe block-state counts. */
public final class PipeCoverRenderer implements BlockEntityRenderer<ExperimentalFluidPipe> {
  public PipeCoverRenderer(BlockEntityRendererProvider.Context context) {}

  @Override
  public void render(
      ExperimentalFluidPipe pipe,
      float partial,
      PoseStack pose,
      MultiBufferSource buffers,
      int light,
      int overlay) {
    for (Direction side : Direction.values()) {
      int pump = pipe.pumps[side.ordinal()];
      if (pump == 0) continue;
      pose.pushPose();
      pose.translate(.5, .5, .5);
      switch (side) {
        case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(180));
        case EAST -> pose.mulPose(Axis.YP.rotationDegrees(90));
        case WEST -> pose.mulPose(Axis.YP.rotationDegrees(270));
        case UP -> pose.mulPose(Axis.XP.rotationDegrees(270));
        case DOWN -> pose.mulPose(Axis.XP.rotationDegrees(90));
        default -> {}
      }
      layer(pose, buffers, "pump_background", -1, .501F, light, overlay);
      layer(
          pose,
          buffers,
          "pump_foreground",
          pump == 1 ? 0xFF00FF00 : 0xFFFFFF00,
          .502F,
          light,
          overlay);
      pose.popPose();
    }
  }

  private static void layer(
      PoseStack poses,
      MultiBufferSource buffers,
      String texture,
      int color,
      float z,
      int light,
      int overlay) {
    var consumer =
        buffers.getBuffer(
            RenderType.entityCutout(
                IC2.getIdentifier("textures/item/experimental/" + texture + ".png")));
    vertex(consumer, poses, -.25F, -.25F, z, 0, 1, color, light, overlay);
    vertex(consumer, poses, .25F, -.25F, z, 1, 1, color, light, overlay);
    vertex(consumer, poses, .25F, .25F, z, 1, 0, color, light, overlay);
    vertex(consumer, poses, -.25F, .25F, z, 0, 0, color, light, overlay);
  }

  private static void vertex(
      VertexConsumer consumer,
      PoseStack poses,
      float x,
      float y,
      float z,
      float u,
      float v,
      int color,
      int light,
      int overlay) {
    consumer
        .addVertex(poses.last(), x, y, z)
        .setColor(color)
        .setUv(u, v)
        .setOverlay(overlay)
        .setLight(light)
        .setNormal(poses.last(), 0, 0, 1);
  }
}
