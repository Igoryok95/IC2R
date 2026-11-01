package ic2.core.gametest.client;

import ic2.core.Ic2Gui;
import ic2.core.block.comp.Energy;
import ic2.core.block.machine.tileentity.TileEntityAirCompressor;
import ic2.core.block.tileentity.Ic2TileEntityBlock;
import ic2.core.fluid.Ic2FluidStack;
import ic2.core.ref.Ic2Blocks;
import ic2.core.ref.Ic2Fluids;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.LoggerFactory;

/** One combined opt-in client pass for the native GUI, localization and actual world model. */
@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public final class AirCompressorClientSmoke {
  private static final BlockPos POS = new BlockPos(0, 128, 0);
  private static CompletableFuture<Void> pending;
  private static int ticks, menuWait;

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    if (!Boolean.getBoolean("ic2.airCompressorSmoke")) return;
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
    var id = mc.player.getUUID();
    if (phase == 1) {
      mc.options.hideGui = true;
      mc.options.guiScale().set(2);
      mc.resizeDisplay();
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
                for (int x = -3; x <= 5; x++)
                  for (int z = -3; z <= 3; z++) {
                    level.setBlockAndUpdate(
                        new BlockPos(x, 127, z), Blocks.SMOOTH_STONE.defaultBlockState());
                    for (int y = 128; y <= 132; y++)
                      level.setBlockAndUpdate(
                          new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                  }
                level.setBlockAndUpdate(
                    POS,
                    Ic2Blocks.AIR_COMPRESSOR
                        .defaultBlockState()
                        .setValue(Ic2TileEntityBlock.horizontalFacingProperty, Direction.NORTH));
                level.setBlockAndUpdate(
                    POS.east(2),
                    Ic2Blocks.COMPRESSOR
                        .defaultBlockState()
                        .setValue(Ic2TileEntityBlock.horizontalFacingProperty, Direction.NORTH));
                var tile = (TileEntityAirCompressor) level.getBlockEntity(POS);
                tile.enabled = true;
                tile.receiver.fillMbUnchecked(
                    Ic2FluidStack.create(Ic2Fluids.AIR.still(), 12800), false);
                tile.setChanged();
                tile.getComponent(Energy.class).addEnergy(1200);
                player.connection.teleport(1.6, 128, -2.7, 20, 19);
              },
              server);
      language(mc, "ru_ru");
    }
    if (phase == 40) capture(mc, "air-compressor-world.png");
    if (phase == 45 || phase == 110) {
      mc.options.hideGui = false;
      pending =
          CompletableFuture.runAsync(
              () -> {
                var player = server.getPlayerList().getPlayer(id);
                ((TileEntityAirCompressor) player.serverLevel().getBlockEntity(POS))
                    .openManagedBe(player, InteractionHand.MAIN_HAND);
              },
              server);
    }
    if (phase == 65 || phase == 130) {
      if (!(mc.screen instanceof Ic2Gui<?> gui)) {
        if (++menuWait < 100) {
          ticks--;
          return;
        }
        throw new IllegalStateException("Air compressor GUI did not open");
      }
      if (gui.getXSize() != 212 || gui.getYSize() != 190 || gui.getMenu().slots.size() != 41)
        throw new IllegalStateException("Unexpected air compressor layout");
      for (var key :
          new String[] {
            "ic2.air_compressor.gui.consumption",
            "ic2.air_compressor.gui.production",
            "ic2.air_compressor.gui.delivery"
          }) {
        var text = Component.translatable(key, "9999.99");
        if (mc.font.width(text) > 140)
          throw new IllegalStateException("Reading exceeds reserved bounds: " + key);
      }
      if (mc.font.width(gui.getTitle()) > 176)
        throw new IllegalStateException("Air compressor title exceeds header");
      capture(mc, "air-compressor-" + mc.options.languageCode + ".png");
    }
    if (phase == 70 && mc.screen instanceof Ic2Gui<?> gui) {
      gui.mouseClicked(gui.getX() + 197, gui.getY() + 28, 0);
    }
    if (phase == 77) {
      if (((TileEntityAirCompressor) mc.level.getBlockEntity(POS)).enabled)
        throw new IllegalStateException("Native torch button did not synchronize its off state");
    }
    if (phase == 80) {
      mc.player.closeContainer();
      language(mc, "en_us");
    }
    if (phase == 145) {
      mc.player.closeContainer();
      pending =
          CompletableFuture.runAsync(
              () -> {
                var player = server.getPlayerList().getPlayer(id);
                player.connection.teleport(.5, 128, -2, 0, 24);
              },
              server);
    }
    if (phase == 170) capture(mc, "air-compressor-jade-en_us.png");
    if (phase == 175) {
      LoggerFactory.getLogger("IC2R-Air-Compressor")
          .info(
              "IC2R_AIR_COMPRESSOR_CLIENT_PASS: native horizontal GUI and 41 slots in RU/EN; bounded readings and title; world model capture");
      mc.stop();
    }
  }

  private static void language(Minecraft mc, String language) {
    mc.options.languageCode = language;
    mc.getLanguageManager().setSelected(language);
    var previous = pending;
    pending =
        previous == null
            ? mc.reloadResourcePacks()
            : previous.thenCompose(
                ignored -> mc.submit(mc::reloadResourcePacks).thenCompose(value -> value));
  }

  private static void capture(Minecraft mc, String name) {
    Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), text -> {});
  }
}
