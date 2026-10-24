package ic2.core.classic;

import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.ContainerBase;
import ic2.core.IHasGui;
import ic2.core.block.comp.Redstone;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.block.machine.tileentity.TileEntityElectricMachine;
import ic2.core.gui.dynamic.DynamicContainer;
import ic2.core.gui.dynamic.IGuiValueProvider;
import ic2.core.network.GrowingBuffer;
import ic2.core.network.GuiSynced;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;

public final class ClassicProcessMachine extends TileEntityElectricMachine
    implements IHasGui, IGuiValueProvider, IUpgradableBlock {
  public final InvSlot input;
  public final InvSlotOutput output;
  public final Redstone redstone;
  public final InvSlotUpgrade upgrade;
  private final boolean centrifugal;
  private final String recipeMachine;
  @GuiSynced public float progress;
  @GuiSynced public int material;
  @GuiSynced public int speed;
  @GuiSynced public int operationLength;
  private String materialId = "";
  private ResourceLocation workingRecipe;

  public ClassicProcessMachine(BlockPos pos, BlockState state) {
    super(
        ClassicMachines.PROCESSOR,
        pos,
        state,
        state.is(ClassicMachines.ALLOY_SMELTER)
            ? 4000
            : state.is(ClassicMachines.RARE_EARTH_CENTRIFUGE) ? 5120 : 1120,
        state.is(ClassicMachines.RARE_EARTH_CENTRIFUGE) ? 2 : 1);
    this.centrifugal = state.is(ClassicMachines.RARE_EARTH_CENTRIFUGE);
    this.recipeMachine =
        state.is(ClassicMachines.ALLOY_SMELTER) ? "alloy_smelter" : "rare_earth_extractor";
    this.input =
        new InvSlot(this, "input", InvSlot.Access.I, recipeMachine.equals("alloy_smelter") ? 2 : 1);
    this.output = new InvSlotOutput(this, "output", 1);
    this.redstone = addComponent(new Redstone(this));
    this.upgrade =
        new InvSlotUpgrade(this, "upgrade", recipeMachine.equals("alloy_smelter") ? 4 : 2);
    this.operationLength = centrifugal ? 99 : recipeMachine.equals("alloy_smelter") ? 250 : 35;
    this.comparator.setUpdate(() -> this.getActive() ? 15 : 0);
  }

  @Override
  protected void updateEntityServer() {
    super.updateEntityServer();
    if (upgrade.tickNoMark()) setChanged();
    ItemStack[] slots = new ItemStack[input.size()];
    for (int i = 0; i < slots.length; i++) slots[i] = input.get(i);
    List<RecipeHolder<ClassicProcessRecipe>> recipes =
        getLevel().getRecipeManager().getAllRecipesFor(ClassicMachines.RECIPE_TYPE);
    RecipeHolder<ClassicProcessRecipe> selected = null;
    int[] plan = null;
    for (var holder : recipes) {
      if (!holder.value().machine().equals(recipeMachine)) continue;
      int[] candidate = holder.value().plan(slots);
      if (candidate != null && canConsume(slots, candidate)) {
        selected = holder;
        plan = candidate;
        break;
      }
    }
    if (centrifugal) {
      if (energy.getEnergy() >= 1 && (!input.isEmpty() || redstone.hasRedstoneInput())) {
        speed = Math.min(10000, speed + 1);
        energy.useEnergy(1);
      } else speed = Math.max(0, speed - 4);
    }
    if (selected == null) {
      progress = Math.max(0, progress - (input.isEmpty() ? progress : 1));
      workingRecipe = null;
      setActive(centrifugal && speed > 0);
      return;
    }
    var recipe = selected.value();
    if (!selected.id().equals(workingRecipe)) {
      progress = 0;
      workingRecipe = selected.id();
    }
    String outputId = BuiltInRegistries.ITEM.getKey(recipe.result().getItem()).toString();
    if (!outputId.equals(materialId)) {
      materialId = outputId;
      material = 0;
    }
    boolean produces = material + recipe.materialUnits() >= 16000;
    int eu = upgrade.getEnergyDemand(centrifugal ? 15 : recipe.euPerTick());
    if ((produces && !output.canAdd(recipe.result()))
        || energy.getEnergy() < eu
        || (centrifugal && speed == 0)) {
      setActive(false);
      return;
    }
    energy.useEnergy(eu);
    setActive(true);
    int baseTicks = centrifugal ? 99 : recipe.ticks();
    int ticks = upgrade.getOperationLength(baseTicks);
    if (ticks != operationLength) {
      progress = progress / operationLength * ticks;
      operationLength = ticks;
    }
    progress += centrifugal ? speed / 300F : 1;
    if (progress >= operationLength) {
      progress = 0;
      int operations = Math.min(64, upgrade.getOperationsPerTick(baseTicks));
      for (int operation = 0; operation < operations; operation++) {
        for (int i = 0; i < slots.length; i++) slots[i] = input.get(i);
        plan = recipe.plan(slots);
        if (plan == null
            || !canConsume(slots, plan)
            || (material + recipe.materialUnits() >= 16000 && !output.canAdd(recipe.result())))
          break;
        for (int i = 0; i < slots.length; i++) {
          if (plan[i] == 0) continue;
          ItemStack stack = input.get(i);
          if (stack.hasCraftingRemainingItem()) {
            ItemStack remainder = stack.getCraftingRemainingItem();
            input.put(i, remainder);
          } else {
            stack.shrink(plan[i]);
            input.put(i, stack);
          }
        }
        material += recipe.materialUnits();
        if (material >= 16000) {
          material -= 16000;
          output.add(recipe.result().copy());
        }
      }
      setChanged();
    }
    if (getLevel().getGameTime() % 20 == 0) setChanged();
  }

  @Override
  protected void onLoaded() {
    super.onLoaded();
    if (!getLevel().isClientSide) updateUpgrades();
  }

  @Override
  public void setChanged() {
    super.setChanged();
    if (upgrade != null
        && recipeMachine != null
        && (getLevel() == null || !getLevel().isClientSide)) updateUpgrades();
  }

  private void updateUpgrades() {
    upgrade.onChanged();
    int tier = upgrade.getTier(centrifugal ? 2 : 1);
    energy.setSinkTier(tier);
    dischargeSlot.setTier(tier);
    int capacity = centrifugal ? 5120 : recipeMachine.equals("alloy_smelter") ? 4000 : 1120;
    energy.setCapacity(
        upgrade.isEmpty()
            ? capacity
            : upgrade.getEnergyStorage(
                capacity,
                centrifugal ? 99 : recipeMachine.equals("alloy_smelter") ? 250 : 35,
                centrifugal ? 15 : recipeMachine.equals("alloy_smelter") ? 16 : 1));
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
  public Set<UpgradableProperty> getUpgradableProperties() {
    return EnumSet.of(
        UpgradableProperty.Processing,
        UpgradableProperty.RedstoneSensitive,
        UpgradableProperty.Transformer,
        UpgradableProperty.EnergyStorage,
        UpgradableProperty.ItemConsuming,
        UpgradableProperty.ItemProducing);
  }

  private static boolean canConsume(ItemStack[] slots, int[] plan) {
    for (int i = 0; i < slots.length; i++) {
      if (plan[i] > 0
          && slots[i].hasCraftingRemainingItem()
          && (plan[i] != 1 || slots[i].getCount() != 1)) return false;
    }
    return true;
  }

  @Override
  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    progress = Math.max(0, tag.getFloat("ClassicProgress"));
    operationLength =
        Math.max(
            1,
            tag.contains("ClassicOperationLength")
                ? tag.getInt("ClassicOperationLength")
                : operationLength);
    material = Math.clamp(tag.getInt("ClassicMaterial"), 0, 15999);
    speed = Math.clamp(tag.getInt("ClassicSpeed"), 0, 10000);
    materialId = tag.getString("ClassicMaterialId");
    workingRecipe = ResourceLocation.tryParse(tag.getString("ClassicRecipe"));
  }

  @Override
  public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.saveAdditional(tag, registries);
    tag.putFloat("ClassicProgress", progress);
    tag.putInt("ClassicOperationLength", operationLength);
    tag.putInt("ClassicMaterial", material);
    tag.putInt("ClassicSpeed", speed);
    tag.putString("ClassicMaterialId", materialId);
    if (workingRecipe != null) tag.putString("ClassicRecipe", workingRecipe.toString());
  }

  @Override
  public double getGuiValue(String name) {
    return switch (name) {
      case "progress" -> progress / Math.max(1, operationLength);
      case "material" -> material / 16000D;
      case "speed" -> speed / 10000D;
      default -> throw new IllegalArgumentException("Unknown Classic processing gauge " + name);
    };
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

  public double storedEnergy() {
    return energy.getEnergy();
  }

  public String getSpeedPercent() {
    return net.minecraft.network.chat.Component.translatable(
            "gui.ic2.classic.speed_percent",
            java.text.DecimalFormat.getNumberInstance(java.util.Locale.ROOT).format(speed / 100D))
        .getString();
  }

  public String getMaterialPercent() {
    return java.text.NumberFormat.getNumberInstance(java.util.Locale.ROOT).format(material / 160D);
  }

  public void addEnergy(double eu) {
    energy.addEnergy(eu);
  }
}
