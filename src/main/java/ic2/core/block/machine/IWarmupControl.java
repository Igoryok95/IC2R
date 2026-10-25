package ic2.core.block.machine;

import ic2.api.network.INetworkClientTileEntityEventListener;
import ic2.core.ContainerBase;
import ic2.core.block.tileentity.Ic2TileEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Local standby request. This never changes a redstone component or block signal output. */
public interface IWarmupControl extends INetworkClientTileEntityEventListener {
  int TOGGLE_WARMUP_EVENT = 100;

  boolean isManualWarmup();

  void setManualWarmup(boolean enabled);

  @Override
  default void onNetworkEvent(Player player, int event) {
    handleWarmupEvent(player, event);
  }

  default boolean handleWarmupEvent(Player player, int event) {
    if (event != TOGGLE_WARMUP_EVENT) return false;
    if (this instanceof Ic2TileEntity tile
        && tile.getLevel() != null
        && !tile.getLevel().isClientSide
        && player.level() == tile.getLevel()
        && !tile.isRemoved()
        && !player.isSpectator()
        && player.distanceToSqr(Vec3.atCenterOf(tile.getBlockPos())) <= 64
        && player.containerMenu instanceof ContainerBase<?> menu
        && menu.base == tile) {
      setManualWarmup(!isManualWarmup());
    }
    return true;
  }
}
