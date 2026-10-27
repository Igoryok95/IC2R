package ic2.core.gametest.client;

import ic2.core.experimental.ExperimentalPipes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.LoggerFactory;

/** Opt-in checks through the registered item colors and native inventory/held-item renderer. */
@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public final class PipeItemClientSmoke {
  private static int ticks;

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    if (!Boolean.getBoolean("ic2.pipeItemSmoke")) return;
    var mc = Minecraft.getInstance();
    var server = mc.getSingleplayerServer();
    if (mc.player == null || mc.level == null || server == null || mc.getOverlay() != null) return;
    mc.options.pauseOnLostFocus = false;
    mc.options.hideGui = false;
    mc.options.fov().set(70);
    int phase = ++ticks;
    var id = mc.player.getUUID();
    if (phase == 1)
      server.execute(
          () -> {
            var player = server.getPlayerList().getPlayer(id);
            player.setGameMode(GameType.CREATIVE);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.getInventory().clearContent();
            int index = 0;
            for (var block : ExperimentalPipes.BLOCKS.values())
              player.getInventory().setItem(index++, new ItemStack(block));
            player.serverLevel().setDayTime(6000);
            player.serverLevel().setWeatherParameters(10000, 0, false, false);
            player.connection.teleport(12.5, 129, -5.5, 0, 8);
          });
    if (phase == 60) {
      int index = 0;
      for (var entry : ExperimentalPipes.BLOCKS.entrySet()) {
        var stack = mc.player.getInventory().getItem(index++);
        if (!stack.is(entry.getValue().asItem()))
          throw new IllegalStateException("Pipe inventory did not synchronize: " + entry.getKey());
        int color = mc.getItemColors().getColor(stack, 0);
        if ((color >>> 24) != 255 || (color & 0xFFFFFF) != entry.getValue().color)
          throw new IllegalStateException(
              "Pipe item tint must be opaque without changing material RGB: " + entry.getKey());
        var model = mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0);
        if (model == mc.getModelManager().getMissingModel()
            || model.getRenderPasses(stack, false).size() != 2)
          throw new IllegalStateException(
              "Pipe item must keep both frame and glazing passes: " + entry.getKey());
      }
      mc.player.getInventory().selected = 2;
      mc.setScreen(new InventoryScreen(mc.player));
    }
    if (phase == 80) capture(mc, "pipe-items-inventory.png");
    if (phase == 90) mc.setScreen(null);
    if (phase == 115) {
      capture(mc, "pipe-item-bronze-hand.png");
      mc.player.getInventory().selected = 6;
    }
    if (phase == 145) capture(mc, "pipe-item-steel-hand.png");
    if (phase == 160) {
      LoggerFactory.getLogger("IC2R-Pipe-Items")
          .info(
              "IC2R_PIPE_ITEM_CLIENT_PASS: eight registered item tints are opaque; native inventory and bronze/steel held-item views captured");
      mc.stop();
    }
  }

  private static void capture(Minecraft mc, String name) {
    Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), text -> {});
  }

  private PipeItemClientSmoke() {}
}
