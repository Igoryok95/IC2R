package ic2.core.gametest;

import com.mojang.authlib.GameProfile;
import ic2.core.IC2;
import ic2.core.block.machine.tileentity.TileEntityMiner;
import ic2.core.block.wiring.tileentity.TileEntityElectricBlock;
import ic2.core.item.ElectricItemManager;
import ic2.core.ref.Ic2Blocks;
import ic2.core.ref.Ic2Items;
import ic2.core.util.Keyboard;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ic2")
@PrefixGameTestTemplate(false)
public final class ChatLocalizationGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";

  @GameTest(template = EMPTY)
  public static void iridiumDrillModesKeepNestedTranslationKeys(GameTestHelper h) {
    var player = playerWith(h, Ic2Items.IRIDIUM_DRILL);
    toggle(h, player);
    mode(
        h,
        player,
        "item.ic2.mining_laser.tooltip.mode",
        "item.ic2.mining_laser.tooltip.mode.silkTouch");
    toggle(h, player);
    mode(
        h,
        player,
        "item.ic2.mining_laser.tooltip.mode",
        "item.ic2.mining_laser.tooltip.mode.normal");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void chainsawModesKeepNestedTranslationKeys(GameTestHelper h) {
    var player = playerWith(h, Ic2Items.CHAINSAW);
    toggle(h, player);
    mode(
        h,
        player,
        "item.ic2.mining_laser.tooltip.mode",
        "item.ic2.mining_laser.tooltip.mode.no_shear");
    toggle(h, player);
    mode(
        h,
        player,
        "item.ic2.mining_laser.tooltip.mode",
        "item.ic2.mining_laser.tooltip.mode.normal");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void foamSprayerModesKeepNestedTranslationKeys(GameTestHelper h) {
    var player = playerWith(h, Ic2Items.FOAM_SPRAYER);
    toggle(h, player);
    mode(h, player, "ic2.tooltip.mode", "ic2.tooltip.mode.single");
    toggle(h, player);
    mode(h, player, "ic2.tooltip.mode", "ic2.tooltip.mode.normal");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void armorHudMessagesKeepTranslationKeys(GameTestHelper h) {
    for (Item helmet : List.of(Ic2Items.NANO_HELMET, Ic2Items.QUANTUM_HELMET)) {
      var player = new MessagePlayer(h.getLevel());
      var stack = ElectricItemManager.getCharged(helmet, Double.POSITIVE_INFINITY);
      player.setItemSlot(EquipmentSlot.HEAD, stack);
      // HUD selection has its own binding, separate from the night-vision mode key.
      int keys = (1 << Keyboard.Key.alt.ordinal()) | (1 << Keyboard.Key.hubMode.ordinal());
      IC2.keyboard.processKeyUpdate(player, keys);
      try {
        helmet.inventoryTick(stack, h.getLevel(), player, 0, false);
      } finally {
        IC2.keyboard.processKeyUpdate(player, 0);
      }
      root(h, player, "ic2.hud.basic");
    }
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void storageAndChargepadMessagesKeepTranslationKeys(GameTestHelper h) {
    var pos = new BlockPos(1, 1, 1);
    var player = new MessagePlayer(h.getLevel());
    h.setBlock(pos, Ic2Blocks.MFSU);
    TileEntityElectricBlock storage = h.getBlockEntity(pos);
    storage.onNetworkEvent(player, 0);
    root(h, player, "ic2.EUStorage.gui.mod.redstone1");
    h.setBlock(pos, Ic2Blocks.MFSU_CHARGEPAD);
    TileEntityElectricBlock chargepad = h.getBlockEntity(pos);
    chargepad.onNetworkEvent(player, 0);
    root(h, player, "ic2.blockChargepad.gui.mod.redstone1");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void debugModeNamesKeepTranslationKeys(GameTestHelper h) {
    var player = playerWith(h, Ic2Items.DEBUG_ITEM);
    Ic2GameTestUtil.pressModeSwitchKey(player);
    try {
      player
          .getMainHandItem()
          .getItem()
          .onItemUseFirst(
              player.getMainHandItem(),
              Ic2GameTestUtil.useOn(
                  h, player, new BlockPos(1, 1, 1), net.minecraft.core.Direction.UP));
    } finally {
      Ic2GameTestUtil.releaseModeSwitchKey(player);
    }
    mode(h, player, "ic2.debug.mode", "ic2.debug.mode.interfaces_fields_retrace");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void debugTileDataKeepsLocalizedLabelsAndDirections(GameTestHelper h) {
    var pos = new BlockPos(1, 1, 1);
    h.setBlock(pos, Ic2Blocks.MFSU);
    var player = playerWith(h, Ic2Items.DEBUG_ITEM);
    ic2.core.util.StackUtil.getOrCreateNbtData(player.getMainHandItem()).putInt("mode", 2);
    player
        .getMainHandItem()
        .getItem()
        .onItemUseFirst(
            player.getMainHandItem(),
            Ic2GameTestUtil.useOn(h, player, pos, net.minecraft.core.Direction.UP));
    h.assertTrue(!player.messages.isEmpty(), "debug data must produce chat messages");
    for (Component message : player.messages) {
      h.assertTrue(
          message.getContents() instanceof TranslatableContents,
          "debug labels remain translatable");
      h.assertTrue(
          ((TranslatableContents) message.getContents()).getKey().startsWith("ic2.debug."),
          "localized debug label key");
      if (((TranslatableContents) message.getContents()).getKey().equals("ic2.debug.block_state")) {
        var args = ((TranslatableContents) message.getContents()).getArgs();
        h.assertTrue(
            args[0] instanceof Component && args[1] instanceof Component,
            "boolean and direction values remain translatable");
      }
    }
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void minerPumpMessagesKeepTranslationKeys(GameTestHelper h) {
    var pos = new BlockPos(1, 1, 1);
    h.setBlock(pos, Ic2Blocks.MINER);
    TileEntityMiner miner = h.getBlockEntity(pos);
    var player = new MessagePlayer(h.getLevel());
    miner.onNetworkEvent(player, 0);
    var message = player.messages.getLast();
    h.assertTrue(
        message.getContents() instanceof TranslatableContents,
        "pump status must reach the client as a translation");
    h.assertTrue(
        ((TranslatableContents) message.getContents())
            .getKey()
            .startsWith("ic2.Miner.gui.pumpMode."),
        "pump mode translation key");
    h.succeed();
  }

  private static MessagePlayer playerWith(GameTestHelper h, Item item) {
    var player = new MessagePlayer(h.getLevel());
    player.setItemInHand(
        InteractionHand.MAIN_HAND,
        item instanceof ic2.api.item.IElectricItem
            ? ElectricItemManager.getCharged(item, Double.POSITIVE_INFINITY)
            : new ItemStack(item));
    return player;
  }

  private static void toggle(GameTestHelper h, MessagePlayer player) {
    Ic2GameTestUtil.pressModeSwitchKey(player);
    try {
      player.getMainHandItem().getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
    } finally {
      Ic2GameTestUtil.releaseModeSwitchKey(player);
    }
  }

  private static TranslatableContents root(GameTestHelper h, MessagePlayer player, String key) {
    h.assertTrue(!player.messages.isEmpty(), "player must receive a message");
    var contents = player.messages.getLast().getContents();
    h.assertTrue(
        contents instanceof TranslatableContents, "message remains translatable on the client");
    var text = (TranslatableContents) contents;
    h.assertValueEqual(text.getKey(), key, "message translation key");
    return text;
  }

  private static void mode(GameTestHelper h, MessagePlayer player, String key, String argument) {
    var text = root(h, player, key);
    h.assertTrue(
        text.getArgs().length == 1 && text.getArgs()[0] instanceof Component,
        "mode name is a nested component");
    var mode = ((Component) text.getArgs()[0]).getContents();
    h.assertTrue(mode instanceof TranslatableContents, "mode name remains translatable");
    h.assertValueEqual(((TranslatableContents) mode).getKey(), argument, "nested mode key");
  }

  private static final class MessagePlayer extends FakePlayer {
    private final List<Component> messages = new ArrayList<>();

    MessagePlayer(ServerLevel level) {
      super(level, new GameProfile(UUID.randomUUID(), "chat-test"));
    }

    @Override
    public void displayClientMessage(Component message, boolean actionBar) {
      messages.add(message);
    }
  }
}
