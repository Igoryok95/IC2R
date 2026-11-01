package ic2.core.block.machine.tileentity;

import ic2.api.network.INetworkClientTileEntityEventListener;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.ContainerBase;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.fluid.Ic2FluidStack;
import ic2.core.fluid.Ic2FluidTank;
import ic2.core.gui.dynamic.DynamicContainer;
import ic2.core.network.GrowingBuffer;
import ic2.core.network.GuiSynced;
import ic2.core.ref.Ic2BlockEntities;
import ic2.core.ref.Ic2Fluids;
import ic2.core.ref.Ic2SoundEvents;
import ic2.core.util.LiquidUtil;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/** Dedicated ambient-air compressor; the receiver exposes extraction only. */
public class TileEntityAirCompressor extends TileEntityElectricMachine
    implements IHasGui, IUpgradableBlock, INetworkClientTileEntityEventListener {
  public static final int RECEIVER_CAPACITY = 16000;
  public static final int OUTPUT_LIMIT = 32;
  private static final int BASE_CYCLE = 300;
  private static final int BASE_DEMAND = 2;
  public final InvSlotUpgrade upgradeSlot = new InvSlotUpgrade(this, "upgrade", 4);
  @GuiSynced public final Ic2FluidTank receiver;
  @GuiSynced public boolean enabled = true;
  @GuiSynced public double consumption, production, delivered;
  private double fractionalAir;
  private double sampleEnergy, sampleAir, sampleOutput;
  private int sampleTicks;
  private int observedAmount;
  private double rate = 1000.0 / BASE_CYCLE;
  private int demand = BASE_DEMAND;

  public TileEntityAirCompressor(BlockPos pos, BlockState state) {
    super(Ic2BlockEntities.AIR_COMPRESSOR, pos, state, 600, 1);
    receiver = addComponent(new Fluids(this)).addTankExtract("receiver", RECEIVER_CAPACITY);
  }

  @Override
  protected void onLoaded() {
    super.onLoaded();
    if (!level.isClientSide) {
      updateUpgrades();
      observedAmount = receiver.getFluidAmount();
    }
  }

  @Override
  public void setChanged() {
    super.setChanged();
    if (IC2.sideProxy.isSimulating()) updateUpgrades();
  }

  private void updateUpgrades() {
    upgradeSlot.onChanged();
    rate =
        1000.0
            * upgradeSlot.getOperationsPerTick(BASE_CYCLE)
            / upgradeSlot.getOperationLength(BASE_CYCLE);
    demand = upgradeSlot.getEnergyDemand(BASE_DEMAND);
    energy.setSinkTier(upgradeSlot.getTier(1));
    dischargeSlot.setTier(energy.getSinkTier());
    energy.setCapacity(upgradeSlot.getEnergyStorage(600, BASE_CYCLE, BASE_DEMAND));
  }

  @Override
  protected void updateEntityServer() {
    super.updateEntityServer();
    int externalDelivery = Math.max(0, observedAmount - receiver.getFluidAmount());
    sampleOutput += externalDelivery;
    boolean running = compressAir();
    if (running) activate(false);
    else shutdown(false);
    int sent = LiquidUtil.distributeAll(this, OUTPUT_LIMIT);
    sampleOutput += sent;
    if (++sampleTicks >= 20) {
      consumption = sampleEnergy / sampleTicks;
      production = sampleAir / sampleTicks;
      delivered = sampleOutput / sampleTicks;
      sampleEnergy = sampleAir = sampleOutput = 0;
      sampleTicks = 0;
      IC2.network.get(true).updateTileEntityField(this, "receiver");
    }
    if (upgradeSlot.tickNoMark()) setChanged();
    observedAmount = receiver.getFluidAmount();
    // Air, partial production and energy must all survive a save, even without an inventory edit.
    if (running || sent > 0 || externalDelivery > 0) super.setChanged();
  }

  private boolean compressAir() {
    if (!enabled) return false;
    double room = receiver.getCapacity() - receiver.getFluidAmount() - fractionalAir;
    if (room < 1.0e-8 || rate <= 0) return false;
    double work = Math.min(rate, room);
    double cost = demand * work / rate;
    if (!energy.useEnergy(cost)) return false;
    double air = fractionalAir + work;
    int whole =
        Math.min(
            receiver.getCapacity() - receiver.getFluidAmount(), (int) Math.floor(air + 1.0e-8));
    if (whole > 0)
      receiver.fillMbUnchecked(Ic2FluidStack.create(Ic2Fluids.AIR.still(), whole), false);
    fractionalAir = Math.max(0, air - whole);
    sampleEnergy += cost;
    sampleAir += work;
    return true;
  }

  @Override
  public List<String> getNetworkedFields() {
    var fields = super.getNetworkedFields();
    fields.add("receiver");
    return fields;
  }

  @Override
  public void onNetworkEvent(Player player, int event) {
    if (event == 0) {
      enabled = !enabled;
      setChanged();
    }
  }

  @Override
  public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    enabled = !tag.contains("compressorEnabled") || tag.getBoolean("compressorEnabled");
    double saved = tag.getDouble("fractionalAir");
    fractionalAir = Double.isFinite(saved) ? Math.clamp(saved, 0, Math.nextDown(1.0)) : 0;
  }

  @Override
  public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.saveAdditional(tag, registries);
    tag.putBoolean("compressorEnabled", enabled);
    tag.putDouble("fractionalAir", fractionalAir);
  }

  public String getConsumptionText() {
    return number(consumption);
  }

  public String getProductionText() {
    return number(production);
  }

  public String getDeliveredText() {
    return number(delivered);
  }

  public String getStatusText() {
    return Component.translatable(
            !enabled
                ? "ic2.air_compressor.status.disabled"
                : receiver.getFluidAmount() >= receiver.getCapacity()
                    ? "ic2.air_compressor.status.full"
                    : getActive()
                        ? "ic2.air_compressor.status.running"
                        : "ic2.air_compressor.status.energy")
        .getString();
  }

  public String getReceiverText() {
    return String.format(
            Locale.ROOT, "%,d / %,d", receiver.getFluidAmount(), receiver.getCapacity())
        .replace(',', ' ');
  }

  private static String number(double value) {
    return String.format(Locale.ROOT, "%.2f", value);
  }

  @Override
  public Set<UpgradableProperty> getUpgradableProperties() {
    return EnumSet.of(
        UpgradableProperty.Processing,
        UpgradableProperty.Transformer,
        UpgradableProperty.EnergyStorage);
  }

  @Override
  public double getEnergy() {
    return energy.getEnergy();
  }

  @Override
  public boolean useEnergy(double amount) {
    return energy.useEnergy(amount);
  }

  @Override
  public SoundEvent getLoopingSoundEvent() {
    return Ic2SoundEvents.MACHINE_COMPRESSOR_OPERATE;
  }

  @Override
  public SoundEvent getInterruptSoundEvent() {
    return Ic2SoundEvents.MACHINE_INTERRUPT1;
  }

  @Override
  public ContainerBase<?> createServerScreenHandler(int id, Player player) {
    return DynamicContainer.create(id, player.getInventory(), this);
  }

  @Override
  public ContainerBase<?> createClientScreenHandler(
      int id, Inventory inventory, GrowingBuffer data) {
    return DynamicContainer.create(id, inventory, this);
  }
}
