package ic2.core.gametest.client;

import ic2.core.IHasGui;
import ic2.core.Ic2Gui;
import ic2.core.block.comp.Energy;
import ic2.core.block.machine.tileentity.TileEntityElectrolyzer;
import ic2.core.block.tileentity.Ic2TileEntity;
import ic2.core.classic.ClassicEnergy;
import ic2.core.classic.ClassicMachines;
import ic2.core.classic.ClassicProcessMachine;
import ic2.core.classic.ClassicResources;
import ic2.core.classic.ClassicWiring;
import ic2.core.ref.Ic2Blocks;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.LoggerFactory;

/** Opt-in visual checks in a dedicated development world, with saved screenshots. */
@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public final class ClassicContentClientSmoke {
  private static int ticks;
  private static volatile List<BlockPos> positions;
  private static final String[] NAMES = {
    "rare-earth", "rare-earth-centrifuge", "alloy", "electrolyzer", "pesu", "iv-transformer"
  };

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    if (!Boolean.getBoolean("ic2.classicSmoke")) return;
    var mc = Minecraft.getInstance();
    if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
    mc.options.pauseOnLostFocus = false;
    var playerId = mc.player.getUUID();
    if (ticks++ == 0) {
      mc.getSingleplayerServer()
          .execute(
              () -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);
                var level = player.serverLevel();
                var base = new BlockPos(0, 128, 0);
                player.setGameMode(GameType.CREATIVE);
                player.getInventory().clearContent();
                level.setDayTime(6000);
                level.setWeatherParameters(10000, 0, false, false);
                level
                    .getEntitiesOfClass(
                        net.minecraft.world.entity.item.ItemEntity.class,
                        new net.minecraft.world.phys.AABB(
                            net.minecraft.world.phys.Vec3.atLowerCornerOf(base.offset(-3, -1, -3)),
                            net.minecraft.world.phys.Vec3.atLowerCornerOf(base.offset(14, 4, 5))))
                    .forEach(net.minecraft.world.entity.Entity::discard);
                var blocks =
                    List.of(
                        ClassicMachines.RARE_EARTH_EXTRACTOR,
                        ClassicMachines.RARE_EARTH_CENTRIFUGE,
                        ClassicMachines.ALLOY_SMELTER,
                        Ic2Blocks.ELECTROLYZER,
                        ClassicEnergy.PESU,
                        ClassicWiring.IV_TRANSFORMER);
                var spots = new java.util.ArrayList<BlockPos>();
                for (int x = -3; x <= 13; x++)
                  for (int z = -3; z <= 4; z++) {
                    level.setBlockAndUpdate(
                        base.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
                    for (int y = 0; y < 3; y++) {
                      var pos = base.offset(x, y, z);
                      if (level.getBlockEntity(pos)
                          instanceof net.minecraft.world.Container inventory)
                        inventory.clearContent();
                      level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    }
                  }
                for (int i = 0; i < blocks.size(); i++) {
                  var pos = base.offset(i * 2, 0, 2);
                  spots.add(pos);
                  level.setBlockAndUpdate(pos, blocks.get(i).defaultBlockState());
                  var tile = (Ic2TileEntity) level.getBlockEntity(pos);
                  tile.getComponent(Energy.class).addEnergy(Double.MAX_VALUE);
                  if (tile instanceof ClassicProcessMachine processor) {
                    processor.input.put(
                        0,
                        new ItemStack(
                            i == 2 ? ClassicResources.ALUMINIUM_DUST : Items.CLAY_BALL, 64));
                    if (i == 2) processor.input.put(1, new ItemStack(Items.SAND, 64));
                    if (i == 1) processor.speed = 6000;
                  }
                  if (tile instanceof TileEntityElectrolyzer electrolyzer) {
                    electrolyzer.classicInput.put(new ItemStack(ClassicResources.DEAD_MAGNET, 64));
                  }
                }
                positions = List.copyOf(spots);
              });
    }
    if (positions == null) return;
    if (ticks % 10 == 0)
      mc.getSingleplayerServer()
          .execute(
              () -> {
                for (var pos : positions) {
                  if (pos.equals(positions.get(4)) && ticks >= 80 + 4 * 140 && ticks < 80 + 5 * 140)
                    continue;
                  var tile =
                      (Ic2TileEntity) mc.getSingleplayerServer().overworld().getBlockEntity(pos);
                  tile.getComponent(Energy.class).addEnergy(Double.MAX_VALUE);
                }
              });
    if (ticks >= 80 && ticks < 80 + NAMES.length * 140 && (ticks - 80) % 140 == 0) {
      int index = (ticks - 80) / 140;
      mc.getSingleplayerServer()
          .execute(
              () -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);
                var pos = positions.get(index);
                if (index == 4) {
                  var energy =
                      ((Ic2TileEntity) player.serverLevel().getBlockEntity(pos))
                          .getComponent(Energy.class);
                  energy.forceAddEnergy(1_250_000_000D - energy.getEnergy());
                }
                player.closeContainer();
                player.connection.teleport(pos.getX() + .5, pos.getY() + .1, pos.getZ() - 2, 0, 12);
                ((IHasGui) player.serverLevel().getBlockEntity(pos))
                    .openManagedBe(player, InteractionHand.MAIN_HAND);
              });
    }
    if (ticks >= 150 && ticks < 150 + NAMES.length * 140 && (ticks - 150) % 140 == 0) {
      int index = (ticks - 150) / 140;
      if (!(mc.screen instanceof Ic2Gui<?> gui))
        throw new IllegalStateException("Expected Classic content screen " + NAMES[index]);
      if (gui.getTitle().getString().startsWith("container.ic2.")
          || mc.font.width(gui.getTitle()) > gui.getXSize() - 12)
        throw new IllegalStateException(
            "Untranslated or oversized Classic screen title: " + gui.getTitle().getString());
      for (var slot : gui.getMenu().slots) {
        if (slot.x < 0
            || slot.y < 0
            || slot.x + 16 > gui.getXSize()
            || slot.y + 16 > gui.getYSize())
          throw new IllegalStateException("Slot outside Classic content screen " + NAMES[index]);
      }
      if (gui instanceof ic2.core.block.wiring.GuiElectricBlock storage) {
        var energy = storage.getMenu().base.energy;
        double previous = energy.getEnergy();
        for (double charge :
            new double[] {
              0, 1, 999, 999999, 99999999, 100000000, 1250000000, 3999999999D, 4000000000D
            }) {
          energy.forceAddEnergy(charge - energy.getEnergy());
          var lines = storage.getReadingLines();
          for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i);
            if (line.getString().startsWith("gui.ic2.") || mc.font.width(line) > 90)
              throw new IllegalStateException(
                  "Storage reading does not fit or is not localized: " + line.getString());
          }
          String tooltip = storage.getEnergyTooltip().getString();
          if (!tooltip.replace(" ", "").equals((long) charge + "/4000000000EU"))
            throw new IllegalStateException("Storage tooltip lost exact energy: " + tooltip);
          LoggerFactory.getLogger("IC2R-CLASSIC")
              .info("IC2R_STORAGE_READING charge={} text={}", charge, tooltip);
        }
        energy.forceAddEnergy(previous - energy.getEnergy());
      }
      Screenshot.grab(
          mc.gameDirectory,
          "classic-" + NAMES[index] + "-" + mc.options.languageCode + ".png",
          mc.getMainRenderTarget(),
          message -> {});
      LoggerFactory.getLogger("IC2R-CLASSIC")
          .info("IC2R_CLASSIC_GUI_CAPTURE {} language={}", NAMES[index], mc.options.languageCode);
    }
    if (ticks >= 149 && ticks < 149 + NAMES.length * 140 && (ticks - 149) % 140 == 0) {
      org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), 4, 4);
    }
    if (ticks >= 169 + 4 * 140 && ticks <= 175 + 4 * 140 && mc.screen instanceof Ic2Gui<?> gui) {
      try {
        var move = net.minecraft.client.MouseHandler.class.getDeclaredMethod("onMove", long.class, double.class, double.class);
        move.setAccessible(true);
        move.invoke(mc.mouseHandler, mc.getWindow().getWindow(),
            (gui.getGuiLeft() + 90D) * mc.getWindow().getScreenWidth() / mc.getWindow().getGuiScaledWidth(),
            (gui.getGuiTop() + 42D) * mc.getWindow().getScreenHeight() / mc.getWindow().getGuiScaledHeight());
      } catch (ReflectiveOperationException error) {
        throw new IllegalStateException("Cannot hover storage gauge in the client check", error);
      }
    }
    if (ticks == 175 + 4 * 140) {
      Screenshot.grab(
          mc.gameDirectory,
          "classic-pesu-tooltip-" + mc.options.languageCode + ".png",
          mc.getMainRenderTarget(),
          message -> {});
    }
    if (ticks == 80 + NAMES.length * 140 + 80) {
      LoggerFactory.getLogger("IC2R-CLASSIC")
          .info(
              "IC2R_CLASSIC_CLIENT_PASS language={} screens={}",
              mc.options.languageCode,
              NAMES.length);
      mc.stop();
    }
  }
}
