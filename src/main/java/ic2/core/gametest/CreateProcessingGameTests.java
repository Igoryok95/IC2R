package ic2.core.gametest;

import ic2.api.recipe.Recipes;
import ic2.core.block.machine.tileentity.TileEntityElectricFurnace;
import ic2.core.block.machine.tileentity.TileEntityInduction;
import ic2.core.block.machine.tileentity.TileEntityIronFurnace;
import ic2.core.block.machine.tileentity.TileEntityMacerator;
import ic2.core.item.ElectricItemManager;
import ic2.core.ref.Ic2Blocks;
import ic2.core.ref.Ic2Items;
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

@GameTestHolder("ic2_create")
@PrefixGameTestTemplate(false)
public final class CreateProcessingGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";

  private static ItemStack create(String name) {
    return new ItemStack(
        BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", name)));
  }

  @GameTest(template = EMPTY, timeoutTicks = 600)
  public static void rawZincDoublesIntoExistingCreateCrushedAndKeepsSmeltingXp(GameTestHelper h) {
    if (!ModList.get().isLoaded("create")) {
      h.succeed();
      return;
    }
    var grindPos = new BlockPos(0, 1, 1);
    var smeltPos = new BlockPos(2, 1, 1);
    h.setBlock(grindPos, Ic2Blocks.MACERATOR);
    h.setBlock(smeltPos, Ic2Blocks.ELECTRIC_FURNACE);
    TileEntityMacerator macerator = h.getBlockEntity(grindPos);
    TileEntityElectricFurnace furnace = h.getBlockEntity(smeltPos);
    macerator.dischargeSlot.put(ElectricItemManager.getCharged(Ic2Items.RE_BATTERY, 10000));
    furnace.dischargeSlot.put(ElectricItemManager.getCharged(Ic2Items.RE_BATTERY, 10000));
    macerator.inputSlot.put(create("raw_zinc"));
    h.startSequence()
        .thenWaitUntil(
            () ->
                h.assertTrue(
                    macerator.outputSlot.get().is(create("crushed_raw_zinc").getItem())
                        && macerator.outputSlot.get().getCount() == 2,
                    "uses the two existing Create crushed ores"))
        .thenExecute(
            () -> {
              furnace.inputSlot.put(macerator.outputSlot.get().copy());
              macerator.outputSlot.clear();
            })
        .thenWaitUntil(
            () -> {
              h.assertTrue(
                  furnace.outputSlot.get().is(create("zinc_ingot").getItem())
                      && furnace.outputSlot.get().getCount() == 2,
                  "both zinc ingots smelted");
              h.assertTrue(
                  Math.abs(
                          furnace
                                  .saveWithFullMetadata(h.getLevel().registryAccess())
                                  .getDouble("xp")
                              - .7)
                      < .0001,
                  "doubling preserves raw zinc XP");
            })
        .thenSucceed();
  }

  @GameTest(template = EMPTY)
  public static void createCrushedMetalsUseNativeWashingAndSmeltingRewards(GameTestHelper h) {
    if (!ModList.get().isLoaded("create")) {
      h.succeed();
      return;
    }
    for (var metal :
        List.of("iron", "gold", "copper", "lead", "tin", "silver", "uranium", "zinc")) {
      var crushed = create("crushed_raw_" + metal);
      h.assertTrue(
          !crushed.isEmpty()
              && (metal.equals("uranium")
                  || Recipes.macerator.get(h.getLevel()).apply(crushed, false) != null)
              && Recipes.oreWashing.get(h.getLevel()).apply(crushed, false) != null
              && Recipes.centrifuge.get(h.getLevel()).apply(crushed, false) != null,
          "complete native processing path: " + metal);
      var smelting = Recipes.furnace.apply(crushed, false);
      h.assertTrue(
          smelting != null && smelting.recipe().getMetaData().getFloat("experience") > 0,
          "positive native smelting reward: " + metal);
    }
    h.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 400)
  public static void ironAndInductionFurnacesRetainCreateXp(GameTestHelper h) {
    if (!ModList.get().isLoaded("create")) {
      h.succeed();
      return;
    }
    var first = new BlockPos(0, 1, 1);
    var second = new BlockPos(2, 1, 1);
    h.setBlock(first, Ic2Blocks.IRON_FURNACE);
    h.setBlock(second, Ic2Blocks.INDUCTION_FURNACE);
    TileEntityIronFurnace iron = h.getBlockEntity(first);
    TileEntityInduction induction = h.getBlockEntity(second);
    iron.inputSlot.put(create("crushed_raw_zinc").copyWithCount(2));
    iron.fuelSlot.put(new ItemStack(Items.COAL));
    induction.inputSlotA.put(create("crushed_raw_zinc"));
    induction.inputSlotB.put(create("crushed_raw_iron"));
    induction.operate();
    h.assertTrue(
        Math.abs(induction.saveWithFullMetadata(h.getLevel().registryAccess()).getDouble("xp") - .7)
            < .0001,
        "both induction channels keep their XP");
    h.succeedWhen(
        () -> {
          h.assertTrue(iron.outputSlot.get().getCount() == 2, "iron furnace output");
          h.assertTrue(
              Math.abs(
                      iron.saveWithFullMetadata(h.getLevel().registryAccess()).getDouble("xp") - .7)
                  < .0001,
              "iron furnace keeps Create XP");
        });
  }
}
