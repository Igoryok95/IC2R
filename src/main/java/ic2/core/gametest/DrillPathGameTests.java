package ic2.core.gametest;

import ic2.api.item.ElectricItem;
import ic2.core.IC2;
import ic2.core.item.ElectricItemManager;
import ic2.core.item.tool.ItemElectricTool;
import ic2.core.ref.Ic2Items;
import ic2.core.util.Keyboard;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ic2")
@PrefixGameTestTemplate(false)
public final class DrillPathGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";
  private static final BlockPos POS = new BlockPos(1, 1, 1);
  private static final double CHARGE = 10000;

  private static List<Item> drills() {
    return List.of(Ic2Items.DRILL, Ic2Items.DIAMOND_DRILL, Ic2Items.IRIDIUM_DRILL);
  }

  private static ServerPlayer player(GameTestHelper helper) {
    var player = helper.makeMockServerPlayerInLevel();
    player.setGameMode(GameType.SURVIVAL);
    return player;
  }

  private static InteractionResult use(GameTestHelper helper, ServerPlayer player, Direction face) {
    var pos = helper.absolutePos(POS);
    return player.gameMode.useItemOn(
        player,
        helper.getLevel(),
        player.getMainHandItem(),
        InteractionHand.MAIN_HAND,
        new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false));
  }

  @GameTest(template = EMPTY)
  public static void everyNativeDrillFlattensAllVanillaSoilsAndPaysItsOwnCost(
      GameTestHelper helper) {
    var player = player(helper);
    for (var item : drills()) {
      double cost = ((ItemElectricTool) item).operationEnergyCost;
      for (var ground :
          List.of(
              Blocks.GRASS_BLOCK,
              Blocks.DIRT,
              Blocks.PODZOL,
              Blocks.COARSE_DIRT,
              Blocks.MYCELIUM,
              Blocks.ROOTED_DIRT)) {
        for (var face : List.of(Direction.UP, Direction.NORTH)) {
          helper.setBlock(POS, ground);
          helper.setBlock(POS.above(), Blocks.AIR);
          var stack = ElectricItemManager.getCharged(item, CHARGE);
          player.setItemInHand(InteractionHand.MAIN_HAND, stack);
          helper.assertTrue(
              use(helper, player, face).consumesAction(), "native drill creates path");
          helper.assertBlockPresent(Blocks.DIRT_PATH, POS);
          helper.assertValueEqual(
              ElectricItem.manager.getCharge(stack),
              CHARGE - cost,
              "one normal drilling operation");
        }
      }
    }
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void drillPathRejectsLowPowerBlockedGroundAndBottomFace(GameTestHelper helper) {
    var player = player(helper);
    for (var item : drills()) {
      double cost = ((ItemElectricTool) item).operationEnergyCost;
      for (double charge : new double[] {0, cost - 1, cost}) {
        helper.setBlock(POS, Blocks.GRASS_BLOCK);
        var stack = ElectricItemManager.getCharged(item, charge);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.assertTrue(
            use(helper, player, Direction.UP).consumesAction() == (charge == cost),
            "path requires one full operation");
        helper.assertBlockPresent(charge == cost ? Blocks.DIRT_PATH : Blocks.GRASS_BLOCK, POS);
        helper.assertValueEqual(
            ElectricItem.manager.getCharge(stack),
            charge == cost ? 0.0 : charge,
            "path charge threshold");
      }
      var stack = ElectricItemManager.getCharged(item, CHARGE);
      player.setItemInHand(InteractionHand.MAIN_HAND, stack);
      helper.setBlock(POS, Blocks.GRASS_BLOCK);
      helper.assertTrue(
          !use(helper, player, Direction.DOWN).consumesAction(), "bottom face cannot flatten");
      helper.assertBlockPresent(Blocks.GRASS_BLOCK, POS);
      helper.setBlock(POS.above(), Blocks.STONE);
      helper.assertTrue(
          !use(helper, player, Direction.UP).consumesAction(), "covered ground cannot flatten");
      helper.assertBlockPresent(Blocks.GRASS_BLOCK, POS);
      helper.setBlock(POS.above(), Blocks.AIR);
      for (var ground : List.of(Blocks.STONE, Blocks.DIRT_PATH)) {
        helper.setBlock(POS, ground);
        helper.assertTrue(
            !use(helper, player, Direction.UP).consumesAction(), "invalid ground cannot flatten");
        helper.assertBlockPresent(ground, POS);
      }
      helper.assertValueEqual(
          ElectricItem.manager.getCharge(stack), CHARGE, "rejected paths spend no EU");
    }
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void drillModeShortcutsDoNotCreatePaths(GameTestHelper helper) {
    var player = player(helper);
    for (var item : drills()) {
      var stack = ElectricItemManager.getCharged(item, CHARGE);
      player.setItemInHand(InteractionHand.MAIN_HAND, stack);
      for (var key : List.of(Keyboard.Key.modeSwitch, Keyboard.Key.alt)) {
        helper.setBlock(POS, Blocks.GRASS_BLOCK);
        IC2.keyboard.processKeyUpdate(player, 1 << key.ordinal());
        try {
          helper.assertTrue(
              !use(helper, player, Direction.UP).consumesAction(), "mode key takes priority");
          helper.assertBlockPresent(Blocks.GRASS_BLOCK, POS);
          helper.assertValueEqual(
              ElectricItem.manager.getCharge(stack), CHARGE, "mode shortcut spends no EU");
        } finally {
          IC2.keyboard.processKeyUpdate(player, 0);
        }
      }
    }
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void drillPathsRespectNativeToolCancellationAndCustomResults(
      GameTestHelper helper) {
    var player = player(helper);
    var pos = helper.absolutePos(POS);
    boolean[] cancel = {true};
    Consumer<BlockEvent.BlockToolModificationEvent> listener =
        event -> {
          if (event.getLevel() == helper.getLevel()
              && event.getPos().equals(pos)
              && event.getItemAbility() == ItemAbilities.SHOVEL_FLATTEN) {
            if (cancel[0]) event.setCanceled(true);
            else event.setFinalState(Blocks.DIRT_PATH.defaultBlockState());
          }
        };
    NeoForge.EVENT_BUS.addListener(listener);
    try {
      for (var item : drills()) {
        var stack = ElectricItemManager.getCharged(item, CHARGE);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        cancel[0] = true;
        helper.setBlock(POS, Blocks.GRASS_BLOCK);
        helper.assertTrue(
            !use(helper, player, Direction.UP).consumesAction(), "native cancellation blocks path");
        helper.assertBlockPresent(Blocks.GRASS_BLOCK, POS);
        helper.assertValueEqual(
            ElectricItem.manager.getCharge(stack), CHARGE, "cancelled path spends no EU");
        cancel[0] = false;
        helper.setBlock(POS, Blocks.STONE);
        helper.assertTrue(
            use(helper, player, Direction.UP).consumesAction(),
            "custom native mapping is accepted");
        helper.assertBlockPresent(Blocks.DIRT_PATH, POS);
      }
    } finally {
      NeoForge.EVENT_BUS.unregister(listener);
    }
    helper.succeed();
  }
}
