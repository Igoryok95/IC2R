package ic2.core.block.heatgenerator.tileentity;

import ic2.core.block.comp.Energy;
import ic2.core.block.machine.tileentity.TileEntityBlastFurnace;
import ic2.core.block.tileentity.Ic2TileEntityBlock;
import ic2.core.block.tileentity.TileEntityHeatSourceInventory;
import ic2.core.ref.Ic2Blocks;
import ic2.core.ref.Ic2Items;
import ic2.integration.jade.MachineStatusData;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ic2")
@PrefixGameTestTemplate(false)
public final class HeatOutputGameTests {
  @GameTest(template = "gametest/empty3x3x3")
  public static void allHeatSourcesReportCompletedTickAndResetWhenIdle(GameTestHelper h) {
    var sources = new ArrayList<TileEntityHeatSourceInventory>();
    var blocks =
        new net.minecraft.world.level.block.Block[] {
          Ic2Blocks.ELECTRIC_HEAT_GENERATOR, Ic2Blocks.FLUID_HEAT_GENERATOR,
          Ic2Blocks.RT_HEAT_GENERATOR, Ic2Blocks.SOLID_HEAT_GENERATOR,
          Ic2Blocks.LIQUID_HEAT_EXCHANGER
        };
    for (int i = 0; i < blocks.length; i++) {
      var pos = new BlockPos(i % 3, 1, i / 3);
      h.setBlock(pos, blocks[i]);
      sources.add(h.getBlockEntity(pos));
    }
    h.runAtTickTime(
        2,
        () -> {
          for (var source : sources) {
            source.setHeatBuffer(10);
            Direction side = source.getFacing();
            h.assertTrue(source.drawHeat(side, 3, false) == 3, "first real draw");
            h.assertTrue(source.drawHeat(side, 2, true) == 2, "simulation reports available heat");
            h.assertTrue(source.drawHeat(side.getOpposite(), 2, false) == 0, "wrong side rejected");
            h.assertTrue(
                source.drawHeat(side, -1, false) == 0, "negative request cannot create heat");
            h.assertTrue(
                source.drawHeat(side, 4, false) == 4 && source.getHeatBuffer() == 3,
                "only real draws drain buffer");
            h.assertTrue(source.gettransmitHeat() == 0, "current incomplete tick is not published");
          }
        });
    h.runAtTickTime(
        3,
        () -> {
          for (var source : sources) {
            h.assertTrue(
                source.gettransmitHeat() == 7, "multiple real draws are summed once per tick");
            h.assertTrue(source.getOutput().startsWith("7 / "), "GUI uses completed tick");
            var data = MachineStatusData.collect(source);
            h.assertTrue(data.getInt("heatOutput") == 7, "Jade and GUI share heat output");
            h.assertTrue(
                data.getInt("maxHeatOutput") == source.getMaxHeatEmittedPerTick(),
                "Jade carries maximum output");
          }
        });
    h.runAtTickTime(
        4,
        () -> {
          for (var source : sources) {
            h.assertTrue(source.gettransmitHeat() == 0, "idle tick clears old transfer");
            h.assertTrue(
                MachineStatusData.collect(source).getInt("heatOutput") == 0,
                "Jade clears old transfer");
          }
          h.succeed();
        });
  }

  @GameTest(template = "gametest/empty3x3x3", timeoutTicks = 40)
  public static void blastFurnaceToggleClearsHeatMeterAndElectricActiveState(GameTestHelper h) {
    var pos = new BlockPos(1, 1, 1);
    h.setBlock(pos, Ic2Blocks.BLAST_FURNACE);
    TileEntityBlastFurnace furnace = h.getBlockEntity(pos);
    var heaterPos = pos.relative(furnace.getFacing());
    h.setBlock(
        heaterPos,
        Ic2Blocks.ELECTRIC_HEAT_GENERATOR
            .defaultBlockState()
            .setValue(Ic2TileEntityBlock.anyFacingProperty, furnace.getFacing().getOpposite()));
    TileEntityElectricHeatGenerator heater = h.getBlockEntity(heaterPos);
    heater.coilSlot.put(0, new ItemStack(Ic2Items.COIL));
    heater.getComponent(Energy.class).addEnergy(10000);
    h.runAtTickTime(
        5,
        () -> {
          h.assertTrue(
              heater.getHeatBuffer() == 10 && !heater.getActive(),
              "full idle buffer does not leave heater active");
          h.assertTrue(heater.gettransmitHeat() == 0, "disabled empty furnace draws no heat");
          furnace.setManualWarmup(true);
        });
    h.runAtTickTime(
        12,
        () -> {
          h.assertTrue(
              furnace.heat > 0 && heater.gettransmitHeat() == 10,
              "enabled furnace receives ten hU per tick");
          h.assertTrue(heater.getActive(), "heater converts EU while replenishing consumed heat");
          h.assertTrue(
              MachineStatusData.collect(heater).getInt("heatOutput") == 10,
              "Jade reports actual hU separately from EU");
          furnace.setManualWarmup(false);
        });
    h.runAtTickTime(
        18,
        () -> {
          h.assertTrue(
              heater.gettransmitHeat() == 0 && !heater.getActive(),
              "disabled furnace clears transfer and working state");
          h.assertTrue(heater.getHeatBuffer() == 10, "stored heat is retained");
          h.assertTrue(
              MachineStatusData.collect(heater).getInt("heatOutput") == 0,
              "Jade stops displaying heat transfer");
          h.succeed();
        });
  }

  private HeatOutputGameTests() {}
}
