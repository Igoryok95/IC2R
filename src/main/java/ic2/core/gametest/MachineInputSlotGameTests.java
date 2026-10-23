package ic2.core.gametest;

import ic2.core.ContainerBase;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.machine.tileentity.TileEntityStandardMachine;
import ic2.core.ref.Ic2Blocks;
import ic2.core.ref.Ic2Items;
import ic2.core.slot.SlotInvSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ic2")
@PrefixGameTestTemplate(false)
public class MachineInputSlotGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";
  private static final BlockPos MACHINE_POS = new BlockPos(1, 1, 1);

  @GameTest(template = EMPTY)
  public static void maceratorInputRejectsEmptyStack(GameTestHelper helper) {
    assertEmptyInputRejected(helper, Ic2Blocks.MACERATOR);
  }

  @GameTest(template = EMPTY)
  public static void electricFurnaceInputRejectsEmptyStack(GameTestHelper helper) {
    assertEmptyInputRejected(helper, Ic2Blocks.ELECTRIC_FURNACE);
  }

  @GameTest(template = EMPTY)
  public static void extractorInputRejectsEmptyStack(GameTestHelper helper) {
    assertEmptyInputRejected(helper, Ic2Blocks.EXTRACTOR);
  }

  @GameTest(template = EMPTY)
  public static void compressorInputRejectsEmptyStack(GameTestHelper helper) {
    assertEmptyInputRejected(helper, Ic2Blocks.COMPRESSOR);
  }

  @GameTest(template = EMPTY)
  public static void maceratorInputCanBeInspectedAfterShiftClick(GameTestHelper helper) {
    assertShiftClickInput(helper, Ic2Blocks.MACERATOR);
  }

  @GameTest(template = EMPTY)
  public static void electricFurnaceInputCanBeInspectedAfterShiftClick(GameTestHelper helper) {
    assertShiftClickInput(helper, Ic2Blocks.ELECTRIC_FURNACE);
  }

  @GameTest(template = EMPTY)
  public static void maceratorStillValidatesNonEmptyInput(GameTestHelper helper) {
    assertInputRecipesUnchanged(helper, Ic2Blocks.MACERATOR);
  }

  @GameTest(template = EMPTY)
  public static void electricFurnaceStillValidatesNonEmptyInput(GameTestHelper helper) {
    assertInputRecipesUnchanged(helper, Ic2Blocks.ELECTRIC_FURNACE);
  }

  @GameTest(template = EMPTY)
  public static void machineOutputCanStillBeShiftClicked(GameTestHelper helper) {
    TileEntityStandardMachine<?, ?, ?> machine = setupMachine(helper, Ic2Blocks.MACERATOR);
    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    machine.outputSlot.put(new ItemStack(Ic2Items.IRON_DUST, 2));
    ContainerBase<?> menu = machine.createServerScreenHandler(1, player);
    Slot output = findSlot(menu, machine.outputSlot);

    helper.assertTrue(!output.mayPlace(new ItemStack(Items.RAW_IRON)), "output must reject input");
    menu.quickMoveStack(player, output.index);

    helper.assertTrue(machine.outputSlot.isEmpty(), "output should move to the player");
    helper.assertValueEqual(player.getInventory().countItem(Ic2Items.IRON_DUST), 2, "output count");
    helper.succeed();
  }

  private static void assertEmptyInputRejected(GameTestHelper helper, Block block) {
    TileEntityStandardMachine<?, ?, ?> machine = setupMachine(helper, block);
    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    Slot slot = new SlotInvSlot(machine.inputSlot, 0, 0, 0);

    helper.assertTrue(
        !machine.inputSlot.accepts(ItemStack.EMPTY), "input must reject empty stacks");
    helper.assertTrue(
        !machine.inputSlot.accepts(new ItemStack(Items.RAW_IRON, 0)),
        "input must reject zero-count stacks");
    helper.assertTrue(!slot.mayPlace(ItemStack.EMPTY), "slot must reject empty stacks");
    helper.assertTrue(!slot.allowModification(player), "empty input inspection must not throw");
    helper.succeed();
  }

  private static void assertShiftClickInput(GameTestHelper helper, Block block) {
    TileEntityStandardMachine<?, ?, ?> machine = setupMachine(helper, block);
    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    machine.inputSlot.put(new ItemStack(Items.RAW_IRON, 32));
    ContainerBase<?> menu = machine.createServerScreenHandler(1, player);
    Slot input = findSlot(menu, machine.inputSlot);

    menu.quickMoveStack(player, input.index);

    helper.assertTrue(machine.inputSlot.isEmpty(), "shift-click should empty the input slot");
    helper.assertValueEqual(player.getInventory().countItem(Items.RAW_IRON), 32, "input count");
    // Smooth Swapping performs this inspection after the transfer has emptied the source slot.
    helper.assertTrue(!input.allowModification(player), "post-transfer inspection must not throw");
    helper.succeed();
  }

  private static void assertInputRecipesUnchanged(GameTestHelper helper, Block block) {
    TileEntityStandardMachine<?, ?, ?> machine = setupMachine(helper, block);
    ItemStack input = new ItemStack(Items.RAW_IRON, 3);

    helper.assertTrue(machine.inputSlot.accepts(input), "raw iron should remain valid input");
    helper.assertTrue(
        !machine.inputSlot.accepts(new ItemStack(Items.BEDROCK)), "bedrock must remain invalid");
    helper.assertValueEqual(input.getCount(), 3, "recipe inspection must preserve input count");
    helper.assertTrue(machine.inputSlot.isEmpty(), "recipe inspection must not populate the slot");
    helper.succeed();
  }

  private static TileEntityStandardMachine<?, ?, ?> setupMachine(
      GameTestHelper helper, Block block) {
    helper.setBlock(MACHINE_POS, block);
    return (TileEntityStandardMachine<?, ?, ?>) helper.getBlockEntity(MACHINE_POS);
  }

  private static Slot findSlot(ContainerBase<?> menu, InvSlot inventorySlot) {
    return menu.slots.stream()
        .filter(slot -> slot instanceof SlotInvSlot invSlot && invSlot.invSlot == inventorySlot)
        .findFirst()
        .orElseThrow();
  }
}
