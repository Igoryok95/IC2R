package ic2.core.gametest.client;

import ic2.core.IHasGui;
import ic2.core.Ic2Gui;
import ic2.core.block.machine.tileentity.TileEntityElectricFurnace;
import ic2.core.block.machine.tileentity.TileEntityInduction;
import ic2.core.block.machine.tileentity.TileEntityIronFurnace;
import ic2.core.ref.Ic2Blocks;
import ic2.core.ref.Ic2Items;
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

/** Runs only in an explicitly requested, isolated development client. */
@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public final class FurnaceExperienceClientSmoke {
  private static int ticks;
  private static volatile int passed;
  private static volatile List<BlockPos> positions;
  private static volatile double beforeXp;
  private static volatile int beforePlayerXp;
  private static final String[] NAMES = {"electric", "induction", "iron"};

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    if (!Boolean.getBoolean("ic2.furnaceXpSmoke")) return;
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
                for (int x = -5; x <= 5; x++)
                  for (int z = -2; z <= 5; z++) {
                    level.setBlockAndUpdate(
                        base.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
                    for (int y = 0; y < 4; y++) {
                      var pos = base.offset(x, y, z);
                      if (level.getBlockEntity(pos)
                          instanceof net.minecraft.world.Container inventory)
                        inventory.clearContent();
                      level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    }
                  }
                var electricPos = base.offset(-2, 0, 2);
                var inductionPos = base.offset(0, 0, 2);
                var ironPos = base.offset(2, 0, 2);
                level.setBlockAndUpdate(
                    electricPos, Ic2Blocks.ELECTRIC_FURNACE.defaultBlockState());
                var electric = (TileEntityElectricFurnace) level.getBlockEntity(electricPos);
                electric.inputSlot.put(new ItemStack(Ic2Items.IRON_DUST, 20));
                for (int i = 0; i < 20; i++) {
                  var recipe = electric.inputSlot.process();
                  electric.operateOnce(recipe, List.of(recipe.getOutput()));
                }
                electric.setChanged();
                level.setBlockAndUpdate(
                    inductionPos, Ic2Blocks.INDUCTION_FURNACE.defaultBlockState());
                var induction = (TileEntityInduction) level.getBlockEntity(inductionPos);
                induction.getComponent(ic2.core.block.comp.Energy.class).addEnergy(10000);
                induction.heat = 10000;
                induction.inputSlotA.put(new ItemStack(Ic2Items.IRON_DUST, 3));
                induction.inputSlotB.put(new ItemStack(Ic2Items.GOLD_DUST, 3));
                level.setBlockAndUpdate(ironPos, Ic2Blocks.IRON_FURNACE.defaultBlockState());
                var iron = (TileEntityIronFurnace) level.getBlockEntity(ironPos);
                iron.inputSlot.put(new ItemStack(Ic2Items.IRON_DUST, 3));
                iron.fuelSlot.put(new ItemStack(Items.COAL));
                player.connection.teleport(
                    base.getX() + 0.5, base.getY() + 0.1, base.getZ() - 1.5, 0, 12);
                positions = List.of(electricPos, inductionPos, ironPos);
              });
    }
    if (positions == null) return;
    if (ticks >= 60 && ticks <= 540 && (ticks - 60) % 240 == 0) {
      int index = (ticks - 60) / 240;
      mc.getSingleplayerServer()
          .execute(
              () -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);
                var pos = positions.get(index);
                var tile = player.serverLevel().getBlockEntity(pos);
                player.connection.teleport(
                    pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() - 1.5, 0, 12);
                beforeXp = tile.saveWithFullMetadata(player.registryAccess()).getDouble("xp");
                beforePlayerXp = player.totalExperience;
                if (beforeXp < 1)
                  throw new IllegalStateException(
                      "No real smelting XP before GUI collection: " + NAMES[index]);
                ((IHasGui) tile).openManagedBe(player, InteractionHand.MAIN_HAND);
              });
    }
    if (ticks >= 110 && ticks <= 590 && (ticks - 110) % 240 == 0) {
      int index = (ticks - 110) / 240;
      var gui = screen(mc);
      for (var slot : gui.getMenu().slots) {
        if (slot.x < 0
            || slot.y < 0
            || slot.x + 16 > gui.getXSize()
            || slot.y + 16 > gui.getYSize())
          throw new IllegalStateException("Furnace GUI slot outside its bounds: " + NAMES[index]);
      }
      Screenshot.grab(
          mc.gameDirectory,
          "furnace-xp-" + NAMES[index] + "-" + mc.options.languageCode + ".png",
          mc.getMainRenderTarget(),
          message -> {});
    }
    if (ticks >= 140 && ticks <= 620 && (ticks - 140) % 240 == 0) {
      var gui = screen(mc);
      if (!gui.mouseClicked(gui.getGuiLeft() + 16, gui.getGuiTop() + 60, 0))
        throw new IllegalStateException("Native furnace XP button did not accept the click");
    }
    if (ticks >= 210 && ticks <= 690 && (ticks - 210) % 240 == 0) {
      int index = (ticks - 210) / 240;
      mc.getSingleplayerServer()
          .execute(
              () -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);
                var tile = player.serverLevel().getBlockEntity(positions.get(index));
                double after = tile.saveWithFullMetadata(player.registryAccess()).getDouble("xp");
                if (after >= 1
                    || Math.abs(after - (beforeXp - Math.floor(beforeXp))) > 0.000001
                    || player.totalExperience - beforePlayerXp != (int) Math.floor(beforeXp))
                  throw new IllegalStateException(
                      "XP GUI payout did not reach the actual player: "
                          + NAMES[index]
                          + " stored="
                          + after
                          + " player="
                          + (player.totalExperience - beforePlayerXp));
                LoggerFactory.getLogger("IC2R-XP")
                    .info(
                        "IC2R_FURNACE_XP_GUI_PASS {} language={} xp={} remaining={} playerAward={}",
                        NAMES[index],
                        mc.options.languageCode,
                        beforeXp,
                        after,
                        player.totalExperience - beforePlayerXp);
                passed++;
              });
    }
    if (ticks == 800) {
      if (passed != 3) throw new IllegalStateException("Not every furnace GUI passed");
      LoggerFactory.getLogger("IC2R-XP")
          .info("IC2R_FURNACE_XP_CLIENT_PASS language={} furnaces=3", mc.options.languageCode);
      mc.stop();
    }
  }

  private static Ic2Gui<?> screen(Minecraft mc) {
    if (!(mc.screen instanceof Ic2Gui<?> gui))
      throw new IllegalStateException("Expected the actual native IC2R furnace screen");
    return gui;
  }
}
