package ic2.core.experimental;

import ic2.api.item.ElectricItem;
import ic2.core.block.generator.tileentity.TileEntitySolarGenerator;
import ic2.core.item.armor.ItemArmorUtility;
import ic2.core.ref.Ic2ArmorMaterials;
import ic2.core.util.StackUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Experimental Solar Helmet and Static Boots charging rules. */
public final class ExperimentalChargingArmor extends ItemArmorUtility {
  private final boolean solar;

  public ExperimentalChargingArmor(boolean solar) {
    super(
        solar ? Ic2ArmorMaterials.SOLAR_HELMET.holder() : Ic2ArmorMaterials.STATIC_BOOTS.holder(),
        new Properties().stacksTo(1),
        solar ? EquipmentSlot.HEAD : EquipmentSlot.FEET);
    this.solar = solar;
  }

  @Override
  public void inventoryTick(
      ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
    if (level.isClientSide
        || !(entity instanceof Player player)
        || player.getItemBySlot(solar ? EquipmentSlot.HEAD : EquipmentSlot.FEET) != stack) return;
    ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
    if (chest.isEmpty()) return;
    double amount;
    if (solar) {
      amount = TileEntitySolarGenerator.getSkyLight(level, player.blockPosition());
    } else {
      var data = StackUtil.getOrCreateNbtData(stack);
      boolean reset = !data.contains("x") || player.isPassenger() || player.isInWater();
      int x = (int) player.getX(), z = (int) player.getZ();
      double distance =
          reset ? 0 : Math.hypot((double) data.getInt("x") - x, (double) data.getInt("z") - z);
      if (reset || distance >= 5) {
        data.putInt("x", x);
        data.putInt("z", z);
        StackUtil.setTag(stack, data);
      }
      if (distance < 5) return;
      amount = Math.min(3, distance / 5);
    }
    if (amount > 0
        && ElectricItem.manager.charge(chest, amount, Integer.MAX_VALUE, true, false) > 0)
      player.containerMenu.broadcastChanges();
  }
}
