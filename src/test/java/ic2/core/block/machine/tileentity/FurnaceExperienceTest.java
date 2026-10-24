package ic2.core.block.machine.tileentity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FurnaceExperienceTest {
  @Test
  void fractionalSmeltingRewardsAccumulateWithoutFloatDrift() {
    double xp = 0;
    for (int i = 0; i < 20; i++) xp = TileEntityIronFurnace.addXP(xp, 0.35F);
    assertEquals(7.0, xp);
    for (int i = 20; i < 1000; i++) xp = TileEntityIronFurnace.addXP(xp, 0.35F);
    assertEquals(350.0, xp);
  }

  @Test
  void collectingWholeExperienceKeepsAnExactReusableFraction() {
    double remainder = TileEntityIronFurnace.fractionalXP(2.55);
    assertEquals(0.55, remainder);
    remainder = TileEntityIronFurnace.addXP(remainder, 0.35F);
    remainder = TileEntityIronFurnace.addXP(remainder, 0.1F);
    assertEquals(1.0, remainder);
    assertEquals(0.0, TileEntityIronFurnace.fractionalXP(remainder));
  }

  @Test
  void mixedRecipesKeepTheirFractionalRemainder() {
    double xp = TileEntityIronFurnace.addXP(0.75, 0.35F);
    xp = TileEntityIronFurnace.addXP(xp, 0.1F);
    assertEquals(1.2, xp);
  }

  @Test
  void zeroAndInvalidRecipeRewardsDoNotCorruptStoredExperience() {
    assertEquals(0.75, TileEntityIronFurnace.addXP(0.75, 0));
    assertEquals(0.75, TileEntityIronFurnace.addXP(0.75, Float.NaN));
    assertEquals(0.75, TileEntityIronFurnace.addXP(0.75, Float.POSITIVE_INFINITY));
  }
}
