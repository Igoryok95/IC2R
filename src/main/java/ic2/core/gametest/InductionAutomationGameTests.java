package ic2.core.gametest;

import ic2.core.block.machine.tileentity.TileEntityInduction;
import ic2.core.ref.Ic2Blocks;
import ic2.core.ref.Ic2Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ic2")
@PrefixGameTestTemplate(false)
public final class InductionAutomationGameTests {
  @GameTest(template = "gametest/empty3x3x3", timeoutTicks = 160)
  public static void realHopperBalancesBothInductionInputs(GameTestHelper h) {
    var pos = new BlockPos(1, 1, 1);
    h.setBlock(pos, Ic2Blocks.INDUCTION_FURNACE);
    h.setBlock(pos.above(), Blocks.HOPPER);
    TileEntityInduction furnace = h.getBlockEntity(pos);
    HopperBlockEntity hopper = h.getBlockEntity(pos.above());
    hopper.setItem(0, new ItemStack(Ic2Items.IRON_DUST, 10));
    h.succeedWhen(
        () -> {
          h.assertTrue(hopper.isEmpty(), "hopper transferred all ten inputs");
          h.assertTrue(
              furnace.inputSlotA.get().getCount() == 5 && furnace.inputSlotB.get().getCount() == 5,
              "parallel channels fill 5/5 instead of 10/0");
          h.assertTrue(
              furnace.outputSlotA.isEmpty() && furnace.outputSlotB.isEmpty(),
              "unpowered machine did not consume test input");
        });
  }

  @GameTest(template = "gametest/empty3x3x3")
  public static void inductionSimulationAndFullOrDifferentInputsKeepItems(GameTestHelper h) {
    var pos = new BlockPos(1, 1, 1);
    h.setBlock(pos, Ic2Blocks.INDUCTION_FURNACE);
    TileEntityInduction furnace = h.getBlockEntity(pos);
    var handler =
        h.getLevel()
            .getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(pos), Direction.UP);
    var input = new ItemStack(Ic2Items.IRON_DUST);
    for (int turn = 0; turn < 6; turn++) {
      boolean accepted = false;
      for (int slot = 0; slot < handler.getSlots(); slot++) {
        if (!handler.insertItem(slot, input, true).isEmpty()) continue;
        h.assertTrue(
            furnace.inputSlotA.get().getCount() + furnace.inputSlotB.get().getCount() == turn,
            "simulation is side-effect free");
        h.assertTrue(
            handler.insertItem(slot, input, false).isEmpty(), "execution matches simulation");
        accepted = true;
        break;
      }
      h.assertTrue(
          accepted
              && furnace.inputSlotA.get().getCount() == (turn + 2) / 2
              && furnace.inputSlotB.get().getCount() == (turn + 1) / 2,
          "alternating capability insertion");
    }
    furnace.inputSlotA.put(new ItemStack(Ic2Items.GOLD_DUST, 64));
    furnace.inputSlotB.put(new ItemStack(Ic2Items.IRON_DUST, 3));
    var remaining = HopperBlockEntity.addItem(null, furnace, input.copyWithCount(2), Direction.UP);
    h.assertTrue(
        remaining.isEmpty()
            && furnace.inputSlotA.get().getCount() == 64
            && furnace.inputSlotB.get().getCount() == 5,
        "full/different input does not block the other channel");
    h.succeed();
  }

  private InductionAutomationGameTests() {}
}
