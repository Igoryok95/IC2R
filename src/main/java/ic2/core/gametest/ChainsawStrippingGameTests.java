package ic2.core.gametest;

import ic2.api.item.ElectricItem;
import ic2.core.item.ElectricItemManager;
import ic2.core.item.tool.ItemElectricToolChainsaw;
import ic2.core.ref.Ic2Items;
import ic2.core.util.StackUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ic2")
@PrefixGameTestTemplate(false)
public final class ChainsawStrippingGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";
  private static final BlockPos POS = new BlockPos(1, 1, 1);
  private static final double CHARGE = 1000;
  private static final double COST = 100;

  private static List<Item> chainsaws() {
    var items = new ArrayList<Item>();
    items.add(Ic2Items.CHAINSAW);
    BuiltInRegistries.ITEM
        .getOptional(ResourceLocation.fromNamespaceAndPath("ic2r_addons", "advanced_chainsaw"))
        .filter(item -> item instanceof ItemElectricToolChainsaw)
        .ifPresent(items::add);
    return List.copyOf(items);
  }

  private static ServerPlayer player(GameTestHelper helper) {
    var player = helper.makeMockServerPlayerInLevel();
    player.setGameMode(GameType.SURVIVAL);
    return player;
  }

  private static UseOnContext context(
      GameTestHelper helper, ServerPlayer player, InteractionHand hand) {
    return new UseOnContext(player, hand, hit(helper));
  }

  private static BlockHitResult hit(GameTestHelper helper) {
    var pos = helper.absolutePos(POS);
    return new BlockHitResult(Vec3.atCenterOf(pos), Direction.EAST, pos, false);
  }

  private static InteractionResult use(
      GameTestHelper helper, ServerPlayer player, InteractionHand hand) {
    return player.gameMode.useItemOn(
        player, helper.getLevel(), player.getItemInHand(hand), hand, hit(helper));
  }

  @GameTest(template = EMPTY)
  public static void chainsawsStripWoodWithNativeAxeResultsAndAxes(GameTestHelper helper) {
    var player = player(helper);
    int checks = 0;
    for (var item : chainsaws()) {
      int perTool = 0;
      for (var block : BuiltInRegistries.BLOCK) {
        var original = block.defaultBlockState();
        if ((!original.is(BlockTags.LOGS) && block != Blocks.BAMBOO_BLOCK)
            || !original.hasProperty(RotatedPillarBlock.AXIS)) continue;
        for (var axis : Direction.Axis.values()) {
          var state = original.setValue(RotatedPillarBlock.AXIS, axis);
          helper.setBlock(POS, state);
          player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
          var expected =
              state.getToolModifiedState(
                  context(helper, player, InteractionHand.MAIN_HAND),
                  ItemAbilities.AXE_STRIP,
                  true);
          if (expected == null) continue;
          var stack = ElectricItemManager.getCharged(item, CHARGE);
          player.setItemInHand(InteractionHand.MAIN_HAND, stack);
          helper.assertTrue(
              use(helper, player, InteractionHand.MAIN_HAND).consumesAction(),
              "native strip interaction must succeed for " + block + " axis=" + axis);
          helper.assertValueEqual(
              helper.getBlockState(POS), expected, "same stripped block and properties as an axe");
          helper.assertValueEqual(
              ElectricItem.manager.getCharge(stack), CHARGE - COST, "one strip costs 100 EU");
          perTool++;
        }
      }
      helper.assertTrue(
          perTool >= 60, "native logs, wood, nether stems and bamboo must be covered");
      checks += perTool;
    }
    org.slf4j.LoggerFactory.getLogger("IC2R-Chainsaw-Stripping")
        .info(
            "IC2R_CHAINSAW_STRIPPING_PASS: {} variants, {} wood/axis cases",
            chainsaws().size(),
            checks);
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void strippingRequiresOneFullOperationAndNeverChargesRejectedUses(
      GameTestHelper helper) {
    var player = player(helper);
    for (var item : chainsaws()) {
      for (double charge : new double[] {0, 99, 100}) {
        helper.setBlock(POS, Blocks.OAK_LOG);
        var stack = ElectricItemManager.getCharged(item, charge);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var result = use(helper, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(
            result.consumesAction() == (charge == COST), "charge threshold for strip");
        helper.assertBlockPresent(charge == COST ? Blocks.STRIPPED_OAK_LOG : Blocks.OAK_LOG, POS);
        helper.assertValueEqual(
            ElectricItem.manager.getCharge(stack),
            charge == COST ? 0.0 : charge,
            "exact EU balance");
      }
      var stack = ElectricItemManager.getCharged(item, CHARGE);
      player.setItemInHand(InteractionHand.MAIN_HAND, stack);
      for (var block : List.of(Blocks.STONE, Blocks.OAK_PLANKS, Blocks.STRIPPED_OAK_LOG)) {
        helper.setBlock(POS, block);
        helper.assertTrue(
            !use(helper, player, InteractionHand.MAIN_HAND).consumesAction(),
            "non-strippable or already stripped block must pass through");
        helper.assertBlockPresent(block, POS);
        helper.assertValueEqual(
            ElectricItem.manager.getCharge(stack), CHARGE, "rejected use must not spend EU");
      }
    }
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void modeSwitchHasPriorityAndBothShearingModesCanStrip(GameTestHelper helper) {
    var player = player(helper);
    for (var item : chainsaws()) {
      var stack = ElectricItemManager.getCharged(item, CHARGE);
      player.setItemInHand(InteractionHand.MAIN_HAND, stack);
      for (boolean shift : new boolean[] {false, true}) {
        helper.setBlock(POS, Blocks.OAK_LOG);
        player.setShiftKeyDown(shift);
        Ic2GameTestUtil.pressModeSwitchKey(player);
        try {
          helper.assertTrue(
              !use(helper, player, InteractionHand.MAIN_HAND).consumesAction(),
              "mode shortcut must not strip the clicked log");
          helper.assertBlockPresent(Blocks.OAK_LOG, POS);
          helper.assertValueEqual(
              ElectricItem.manager.getCharge(stack), CHARGE, "mode shortcut must not spend EU");
          var previous = StackUtil.getOrCreateNbtData(stack).getBoolean("disableShear");
          stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
          if (!shift || item == Ic2Items.CHAINSAW)
            helper.assertTrue(
                previous != StackUtil.getOrCreateNbtData(stack).getBoolean("disableShear"),
                "normal chainsaw mode shortcut still toggles shearing");
        } finally {
          Ic2GameTestUtil.releaseModeSwitchKey(player);
          player.setShiftKeyDown(false);
        }
      }
      for (boolean disabled : new boolean[] {false, true}) {
        StackUtil.getOrCreateNbtData(stack).putBoolean("disableShear", disabled);
        helper.setBlock(POS, Blocks.SPRUCE_WOOD);
        helper.assertTrue(
            use(helper, player, InteractionHand.MAIN_HAND).consumesAction(),
            "stripping must work with or without shearing enabled");
        helper.assertBlockPresent(Blocks.STRIPPED_SPRUCE_WOOD, POS);
      }
    }
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void offhandStripsAndShieldIntentMatchesVanillaAxe(GameTestHelper helper) {
    var player = player(helper);
    for (var item : chainsaws()) {
      var stack = ElectricItemManager.getCharged(item, CHARGE);
      player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
      player.setItemInHand(InteractionHand.OFF_HAND, stack);
      helper.setBlock(POS, Blocks.BIRCH_LOG);
      helper.assertTrue(
          use(helper, player, InteractionHand.OFF_HAND).consumesAction(), "offhand strip succeeds");
      helper.assertBlockPresent(Blocks.STRIPPED_BIRCH_LOG, POS);
      helper.assertValueEqual(
          ElectricItem.manager.getCharge(stack), CHARGE - COST, "offhand operation costs EU once");

      player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
      player.setItemInHand(InteractionHand.MAIN_HAND, stack);
      helper.setBlock(POS, Blocks.BIRCH_LOG);
      helper.assertTrue(
          !use(helper, player, InteractionHand.MAIN_HAND).consumesAction(),
          "raising shield takes priority");
      helper.assertBlockPresent(Blocks.BIRCH_LOG, POS);
      helper.assertValueEqual(
          ElectricItem.manager.getCharge(stack), CHARGE - COST, "shield intent spends no EU");
      player.setShiftKeyDown(true);
      try {
        helper.assertTrue(
            use(helper, player, InteractionHand.MAIN_HAND).consumesAction(),
            "sneak bypasses shield intent");
        helper.assertBlockPresent(Blocks.STRIPPED_BIRCH_LOG, POS);
      } finally {
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
      }
    }
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void cancelledNativeStrippingLeavesWoodAndChargeUntouched(GameTestHelper helper) {
    var player = player(helper);
    var absolute = helper.absolutePos(POS);
    Consumer<BlockEvent.BlockToolModificationEvent> cancel =
        event -> {
          if (event.getPos().equals(absolute)
              && event.getLevel() == helper.getLevel()
              && event.getItemAbility() == ItemAbilities.AXE_STRIP) event.setCanceled(true);
        };
    NeoForge.EVENT_BUS.addListener(cancel);
    try {
      for (var item : chainsaws()) {
        helper.setBlock(POS, Blocks.OAK_LOG);
        var stack = ElectricItemManager.getCharged(item, CHARGE);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.assertTrue(
            !use(helper, player, InteractionHand.MAIN_HAND).consumesAction(),
            "cancelled strip must pass through");
        helper.assertBlockPresent(Blocks.OAK_LOG, POS);
        helper.assertValueEqual(
            ElectricItem.manager.getCharge(stack), CHARGE, "cancelled strip spends no EU");
      }
    } finally {
      NeoForge.EVENT_BUS.unregister(cancel);
    }
    helper.succeed();
  }
}
