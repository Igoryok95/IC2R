package ic2.core.block.machine.tileentity;

import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.ContainerBase;
import ic2.core.IHasGui;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.block.tileentity.TileEntityInventory;
import ic2.core.gui.dynamic.DynamicContainer;
import ic2.core.network.GrowingBuffer;
import ic2.core.ref.Ic2BlockEntities;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/** Experimental compact buffer: nine shared slots, four transfer upgrades, no EU network. */
public class TileEntityBetterItemBuffer extends TileEntityInventory
    implements IHasGui, IUpgradableBlock {
  public final InvSlot bufferSlot =
      new InvSlot(this, "buffer", InvSlot.Access.IO, 9, InvSlot.InvSide.ANY);
  public final InvSlotUpgrade upgradeSlot = new InvSlotUpgrade(this, "upgrade", 4);

  public TileEntityBetterItemBuffer(BlockPos pos, BlockState state) {
    super(Ic2BlockEntities.ITEM_BUFFER_2, pos, state);
    comparator.setUpdate(() -> calcRedstoneFromInvSlots(bufferSlot));
  }

  @Override
  protected void updateEntityServer() {
    super.updateEntityServer();
    upgradeSlot.tick();
  }

  @Override
  public ContainerBase<?> createServerScreenHandler(int syncId, Player player) {
    return DynamicContainer.create(syncId, player.getInventory(), this);
  }

  @Override
  public ContainerBase<?> createClientScreenHandler(
      int syncId, Inventory inventory, GrowingBuffer data) {
    return DynamicContainer.create(syncId, inventory, this);
  }

  @Override
  public Set<UpgradableProperty> getUpgradableProperties() {
    return EnumSet.of(UpgradableProperty.ItemProducing, UpgradableProperty.ItemConsuming);
  }

  @Override
  public double getEnergy() {
    return 0;
  }

  @Override
  public boolean useEnergy(double amount) {
    return true;
  }
}
