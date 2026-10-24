package ic2.core.gametest;

import ic2.api.item.ElectricItem;
import ic2.api.recipe.Recipes;
import ic2.core.classic.ClassicChargingArmor;
import ic2.core.classic.ClassicMachines;
import ic2.core.classic.ClassicProcessMachine;
import ic2.core.classic.ClassicProcessRecipe;
import ic2.core.classic.ClassicResources;
import ic2.core.ref.Ic2Items;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ic2")
@PrefixGameTestTemplate(false)
public final class ClassicResourcesGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";
  private static final BlockPos POS = new BlockPos(1, 1, 1);

  @GameTest(template = EMPTY)
  public static void newOresKeepClassicDropRangesAndSilkTouch(GameTestHelper h) {
    ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
    ItemStack silk = tool.copy();
    silk.enchant(
        h.getLevel()
            .registryAccess()
            .lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(Enchantments.SILK_TOUCH),
        1);
    Block[] ores = {
      ClassicResources.SILVER_ORE,
      ClassicResources.DEEPSLATE_SILVER_ORE,
      ClassicResources.SILVER_ORE_NETHER,
      ClassicResources.ALUMINIUM_ORE_NETHER
    };
    int[] min = {1, 1, 2, 15};
    int[] max = {3, 3, 5, 25};
    for (int i = 0; i < ores.length; i++) {
      for (int repeat = 0; repeat < 30; repeat++) {
        var drops =
            Block.getDrops(
                ores[i].defaultBlockState(), h.getLevel(), h.absolutePos(POS), null, null, tool);
        h.assertValueEqual(drops.size(), 1, "one raw material drop");
        h.assertTrue(
            drops
                .getFirst()
                .is(i == 3 ? ClassicResources.RAW_ALUMINIUM : ClassicResources.RAW_SILVER),
            "correct raw material");
        h.assertTrue(
            drops.getFirst().getCount() >= min[i] && drops.getFirst().getCount() <= max[i],
            "original Classic drop range");
      }
      var preserved =
          Block.getDrops(
              ores[i].defaultBlockState(), h.getLevel(), h.absolutePos(POS), null, null, silk);
      h.assertTrue(
          preserved.size() == 1
              && preserved.getFirst().is(ores[i].asItem())
              && preserved.getFirst().getCount() == 1,
          "Silk Touch preserves the ore block");
    }
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void goldStillSuppliesSilverWithoutNewChunks(GameTestHelper h) {
    for (var form : List.of(Ic2Items.CRUSHED_GOLD, Ic2Items.PURIFIED_GOLD)) {
      var result = Recipes.centrifuge.get(h.getLevel()).apply(new ItemStack(form), false);
      h.assertTrue(
          result != null
              && result.getOutput().stream().anyMatch(s -> s.is(Ic2Items.SMALL_SILVER_DUST)),
          "original silver byproduct remains available");
    }
    var dust =
        Recipes.compressor
            .get(h.getLevel())
            .apply(new ItemStack(Ic2Items.SMALL_SILVER_DUST, 9), false);
    h.assertTrue(
        dust != null && dust.getOutput().stream().anyMatch(s -> s.is(Ic2Items.SILVER_DUST)),
        "small silver dust can be consolidated");
    h.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 120)
  public static void rareEarthExtractorAccumulatesFractionalMaterialAndSurvivesReload(
      GameTestHelper h) {
    h.setBlock(POS, ClassicMachines.RARE_EARTH_EXTRACTOR);
    ClassicProcessMachine machine = h.getBlockEntity(POS);
    machine.input.put(new ItemStack(Items.CLAY_BALL, 2));
    machine.addEnergy(1120);
    h.startSequence()
        .thenWaitUntil(
            () -> {
              h.assertValueEqual(machine.material, 8000, "first clay contributes half a dust");
              h.assertTrue(machine.output.isEmpty(), "fractional output cannot duplicate an item");
            })
        .thenExecute(
            () -> {
              var saved = machine.saveWithFullMetadata(h.getLevel().registryAccess());
              machine.loadWithComponents(saved, h.getLevel().registryAccess());
              h.assertValueEqual(machine.material, 8000, "fraction retained after loading");
            })
        .thenWaitUntil(
            () -> {
              h.assertTrue(
                  machine.output.get().is(ClassicResources.ALUMINIUM_DUST),
                  "two clay produce aluminium dust");
              h.assertValueEqual(machine.output.get().getCount(), 1, "one dust only");
              h.assertTrue(machine.input.isEmpty(), "both inputs consumed");
              h.assertValueEqual(machine.material, 0, "no phantom remainder");
            })
        .thenSucceed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 320)
  public static void alloySmelterRequiresSandAndFinishesOriginalRecipe(GameTestHelper h) {
    h.setBlock(POS, ClassicMachines.ALLOY_SMELTER);
    ClassicProcessMachine machine = h.getBlockEntity(POS);
    machine.input.put(0, new ItemStack(ClassicResources.ALUMINIUM_DUST));
    machine.addEnergy(4000);
    h.startSequence()
        .thenIdle(10)
        .thenExecute(
            () -> {
              h.assertTrue(machine.output.isEmpty(), "dust alone is insufficient");
              h.assertTrue(machine.storedEnergy() == 4000, "invalid input does not consume EU");
              machine.input.put(1, new ItemStack(Items.SAND));
            })
        .thenWaitUntil(
            () -> {
              h.assertTrue(
                  machine.output.get().is(ClassicResources.ALUMINIUM_INGOT),
                  "original alloy-smelting output");
              h.assertTrue(machine.input.isEmpty(), "dust and sand consumed together");
              h.assertTrue(machine.storedEnergy() == 0, "250 ticks at 16 EU/t");
            })
        .thenSucceed();
  }

  @GameTest(template = EMPTY)
  public static void overlappingRecipeInputsCannotConsumeTheSameItemsTwice(GameTestHelper h) {
    var recipe =
        new ClassicProcessRecipe(
            "test",
            List.of(
                new ClassicProcessRecipe.Input(
                    Ingredient.of(Items.IRON_INGOT, Items.GOLD_INGOT), 1),
                new ClassicProcessRecipe.Input(Ingredient.of(Items.IRON_INGOT), 1)),
            new ItemStack(Items.DIAMOND),
            1,
            1,
            16000);
    h.assertTrue(
        recipe.plan(new ItemStack[] {new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY}) == null,
        "one item cannot satisfy two ingredients");
    var plan =
        recipe.plan(
            new ItemStack[] {new ItemStack(Items.IRON_INGOT), new ItemStack(Items.GOLD_INGOT)});
    h.assertTrue(
        plan != null && plan[0] == 1 && plan[1] == 1,
        "overlapping matches backtrack to a valid allocation");
    h.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 60)
  public static void restoredProcessorSupportsNativeUpgradesAndPersistsThem(GameTestHelper h) {
    h.setBlock(POS, ClassicMachines.RARE_EARTH_EXTRACTOR);
    ClassicProcessMachine machine = h.getBlockEntity(POS);
    machine.upgrade.put(0, new ItemStack(Ic2Items.OVERCLOCKER_UPGRADE));
    machine.upgrade.put(1, new ItemStack(Ic2Items.TRANSFORMER_UPGRADE));
    machine.setChanged();
    var saved = machine.saveWithFullMetadata(h.getLevel().registryAccess());
    machine.loadWithComponents(saved, h.getLevel().registryAccess());
    machine.setChanged();
    h.assertTrue(
        machine.upgrade.get(0).is(Ic2Items.OVERCLOCKER_UPGRADE)
            && machine.getComponent(ic2.core.block.comp.Energy.class).getSinkTier() == 2,
        "original upgrade slots retain functional native IC2R upgrades after loading");
    machine.input.put(new ItemStack(Items.CLAY_BALL));
    machine.addEnergy(1120);
    h.startSequence()
        .thenWaitUntil(
            () -> {
              h.assertTrue(
                  machine.material == 8000 && machine.input.isEmpty(),
                  "overclocked processing still consumes exactly one clay");
              h.assertTrue(
                  machine.operationLength < 35, "overclocking actually shortens the operation");
              h.assertTrue(
                  machine.storedEnergy() < 1120 - 35, "increased speed uses more EU per operation");
            })
        .thenSucceed();
  }

  @GameTest(template = EMPTY)
  public static void solarHelmetOnlyWorksWhenEquippedAndInSunlight(GameTestHelper h) {
    var player = h.makeMockServerPlayerInLevel();
    player.moveTo(h.absolutePos(POS).getX(), h.absolutePos(POS).getY(), h.absolutePos(POS).getZ());
    ItemStack leggings = new ItemStack(Ic2Items.NANO_LEGGINGS);
    player.setItemSlot(EquipmentSlot.LEGS, leggings);
    h.getLevel().setDayTime(6000);
    h.getLevel().setWeatherParameters(6000, 0, false, false);
    ClassicChargingArmor.tickEquipped(player);
    h.assertTrue(ElectricItem.manager.getCharge(leggings) == 0, "no charger, no free energy");
    player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ClassicResources.SOLAR_HELMET));
    ClassicChargingArmor.tickEquipped(player);
    h.assertTrue(ElectricItem.manager.getCharge(leggings) == 1, "original one EU per tick");
    h.setBlock(POS.above(2), net.minecraft.world.level.block.Blocks.STONE);
    h.runAfterDelay(
        5,
        () -> {
          double before = ElectricItem.manager.getCharge(leggings);
          ClassicChargingArmor.tickEquipped(player);
          h.assertTrue(
              ElectricItem.manager.getCharge(leggings) == before,
              "roof blocks production after the light engine updates");
          h.succeed();
        });
  }
}
