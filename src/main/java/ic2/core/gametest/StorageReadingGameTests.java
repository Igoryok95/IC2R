package ic2.core.gametest;

import ic2.core.block.machine.tileentity.TileEntityTeleporter;
import ic2.core.block.wiring.tileentity.TileEntityElectricMFSU;
import ic2.core.ref.Ic2Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ic2")
@PrefixGameTestTemplate(false)
public final class StorageReadingGameTests {
  @GameTest(template = "gametest/empty3x3x3")
  public static void teleporterKeepsLargeReadingsAndDrainsOnlyItsCost(GameTestHelper h) {
    BlockPos pos = new BlockPos(1, 1, 1);
    h.setBlock(pos, Ic2Blocks.TELEPORTER);
    h.setBlock(pos.above(), Ic2Blocks.MFSU);
    TileEntityElectricMFSU first = h.getBlockEntity(pos.above());
    // The enlarged test buffer exercises the API without adding a new storage block.
    first.energy.setCapacity(4_000_000_000D);
    first.energy.addEnergy(4_000_000_000D);
    TileEntityTeleporter teleporter = h.getBlockEntity(pos);
    h.assertTrue(
        first.getStoredLong() == 4_000_000_000L
            && first.getCapacityLong() == 4_000_000_000L
            && teleporter.getAvailableEnergy() == Integer.MAX_VALUE,
        "exact storage readings survive the legacy integer limit");
    teleporter.consumeEnergy(100);
    h.assertTrue(first.getStoredLong() == 3_999_999_900L, "only the real cost is drained");
    h.setBlock(pos.east(), Ic2Blocks.MFSU);
    TileEntityElectricMFSU second = h.getBlockEntity(pos.east());
    second.energy.setCapacity(4_000_000_000D);
    second.energy.addEnergy(4_000_000_000D);
    teleporter.consumeEnergy(Integer.MAX_VALUE);
    h.assertTrue(
        first.getStoredLong() + second.getStoredLong() == 7_999_999_900L - Integer.MAX_VALUE,
        "maximum integer cost is distributed without overflow");
    h.succeed();
  }

  @GameTest(template = "gametest/empty3x3x3")
  public static void teleporterWithNoEnergySourcesDoesNotDivideByZero(GameTestHelper h) {
    BlockPos pos = new BlockPos(1, 1, 1);
    h.setBlock(pos, Ic2Blocks.TELEPORTER);
    TileEntityTeleporter teleporter = h.getBlockEntity(pos);
    teleporter.consumeEnergy(1);
    h.assertTrue(teleporter.getAvailableEnergy() == 0, "no sources report no available energy");
    h.succeed();
  }
}
