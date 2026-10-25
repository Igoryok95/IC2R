package ic2.core.gametest;

import ic2.api.recipe.Recipes;
import ic2.core.block.comp.Energy;
import ic2.core.block.machine.tileentity.TileEntityElectricFurnace;
import ic2.core.block.machine.tileentity.TileEntityInduction;
import ic2.core.block.machine.tileentity.TileEntityIronFurnace;
import ic2.core.block.machine.tileentity.TileEntityMacerator;
import ic2.core.ref.Ic2Blocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ic2_mekanism")
@PrefixGameTestTemplate(false)
public final class MekanismProcessingGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";

  private static ItemStack item(String id) {
    return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
  }

  private static boolean absent(GameTestHelper h) {
    if (ModList.get().isLoaded("mekanism")) return false;
    h.succeed();
    return true;
  }

  @GameTest(template = EMPTY, timeoutTicks = 1200)
  public static void rawOsmiumDoublesAndElectricFurnaceKeepsPointSixXp(GameTestHelper h) {
    if (absent(h)) return;
    var first = new BlockPos(0, 1, 1);
    var second = new BlockPos(2, 1, 1);
    h.setBlock(first, Ic2Blocks.MACERATOR);
    h.setBlock(second, Ic2Blocks.ELECTRIC_FURNACE);
    TileEntityMacerator macerator = h.getBlockEntity(first);
    TileEntityElectricFurnace furnace = h.getBlockEntity(second);
    macerator.getComponent(Energy.class).addEnergy(10000);
    furnace.dischargeSlot.put(
        ic2.core.item.ElectricItemManager.getCharged(ic2.core.ref.Ic2Items.RE_BATTERY, 10000));
    macerator.inputSlot.put(item("mekanism:raw_osmium"));
    h.startSequence()
        .thenWaitUntil(
            () ->
                h.assertTrue(
                    macerator.outputSlot.get().is(item("mekanism:dirty_dust_osmium").getItem())
                        && macerator.outputSlot.get().getCount() == 2,
                    "raw osmium doubles into existing dirty dust"))
        .thenExecute(
            () -> {
              var clean =
                  Recipes.oreWashing.get(h.getLevel()).apply(macerator.outputSlot.get(), false);
              h.assertTrue(clean != null, "native washing recipe accepts doubled output");
              var dust = clean.getOutput().iterator().next().copyWithCount(2);
              furnace.inputSlot.put(dust);
              macerator.outputSlot.clear();
            })
        .thenWaitUntil(
            () -> {
              h.assertTrue(furnace.outputSlot.get().getCount() == 2, "both osmium ingots smelted");
              h.assertTrue(
                  Math.abs(
                          furnace
                                  .saveWithFullMetadata(h.getLevel().registryAccess())
                                  .getDouble("xp")
                              - .6)
                      < 1e-6,
                  "processed osmium retains the raw smelting XP");
            })
        .thenSucceed();
  }

  @GameTest(template = EMPTY)
  public static void allMekanismMetalsAndFluoriteHaveNativeProcessingRoutes(GameTestHelper h) {
    if (absent(h)) return;
    for (String metal : List.of("iron", "gold", "copper", "tin", "lead", "uranium", "osmium")) {
      var dirty = item("mekanism:dirty_dust_" + metal);
      h.assertTrue(
          Recipes.macerator.get(h.getLevel()).apply(item("mekanism:clump_" + metal), false) != null
              && Recipes.macerator.get(h.getLevel()).apply(dirty, false) != null
              && Recipes.oreWashing.get(h.getLevel()).apply(dirty, false) != null
              && Recipes.centrifuge.get(h.getLevel()).apply(dirty, false) != null,
          "existing intermediates are accepted: " + metal);
    }
    for (String metal : List.of("tin", "lead", "uranium", "osmium"))
      for (String prefix : List.of("raw_", "block_raw_"))
        h.assertTrue(
            Recipes.macerator.get(h.getLevel()).apply(item("mekanism:" + prefix + metal), false)
                != null,
            "tagged raw material/block: " + metal);
    var fluorite = Recipes.macerator.get(h.getLevel()).apply(item("mekanism:fluorite_ore"), false);
    h.assertTrue(
        fluorite != null && fluorite.getOutput().iterator().next().getCount() == 6,
        "fluorite preserves Mekanism's six-gem ore yield");
    h.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 500)
  public static void createOsmiumAndOtherFurnacesRetainXp(GameTestHelper h) {
    if (absent(h)) return;
    var input = item("mekanism:dust_osmium");
    if (ModList.get().isLoaded("create")) {
      input = item("create:crushed_raw_osmium");
      h.assertTrue(
          Recipes.macerator.get(h.getLevel()).apply(input, false) != null
              && Recipes.oreWashing.get(h.getLevel()).apply(input, false) != null
              && Recipes.centrifuge.get(h.getLevel()).apply(input, false) != null,
          "Create crushed osmium is supported without duplicate items");
    }
    var first = new BlockPos(0, 1, 1);
    var second = new BlockPos(2, 1, 1);
    h.setBlock(first, Ic2Blocks.IRON_FURNACE);
    h.setBlock(second, Ic2Blocks.INDUCTION_FURNACE);
    TileEntityIronFurnace iron = h.getBlockEntity(first);
    TileEntityInduction induction = h.getBlockEntity(second);
    iron.inputSlot.put(input.copyWithCount(2));
    iron.fuelSlot.put(new ItemStack(Items.COAL));
    induction.inputSlotA.put(input.copy());
    induction.inputSlotB.put(input.copy());
    induction.operate();
    h.assertTrue(
        Math.abs(induction.saveWithFullMetadata(h.getLevel().registryAccess()).getDouble("xp") - .6)
            < 1e-6,
        "both induction channels retain osmium XP");
    h.succeedWhen(
        () -> {
          h.assertTrue(
              iron.outputSlot.get().getCount() == 2, "iron furnace smelted both osmium items");
          h.assertTrue(
              Math.abs(
                      iron.saveWithFullMetadata(h.getLevel().registryAccess()).getDouble("xp") - .6)
                  < 1e-6,
              "iron furnace retains osmium XP");
        });
  }

  private MekanismProcessingGameTests() {}
}
