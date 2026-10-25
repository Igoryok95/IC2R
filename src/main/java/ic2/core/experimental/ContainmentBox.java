package ic2.core.experimental;

import ic2.core.ContainerBase;
import ic2.core.IHasGui;
import ic2.core.item.IHandHeldInventory;
import ic2.core.item.ItemNuclearResource;
import ic2.core.item.reactor.ItemReactorMOX;
import ic2.core.item.reactor.ItemReactorUranium;
import ic2.core.item.tool.HandHeldInventory;
import ic2.core.network.GrowingBuffer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ContainmentBox extends Item implements IHandHeldInventory {
  public ContainmentBox() {
    super(new Properties().stacksTo(1));
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (!level.isClientSide) getInventory(player, hand, stack).openManagedItem(player, hand, null);
    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
  }

  @Override
  public IHasGui getInventory(Player player, InteractionHand hand, ItemStack stack) {
    return new Storage(player, hand, stack);
  }

  public static final class Storage extends HandHeldInventory {
    public Storage(Player player, InteractionHand hand, ItemStack stack) {
      super(player, hand, stack, 12);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
      return stack.getItem() instanceof ItemNuclearResource
          || stack.getItem() instanceof ItemReactorUranium
          || stack.getItem() instanceof ItemReactorMOX;
    }

    @Override
    public ContainerBase<?> createServerScreenHandler(int id, Player player) {
      return menu(id, player.getInventory());
    }

    @Override
    public ContainerBase<?> createClientScreenHandler(
        int id, Inventory inventory, GrowingBuffer data) {
      return menu(id, inventory);
    }

    private ContainerBase<?> menu(int id, Inventory playerInventory) {
      return new ContainmentMenu(id, this);
    }
  }
}
