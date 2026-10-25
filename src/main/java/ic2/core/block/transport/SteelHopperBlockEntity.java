package ic2.core.block.transport;

import ic2.core.ref.Ic2BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Vanilla transfers and inventory semantics with a shorter successful-transfer cooldown. */
public final class SteelHopperBlockEntity extends HopperBlockEntity {
  public static final int TRANSFER_INTERVAL = 2;
  public static final int ITEMS_PER_SECOND = 20 / TRANSFER_INTERVAL;

  public SteelHopperBlockEntity(BlockPos pos, BlockState state) {
    super(pos, state);
  }

  @Override
  public BlockEntityType<?> getType() {
    return Ic2BlockEntities.STEEL_HOPPER;
  }

  @Override
  protected Component getDefaultName() {
    return Component.translatable("block.ic2.steel_hopper");
  }

  @Override
  public void setCooldown(int ticks) {
    // Preserve vanilla's one-tick ordering adjustment for hopper-to-hopper transfers.
    if (ticks == MOVE_ITEM_SPEED || ticks == MOVE_ITEM_SPEED - 1) {
      ticks -= MOVE_ITEM_SPEED - TRANSFER_INTERVAL;
    }
    super.setCooldown(ticks);
  }
}
