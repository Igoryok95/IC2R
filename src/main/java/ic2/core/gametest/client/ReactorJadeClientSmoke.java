package ic2.core.gametest.client;

import ic2.core.block.reactor.tileentity.TileEntityNuclearReactorElectric;
import ic2.core.ref.Ic2Blocks;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.LoggerFactory;

/** One opt-in pass through Jade's real server request and chamber overlay in both languages. */
@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public final class ReactorJadeClientSmoke {
  private static final BlockPos CORE = new BlockPos(0, 129, 2);
  private static final BlockPos ORPHAN = new BlockPos(3, 129, 1);
  private static int ticks;
  private static CompletableFuture<Void> reload;

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    if (!Boolean.getBoolean("ic2.reactorJadeSmoke")) return;
    var mc = Minecraft.getInstance();
    var server = mc.getSingleplayerServer();
    if (mc.player == null
        || mc.level == null
        || server == null
        || mc.getOverlay() != null
        || reload != null && !reload.isDone()) return;
    mc.options.pauseOnLostFocus = false;
    mc.options.hideGui = false;
    int phase = ++ticks;
    var id = mc.player.getUUID();
    if (phase == 1) {
      server.execute(
          () -> {
            var player = server.getPlayerList().getPlayer(id);
            var level = player.serverLevel();
            player.setGameMode(GameType.CREATIVE);
            player.getInventory().clearContent();
            player.getInventory().selected = 8;
            level.setDayTime(6000);
            level.setWeatherParameters(10000, 0, false, false);
            for (int x = -3; x <= 5; x++)
              for (int z = -2; z <= 6; z++) {
                level.setBlockAndUpdate(
                    new BlockPos(x, 127, z), Blocks.SMOOTH_STONE.defaultBlockState());
                for (int y = 128; y <= 132; y++) {
                  var pos = new BlockPos(x, y, z);
                  if (level.getBlockEntity(pos) instanceof net.minecraft.world.Container container)
                    container.clearContent();
                  level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
              }
            level.setBlockAndUpdate(CORE, Ic2Blocks.NUCLEAR_REACTOR.defaultBlockState());
            for (var side : Direction.values())
              level.setBlockAndUpdate(
                  CORE.relative(side), Ic2Blocks.REACTOR_CHAMBER.defaultBlockState());
            var reactor = (TileEntityNuclearReactorElectric) level.getBlockEntity(CORE);
            reactor.heat = 4032;
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            aim(player, CORE.north());
          });
      mc.options.languageCode = "ru_ru";
      mc.getLanguageManager().setSelected("ru_ru");
      reload = mc.reloadResourcePacks();
    }
    if (phase == 80 || phase == 150) {
      verify(mc, CORE.north(), true);
      capture(mc, "reactor-chamber-jade-" + mc.options.languageCode + ".png");
    }
    if (phase == 90) {
      mc.options.languageCode = "en_us";
      mc.getLanguageManager().setSelected("en_us");
      reload = mc.reloadResourcePacks();
    }
    if (phase == 160)
      server.execute(
          () -> {
            var player = server.getPlayerList().getPlayer(id);
            player.serverLevel().setBlock(ORPHAN, Ic2Blocks.REACTOR_CHAMBER.defaultBlockState(), 2);
            aim(player, ORPHAN);
          });
    if (phase == 220) {
      verify(mc, ORPHAN, false);
      capture(mc, "reactor-chamber-jade-orphan.png");
    }
    if (phase == 230) {
      LoggerFactory.getLogger("IC2R-Reactor-Jade")
          .info(
              "IC2R_REACTOR_JADE_CLIENT_PASS: real Jade server data through chamber in RU/EN; no status for standalone chamber");
      mc.stop();
    }
  }

  private static void aim(net.minecraft.server.level.ServerPlayer player, BlockPos target) {
    double x = target.getX() + .5, y = target.getY() - .3, z = target.getZ() - 3;
    var eye = new Vec3(x, y + player.getEyeHeight(), z);
    var delta = Vec3.atCenterOf(target).subtract(eye);
    float pitch =
        (float)
            -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)));
    player.connection.teleport(x, y, z, 0, pitch);
  }

  private static void verify(Minecraft mc, BlockPos target, boolean connected) {
    if (!(mc.hitResult instanceof BlockHitResult hit) || !hit.getBlockPos().equals(target))
      throw new IllegalStateException("Test camera is not aimed at the chamber: " + mc.hitResult);
    try {
      var center = Class.forName("snownee.jade.impl.ObjectDataCenter");
      var data = (CompoundTag) center.getMethod("getServerData").invoke(null);
      var status = data.getCompound("ic2_status");
      if (connected
          && (!status.contains("reactorHeat")
              || !status.contains("reactorMaxHeat")
              || !status.contains("reactorOutput")
              || !status.contains("active")))
        throw new IllegalStateException(
            "Jade did not receive complete reactor data through the chamber: " + data);
      if (!connected && data.contains("ic2_status"))
        throw new IllegalStateException(
            "Standalone chamber received unwanted reactor data: " + data);
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException("Jade must be installed for this opt-in client pass", ex);
    }
  }

  private static void capture(Minecraft mc, String name) {
    Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), text -> {});
  }

  private ReactorJadeClientSmoke() {}
}
