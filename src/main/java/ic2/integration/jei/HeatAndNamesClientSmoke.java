package ic2.integration.jei;

import com.google.gson.JsonParser;
import ic2.core.block.comp.Energy;
import ic2.core.block.heatgenerator.gui.GuiElectricHeatGenerator;
import ic2.core.block.heatgenerator.tileentity.TileEntityElectricHeatGenerator;
import ic2.core.block.machine.tileentity.TileEntityBlastFurnace;
import ic2.core.block.tileentity.Ic2TileEntityBlock;
import ic2.core.ref.Ic2Blocks;
import ic2.core.ref.Ic2Items;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.LoggerFactory;

/** Opt-in client pass; registered only by JEI when the development property is enabled. */
final class HeatAndNamesClientSmoke {
  private static final BlockPos HEATER = new BlockPos(0, 129, 2);
  private static IJeiRuntime runtime;
  private static boolean registered;
  private static int ticks;
  private static int guiWaitTicks;
  private static CompletableFuture<Void> reload;

  static void start(IJeiRuntime value) {
    runtime = value;
    if (!registered) {
      registered = true;
      NeoForge.EVENT_BUS.addListener(HeatAndNamesClientSmoke::tick);
    }
  }

  private static void tick(ClientTickEvent.Post event) {
    var mc = Minecraft.getInstance();
    var server = mc.getSingleplayerServer();
    if (mc.player == null
        || mc.level == null
        || server == null
        || mc.getOverlay() != null
        || reload != null && !reload.isDone()) return;
    mc.options.pauseOnLostFocus = false;
    int phase = ++ticks;
    var id = mc.player.getUUID();
    if (phase == 1) {
      server.execute(
          () -> {
            var player = server.getPlayerList().getPlayer(id);
            var level = player.serverLevel();
            player.setGameMode(GameType.CREATIVE);
            for (int x = -2; x <= 2; x++)
              for (int z = -2; z <= 5; z++)
                for (int y = 128; y <= 132; y++)
                  level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(
                HEATER,
                Ic2Blocks.ELECTRIC_HEAT_GENERATOR
                    .defaultBlockState()
                    .setValue(Ic2TileEntityBlock.anyFacingProperty, Direction.SOUTH));
            level.setBlockAndUpdate(
                HEATER.south(),
                Ic2Blocks.BLAST_FURNACE
                    .defaultBlockState()
                    .setValue(Ic2TileEntityBlock.anyFacingProperty, Direction.NORTH));
            var heater = (TileEntityElectricHeatGenerator) level.getBlockEntity(HEATER);
            heater.coilSlot.put(0, new ItemStack(Ic2Items.COIL));
            heater.getComponent(Energy.class).addEnergy(10000);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.connection.teleport(.5, 127.9, -1, 0, 0);
          });
      language(mc, "ru_ru");
    }
    if (phase == 50 || phase == 210) {
      checkAliases();
      checkJade(0);
      open(mc);
    }
    if (phase == 65 || phase == 115 || phase == 225) {
      int expected = phase == 115 ? 10 : 0;
      if (!(mc.screen instanceof GuiElectricHeatGenerator gui)
          || gui.getMenu().base.gettransmitHeat() != expected) {
        if (++guiWaitTicks < 100) {
          ticks--;
          return;
        }
        throw new IllegalStateException(
            "Heat generator GUI did not synchronize "
                + expected
                + " at phase "
                + phase
                + ": screen="
                + mc.screen
                + ", value="
                + (mc.screen instanceof GuiElectricHeatGenerator current
                    ? current.getMenu().base.gettransmitHeat()
                    : "no heat screen"));
      }
      guiWaitTicks = 0;
      for (int amount : new int[] {0, 10, 100}) {
        var label = Component.translatable("ic2.ElectricHeatGenerator.gui.hUmax", amount, 100);
        if (mc.font.width(label) > 109)
          throw new IllegalStateException("Heat output exceeds LCD width");
      }
      Screenshot.grab(
          mc.gameDirectory,
          "heat-generator-" + mc.options.languageCode + "-" + expected + ".png",
          mc.getMainRenderTarget(),
          text -> {});
    }
    if (phase == 70 || phase == 120 || phase == 230) mc.player.closeContainer();
    if (phase == 80 || phase == 120)
      server.execute(
          () -> {
            var furnace =
                (TileEntityBlastFurnace)
                    server
                        .getPlayerList()
                        .getPlayer(id)
                        .serverLevel()
                        .getBlockEntity(HEATER.south());
            furnace.setManualWarmup(phase == 80);
          });
    if (phase == 100) {
      checkJade(10);
      open(mc);
    }
    if (phase == 150) {
      checkJade(0);
      Screenshot.grab(
          mc.gameDirectory, "heat-generator-jade-idle.png", mc.getMainRenderTarget(), text -> {});
      language(mc, "en_us");
    }
    if (phase == 240) {
      LoggerFactory.getLogger("IC2R-Heat-And-Names")
          .info(
              "IC2R_HEAT_NAMES_CLIENT_PASS: RU/EN localized JEI aliases; real GUI and Jade heat 0/10/0; maximum LCD width");
      mc.stop();
    }
  }

  private static void language(Minecraft mc, String language) {
    mc.options.languageCode = language;
    mc.getLanguageManager().setSelected(language);
    reload = mc.reloadResourcePacks();
  }

  private static void open(Minecraft mc) {
    var id = mc.player.getUUID();
    var server = mc.getSingleplayerServer();
    server.execute(
        () -> {
          var player = server.getPlayerList().getPlayer(id);
          boolean opened =
              ((TileEntityElectricHeatGenerator) player.serverLevel().getBlockEntity(HEATER))
                  .openManagedBe(player, InteractionHand.MAIN_HAND);
          LoggerFactory.getLogger("IC2R-Heat-And-Names")
              .info(
                  "Heat GUI open: {}, player distance: {}",
                  opened,
                  player.distanceToSqr(HEATER.getX() + .5, HEATER.getY() + .5, HEATER.getZ() + .5));
        });
  }

  private static void checkAliases() {
    try (var stream =
        HeatAndNamesClientSmoke.class.getResourceAsStream("/assets/ic2/search_aliases.json")) {
      var aliases =
          JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
              .getAsJsonObject();
      var filter = runtime.getIngredientFilter();
      for (var entry : aliases.entrySet())
        for (var key : entry.getValue().getAsJsonArray()) {
          var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.getKey()));
          String query = Component.translatable(key.getAsString()).getString();
          filter.setFilterText(query);
          if (filter.getFilteredIngredients(VanillaTypes.ITEM_STACK).stream()
              .noneMatch(stack -> stack.is(item)))
            throw new IllegalStateException("JEI did not find " + entry.getKey() + " by " + query);
        }
      filter.setFilterText("");
    } catch (java.io.IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static void checkJade(int expected) {
    try {
      var center = Class.forName("snownee.jade.impl.ObjectDataCenter");
      var data = (CompoundTag) center.getMethod("getServerData").invoke(null);
      var status = data.getCompound("ic2_status");
      if (!status.contains("heatOutput")
          || status.getInt("heatOutput") != expected
          || status.getInt("maxHeatOutput") != 10)
        throw new IllegalStateException("Incorrect Jade heat output: " + data);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Jade is required for this opt-in pass", e);
    }
  }

  private HeatAndNamesClientSmoke() {}
}
