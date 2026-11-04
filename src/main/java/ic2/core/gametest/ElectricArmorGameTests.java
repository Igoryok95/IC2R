package ic2.core.gametest;

import ic2.api.item.ElectricItem;
import ic2.core.item.ElectricItemManager;
import ic2.core.item.armor.ItemArmorElectric;
import ic2.core.ref.Ic2Items;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ic2")
@PrefixGameTestTemplate(false)
public final class ElectricArmorGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";
  private static final EquipmentSlot[] SLOTS = {
    EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
  };

  private static Item[][] sets() {
    return new Item[][] {
      {Ic2Items.NANO_HELMET, Ic2Items.NANO_CHESTPLATE, Ic2Items.NANO_LEGGINGS, Ic2Items.NANO_BOOTS},
      {
        Ic2Items.QUANTUM_HELMET,
        Ic2Items.QUANTUM_CHESTPLATE,
        Ic2Items.QUANTUM_LEGGINGS,
        Ic2Items.QUANTUM_BOOTS
      },
      {
        Ic2Items.NANO_HELMET,
        Ic2Items.QUANTUM_CHESTPLATE,
        Ic2Items.NANO_LEGGINGS,
        Ic2Items.QUANTUM_BOOTS
      }
    };
  }

  @GameTest(template = EMPTY)
  public static void chargedPiecesAccumulateAndRemoveIndependently(GameTestHelper helper) {
    for (Item[] set : sets()) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      int expected = 0;
      Set<ResourceLocation> ids = new HashSet<>();
      for (int i = 0; i < SLOTS.length; i++) {
        ItemStack stack = ElectricItemManager.getCharged(set[i], Double.POSITIVE_INFINITY);
        stack.forEachModifier(
            SLOTS[i],
            (attribute, modifier) -> {
              if (attribute == Attributes.ARMOR)
                helper.assertTrue(
                    ids.add(modifier.id()), "armor modifier IDs must differ between slots");
            });
        player.setItemSlot(SLOTS[i], stack);
        expected += i == 0 || i == 3 ? 3 : i == 1 ? 8 : 6;
        refreshEquipment(player);
        assertArmor(helper, player, expected, "equipping " + SLOTS[i]);
      }
      helper.assertTrue(ids.size() == 4, "all four charged pieces must contribute");
      for (int i : new int[] {1, 0, 3, 2}) {
        player.setItemSlot(SLOTS[i], ItemStack.EMPTY);
        expected -= i == 0 || i == 3 ? 3 : i == 1 ? 8 : 6;
        refreshEquipment(player);
        assertArmor(helper, player, expected, "removing " + SLOTS[i]);
      }
    }
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void chargeUpdatesDoNotReplaceOtherPieces(GameTestHelper helper) {
    for (Item[] set : sets()) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      for (int i = 0; i < SLOTS.length; i++)
        player.setItemSlot(SLOTS[i], ElectricItemManager.getCharged(set[i], 100_000));
      refreshEquipment(player);
      assertArmor(helper, player, 20, "full charged suit");
      for (EquipmentSlot slot : SLOTS) {
        ItemStack stack = player.getItemBySlot(slot);
        ElectricItem.manager.discharge(stack, 8, Integer.MAX_VALUE, true, false, false);
        refreshEquipment(player);
        assertArmor(helper, player, 20, "movement energy use in " + slot);
      }
      ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
      int threshold = ((ItemArmorElectric) boots.getItem()).getEnergyPerDamage();
      ElectricItem.manager.discharge(
          boots, Double.POSITIVE_INFINITY, Integer.MAX_VALUE, true, false, false);
      ElectricItem.manager.charge(boots, threshold - 1, Integer.MAX_VALUE, true, false);
      refreshEquipment(player);
      assertArmor(helper, player, 17, "only boots below the protection threshold");
      ElectricItem.manager.charge(boots, 1, Integer.MAX_VALUE, true, false);
      refreshEquipment(player);
      assertArmor(helper, player, 20, "boots recharged without reequip");
      player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
      refreshEquipment(player);
      assertArmor(helper, player, 18, "mixed vanilla and electric armor");
    }
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void actualDamageUsesAllChargedArmorPoints(GameTestHelper helper) {
    var attacker = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 1));
    var source = helper.getLevel().damageSources().mobAttack(attacker);
    for (Item[] set : sets()) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      for (int i = 0; i < SLOTS.length; i++)
        player.setItemSlot(SLOTS[i], ElectricItemManager.getCharged(set[i], 100_000));
      refreshEquipment(player);
      double energyBefore = charge(player);
      float remaining = 10;
      for (EquipmentSlot slot : EquipmentSlot.values()) {
        ItemStack stack = player.getItemBySlot(slot);
        if (!(stack.getItem() instanceof ItemArmorElectric armor)) continue;
        double share =
            switch (slot) {
              case HEAD, FEET -> 0.15;
              case CHEST -> 0.4;
              case LEGS -> 0.3;
              default -> 0;
            };
        remaining -= (float) (remaining * share * armor.getDamageAbsorptionRatio());
      }
      float expected = CombatRules.getDamageAfterAbsorb(player, remaining, source, 20, 8);
      float before = player.getHealth();
      helper.assertTrue(
          player.hurt(source, 10), "a normal attack must run through real damage handling");
      helper.assertTrue(
          Math.abs(before - player.getHealth() - expected) < 0.0001,
          "actual health loss must include all four armor modifiers");
      helper.assertTrue(charge(player) < energyBefore, "electric absorption must consume EU");
      refreshEquipment(player);
      assertArmor(helper, player, 20, "damage charge update");

      Player empty = helper.makeMockPlayer(GameType.SURVIVAL);
      for (int i = 0; i < SLOTS.length; i++) empty.setItemSlot(SLOTS[i], new ItemStack(set[i]));
      refreshEquipment(empty);
      assertArmor(helper, empty, 0, "empty electric suit");
      helper.assertTrue(empty.hurt(source, 10), "empty armor must not suppress damage");
      helper.assertTrue(
          Math.abs(empty.getHealth() - 10) < 0.0001, "empty armor grants no charged protection");
    }
    helper.succeed();
  }

  private static double charge(Player player) {
    double total = 0;
    for (EquipmentSlot slot : SLOTS)
      total += ElectricItem.manager.getCharge(player.getItemBySlot(slot));
    return total;
  }

  private static void assertArmor(
      GameTestHelper helper, Player player, int expected, String context) {
    helper.assertTrue(
        player.getArmorValue() == expected,
        context + ": expected " + expected + ", got " + player.getArmorValue());
  }

  /** Executes the actual equipment transition, which fake players do not tick themselves. */
  public static void refreshEquipment(LivingEntity entity) {
    try {
      var method = LivingEntity.class.getDeclaredMethod("detectEquipmentUpdates");
      method.setAccessible(true);
      method.invoke(entity);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException("Cannot run native equipment update", exception);
    }
  }
}
