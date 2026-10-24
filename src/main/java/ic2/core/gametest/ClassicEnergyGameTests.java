package ic2.core.gametest;

import ic2.api.item.ElectricItem;
import ic2.core.block.comp.Energy;
import ic2.core.block.machine.tileentity.TileEntityElectrolyzer;
import ic2.core.block.machine.tileentity.TileEntityTeleporter;
import ic2.core.classic.ClassicEnergy;
import ic2.core.classic.ClassicResources;
import ic2.core.ref.Ic2Blocks;
import ic2.core.ref.Ic2Items;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ic2")
@PrefixGameTestTemplate(false)
public final class ClassicEnergyGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";
  private static final BlockPos POS = new BlockPos(1, 1, 1);

  @GameTest(template = EMPTY)
  public static void pesuPreservesFourBillionEuAndTeleporterDoesNotTruncateIt(GameTestHelper h) {
    h.setBlock(POS, ClassicEnergy.PESU);
    ClassicEnergy.Storage store = h.getBlockEntity(POS);
    store.energy.addEnergy(4_000_000_000D);
    h.assertTrue(
        store.getStoredLong() == 4_000_000_000L && store.getCapacityLong() == 4_000_000_000L,
        "exact capacity and charge exceed signed integer range");
    var saved = store.saveWithFullMetadata(h.getLevel().registryAccess());
    store.loadWithComponents(saved, h.getLevel().registryAccess());
    h.assertTrue(
        store.energy.getEnergy() == 4_000_000_000D, "save/load preserves the full EU amount");
    h.setBlock(POS.below(), Ic2Blocks.TELEPORTER);
    TileEntityTeleporter teleporter = h.getBlockEntity(POS.below());
    h.assertValueEqual(
        teleporter.getAvailableEnergy(),
        Integer.MAX_VALUE,
        "legacy total saturates without wrapping");
    teleporter.consumeEnergy(100);
    h.assertTrue(
        store.getStoredLong() == 3_999_999_900L, "small teleport drains only its real cost");
    h.setBlock(POS.below().east(), ClassicEnergy.PESU);
    ClassicEnergy.Storage second = h.getBlockEntity(POS.below().east());
    second.energy.addEnergy(4_000_000_000D);
    teleporter.consumeEnergy(Integer.MAX_VALUE);
    h.assertTrue(
        store.getStoredLong() + second.getStoredLong() == 7_999_999_900L - Integer.MAX_VALUE,
        "maximum integer cost split across stores cannot overflow its division");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void glowtronicCraftKeepsLapotronCharge(GameTestHelper h) {
    var id = ResourceLocation.fromNamespaceAndPath("ic2", "shaped/glowtronic_crystal");
    var recipe = (CraftingRecipe) h.getLevel().getRecipeManager().byKey(id).orElseThrow().value();
    ItemStack lapotron = new ItemStack(Ic2Items.LAPOTRON_CRYSTAL);
    ElectricItem.manager.charge(lapotron, 7_654_321, Integer.MAX_VALUE, true, false);
    ItemStack glow = new ItemStack(Items.GLOWSTONE_DUST);
    ItemStack circuit = new ItemStack(ClassicResources.COMPLEX_CIRCUIT);
    var input =
        CraftingInput.of(
            3, 3, List.of(glow, circuit, glow, glow, lapotron, glow, glow, circuit, glow));
    h.assertTrue(
        recipe.matches(input, h.getLevel()),
        "original progression recipe is present in standalone IC2R");
    ItemStack result = recipe.assemble(input, h.getLevel().registryAccess());
    h.assertTrue(
        result.is(ClassicEnergy.GLOWTRONIC_CRYSTAL)
            && ElectricItem.manager.getCharge(result) == 7_654_321,
        "upgrading a charged crystal keeps its EU");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void electrolyzerChargesMagnetsAndRetainsPartialEnergyAcrossReload(
      GameTestHelper h) {
    h.setBlock(POS, Ic2Blocks.ELECTROLYZER);
    TileEntityElectrolyzer machine = h.getBlockEntity(POS);
    Energy energy = machine.getComponent(Energy.class);
    machine.classicInput.put(new ItemStack(ClassicResources.DEAD_MAGNET));
    double spent = 0;
    for (int i = 0; i < 40; i++) {
      energy.addEnergy(32000);
      double before = energy.getEnergy();
      machine.updateEntityServer();
      spent += before - energy.getEnergy();
    }
    var saved = machine.saveWithFullMetadata(h.getLevel().registryAccess());
    machine.loadWithComponents(saved, h.getLevel().registryAccess());
    h.assertValueEqual(machine.classicCharge, 5120, "partial charge persists");
    for (int i = 0; i < 117; i++) {
      energy.addEnergy(32000);
      double before = energy.getEnergy();
      machine.updateEntityServer();
      spent += before - energy.getEnergy();
    }
    h.assertTrue(
        machine.classicInput.isEmpty() && machine.classicOutput.get().is(ClassicResources.MAGNET),
        "dead magnet becomes exactly one charged magnet");
    h.assertTrue(
        spent == 20000 && machine.classicCharge == 0,
        "original 20000 EU cost, no lost or extra charge");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void electrolyzerRecoveryCannotDuplicateCellsOrStoredEu(GameTestHelper h) {
    h.setBlock(POS, Ic2Blocks.ELECTROLYZER);
    TileEntityElectrolyzer machine = h.getBlockEntity(POS);
    Energy energy = machine.getComponent(Energy.class);
    machine.classicOutput.put(new ItemStack(ClassicResources.ELECTROLYZED_WATER_CELL));
    machine.updateEntityServer();
    h.assertTrue(
        machine.classicOutput.isEmpty() && machine.classicInput.get().is(Ic2Items.WATER_CELL),
        "one charged cell returns one water cell");
    for (int i = 0; i < 125; i++) machine.updateEntityServer();
    h.assertTrue(
        energy.getEnergy() == 16000 && machine.classicReturn == 0,
        "tier 2 recovers exactly 16000 EU including its buffered remainder");
    for (int i = 0; i < 30; i++) machine.updateEntityServer();
    h.assertTrue(
        energy.getEnergy() == 16000 && machine.classicInput.get().getCount() == 1,
        "depleted cell cannot return energy twice");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void blockedElectrolyzerOutputDoesNotSpendChargingEnergy(GameTestHelper h) {
    h.setBlock(POS, Ic2Blocks.ELECTROLYZER);
    TileEntityElectrolyzer machine = h.getBlockEntity(POS);
    machine.getComponent(Energy.class).addEnergy(32000);
    machine.classicInput.put(new ItemStack(ClassicResources.DEAD_MAGNET));
    machine.classicOutput.put(new ItemStack(ClassicResources.ELECTROLYZED_WATER_CELL));
    machine.updateEntityServer();
    h.assertTrue(
        machine.getEnergy() == 32000 && machine.classicCharge == 0,
        "an incompatible occupied output cannot consume energy or input");
    h.succeed();
  }
}
