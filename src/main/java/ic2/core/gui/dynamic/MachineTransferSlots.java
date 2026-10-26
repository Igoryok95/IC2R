package ic2.core.gui.dynamic;

import ic2.core.ContainerBase;
import ic2.core.block.invslot.InvSlotProcessable;
import ic2.core.slot.SlotInvSlot;
import java.util.List;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/** Resolve slot roles from the actual menu, including addon layouts and optional slots. */
public final class MachineTransferSlots {
  public static List<Slot> inputs(AbstractContainerMenu menu) {
    return menu.slots.stream()
        .filter(
            slot ->
                slot instanceof SlotInvSlot inventory
                    && (inventory.invSlot instanceof InvSlotProcessable<?, ?, ?>
                        || inventory.invSlot.name.equals("canInput")))
        .sorted(
            java.util.Comparator.comparingInt(
                slot -> ((SlotInvSlot) slot).invSlot.name.equals("canInput") ? 0 : 1))
        .toList();
  }

  public static List<Slot> inventory(AbstractContainerMenu menu) {
    if (!(menu instanceof ContainerBase<?> container)) return List.of();
    var inventory = container.getPlayer().getInventory();
    return menu.slots.stream()
        .filter(
            slot ->
                slot.container == inventory
                    && slot.getContainerSlot() >= 0
                    && slot.getContainerSlot() < 36)
        .toList();
  }

  private MachineTransferSlots() {}
}
