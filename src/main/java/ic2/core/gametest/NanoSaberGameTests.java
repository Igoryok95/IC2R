package ic2.core.gametest;

import ic2.api.item.ElectricItem;
import ic2.core.item.ElectricItemManager;
import ic2.core.item.tool.AbstractItemNanoSaber;
import ic2.core.ref.Ic2Items;
import ic2.core.util.StackUtil;
import ic2.forge.NanoSaberCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ic2")
@PrefixGameTestTemplate(false)
public class NanoSaberGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";

  private static final double MAX_CHARGE = 160000.0;
  private static final double HIT_COST = 400.0;
  private static final double NANO_ARMOR_HIT_COST = 48000.0;

  @GameTest(template = EMPTY)
  public static void nanosaberActivatesWhenCharged(GameTestHelper helper) {
    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    ItemStack saber = ElectricItemManager.getCharged(Ic2Items.NANO_SABER, Double.POSITIVE_INFINITY);
    player.setItemInHand(InteractionHand.MAIN_HAND, saber);

    helper.assertFalse(AbstractItemNanoSaber.isActive(saber), "saber should start inactive");

    saber.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
    helper.assertTrue(
        AbstractItemNanoSaber.isActive(saber), "charged saber should activate on use");

    saber.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
    helper.assertFalse(
        AbstractItemNanoSaber.isActive(saber), "second use should deactivate the saber");

    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void nanosaberRequiresChargeToActivate(GameTestHelper helper) {
    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    ItemStack saber = new ItemStack(Ic2Items.NANO_SABER);
    player.setItemInHand(InteractionHand.MAIN_HAND, saber);

    saber.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
    helper.assertFalse(AbstractItemNanoSaber.isActive(saber), "empty saber must not activate");

    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void nanosaberAttackDrainsEnergy(GameTestHelper helper) {
    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    ItemStack saber = ElectricItemManager.getCharged(Ic2Items.NANO_SABER, Double.POSITIVE_INFINITY);
    player.setItemInHand(InteractionHand.MAIN_HAND, saber);
    saber.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
    helper.assertTrue(
        AbstractItemNanoSaber.isActive(saber), "saber should be active before attacking");

    LivingEntity target = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 1));
    saber.getItem().hurtEnemy(saber, target, player);

    helper.assertValueEqual(
        ElectricItem.manager.getCharge(saber), MAX_CHARGE - HIT_COST, "charge after one hit");
    helper.assertTrue(
        AbstractItemNanoSaber.isActive(saber), "saber should stay active while charged");

    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void activeNanosaberRevealsNanoArmorBreakChargeBoundary(GameTestHelper helper) {
    Player attacker = helper.makeMockPlayer(GameType.SURVIVAL);
    ItemStack saber = ElectricItemManager.getCharged(Ic2Items.NANO_SABER, Double.POSITIVE_INFINITY);
    attacker.setItemInHand(InteractionHand.MAIN_HAND, saber);
    saber.getItem().use(helper.getLevel(), attacker, InteractionHand.MAIN_HAND);

    LivingEntity target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
    target.setItemSlot(EquipmentSlot.FEET, new ItemStack(Ic2Items.NANO_BOOTS));
    target.setItemSlot(
        EquipmentSlot.LEGS,
        ElectricItemManager.getCharged(Ic2Items.NANO_LEGGINGS, NANO_ARMOR_HIT_COST - 1.0));
    target.setItemSlot(
        EquipmentSlot.CHEST,
        ElectricItemManager.getCharged(Ic2Items.NANO_CHESTPLATE, NANO_ARMOR_HIT_COST));
    target.setItemSlot(
        EquipmentSlot.HEAD,
        ElectricItemManager.getCharged(Ic2Items.NANO_HELMET, NANO_ARMOR_HIT_COST + 1.0));

    saber.getItem().hurtEnemy(saber, target, attacker);

    ItemStack boots = target.getItemBySlot(EquipmentSlot.FEET);
    ItemStack leggings = target.getItemBySlot(EquipmentSlot.LEGS);
    ItemStack chestplate = target.getItemBySlot(EquipmentSlot.CHEST);
    ItemStack helmet = target.getItemBySlot(EquipmentSlot.HEAD);
    helper.assertTrue(
        boots.isEmpty(), "an already discharged NanoSuit piece is removed by an active Nano Saber");
    helper.assertTrue(
        !leggings.isEmpty() && leggings.is(Ic2Items.NANO_LEGGINGS),
        "a NanoSuit piece just below the 48000 EU drain cost unexpectedly survives");
    Ic2GameTestAssertions.assertNear(
        helper,
        ElectricItem.manager.getCharge(leggings),
        NANO_ARMOR_HIT_COST - 1.0,
        "charge below the Nano Saber drain threshold");
    helper.assertTrue(
        chestplate.isEmpty(),
        "a NanoSuit piece with exactly 48000 EU is drained to zero and removed");
    helper.assertTrue(
        !helmet.isEmpty() && helmet.is(Ic2Items.NANO_HELMET),
        "a NanoSuit piece with 48001 EU should survive");
    Ic2GameTestAssertions.assertNear(
        helper,
        ElectricItem.manager.getCharge(helmet),
        1.0,
        "charge above the Nano Saber threshold");

    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void nanosaberDeactivatesWhenDepleted(GameTestHelper helper) {
    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    // enough to activate (16 EU), not enough for a hit (400 EU)
    ItemStack saber = ElectricItemManager.getCharged(Ic2Items.NANO_SABER, 300.0);
    player.setItemInHand(InteractionHand.MAIN_HAND, saber);
    saber.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
    helper.assertTrue(
        AbstractItemNanoSaber.isActive(saber), "saber with 300 EU should still activate");

    LivingEntity target = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 1));
    saber.getItem().hurtEnemy(saber, target, player);

    helper.assertFalse(
        AbstractItemNanoSaber.isActive(saber),
        "saber should turn off when it cannot pay the hit cost");

    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void nanosaberDamageBoostWhenActive(GameTestHelper helper) {
    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    ItemStack saber = ElectricItemManager.getCharged(Ic2Items.NANO_SABER, Double.POSITIVE_INFINITY);
    player.setItemInHand(InteractionHand.MAIN_HAND, saber);

    helper.assertValueEqual(getAttackDamageBonus(saber), 4.0, "inactive attack damage bonus");

    saber.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
    helper.assertValueEqual(getAttackDamageBonus(saber), 20.0, "active attack damage bonus");

    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void nanosaberTogglesKeepSnapshotsAndUnrelatedAttributesIntact(GameTestHelper h) {
    var saber = ElectricItemManager.getCharged(Ic2Items.NANO_SABER, MAX_CHARGE);
    var luck =
        new AttributeModifier(
            ResourceLocation.parse("ic2:test_saber_luck"),
            2,
            AttributeModifier.Operation.ADD_VALUE);
    saber.set(
        DataComponents.ATTRIBUTE_MODIFIERS,
        ItemAttributeModifiers.builder()
            .add(Attributes.LUCK, luck, EquipmentSlotGroup.MAINHAND)
            .build()
            .withTooltip(false));
    var inactive = saber.copy();
    NanoSaberCapabilities.setActive(saber, true);
    h.assertFalse(
        AbstractItemNanoSaber.isActive(inactive),
        "activation mutated the previous equipment snapshot");
    h.assertFalse(
        ItemStack.matches(inactive, saber),
        "activation must be visible to equipment change detection");
    h.assertTrue(
        StackUtil.getTag(inactive) != StackUtil.getTag(saber),
        "snapshots must not share the mutable active tag");
    var active = saber.copy();
    NanoSaberCapabilities.setActive(saber, false);
    h.assertTrue(
        AbstractItemNanoSaber.isActive(active), "deactivation mutated the active snapshot");
    h.assertFalse(
        ItemStack.matches(active, saber), "deactivation must change the equipment component");
    var modifiers = saber.get(DataComponents.ATTRIBUTE_MODIFIERS);
    h.assertTrue(
        modifiers.modifiers().stream()
            .anyMatch(
                entry ->
                    entry.attribute().equals(Attributes.LUCK) && entry.modifier().equals(luck)),
        "unrelated attribute was removed");
    h.assertFalse(modifiers.showInTooltip(), "unrelated tooltip setting changed");
    h.assertValueEqual(
        ElectricItem.manager.getCharge(saber), MAX_CHARGE, "toggle must preserve energy");
    h.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 100)
  public static void nanosaberChangesRealCombatDamageWithoutReequipping(GameTestHelper h) {
    var saber = ElectricItemManager.getCharged(Ic2Items.NANO_SABER, MAX_CHARGE);
    var player = tickingPlayer(h, saber);
    h.runAtTickTime(
        5,
        () -> {
          assertCombat(h, player, 5, 1);
          saber.getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        });
    h.runAtTickTime(10, () -> assertCombat(h, player, 21, 4));
    h.runAtTickTime(
        30,
        () -> {
          h.assertTrue(player.getMainHandItem() == saber, "test changed the held stack");
          var zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
          zombie.setNoAi(true);
          player.setOnGround(true);
          h.assertTrue(player.getAttackStrengthScale(.5F) == 1, "attack must be fully charged");
          player.attack(zombie);
          h.assertTrue(
              zombie.getHealth() == 0,
              "active saber must kill an ordinary unarmored zombie in one charged hit");
          zombie.discard();
          saber.getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        });
    h.runAtTickTime(35, () -> assertCombat(h, player, 5, 1));
    h.runAtTickTime(
        65,
        () -> {
          var zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
          zombie.setNoAi(true);
          player.setOnGround(true);
          h.assertTrue(
              player.getAttackStrengthScale(.5F) == 1, "inactive attack must be fully charged");
          player.attack(zombie);
          h.assertTrue(
              zombie.getHealth() > 15 && zombie.getHealth() < 15.2,
              "inactive saber kept the active damage bonus");
          zombie.discard();
          h.succeed();
        });
  }

  @GameTest(template = EMPTY, timeoutTicks = 70)
  public static void nanosaberTracksChargeBoundaryWithoutReequipping(GameTestHelper h) {
    var saber = ElectricItemManager.getCharged(Ic2Items.NANO_SABER, MAX_CHARGE);
    var player = tickingPlayer(h, saber);
    h.runAtTickTime(5, () -> saber.getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND));
    h.runAtTickTime(
        10,
        () -> {
          assertCombat(h, player, 21, 4);
          NanoSaberCapabilities.setEnergyTick(saber, 0);
          ElectricItem.manager.discharge(
              saber,
              ElectricItem.manager.getCharge(saber) - 399,
              Integer.MAX_VALUE,
              true,
              false,
              false);
        });
    h.runAtTickTime(
        14,
        () -> {
          assertCombat(h, player, 5, 1);
          h.assertTrue(
              AbstractItemNanoSaber.isActive(saber),
              "low charge must not change the existing activation rule");
          NanoSaberCapabilities.setEnergyTick(saber, 0);
          ElectricItem.manager.charge(saber, 1, Integer.MAX_VALUE, true, false);
        });
    h.runAtTickTime(
        18,
        () -> {
          assertCombat(h, player, 21, 4);
          h.assertValueEqual(
              ElectricItem.manager.getCharge(saber), 400.0, "exact powered-hit threshold");
          NanoSaberCapabilities.setEnergyTick(saber, 0);
          ElectricItem.manager.discharge(saber, 400, Integer.MAX_VALUE, true, false, false);
        });
    h.runAtTickTime(
        22,
        () -> {
          assertCombat(h, player, 5, 1);
          ((AbstractItemNanoSaber) saber.getItem()).consumeEnergy(saber, 400, player);
          h.assertFalse(
              AbstractItemNanoSaber.isActive(saber), "failed hit must deactivate the saber");
          ElectricItem.manager.charge(saber, MAX_CHARGE, Integer.MAX_VALUE, true, false);
        });
    h.runAtTickTime(
        26,
        () -> {
          assertCombat(h, player, 5, 1);
          saber.getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        });
    h.runAtTickTime(
        30,
        () -> {
          assertCombat(h, player, 21, 4);
          h.succeed();
        });
  }

  private static ServerPlayer tickingPlayer(GameTestHelper h, ItemStack saber) {
    var player = h.makeMockServerPlayerInLevel();
    player.setNoGravity(true);
    h.onEachTick(player::doTick);
    var pos = h.absolutePos(new BlockPos(1, 1, 1));
    player.teleportTo(pos.getX() + .5, pos.getY(), pos.getZ() + 1.5);
    player.setItemInHand(InteractionHand.MAIN_HAND, saber);
    return player;
  }

  private static void assertCombat(
      GameTestHelper h, ServerPlayer player, double damage, double speed) {
    h.assertValueEqual(
        player.getAttributeValue(Attributes.ATTACK_DAMAGE),
        damage,
        "actual equipped attack damage");
    h.assertValueEqual(
        player.getAttributeValue(Attributes.ATTACK_SPEED), speed, "actual equipped attack speed");
  }

  private static double getAttackDamageBonus(ItemStack stack) {
    double[] total = {0.0};
    stack.forEachModifier(
        EquipmentSlotGroup.MAINHAND,
        (attribute, modifier) -> {
          if (attribute == Attributes.ATTACK_DAMAGE) {
            total[0] += modifier.amount();
          }
        });

    return total[0];
  }
}
