package ic2.core.gametest.client;

import ic2.core.block.transport.SteelHopperBlockEntity;
import ic2.core.ref.Ic2Blocks;
import ic2.core.ref.Ic2Items;
import java.util.concurrent.CompletableFuture;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.LoggerFactory;

/** One opt-in pass over the new block, native menu, localized item names and rarity. */
@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public final class SteelHopperClientSmoke {
  private static final BlockPos STEEL = new BlockPos(-1, 129, 2);
  private static int ticks, checkedMenus, checkedTooltips;
  private static CompletableFuture<Void> englishReload;

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    if (!Boolean.getBoolean("ic2.steelHopperSmoke")) return;
    var mc = Minecraft.getInstance();
    var server = mc.getSingleplayerServer();
    if (mc.player == null || mc.level == null || server == null) return;
    if (englishReload != null && (!englishReload.isDone() || mc.getOverlay() != null)) return;
    mc.options.pauseOnLostFocus = false;
    int phase = ++ticks;
    var id = mc.player.getUUID();
    if (phase == 1) {
      server.execute(
          () -> {
            var player = server.getPlayerList().getPlayer(id);
            var level = player.serverLevel();
            player.setGameMode(GameType.CREATIVE);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.getInventory().clearContent();
            player.getInventory().setItem(0, new ItemStack(Ic2Items.STEEL_HOPPER));
            player.getInventory().setItem(1, new ItemStack(Items.HOPPER));
            player.getInventory().selected = 8;
            level.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS).set(false, server);
            level.setDayTime(6000);
            level.setWeatherParameters(10000, 0, false, false);
            for (int x = -5; x <= 7; x++) {
              for (int z = -4; z <= 8; z++) {
                level.setBlockAndUpdate(
                    new BlockPos(x, 127, z), Blocks.SMOOTH_STONE.defaultBlockState());
                for (int y = 128; y <= 134; y++) {
                  var pos = new BlockPos(x, y, z);
                  if (level.getBlockEntity(pos) instanceof net.minecraft.world.Container container)
                    container.clearContent();
                  level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
              }
            }
            for (var pos : new BlockPos[] {STEEL, new BlockPos(2, 129, 2)}) {
              level.setBlockAndUpdate(pos.below(), Blocks.CHEST.defaultBlockState());
              level.setBlockAndUpdate(
                  pos,
                  (pos.equals(STEEL) ? Ic2Blocks.STEEL_HOPPER : Blocks.HOPPER)
                      .defaultBlockState()
                      .setValue(HopperBlock.FACING, Direction.DOWN));
            }
            player.connection.teleport(1, 129, 0, 0, 30);
          });
    }
    if (phase == 35) mc.options.hideGui = true;
    if (phase == 40) capture(mc, "steel-hoppers-world-" + mc.options.languageCode + ".png");
    if (phase == 45) {
      mc.options.hideGui = false;
      if (Boolean.getBoolean("ic2.steelHopperWorldOnly")) {
        LoggerFactory.getLogger("IC2R-Steel-Hopper").info("IC2R_STEEL_HOPPER_WORLD_CAPTURE_PASS");
        mc.stop();
      }
    }
    if (phase == 60 || phase == 140) {
      server.execute(
          () -> {
            var player = server.getPlayerList().getPlayer(id);
            var level = player.serverLevel();
            level.setBlockAndUpdate(STEEL.south(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            var hopper = (SteelHopperBlockEntity) level.getBlockEntity(STEEL);
            var items =
                new ItemStack[] {
                  new ItemStack(Ic2Items.STEEL_INGOT, 5),
                  new ItemStack(Items.IRON_INGOT, 17),
                  new ItemStack(Items.COPPER_INGOT, 23),
                  new ItemStack(Items.COAL, 31),
                  new ItemStack(Ic2Items.IRON_DUST, 42)
                };
            for (int i = 0; i < items.length; i++) hopper.setItem(i, items[i]);
            hopper.setChanged();
            player.openMenu(hopper);
          });
    }
    if (phase == 85 || phase == 165) {
      var gui = menu(mc);
      String expected = net.minecraft.client.resources.language.I18n.get("block.ic2.steel_hopper");
      if (!gui.getTitle().getString().equals(expected) || gui.getMenu().slots.size() != 41)
        throw new IllegalStateException("Steel hopper title or native menu slots are invalid");
      for (int slot = 0; slot < 5; slot++)
        if (gui.getMenu().getSlot(slot).getItem().isEmpty())
          throw new IllegalStateException("Missing synchronized hopper slot " + slot);
      pointer(mc, -1000, -1000);
      checkedMenus++;
    }
    if (phase == 90 || phase == 170)
      capture(mc, "steel-hopper-gui-" + mc.options.languageCode + ".png");
    if (phase == 110 || phase == 190) {
      var gui = menu(mc);
      var slot = gui.getMenu().getSlot(32);
      var stack = slot.getItem();
      var color = stack.getDisplayName().getStyle().getColor();
      if (!stack.is(Ic2Items.STEEL_HOPPER)
          || color == null
          || color.getValue() != ChatFormatting.YELLOW.getColor())
        throw new IllegalStateException("Steel hopper item title must be yellow");
      pointer(mc, gui.getGuiLeft() + slot.x + 8, gui.getGuiTop() + slot.y + 8);
      checkedTooltips++;
    }
    if (phase == 115 || phase == 195)
      capture(mc, "steel-hopper-tooltip-" + mc.options.languageCode + ".png");
    if (phase == 130) {
      mc.player.closeContainer();
      mc.options.languageCode = "en_us";
      mc.getLanguageManager().setSelected("en_us");
      englishReload = mc.reloadResourcePacks();
    }
    if (phase == 210) {
      mc.player.closeContainer();
      server.execute(
          () ->
              server
                  .getPlayerList()
                  .getPlayer(id)
                  .serverLevel()
                  .setBlockAndUpdate(STEEL.south(), Blocks.AIR.defaultBlockState()));
      mc.player.getInventory().selected = 0;
    }
    if (phase == 220) capture(mc, "steel-hopper-hand-en_us.png");
    if (phase == 225) mc.options.hideGui = true;
    if (phase == 230) capture(mc, "steel-hoppers-world-en_us.png");
    if (phase == 250) {
      if (checkedMenus != 2 || checkedTooltips != 2)
        throw new IllegalStateException("Incomplete steel hopper client pass");
      mc.options.hideGui = false;
      LoggerFactory.getLogger("IC2R-Steel-Hopper")
          .info(
              "IC2R_STEEL_HOPPER_CLIENT_PASS: native menu, five slots, EN/RU and yellow rarity checked");
      mc.stop();
    }
  }

  private static HopperScreen menu(Minecraft mc) {
    if (!(mc.screen instanceof HopperScreen gui))
      throw new IllegalStateException("Expected the native hopper GUI");
    return gui;
  }

  private static void capture(Minecraft mc, String name) {
    Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), text -> {});
  }

  private static void pointer(Minecraft mc, double x, double y) {
    try {
      var mouse = mc.mouseHandler;
      var xField = mouse.getClass().getDeclaredField("xpos");
      var yField = mouse.getClass().getDeclaredField("ypos");
      xField.setAccessible(true);
      yField.setAccessible(true);
      xField.setDouble(
          mouse, x * mc.getWindow().getScreenWidth() / mc.getWindow().getGuiScaledWidth());
      yField.setDouble(
          mouse, y * mc.getWindow().getScreenHeight() / mc.getWindow().getGuiScaledHeight());
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException("Cannot position the isolated test pointer", ex);
    }
  }
}
