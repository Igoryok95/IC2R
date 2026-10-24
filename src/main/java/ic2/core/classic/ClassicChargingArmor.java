package ic2.core.classic;

import ic2.api.item.ElectricItem;
import ic2.api.item.IMetalArmor;
import ic2.core.ref.Ic2ArmorMaterials;
import ic2.core.util.StackUtil;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** The basic Classic chargers, retaining their original one-EU and walking mechanics. */
public final class ClassicChargingArmor extends ArmorItem implements IMetalArmor {
  private final boolean solar;

  public ClassicChargingArmor(boolean solar) {
    super(
        (solar ? Ic2ArmorMaterials.SOLAR_HELMET : Ic2ArmorMaterials.STATIC_BOOTS).holder(),
        solar ? Type.HELMET : Type.BOOTS,
        new Properties().stacksTo(1));
    this.solar = solar;
  }

  public static void tickEquipped(Player player) {
    if (player.level().isClientSide) return;
    ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
    if (helmet.getItem() instanceof ClassicChargingArmor armor
        && armor.solar
        && hasSunlight(player.level(), player.blockPosition())) {
      chargeArmor(player, 1);
    }
    ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
    if (boots.getItem() instanceof ClassicChargingArmor armor && !armor.solar) {
      tickBoots(player, boots);
    }
  }

  public static boolean hasSunlight(Level level, BlockPos pos) {
    return level.dimensionType().hasSkyLight()
        && level.isDay()
        && level.canSeeSky(pos)
        && (!level.getBiome(pos).value().hasPrecipitation()
            || (!level.isRaining() && !level.isThundering()));
  }

  private static double chargeArmor(Player player, double energy) {
    for (EquipmentSlot slot : EquipmentSlot.values()) {
      if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
      if (energy <= 0) break;
      ItemStack target = player.getItemBySlot(slot);
      energy -= ElectricItem.manager.charge(target, energy, Integer.MAX_VALUE, false, false);
    }
    return energy;
  }

  private static void tickBoots(Player player, ItemStack boots) {
    CompoundTag data = StackUtil.getOrCreateNbtData(boots).copy();
    int charge = data.getInt("Charge");
    if (player.level().getGameTime() % 10 == 0 && charge > 0) {
      charge = (int) chargeArmor(player, charge);
    }
    if (charge < 20) {
      boolean stopped = player.isPassenger() || player.isInWater() || !player.onGround();
      int x = (int) player.getX();
      int z = (int) player.getZ();
      if (stopped || !data.contains("x")) data.putInt("x", x);
      if (stopped || !data.contains("z")) data.putInt("z", z);
      double distance = Math.hypot((double) data.getInt("x") - x, (double) data.getInt("z") - z);
      if (distance >= 5) {
        data.putInt("x", x);
        data.putInt("z", z);
        var ground = player.level().getBlockState(player.getOnPos());
        boolean wool = ground.is(BlockTags.WOOL) || ground.is(BlockTags.WOOL_CARPETS);
        charge += Math.min(3, Mth.ceil(wool ? distance : distance / 5));
      }
    }
    data.putInt("Charge", charge);
    StackUtil.setTag(boots, data);
  }

  @Override
  public boolean isMetalArmor(ItemStack stack, Player player) {
    return !solar;
  }

  @Override
  public void appendHoverText(
      ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
    super.appendHoverText(stack, context, tooltip, flag);
    tooltip.add(
        Component.translatable(
            solar ? "tooltip.ic2.solar_helmet.production" : "tooltip.ic2.static_boots.production"));
  }
}
