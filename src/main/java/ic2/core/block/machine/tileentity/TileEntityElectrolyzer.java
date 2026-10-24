package ic2.core.block.machine.tileentity;

import ic2.api.recipe.IElectrolyzerRecipeManager;
import ic2.api.recipe.Recipes;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.ContainerBase;
import ic2.core.IHasGui;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.block.machine.container.ContainerElectrolyzer;
import ic2.core.classic.ClassicResources;
import ic2.core.fluid.Ic2FluidStack;
import ic2.core.fluid.Ic2FluidTank;
import ic2.core.gui.CustomGauge;
import ic2.core.network.GrowingBuffer;
import ic2.core.recipe.ElectrolyzerRecipeManager;
import ic2.core.ref.Ic2BlockEntities;
import ic2.core.ref.Ic2Fluids;
import ic2.core.ref.Ic2Items;
import ic2.core.ref.Ic2SoundEvents;
import ic2.core.util.LiquidUtil;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TileEntityElectrolyzer extends TileEntityElectricMachine
    implements IUpgradableBlock, IHasGui, CustomGauge.IGaugeRatioProvider {
  public final InvSlotUpgrade upgradeSlot;
  public final InvSlot classicInput;
  public final InvSlot classicOutput;
  public int classicCharge;
  public int classicReturn;
  protected final Fluids fluids = this.addComponent(new Fluids(this));
  protected int progress = 0;
  protected IElectrolyzerRecipeManager.ElectrolyzerRecipe recipe = null;
  protected Ic2FluidTank input;

  public TileEntityElectrolyzer(BlockPos pos, BlockState state) {
    super(Ic2BlockEntities.ELECTROLYZER, pos, state, 32000, 2);
    this.input =
        this.fluids.addTankInsert("input", 8000, Fluids.fluidPredicate(Recipes.electrolyzer));
    this.upgradeSlot = new InvSlotUpgrade(this, "upgradeSlot", 4);
    this.classicInput =
        new InvSlot(this, "classicInput", InvSlot.Access.IO, 1) {
          @Override
          public boolean accepts(ItemStack stack) {
            return stack.is(Ic2Items.WATER_CELL) || stack.is(ClassicResources.DEAD_MAGNET);
          }
        };
    this.classicOutput =
        new InvSlot(this, "classicOutput", InvSlot.Access.IO, 1) {
          @Override
          public boolean accepts(ItemStack stack) {
            return stack.is(ClassicResources.ELECTROLYZED_WATER_CELL);
          }
        };
  }

  public static void init() {
    Recipes.electrolyzer = new ElectrolyzerRecipeManager();
    Recipes.electrolyzer.addRecipe(
        net.minecraft.world.level.material.Fluids.WATER,
        40,
        32,
        new IElectrolyzerRecipeManager.ElectrolyzerOutput(
            Ic2Fluids.HYDROGEN.still(), 26, Direction.DOWN),
        new IElectrolyzerRecipeManager.ElectrolyzerOutput(
            Ic2Fluids.OXYGEN.still(), 13, Direction.UP));
  }

  @Override
  protected void loadAdditional(
      CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
    super.loadAdditional(nbt, registries);
    this.progress = nbt.getInt("progress");
    this.classicCharge = Math.clamp(nbt.getInt("classicCharge"), 0, 20000);
    this.classicReturn = Math.clamp(nbt.getInt("classicReturn"), 0, 20000);
  }

  @Override
  public void saveAdditional(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
    super.saveAdditional(nbt, registries);
    nbt.putInt("progress", this.progress);
    nbt.putInt("classicCharge", this.classicCharge);
    nbt.putInt("classicReturn", this.classicReturn);
  }

  @Override
  public void updateEntityServer() {
    super.updateEntityServer();
    if (this.tickClassicElectrochemistry()) {
      this.setActive(true);
      this.progress = 0;
      if (this.getLevel().getGameTime() % 20 == 0) this.setChanged();
      return;
    }
    boolean needsInvUpdate = false;
    if (this.canOperate()) {
      assert this.recipe != null;
      this.setActive(true);
      this.energy.useEnergy(this.recipe.EUaTick());
      this.progress++;
      if (this.progress >= this.recipe.ticksNeeded()) {
        this.operate();
        this.progress = 0;
        needsInvUpdate = true;
      }
    } else {
      this.setActive(false);
      this.progress = 0;
    }

    needsInvUpdate |= this.upgradeSlot.tickNoMark();
    if (needsInvUpdate) {
      super.setChanged();
    }
  }

  protected boolean canOperate() {
    if (this.input.isEmpty()) {
      return false;
    }

    this.recipe =
        Recipes.electrolyzer.getElectrolysisInformation(this.input.getFluidStack().getFluid());
    if (this.recipe != null
        && !(this.energy.getEnergy() < this.recipe.EUaTick())
        && this.input.getFluidAmount() >= this.recipe.inputAmount()) {
      for (IElectrolyzerRecipeManager.ElectrolyzerOutput output : this.recipe.outputs()) {
        if (!this.canFillTank(output.tankDirection(), output.getOutput())) {
          return false;
        }
      }

      return true;
    } else {
      return false;
    }
  }

  private static boolean canMerge(ItemStack current, ItemStack result) {
    return current.isEmpty()
        || (ItemStack.isSameItemSameComponents(current, result)
            && current.getCount() + result.getCount() <= current.getMaxStackSize());
  }

  private static void merge(InvSlot slot, ItemStack result) {
    if (slot.isEmpty()) slot.put(result.copy());
    else {
      ItemStack combined = slot.get().copy();
      combined.grow(result.getCount());
      slot.put(combined);
    }
  }

  /** Classic's 70% charging / 30% recovery thresholds, hosted by the existing EU buffer. */
  private boolean tickClassicElectrochemistry() {
    if (this.classicReturn > 0) {
      int accepted = (int) this.energy.addEnergy(Math.min(128, this.classicReturn));
      this.classicReturn -= accepted;
      if (accepted > 0) this.setChanged();
      return accepted > 0;
    }
    ItemStack item = this.classicInput.get();
    ItemStack result =
        item.is(ClassicResources.DEAD_MAGNET)
            ? new ItemStack(ClassicResources.MAGNET)
            : item.is(Ic2Items.WATER_CELL)
                ? new ItemStack(ClassicResources.ELECTROLYZED_WATER_CELL)
                : ItemStack.EMPTY;
    if (!result.isEmpty()
        && canMerge(this.classicOutput.get(), result)
        && this.energy.getEnergy() >= this.energy.getCapacity() * 0.7) {
      int moved =
          (int) Math.min(128, Math.min(this.energy.getEnergy(), 20000 - this.classicCharge));
      this.energy.useEnergy(moved);
      this.classicCharge += moved;
      if (this.classicCharge >= 20000) {
        this.classicInput.get().shrink(1);
        merge(this.classicOutput, result);
        this.classicCharge = 0;
      }
      this.setChanged();
      return true;
    }
    if (this.energy.getEnergy() < this.energy.getCapacity() * 0.3
        && this.classicOutput.get().is(ClassicResources.ELECTROLYZED_WATER_CELL)
        && canMerge(this.classicInput.get(), new ItemStack(Ic2Items.WATER_CELL))) {
      this.classicOutput.get().shrink(1);
      merge(this.classicInput, new ItemStack(Ic2Items.WATER_CELL));
      this.classicReturn = Math.min(20000, 2000 * (6 + this.energy.getSinkTier()));
      this.setChanged();
      return true;
    }
    return false;
  }

  public double getClassicRatio() {
    return this.classicReturn > 0 ? this.classicReturn / 20000D : this.classicCharge / 20000D;
  }

  protected void operate() {
    assert this.recipe != null;
    this.input.drainMbUnchecked(this.recipe.inputAmount(), false);

    for (IElectrolyzerRecipeManager.ElectrolyzerOutput output : this.recipe.outputs()) {
      this.fillTank(output.tankDirection(), output.getOutput());
    }
  }

  protected boolean canFillTank(Direction facing, Ic2FluidStack fluid) {
    BlockEntity te = this.getLevel().getBlockEntity(this.worldPosition.relative(facing));
    return te instanceof TileEntityTank
        ? LiquidUtil.fillTile(te, facing, fluid, true) == fluid.getAmountMb()
        : false;
  }

  protected void fillTank(Direction facing, Ic2FluidStack fluid) {
    BlockEntity te = this.getLevel().getBlockEntity(this.worldPosition.relative(facing));
    if (te instanceof TileEntityTank) {
      LiquidUtil.fillTile(te, facing, fluid, false);
    }
  }

  @Override
  public Set<UpgradableProperty> getUpgradableProperties() {
    return EnumSet.of(UpgradableProperty.FluidConsuming);
  }

  @Override
  public double getEnergy() {
    return this.energy.getEnergy();
  }

  @Override
  public boolean useEnergy(double amount) {
    return this.energy.useEnergy(amount);
  }

  @Override
  public ContainerBase<?> createServerScreenHandler(int syncId, Player player) {
    return new ContainerElectrolyzer(syncId, player.getInventory(), this);
  }

  @Override
  public ContainerBase<?> createClientScreenHandler(
      int syncId, Inventory inventory, GrowingBuffer data) {
    return new ContainerElectrolyzer(syncId, inventory, this);
  }

  public Ic2FluidTank getInput() {
    return this.input;
  }

  public boolean hasRecipe() {
    return this.getCurrentRecipe() != null;
  }

  public IElectrolyzerRecipeManager.ElectrolyzerRecipe getCurrentRecipe() {
    return this.recipe;
  }

  @Override
  public double getRatio() {
    return this.recipe == null ? 0.0 : (double) this.progress / this.recipe.ticksNeeded();
  }

  @Override
  public SoundEvent getLoopingSoundEvent() {
    return Ic2SoundEvents.MACHINE_ELECTROLYZER_LOOP;
  }
}
