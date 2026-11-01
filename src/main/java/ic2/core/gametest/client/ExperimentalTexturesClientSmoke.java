package ic2.core.gametest.client;

import ic2.core.block.machine.gui.GuiScanner;
import ic2.core.block.machine.tileentity.TileEntityScanner;
import ic2.core.block.tileentity.Ic2TileEntityBlock;
import ic2.core.ref.Ic2Blocks;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.slf4j.LoggerFactory;

/** One opt-in pass over native baked faces and the localized Scanner at four GUI scales. */
@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public final class ExperimentalTexturesClientSmoke {
  private static final BlockPos SCANNER = new BlockPos(0, 129, -5);
  private static final List<BlockPos> DISPLAY = new ArrayList<>();
  private static CompletableFuture<Void> pending;
  private static int ticks, menuWait, menus;

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    if (!Boolean.getBoolean("ic2.experimentalTexturesSmoke")) return;
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
      mc.options.hideGui = true;
      mc.options.guiScale().set(2);
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
                for (int x = -10; x <= 10; x++)
                  for (int z = -8; z <= 5; z++) {
                    level.setBlockAndUpdate(
                        new BlockPos(x, 127, z), Blocks.SMOOTH_STONE.defaultBlockState());
                    for (int y = 128; y <= 133; y++)
                      level.setBlockAndUpdate(
                          new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                  }
                Block[] nativeBlocks = {
                  Ic2Blocks.MACERATOR,
                  Ic2Blocks.COMPRESSOR,
                  Ic2Blocks.EXTRACTOR,
                  Ic2Blocks.RECYCLER,
                  Ic2Blocks.METAL_FORMER,
                  Ic2Blocks.METAL_FORMER,
                  Ic2Blocks.METAL_FORMER,
                  Ic2Blocks.ORE_WASHING_PLANT
                };
                String[] advanced = {
                  "rotary_macerator",
                  "singularity_compressor",
                  "centrifuge_extractor",
                  "compacting_recycler",
                  "liquescent_extruder",
                  "impellerized_roller",
                  "water_jet_cutter",
                  "thermal_washer"
                };
                for (int i = 0; i < nativeBlocks.length; i++) {
                  var top = new BlockPos(-7 + i * 2, 130, 2);
                  level.setBlockAndUpdate(top, north(nativeBlocks[i].defaultBlockState()));
                  DISPLAY.add(top);
                  var idAdvanced =
                      ResourceLocation.fromNamespaceAndPath("ic2r_addons", advanced[i]);
                  if (!BuiltInRegistries.BLOCK.containsKey(idAdvanced))
                    throw new IllegalStateException("Supply Addons JAR for visual comparison");
                  level.setBlockAndUpdate(
                      top.below(),
                      north(BuiltInRegistries.BLOCK.get(idAdvanced).defaultBlockState()));
                  DISPLAY.add(top.below());
                }
                level.setBlockAndUpdate(SCANNER, north(Ic2Blocks.UU_SCANNER.defaultBlockState()));
                player.connection.teleport(.5, 128.8, -10, 0, 2);
              },
              server);
      language(mc, "ru_ru");
    }
    if (phase == 50) verifyModels(mc);
    if (phase == 55) capture(mc, "experimental-machines-32-comparison.png");
    if (phase == 60 || phase == 180) {
      mc.options.hideGui = false;
      var id = mc.player.getUUID();
      pending =
          CompletableFuture.runAsync(
              () -> {
                var player = server.getPlayerList().getPlayer(id);
                ((TileEntityScanner) player.serverLevel().getBlockEntity(SCANNER))
                    .openManagedBe(player, InteractionHand.MAIN_HAND);
              },
              server);
    }
    for (int i = 0; i < 4; i++) {
      if (phase == 80 + i * 15 || phase == 200 + i * 15) {
        mc.options.guiScale().set(i + 1);
        mc.resizeDisplay();
      }
      if (phase == 88 + i * 15 || phase == 208 + i * 15) {
        if (!(mc.screen instanceof GuiScanner gui)) {
          if (++menuWait < 100) {
            ticks--;
            return;
          }
          throw new IllegalStateException("Scanner menu did not open");
        }
        menuWait = 0;
        int leftWidth = mc.font.width(gui.getTitle());
        int rightWidth = mc.font.width(Component.translatable("ic2.Scanner.gui.info5"));
        if (57 - leftWidth / 2 < 14
            || 57 + (leftWidth + 1) / 2 > 100
            || 134 - rightWidth / 2 < 103
            || 134 + (rightWidth + 1) / 2 > 168)
          throw new IllegalStateException(
              "Scanner headings exceed their separate bounds in " + mc.options.languageCode);
        capture(mc, "scanner-header-" + mc.options.languageCode + "-scale-" + (i + 1) + ".png");
        menus++;
      }
    }
    if (phase == 140) {
      mc.player.closeContainer();
      language(mc, "en_us");
    }
    if (phase == 265) {
      mc.player.closeContainer();
      mc.options.guiScale().set(2);
      mc.resizeDisplay();
      LoggerFactory.getLogger("IC2R-Experimental-Textures")
          .info(
              "IC2R_EXPERIMENTAL_TEXTURES_CLIENT_PASS: {} Scanner captures in RU/EN at GUI scales 1-4; 16 native/Addons models, active/inactive, four orientations",
              menus);
      mc.stop();
    }
  }

  private static BlockState north(BlockState state) {
    if (state.hasProperty(Ic2TileEntityBlock.anyFacingProperty))
      return state.setValue(Ic2TileEntityBlock.anyFacingProperty, Direction.NORTH);
    if (state.hasProperty(Ic2TileEntityBlock.horizontalFacingProperty))
      return state.setValue(Ic2TileEntityBlock.horizontalFacingProperty, Direction.NORTH);
    return state;
  }

  private static void verifyModels(Minecraft mc) {
    int quads = 0;
    var random = RandomSource.create(0);
    for (var pos : DISPLAY) {
      var base = mc.level.getBlockState(pos);
      for (boolean active : new boolean[] {false, true}) {
        var state =
            base.hasProperty(Ic2TileEntityBlock.ACTIVE)
                ? base.setValue(Ic2TileEntityBlock.ACTIVE, active)
                : base;
        for (int rotation = 0; rotation < 4; rotation++) {
          var model = mc.getBlockRenderer().getBlockModel(state);
          if (model == mc.getModelManager().getMissingModel())
            throw new IllegalStateException("Missing machine model: " + state);
          for (int face = 0; face < 7; face++) {
            for (var quad :
                model.getQuads(
                    state,
                    face == 6 ? null : Direction.values()[face],
                    random,
                    ModelData.EMPTY,
                    null)) {
              var sprite = quad.getSprite().contents();
              if (sprite.width() != 32 || sprite.height() != 32)
                throw new IllegalStateException(
                    "Expected original 32x32 face: " + state + " / " + sprite.name());
              quads++;
            }
          }
          state = state.rotate(Rotation.CLOCKWISE_90);
        }
      }
    }
    if (quads < DISPLAY.size() * 6 * 8)
      throw new IllegalStateException("Machine faces were not fully checked: " + quads);
    LoggerFactory.getLogger("IC2R-Experimental-Textures")
        .info("IC2R_EXPERIMENTAL_TEXTURES_MODELS_PASS: {} original 32x32 baked quads", quads);
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

  private ExperimentalTexturesClientSmoke() {}
}
