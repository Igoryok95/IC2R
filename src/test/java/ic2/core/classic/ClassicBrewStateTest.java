package ic2.core.classic;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

class ClassicBrewStateTest {
  @Test
  void classicBeerCompositionIsFrozenOnOpeningAndServingConservesVolume() {
    var brew = new ClassicBrewState();
    var random = RandomSource.create(1);
    assertEquals(8000, brew.addWater(8000));
    assertEquals(4, brew.addWheat(4, random));
    assertEquals(4, brew.addHops(4, random));
    brew.quality = 2;
    assertEquals(3, brew.solidRatio());
    assertEquals(3, brew.alcoholLevel());
    assertFalse(brew.open());
    assertEquals(3, brew.solids);
    assertEquals(3, brew.alcohol);
    assertEquals(0, brew.addWater(1000));
    assertEquals(0, brew.addWheat(1, random));
    assertNull(brew.serve(1, 500));
    brew.tapped = true;
    var drink = brew.serve(3, 500);
    assertEquals(3, drink.count());
    assertEquals(2, drink.quality());
    assertEquals(6500, brew.amount);
  }

  @Test
  void maturationUsesOriginalBeerStagesAndStopsWhenOpenedOrTapped() {
    var brew = new ClassicBrewState();
    brew.addWater(1000);
    brew.age = 23999;
    brew.tick();
    assertEquals(1, brew.quality);
    assertEquals(0, brew.age);
    assertEquals(72000, brew.beerStageTicks());
    brew.quality = 4;
    assertEquals(17496000, brew.beerStageTicks());
    brew.tapped = true;
    brew.tick();
    assertEquals(0, brew.age);
    brew.tapped = false;
    brew.open();
    brew.tick();
    assertEquals(0, brew.age);
  }

  @Test
  void whiskyUsesOriginalAgingEvaporationAndGristYield() {
    var brew = new ClassicBrewState();
    assertEquals(8, brew.addWhiskyGrist(8));
    assertEquals(10000, brew.addWater(10000));
    brew.quality = 9;
    brew.age = ClassicBrewState.WHISKY_YEAR_TICKS - 1;
    brew.tick();
    assertEquals(10, brew.quality);
    assertEquals(1, brew.whiskyYear());
    brew.open();
    assertEquals(4500, brew.amount);
    assertEquals(1, brew.alcohol);
    brew.tapped = true;
    assertEquals(1, brew.serve(1, 500).whiskyYear());
    assertEquals(4000, brew.amount);
  }

  @Test
  void rumUsesOriginalVolumeDependentTimeAndKeepsMaturityAfterServing() {
    var brew = new ClassicBrewState();
    assertEquals(8, brew.addSugarCane(8));
    int matureAt = (int) (1200 * 8 * Math.pow(0.95, 7));
    assertEquals(matureAt, brew.rumTicks());
    assertEquals(0, brew.addSugarCane(1));
    assertEquals(0, brew.addWater(1000));
    brew.age = matureAt;
    brew.tapped = true;
    var serving = brew.serve(2, 1000);
    assertEquals(100, serving.rumProgress());
    assertEquals(6000, brew.amount);
    assertEquals(brew.rumTicks(), brew.age);
    assertEquals(100, brew.serve(1, 1000).rumProgress());
  }

  @Test
  void depletedBarrelResetsItsRecipeAndAcceptsAnotherBrew() {
    var brew = new ClassicBrewState();
    brew.addSugarCane(1);
    brew.age = 1200;
    brew.tapped = true;
    assertNotNull(brew.serve(1, 1000));
    assertTrue(brew.isEmpty());
    assertEquals(0, brew.age);
    assertFalse(brew.opened);
    brew.tapped = false;
    assertEquals(1000, brew.addWater(1000));
  }

  @Test
  void allBrewStateSurvivesSavingAndReloading() {
    var brew = new ClassicBrewState();
    brew.addWater(16000);
    brew.addWheat(8, RandomSource.create(1));
    brew.addHops(12, RandomSource.create(1));
    brew.quality = 3;
    brew.age = 100;
    brew.open();
    brew.tapped = true;
    var restored = new ClassicBrewState();
    restored.load(brew.save());
    assertEquals(brew.save(), restored.save());
    assertEquals(brew.serve(1, 500), restored.serve(1, 500));
    assertEquals(brew.save(), restored.save());
  }

  @Test
  void potionMashPreservesOriginalModifiersAndSpoiledCloudTrigger() {
    var brew = new ClassicBrewState();
    assertTrue(brew.beginPotion());
    assertEquals(0, brew.addWater(1000));
    assertTrue(brew.selectPotion("minecraft:speed"));
    assertEquals(3000, brew.addWater(4000));
    assertEquals(20, brew.addPotionModifier(64, false));
    assertEquals(20, brew.addPotionModifier(64, true));
    brew.quality = 3;
    brew.open();
    assertEquals(2, brew.alcohol);
    assertEquals(1, brew.solids);
    var spoiled = new ClassicBrewState();
    spoiled.beginPotion();
    spoiled.selectPotion("minecraft:speed");
    spoiled.addWater(1000);
    spoiled.quality = 6;
    assertTrue(spoiled.open());
    assertTrue(spoiled.isEmpty());
  }
}
