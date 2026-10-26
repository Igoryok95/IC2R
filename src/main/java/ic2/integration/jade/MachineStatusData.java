package ic2.integration.jade;

import ic2.api.energy.EnergyNet;
import ic2.api.reactor.IReactor;
import ic2.core.block.comp.Energy;
import ic2.core.block.machine.tileentity.TileEntityBlastFurnace;
import ic2.core.block.machine.tileentity.TileEntityCentrifuge;
import ic2.core.block.machine.tileentity.TileEntityInduction;
import ic2.core.block.machine.tileentity.TileEntityStandardMachine;
import ic2.core.block.tileentity.Ic2TileEntity;
import ic2.core.block.wiring.tileentity.TileEntityElectricBlock;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;

/** Server-owned readings, independent of the optional Jade classes. */
public final class MachineStatusData {
  public static CompoundTag collect(Ic2TileEntity tile) {
    var data = new CompoundTag();
    if (!BuiltInRegistries.BLOCK
        .getKey(tile.getBlockState().getBlock())
        .getNamespace()
        .equals("ic2")) return data;
    var energy = tile.getComponent(Energy.class);
    if (energy != null) {
      data.putDouble("energy", energy.getEnergy());
      data.putDouble("capacity", energy.getCapacity());
      data.putInt("tier", Math.max(energy.getSinkTier(), energy.getSourceTier()));
      if (energy.getDelegate() != null) {
        var stats = EnergyNet.instance.getNodeStats(energy.getDelegate());
        if (stats != null) {
          data.putDouble("input", stats.getEnergyIn());
          data.putDouble("output", stats.getEnergyOut());
        }
      }
    }
    if (tile.getBlockType().canActive()) data.putBoolean("active", tile.getActive());
    if (tile instanceof TileEntityElectricBlock storage)
      data.putDouble("ratedOutput", storage.getOutputEnergyUnitsPerTick());
    if (tile instanceof TileEntityStandardMachine<?, ?, ?> machine)
      data.putDouble("progress", machine.getProgress());
    if (tile instanceof TileEntityInduction furnace) {
      data.putDouble("heat", furnace.heat / 10000.0);
      data.putDouble("progress", furnace.progress / 4000.0);
    } else if (tile instanceof TileEntityCentrifuge centrifuge) {
      data.putDouble("heat", centrifuge.heat / (double) Math.max(1, centrifuge.workheat));
    } else if (tile instanceof TileEntityBlastFurnace furnace) {
      data.putDouble("heat", furnace.heat / (double) TileEntityBlastFurnace.maxHeat);
      data.putDouble("progress", furnace.getGuiValue("progress"));
    }
    if (tile instanceof IReactor reactor) {
      data.putDouble("reactorHeat", reactor.getHeat());
      data.putDouble("reactorMaxHeat", reactor.getMaxHeat());
      data.putDouble("reactorOutput", reactor.getReactorEUEnergyOutput());
    }
    return data;
  }

  public static String percent(double percent) {
    return String.format(Locale.ROOT, "%.2f", Math.clamp(percent, 0, 100));
  }

  private MachineStatusData() {}
}
