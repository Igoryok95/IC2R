package ic2.core.gametest;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.tile.IEnergyConductor;
import ic2.core.block.tileentity.Ic2TileEntityBlock;
import ic2.core.block.wiring.AbstractCableBlock;
import ic2.core.classic.ClassicEnergy;
import ic2.core.classic.ClassicWiring;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ic2")
@PrefixGameTestTemplate(false)
public final class ClassicWiringGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";

  @GameTest(template = EMPTY)
  public static void restoredCablesKeepInsulationLossAndFoamCounterparts(GameTestHelper h) {
    h.assertTrue(
        Math.abs(ClassicWiring.ALUMINIUM.getLoss() - 1.2) < .0001
            && Math.abs(ClassicWiring.QUADRUPLE_INSULATED_ALUMINIUM.getLoss() - 1) < .0001,
        "aluminium insulation retains the original conduction losses");
    h.assertTrue(
        Math.abs(ClassicWiring.BRONZE.getLoss() - .7) < .0001
            && Math.abs(ClassicWiring.DOUBLE_INSULATED_BRONZE.getLoss() - .6) < .0001,
        "bronze insulation retains the original conduction losses");
    for (var cable : ClassicWiring.CABLES.values()) {
      var foam = cable.getFoamCableBlock();
      h.assertTrue(
          foam != null && foam.getCableCounterpart() == cable,
          "spraying and removing foam has a real counterpart");
    }
    var blue =
        ClassicWiring.PLASMA
            .defaultBlockState()
            .setValue(AbstractCableBlock.colorProperty, DyeColor.BLUE);
    h.assertTrue(
        AbstractCableBlock.getColor(blue, ClassicWiring.PLASMA.type, 0) == DyeColor.BLUE,
        "plasma cables support original color isolation");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void plasmaCableTransmitsPesuPacketsWithoutLosingItsInsulation(GameTestHelper h) {
    BlockPos wire = new BlockPos(1, 1, 1);
    h.setBlock(wire.above(), ClassicEnergy.PESU);
    h.setBlock(wire, ClassicWiring.PLASMA);
    h.setBlock(wire.below(), ClassicEnergy.PESU);
    ClassicEnergy.Storage source = h.getBlockEntity(wire.above());
    ClassicEnergy.Storage sink = h.getBlockEntity(wire.below());
    source.energy.addEnergy(65536);
    h.succeedWhen(
        () -> {
          h.assertTrue(
              sink.energy.getEnergy() > 32000,
              "32768-EU source packet reaches PESU through plasma cable");
          h.assertBlockPresent(ClassicWiring.PLASMA, wire);
          var conductor =
              (IEnergyConductor) EnergyNet.instance.getTile(h.getLevel(), h.absolutePos(wire));
          h.assertTrue(
              conductor.getInsulationBreakdownEnergy() == 32769
                  && conductor.getInsulationEnergyAbsorption() == 32769,
              "plasma insulation supports its full voltage");
          h.assertTrue(
              source.energy.getEnergy() + sink.energy.getEnergy() <= 65536,
              "no energy is created along the path");
        });
  }

  @GameTest(template = EMPTY)
  public static void ivTransformerStepsDownPesuForIsu(GameTestHelper h) {
    h.setBlock(new BlockPos(1, 2, 1), ClassicEnergy.PESU);
    h.setBlock(
        new BlockPos(1, 1, 1),
        ClassicWiring.IV_TRANSFORMER
            .defaultBlockState()
            .setValue(Ic2TileEntityBlock.anyFacingProperty, Direction.UP));
    h.setBlock(new BlockPos(1, 0, 1), ClassicEnergy.ISU);
    ClassicEnergy.Storage source = h.getBlockEntity(new BlockPos(1, 2, 1));
    ClassicEnergy.Storage sink = h.getBlockEntity(new BlockPos(1, 0, 1));
    ClassicWiring.IvTransformer transformer = h.getBlockEntity(new BlockPos(1, 1, 1));
    source.energy.addEnergy(65536);
    h.succeedWhen(
        () -> {
          h.assertTrue(sink.energy.getEnergy() >= 8192, "step-down output safely charges ISU");
          h.assertTrue(
              transformer.getInputFlow() == 32768 && transformer.getOutputFlow() == 8192,
              "original IV transformation ratio is retained");
          h.assertBlockPresent(ClassicEnergy.ISU, new BlockPos(1, 0, 1));
        });
  }
}
