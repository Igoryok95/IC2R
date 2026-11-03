package ic2.core.gametest.client;

import ic2.core.block.tileentity.Ic2TileEntityBlock;
import ic2.core.ref.Ic2Blocks;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.LoggerFactory;

/** Single opt-in comparison of actual baked lamp models and world targeting bounds. */
@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public final class LuminatorShapeClientSmoke {
  private static final BlockPos ORIGIN = new BlockPos(60, 128, 0);
  private static CompletableFuture<Void> pending;
  private static int ticks;

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    if (!Boolean.getBoolean("ic2.luminatorShapeSmoke")) return;
    var mc = Minecraft.getInstance();
    var server = mc.getSingleplayerServer();
    if (mc.player == null
        || mc.level == null
        || server == null
        || mc.getOverlay() != null
        || pending != null && !pending.isDone()) return;
    if (pending != null) pending.join();
    mc.options.pauseOnLostFocus = false;
    int phase = ++ticks;
    if (phase == 1) {
      mc.options.hideGui = false;
      mc.options.guiScale().set(2);
      mc.resizeDisplay();
      var id = mc.player.getUUID();
      pending =
          CompletableFuture.runAsync(
              () -> {
                var player = server.getPlayerList().getPlayer(id);
                var level = player.serverLevel();
                player.setGameMode(GameType.CREATIVE);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.getInventory().clearContent();
                level.setDayTime(6000);
                level.setWeatherParameters(10000, 0, false, false);
                for (var pos : BlockPos.betweenClosed(58, 123, -4, 78, 132, 3))
                  level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                for (var facing : Direction.values()) {
                  var pos = ORIGIN.east(facing.ordinal() * 3);
                  level.setBlockAndUpdate(
                      pos.relative(facing.getOpposite()), Blocks.OAK_PLANKS.defaultBlockState());
                  level.setBlockAndUpdate(
                      pos,
                      Ic2Blocks.LUMINATOR_FLAT
                          .defaultBlockState()
                          .setValue(Ic2TileEntityBlock.anyFacingProperty, facing));
                }
                player.connection.teleport(60.5, 125.3, -1.5, 0, -45.2f);
              },
              server);
    }
    if (phase == 35) {
      for (var facing : Direction.values()) {
        var pos = ORIGIN.east(facing.ordinal() * 3);
        var state = mc.level.getBlockState(pos);
        require(state.is(Ic2Blocks.LUMINATOR_FLAT), "lamp did not reach client: " + facing);
        var outline = state.getShape(mc.level, pos).bounds();
        for (boolean active : new boolean[] {false, true}) {
          var modelState = state.setValue(Ic2TileEntityBlock.ACTIVE, active);
          var model = mc.getBlockRenderer().getBlockModelShaper().getBlockModel(modelState);
          double minX = Double.POSITIVE_INFINITY, minY = minX, minZ = minX;
          double maxX = Double.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
          for (int face = -1; face < 6; face++) {
            for (var quad :
                model.getQuads(
                    modelState,
                    face < 0 ? null : Direction.values()[face],
                    RandomSource.create(42))) {
              int[] vertices = quad.getVertices();
              int stride = vertices.length / 4;
              for (int vertex = 0; vertex < 4; vertex++) {
                double x = Float.intBitsToFloat(vertices[vertex * stride]);
                double y = Float.intBitsToFloat(vertices[vertex * stride + 1]);
                double z = Float.intBitsToFloat(vertices[vertex * stride + 2]);
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                minZ = Math.min(minZ, z);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                maxZ = Math.max(maxZ, z);
              }
            }
          }
          var rendered = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
          require(
              Math.abs(rendered.minX - outline.minX) < 1e-6
                  && Math.abs(rendered.minY - outline.minY) < 1e-6
                  && Math.abs(rendered.minZ - outline.minZ) < 1e-6
                  && Math.abs(rendered.maxX - outline.maxX) < 1e-6
                  && Math.abs(rendered.maxY - outline.maxY) < 1e-6
                  && Math.abs(rendered.maxZ - outline.maxZ) < 1e-6,
              "baked lamp model and outline differ: "
                  + facing
                  + " active="
                  + active
                  + " model="
                  + rendered
                  + " outline="
                  + outline);
        }
      }
      require(
          mc.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(ORIGIN),
          "camera does not target the ceiling lamp for the outline screenshot");
      Screenshot.grab(
          mc.gameDirectory,
          "luminator-ceiling-outline-21.1.89.png",
          mc.getMainRenderTarget(),
          ignored -> {});
      LoggerFactory.getLogger("IC2R-Luminator-Shape")
          .info(
              "IC2R_LUMINATOR_SHAPE_CLIENT_PASS: twelve baked models match targeting bounds; real ceiling lamp selected");
    }
    if (phase == 42) mc.stop();
  }

  private static void require(boolean valid, String message) {
    if (!valid) throw new IllegalStateException(message);
  }

  private LuminatorShapeClientSmoke() {}
}
