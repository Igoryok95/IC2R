package ic2.core.gametest;

import ic2.api.network.INetworkClientTileEntityEventListener;
import ic2.api.recipe.Recipes;
import ic2.core.block.machine.tileentity.*;
import ic2.core.item.ElectricItemManager;
import ic2.core.ref.Ic2Blocks;
import ic2.core.ref.Ic2Items;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ic2")
@PrefixGameTestTemplate(false)
public final class FurnaceExperienceGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";
  private static final BlockPos POS = new BlockPos(1, 1, 1);

  @GameTest(template = EMPTY)
  public static void allProcessedOreSmeltingRecipesAwardExperience(GameTestHelper h) {
    int recipes = 0;
    for (String metal : List.of("copper", "gold", "iron", "lead", "silver", "tin", "uranium")) {
      for (String form : List.of(metal + "_dust", "crushed_" + metal, "purified_" + metal)) {
        var id = ResourceLocation.fromNamespaceAndPath("ic2", form);
        if (!BuiltInRegistries.ITEM.containsKey(id)) continue;
        var input = new ItemStack(BuiltInRegistries.ITEM.get(id));
        var recipe = Recipes.furnace.apply(input, false);
        h.assertTrue(
            recipe != null && recipe.recipe().getMetaData().getFloat("experience") > 0,
            "Processed ore has a real positive smelting reward: " + id);
        recipes++;
      }
    }
    h.assertValueEqual(recipes, 20, "all smeltable processed ore forms");
    h.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 600)
  public static void maceratorAndElectricFurnaceRetainRawIronExperience(GameTestHelper h) {
    var grindPos = new BlockPos(0, 1, 1);
    var smeltPos = new BlockPos(2, 1, 1);
    h.setBlock(grindPos, Ic2Blocks.MACERATOR);
    h.setBlock(smeltPos, Ic2Blocks.ELECTRIC_FURNACE);
    TileEntityMacerator macerator = h.getBlockEntity(grindPos);
    TileEntityElectricFurnace furnace = h.getBlockEntity(smeltPos);
    macerator.dischargeSlot.put(ElectricItemManager.getCharged(Ic2Items.RE_BATTERY, 10000));
    furnace.dischargeSlot.put(ElectricItemManager.getCharged(Ic2Items.RE_BATTERY, 10000));
    macerator.inputSlot.put(new ItemStack(Items.RAW_IRON));
    h.startSequence()
        .thenWaitUntil(
            () -> h.assertValueEqual(macerator.outputSlot.get().getCount(), 2, "crushed iron"))
        .thenExecute(
            () -> {
              furnace.inputSlot.put(macerator.outputSlot.get().copy());
              macerator.outputSlot.clear();
            })
        .thenWaitUntil(
            () -> {
              h.assertTrue(
                  furnace.outputSlot.get().is(Items.IRON_INGOT)
                      && furnace.outputSlot.get().getCount() == 2,
                  "Both crushed items were smelted");
              double rawXp =
                  Recipes.furnace
                      .apply(new ItemStack(Items.RAW_IRON), false)
                      .recipe()
                      .getMetaData()
                      .getFloat("experience");
              assertXp(h, furnace, rawXp);
            })
        .thenSucceed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 400)
  public static void ironFurnaceAlsoRetainsProcessedOreExperience(GameTestHelper h) {
    h.setBlock(POS, Ic2Blocks.IRON_FURNACE);
    TileEntityIronFurnace furnace = h.getBlockEntity(POS);
    furnace.inputSlot.put(new ItemStack(Ic2Items.IRON_DUST, 2));
    furnace.fuelSlot.put(new ItemStack(Items.COAL));
    h.succeedWhen(
        () -> {
          h.assertValueEqual(furnace.outputSlot.get().getCount(), 2, "smelted iron ingots");
          assertXp(h, furnace, 0.7);
        });
  }

  @GameTest(template = EMPTY, timeoutTicks = 100)
  public static void inductionCountsBothParallelInputs(GameTestHelper h) {
    var furnace = induction(h);
    furnace.inputSlotA.put(new ItemStack(Ic2Items.IRON_DUST, 2));
    furnace.inputSlotB.put(new ItemStack(Ic2Items.GOLD_DUST, 2));
    h.succeedWhen(
        () -> {
          h.assertValueEqual(furnace.outputSlotA.get().getCount(), 2, "first channel output");
          h.assertValueEqual(furnace.outputSlotB.get().getCount(), 2, "second channel output");
          assertXp(h, furnace, 1.7);
        });
  }

  @GameTest(template = EMPTY, timeoutTicks = 100)
  public static void inductionBlockedOutputDoesNotGrantExperience(GameTestHelper h) {
    var furnace = induction(h);
    furnace.inputSlotA.put(new ItemStack(Ic2Items.IRON_DUST));
    furnace.outputSlotA.put(new ItemStack(Items.GOLD_INGOT, 64));
    furnace.inputSlotB.put(new ItemStack(Ic2Items.GOLD_DUST));
    h.succeedWhen(
        () -> {
          h.assertValueEqual(furnace.outputSlotB.get().getCount(), 1, "unblocked output");
          h.assertValueEqual(furnace.inputSlotA.get().getCount(), 1, "blocked input stays intact");
          assertXp(h, furnace, 0.5);
        });
  }

  @GameTest(template = EMPTY)
  public static void collectedExperienceAndFractionPersistForEveryFurnace(GameTestHelper h) {
    Player player = h.makeMockPlayer(GameType.SURVIVAL);
    player.setPos(h.absolutePos(POS).getCenter());
    AABB area = new AABB(h.absolutePos(POS)).inflate(2);
    int earned = orbs(h, area);
    for (Block block :
        List.of(Ic2Blocks.IRON_FURNACE, Ic2Blocks.ELECTRIC_FURNACE, Ic2Blocks.INDUCTION_FURNACE)) {
      h.setBlock(POS, block);
      BlockEntity furnace = h.getBlockEntity(POS);
      var saved = furnace.saveWithFullMetadata(h.getLevel().registryAccess());
      saved.putDouble("xp", 4.75);
      furnace.loadWithComponents(saved, h.getLevel().registryAccess());
      var listener = (INetworkClientTileEntityEventListener) furnace;
      listener.onNetworkEvent(player, 1);
      assertXp(h, furnace, 4.75);
      listener.onNetworkEvent(player, 0);
      earned += 4;
      h.assertValueEqual(orbs(h, area), earned, "real experience orb value");
      assertXp(h, furnace, 0.75);
      var afterCollection = furnace.saveWithFullMetadata(h.getLevel().registryAccess());
      furnace.loadWithComponents(afterCollection, h.getLevel().registryAccess());
      listener.onNetworkEvent(player, 0);
      assertXp(h, furnace, 0.75);
      h.assertValueEqual(orbs(h, area), earned, "repeated / reloaded click does not duplicate XP");
    }
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void twentyDustOperationsProduceExactlySevenCollectableExperience(
      GameTestHelper h) {
    h.setBlock(POS, Ic2Blocks.ELECTRIC_FURNACE);
    TileEntityElectricFurnace furnace = h.getBlockEntity(POS);
    furnace.inputSlot.put(new ItemStack(Ic2Items.IRON_DUST, 20));
    for (int i = 0; i < 20; i++) {
      var recipe = furnace.inputSlot.process();
      furnace.operateOnce(recipe, List.of(recipe.getOutput()));
    }
    assertXp(h, furnace, 7);
    Player player = h.makeMockPlayer(GameType.SURVIVAL);
    player.setPos(h.absolutePos(POS).getCenter());
    AABB area = new AABB(h.absolutePos(POS)).inflate(2);
    int before = orbs(h, area);
    furnace.onNetworkEvent(player, 0);
    h.assertValueEqual(
        orbs(h, area) - before, 7, "full reward is collectable without float rounding loss");
    assertXp(h, furnace, 0);
    h.succeed();
  }

  private static TileEntityInduction induction(GameTestHelper h) {
    h.setBlock(POS, Ic2Blocks.INDUCTION_FURNACE);
    TileEntityInduction furnace = h.getBlockEntity(POS);
    furnace.dischargeSlot.put(ElectricItemManager.getCharged(Ic2Items.ADVANCED_RE_BATTERY, 100000));
    furnace.heat = 10000;
    return furnace;
  }

  private static void assertXp(GameTestHelper h, BlockEntity furnace, double expected) {
    double actual = furnace.saveWithFullMetadata(h.getLevel().registryAccess()).getDouble("xp");
    h.assertTrue(
        Math.abs(actual - expected) < 0.000001,
        "stored XP: expected " + expected + ", got " + actual);
  }

  private static int orbs(GameTestHelper h, AABB area) {
    return h.getLevel().getEntitiesOfClass(ExperienceOrb.class, area).stream()
        .mapToInt(ExperienceOrb::getValue)
        .sum();
  }
}
