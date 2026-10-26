package ic2.core.gametest;

import ic2.core.IHasGui;
import ic2.core.block.comp.Energy;
import ic2.core.block.machine.tileentity.TileEntityBlastFurnace;
import ic2.core.block.machine.tileentity.TileEntityCentrifuge;
import ic2.core.block.machine.tileentity.TileEntityInduction;
import ic2.core.block.tileentity.Ic2TileEntity;
import ic2.core.gui.dynamic.MachineTransferSlots;
import ic2.core.ref.Ic2Blocks;
import ic2.core.slot.SlotInvSlot;
import ic2.integration.jade.MachineStatusData;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ic2")
@PrefixGameTestTemplate(false)
public final class MachineIntegrationGameTests {
  @GameTest(template = "gametest/empty3x3x3")
  public static void nativeMachineTransfersUseActualInputsAndPlayerInventory(GameTestHelper h) {
    var player = h.makeMockPlayer(GameType.SURVIVAL);
    var pos = new BlockPos(1, 1, 1);
    for (var block :
        List.of(
            Ic2Blocks.MACERATOR,
            Ic2Blocks.COMPRESSOR,
            Ic2Blocks.EXTRACTOR,
            Ic2Blocks.CENTRIFUGE,
            Ic2Blocks.BLOCK_CUTTER,
            Ic2Blocks.BLAST_FURNACE,
            Ic2Blocks.ORE_WASHING_PLANT,
            Ic2Blocks.METAL_FORMER)) {
      h.setBlock(pos, block);
      var machine = (IHasGui) h.getBlockEntity(pos);
      var menu = machine.createServerScreenHandler(1, player);
      var input = MachineTransferSlots.inputs(menu);
      var inventory = MachineTransferSlots.inventory(menu);
      h.assertTrue(
          input.size() == 1 && input.getFirst() instanceof SlotInvSlot,
          "a machine recipe targets only its real processing input");
      h.assertTrue(
          inventory.size() == 36
              && inventory.stream().allMatch(slot -> slot.container == player.getInventory()),
          "all 36 actual player slots are available");
      h.assertTrue(
          input.stream().noneMatch(inventory::contains),
          "outputs, upgrades, battery and tank containers are not recipe inputs or player inventory");
    }
    h.setBlock(pos, Ic2Blocks.SOLID_CANNER);
    var canner = ((IHasGui) h.getBlockEntity(pos)).createServerScreenHandler(1, player);
    var cannerInputs = MachineTransferSlots.inputs(canner);
    h.assertTrue(
        cannerInputs.size() == 2
            && ((SlotInvSlot) cannerInputs.get(0)).invSlot.name.equals("canInput")
            && ((SlotInvSlot) cannerInputs.get(1)).invSlot.name.equals("input"),
        "canner matches container-first recipe order without battery or upgrade slots");
    h.succeed();
  }

  @GameTest(template = "gametest/empty3x3x3")
  public static void jadeReadingsExposeEnergyAndFractionalMachineProgressWithoutJadeInstalled(
      GameTestHelper h) {
    var pos = new BlockPos(1, 1, 1);
    for (var block :
        List.of(
            Ic2Blocks.BATBOX,
            Ic2Blocks.CESU,
            Ic2Blocks.MFE,
            Ic2Blocks.MFSU,
            Ic2Blocks.BATBOX_CHARGEPAD,
            Ic2Blocks.CESU_CHARGEPAD,
            Ic2Blocks.MFE_CHARGEPAD,
            Ic2Blocks.MFSU_CHARGEPAD)) {
      h.setBlock(pos, block);
      Ic2TileEntity tile = h.getBlockEntity(pos);
      tile.getComponent(Energy.class).addEnergy(1234.5);
      var data = MachineStatusData.collect(tile);
      h.assertTrue(
          data.getDouble("energy") == 1234.5
              && data.getDouble("capacity") == tile.getComponent(Energy.class).getCapacity()
              && data.getDouble("ratedOutput") > 0,
          "all storage variants supply exact server readings");
    }
    h.setBlock(pos, Ic2Blocks.INDUCTION_FURNACE);
    TileEntityInduction induction = h.getBlockEntity(pos);
    induction.heat = 1234;
    induction.progress = 1000;
    var data = MachineStatusData.collect(induction);
    h.assertTrue(
        Math.abs(data.getDouble("heat") - .1234) < .000001 && data.getDouble("progress") == .25,
        "induction exposes fractional heat and real progress");
    h.assertTrue(
        MachineStatusData.percent(induction.heat / 100.0).equals("12.34"),
        "fractional GUI heat uses two decimals");
    h.setBlock(pos, Ic2Blocks.CENTRIFUGE);
    TileEntityCentrifuge centrifuge = h.getBlockEntity(pos);
    centrifuge.heat = 1000;
    h.assertTrue(
        MachineStatusData.collect(centrifuge).getDouble("heat") == .2,
        "centrifuge uses its real work-heat target");
    h.setBlock(pos, Ic2Blocks.BLAST_FURNACE);
    TileEntityBlastFurnace blast = h.getBlockEntity(pos);
    blast.heat = 25000;
    h.assertTrue(
        MachineStatusData.collect(blast).getDouble("heat") == .5,
        "heat-powered blast furnace does not require an EU component");
    h.succeed();
  }
}
