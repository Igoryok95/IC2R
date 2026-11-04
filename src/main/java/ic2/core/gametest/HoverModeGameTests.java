package ic2.core.gametest;

import com.mojang.authlib.GameProfile;
import ic2.api.item.ElectricItem;
import ic2.core.IC2;
import ic2.core.item.ElectricItemManager;
import ic2.core.item.armor.ItemArmorJetpack;
import ic2.core.item.armor.jetpack.JetpackHandler;
import ic2.core.util.Keyboard;
import ic2.core.util.StackUtil;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ic2")
@PrefixGameTestTemplate(false)
public final class HoverModeGameTests {
  @GameTest(template = "gametest/empty3x3x3")
  public static void landingKeepsHoverArmedWithoutGroundIdleDrain(GameTestHelper helper) {
    for (ItemStack pack : packs()) {
      var player = new MessagePlayer(helper.getLevel());
      player.setGameMode(GameType.SURVIVAL);
      player.setItemSlot(EquipmentSlot.CHEST, pack);
      CustomData.update(
          DataComponents.CUSTOM_DATA, pack, tag -> tag.putBoolean("hover_mode", true));
      try {
        player.setOnGround(true);
        double before = charge(pack);
        for (int i = 0; i < 25; i++) {
          JetpackHandler.onPlayerTick(player);
          helper.assertTrue(hover(pack), "touching the ground must preserve the selected mode");
        }
        helper.assertTrue(charge(pack) == before, "an armed grounded pack has no idle energy cost");
        helper.assertTrue(
            player.messages.isEmpty(), "landing must not send disabled-mode messages");

        player.setOnGround(false);
        player.setDeltaMovement(Vec3.ZERO);
        JetpackHandler.onPlayerTick(player);
        helper.assertTrue(
            hover(pack) && charge(pack) < before, "hover resumes when leaving the block");

        player.setShiftKeyDown(true);
        player.setDeltaMovement(0, -0.2, 0);
        JetpackHandler.onPlayerTick(player);
        helper.assertTrue(
            hover(pack) && player.getDeltaMovement().y < 0,
            "sneaking descends without disabling hover");
        helper.assertTrue(player.messages.isEmpty(), "descent does not toggle or announce a mode");
      } finally {
        clear(player);
      }
    }
    helper.succeed();
  }

  @GameTest(template = "gametest/empty3x3x3")
  public static void holdingHoverKeysTogglesOnceAndReleaseRearmsThem(GameTestHelper helper) {
    int chord = Keyboard.Key.toInt(EnumSet.of(Keyboard.Key.jump, Keyboard.Key.modeSwitch));
    for (ItemStack pack : packs()) {
      var player = new MessagePlayer(helper.getLevel());
      player.setGameMode(GameType.SURVIVAL);
      player.setItemSlot(EquipmentSlot.CHEST, pack);
      player.setOnGround(false);
      try {
        IC2.keyboard.processKeyUpdate(player, chord);
        for (int i = 0; i < 25; i++) {
          player.setOnGround(i >= 12);
          JetpackHandler.onPlayerTick(player);
          helper.assertTrue(hover(pack), "held keys must not repeat, including while landing");
          helper.assertTrue(player.messages.size() == 1, "one message per press, not per cooldown");
        }
        message(helper, player, "ic2.hover_mode.enabled");

        IC2.keyboard.processKeyUpdate(player, 0);
        JetpackHandler.onPlayerTick(player);
        IC2.keyboard.processKeyUpdate(player, chord);
        JetpackHandler.onPlayerTick(player);
        helper.assertFalse(hover(pack), "the next press can disable hover immediately");
        helper.assertTrue(player.messages.size() == 2, "the second press sends one more message");
        message(helper, player, "ic2.hover_mode.disabled");

        IC2.keyboard.processKeyUpdate(player, 0);
        JetpackHandler.onPlayerTick(player);
        IC2.keyboard.processKeyUpdate(player, chord);
        JetpackHandler.onPlayerTick(player);
        helper.assertTrue(hover(pack), "hover can also be armed on the ground");
        helper.assertTrue(
            player.messages.size() == 3, "ground activation sends only the enabled message");
      } finally {
        clear(player);
      }
    }
    helper.succeed();
  }

  private static List<ItemStack> packs() {
    ItemStack electric =
        ElectricItemManager.getCharged(ic2.core.ref.Ic2Items.JETPACK_ELECTRIC, 10000);
    ItemStack fuel = new ItemStack(ic2.core.ref.Ic2Items.JETPACK);
    ((ItemArmorJetpack) fuel.getItem()).fillTank(fuel);
    ItemStack attached = new ItemStack(Items.DIAMOND_CHESTPLATE);
    JetpackHandler.setJetpackAttached(attached, true);
    ElectricItem.manager.charge(attached, 10000, Integer.MAX_VALUE, true, false);
    var packs = new ArrayList<>(List.of(electric, fuel, attached));
    for (String id : List.of("advanced_nano", "advanced_jetpack")) {
      BuiltInRegistries.ITEM
          .getOptional(ResourceLocation.fromNamespaceAndPath("ic2r_addons", id))
          .ifPresent(
              item -> {
                ItemStack stack = ElectricItemManager.getCharged(item, 10000);
                CustomData.update(
                    DataComponents.CUSTOM_DATA,
                    stack,
                    root -> {
                      CompoundTag equipment = new CompoundTag();
                      equipment.putBoolean("engine", true);
                      root.put("ic2r_addons", equipment);
                    });
                packs.add(stack);
              });
    }
    return packs;
  }

  private static double charge(ItemStack stack) {
    return stack.getItem() instanceof ItemArmorJetpack fuel
        ? fuel.getCharge(stack)
        : ElectricItem.manager.getCharge(stack);
  }

  private static boolean hover(ItemStack stack) {
    return StackUtil.getOrCreateNbtData(stack).getBoolean("hover_mode");
  }

  private static void clear(MessagePlayer player) {
    IC2.keyboard.removePlayerReferences(player);
    player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
    JetpackHandler.onPlayerTick(player);
  }

  private static void message(GameTestHelper helper, MessagePlayer player, String key) {
    var contents = player.messages.getLast().getContents();
    helper.assertTrue(contents instanceof TranslatableContents, "hover messages remain localized");
    helper.assertTrue(
        ((TranslatableContents) contents).getKey().equals(key), "correct mode message");
  }

  private static final class MessagePlayer extends FakePlayer {
    private final List<Component> messages = new ArrayList<>();

    MessagePlayer(ServerLevel level) {
      super(level, new GameProfile(UUID.randomUUID(), "hover-test"));
    }

    @Override
    public void displayClientMessage(Component message, boolean actionBar) {
      messages.add(message);
    }
  }
}
