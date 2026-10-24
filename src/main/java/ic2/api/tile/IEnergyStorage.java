package ic2.api.tile;

import net.minecraft.core.Direction;

public interface IEnergyStorage {
  int getStored();

  void setStored(int var1);

  int addEnergy(int var1);

  int getCapacity();

  /** Exact readings for stores larger than the legacy integer API can represent. */
  default long getStoredLong() {
    return getStored();
  }

  default long getCapacityLong() {
    return getCapacity();
  }

  int getOutput();

  double getOutputEnergyUnitsPerTick();

  boolean isTeleporterCompatible(Direction var1);
}
