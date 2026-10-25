package ic2.core.experimental;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

class ExperimentalBrewStateTest {
  @Test
  void beerUsesExperimentalAgingAndTapPausesIt() {
    var brew = new ExperimentalBrewState();
    var random = RandomSource.create(1);
    assertTrue(brew.add(0, 4, random));
    assertTrue(brew.add(1, 2, random));
    assertTrue(brew.add(2, 2, random));
    brew.age = 23999;
    brew.tick(true);
    assertEquals(23999, brew.age);
    brew.tick(false);
    assertEquals(1, brew.quality);
    assertEquals(0, brew.age);
    brew.age = 71999;
    brew.tick(false);
    assertEquals(2, brew.quality);
    brew.open();
    assertEquals(3, brew.alcohol);
    assertEquals(3, brew.solids);
    assertFalse(brew.add(1, 1, random));
  }

  @Test
  void rumKeepsMaturityWhenServedAndSurvivesSaving() {
    var brew = new ExperimentalBrewState();
    assertTrue(brew.add(3, 16, RandomSource.create(2)));
    brew.age = ExperimentalBrewState.rumTime(16);
    assertEquals(100, brew.progress());
    var copy = new ExperimentalBrewState();
    copy.load(brew.save());
    assertEquals(brew.value(), copy.value());
    assertTrue(copy.drain());
    assertEquals(15, copy.amount);
    assertEquals(100, copy.progress());
    for (int i = 0; i < 15; i++) assertTrue(copy.drain());
    assertEquals(0, copy.type);
    assertEquals(0, copy.amount);
    assertFalse(copy.opened);
  }

  @Test
  void ingredientCapacityAndRumAdditionWindowAreEnforced() {
    var brew = new ExperimentalBrewState();
    var random = RandomSource.create(3);
    assertTrue(brew.add(3, 31, random));
    assertFalse(brew.add(3, 2, random));
    brew.age = 601;
    assertFalse(brew.add(3, 1, random));
    assertFalse(brew.add(0, 1, random));
    var beer = new ExperimentalBrewState();
    assertTrue(beer.add(1, 64, random));
    assertFalse(beer.add(1, 1, random));
    assertFalse(beer.add(0, 33, random));
  }
}
