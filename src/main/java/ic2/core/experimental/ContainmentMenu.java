package ic2.core.experimental;

import ic2.core.item.ContainerHandHeldInventory;
import ic2.core.ref.Ic2ScreenHandlers;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ContainmentMenu extends ContainerHandHeldInventory<ContainmentBox.Storage> {
  public ContainmentMenu(int id, ContainmentBox.Storage inventory) {
    super(Ic2ScreenHandlers.CONTAINMENT_BOX, id, inventory);
    for (int row = 0; row < 3; row++)
      for (int col = 0; col < 4; col++) {
        int index = row * 4 + col;
        addSlot(
            new Slot(inventory, index, 53 + col * 18, 20 + row * 18) {
              @Override
              public boolean mayPlace(ItemStack stack) {
                return inventory.canPlaceItem(index, stack);
              }
            });
      }
    for (int row = 0; row < 3; row++)
      for (int col = 0; col < 9; col++)
        addSlot(new Slot(player.getInventory(), 9 + row * 9 + col, 8 + col * 18, 84 + row * 18));
    for (int col = 0; col < 9; col++)
      addSlot(new Slot(player.getInventory(), col, 8 + col * 18, 142));
  }
}
