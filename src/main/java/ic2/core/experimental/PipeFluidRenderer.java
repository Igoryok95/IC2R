package ic2.core.experimental;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/** Atlas-safe scrolling fluid inside the glass shell, independent of BuildCraft. */
final class PipeFluidRenderer {
  static void render(
      ExperimentalFluidPipe pipe,
      float partial,
      PoseStack pose,
      MultiBufferSource buffers,
      int light,
      int overlay) {
    double time = pipe.getLevel().getGameTime() + partial;
    var fluid = pipe.visuals.fluid();
    double amount = pipe.visuals.amount(time);
    if (fluid.isEmpty() || amount <= .01) return;
    double fraction = Math.clamp(amount / pipe.tank.getCapacity(), 0, 1);
    double width = ((ExperimentalPipeBlock) pipe.getBlockState().getBlock()).width;
    double a = (16 - width) / 32 + 1.01 / 16, b = 1 - a;
    boolean gas = fluid.getFluid().getFluidType().getDensity(fluid) < 0;
    double bottom = gas ? b - (b - a) * fraction : a;
    double top = gas ? b : a + (b - a) * fraction;
    var extension = IClientFluidTypeExtensions.of(fluid.getFluid());
    var texture = extension.getStillTexture(fluid);
    if (texture == null) return;
    var sprite =
        Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
    int tint = extension.getTintColor(fluid);
    if ((tint >>> 24) == 0) tint |= 0xFF000000;
    int emission = fluid.getFluid().getFluidType().getLightLevel(fluid) << 4;
    light = (light & 0xFFFF0000) | Math.max(light & 0xFFFF, emission);
    var buffer =
        buffers.getBuffer(RenderType.itemEntityTranslucentCull(TextureAtlas.LOCATION_BLOCKS));
    boolean[] centerFaces = {true, true, true, true, true, true};
    for (Direction side : Direction.values())
      if (pipe.getBlockState().getValue(ExperimentalPipeBlock.CONNECTIONS[side.ordinal()])) {
        boolean[] faces = {true, true, true, true, true, true};
        faces[side.getOpposite().ordinal()] = false;
        double[] lo = {a, bottom, a}, hi = {b, top, b};
        int axis = side.getAxis().ordinal();
        if (side.getAxis() == Direction.Axis.Y) {
          double radius = (b - a) * Math.sqrt(fraction) / 2;
          lo[0] = lo[2] = .5 - radius;
          hi[0] = hi[2] = .5 + radius;
          if (side == Direction.UP) {
            lo[1] = gas ? b : top;
            hi[1] = 1;
          } else {
            lo[1] = 0;
            hi[1] = gas ? bottom : a;
          }
        } else {
          centerFaces[side.ordinal()] = false;
          if (side.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            lo[axis] = b;
            hi[axis] = 1;
          } else {
            lo[axis] = 0;
            hi[axis] = a;
          }
        }
        box(
            lo,
            hi,
            faces,
            pipe.visuals.offset(side, time),
            sprite,
            buffer,
            pose.last(),
            tint,
            light,
            overlay);
      }
    box(
        new double[] {a, bottom, a},
        new double[] {b, top, b},
        centerFaces,
        pipe.visuals.offset(null, time),
        sprite,
        buffer,
        pose.last(),
        tint,
        light,
        overlay);
  }

  private static void box(
      double[] lo,
      double[] hi,
      boolean[] visible,
      Vec3 offset,
      TextureAtlasSprite sprite,
      VertexConsumer buffer,
      PoseStack.Pose pose,
      int tint,
      int light,
      int overlay) {
    if (hi[0] <= lo[0] || hi[1] <= lo[1] || hi[2] <= lo[2]) return;
    double[] scroll = {offset.x, offset.y, offset.z};
    for (Direction face : Direction.values()) {
      if (!visible[face.ordinal()]) continue;
      int axis = face.getAxis().ordinal(), u = (axis + 1) % 3, v = (axis + 2) % 3;
      boolean positive = face.getAxisDirection() == Direction.AxisDirection.POSITIVE;
      double plane = positive ? hi[axis] : lo[axis];
      for (var us : tiles(lo[u], hi[u], scroll[u]))
        for (var vs : tiles(lo[v], hi[v], scroll[v])) {
          double[][] corners = {
            {us[0], vs[0], us[2], vs[2]},
            {us[1], vs[0], us[3], vs[2]},
            {us[1], vs[1], us[3], vs[3]},
            {us[0], vs[1], us[2], vs[3]}
          };
          for (int i : positive ? new int[] {0, 1, 2, 3} : new int[] {0, 3, 2, 1}) {
            var corner = corners[i];
            double[] pos = {0, 0, 0};
            pos[axis] = plane;
            pos[u] = corner[0];
            pos[v] = corner[1];
            buffer
                .addVertex(pose, (float) pos[0], (float) pos[1], (float) pos[2])
                .setColor(tint)
                .setUv(sprite.getU((float) corner[2]), sprite.getV((float) corner[3]))
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, face.getStepX(), face.getStepY(), face.getStepZ());
          }
        }
    }
  }

  /** Split at sprite borders rather than sampling neighboring atlas textures when scrolling. */
  private static List<double[]> tiles(double from, double to, double offset) {
    var result = new ArrayList<double[]>();
    for (double start = from; start < to - 1e-8; ) {
      double tile = Math.floor(start + offset + 1e-8);
      double end = Math.min(to, tile + 1 - offset);
      if (end <= start + 1e-8) {
        tile++;
        end = Math.min(to, tile + 1 - offset);
      }
      result.add(
          new double[] {
            start,
            end,
            Math.clamp(start + offset - tile, 0, 1),
            Math.clamp(end + offset - tile, 0, 1)
          });
      start = end;
    }
    return result;
  }

  private PipeFluidRenderer() {}
}
