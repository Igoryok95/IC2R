package ic2.core.block.machine.tileentity;

import ic2.core.block.comp.Energy;
import ic2.core.experimental.ExperimentalFluidPipe;
import ic2.core.experimental.ExperimentalPipes;
import ic2.core.fluid.Ic2FluidStack;
import ic2.core.ref.Ic2Blocks;
import ic2.core.ref.Ic2Fluids;
import ic2.core.ref.Ic2Items;
import ic2.integration.jade.MachineStatusData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ic2")
@PrefixGameTestTemplate(false)
public final class AirCompressorGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";

  private static TileEntityAirCompressor place(GameTestHelper h, BlockPos pos) {
    h.setBlock(pos, Ic2Blocks.AIR_COMPRESSOR);
    var tile = (TileEntityAirCompressor) h.getBlockEntity(pos);
    tile.setChanged();
    return tile;
  }

  @GameTest(template = EMPTY)
  public static void baseProductionAndIdleEnergyAreConservative(GameTestHelper h) {
    var tile = place(h, new BlockPos(1, 1, 1));
    var energy = tile.getComponent(Energy.class);
    energy.addEnergy(600);
    for (int i = 0; i < 300; i++) tile.updateEntityServer();
    h.assertTrue(
        tile.receiver.getFluidAmount() == 1000, "300 ticks produce exactly one capsule of air");
    h.assertTrue(Math.abs(energy.getEnergy()) < 1e-7, "base production costs exactly 600 EU");
    tile.updateEntityServer();
    h.assertTrue(
        !tile.getActive() && tile.receiver.getFluidAmount() == 1000,
        "no free production without EU");
    tile.receiver.fillMbUnchecked(Ic2FluidStack.create(Ic2Fluids.AIR.still(), 16000), false);
    energy.addEnergy(100);
    for (int i = 0; i < 20; i++) tile.updateEntityServer();
    h.assertTrue(
        energy.getEnergy() == 100 && !tile.getActive(),
        "full receiver is silent and consumes no EU");
    h.assertTrue(tile.consumption == 0 && tile.production == 0, "idle telemetry settles to zero");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void fractionalProductionAndLocalSwitchSurviveSaving(GameTestHelper h) {
    var pos = new BlockPos(1, 1, 1);
    var tile = place(h, pos);
    tile.getComponent(Energy.class).addEnergy(600);
    tile.updateEntityServer();
    var saved = tile.saveWithFullMetadata(h.getLevel().registryAccess());
    tile.loadWithComponents(saved, h.getLevel().registryAccess());
    tile.setChanged();
    for (int i = 0; i < 299; i++) tile.updateEntityServer();
    h.assertTrue(
        tile.receiver.getFluidAmount() == 1000, "fractional mB survives a mid-cycle reload");
    tile.getComponent(Energy.class).addEnergy(100);
    tile.onNetworkEvent(h.makeMockServerPlayerInLevel(), 0);
    double before = tile.getEnergy();
    tile.updateEntityServer();
    h.assertTrue(!tile.enabled && tile.getEnergy() == before, "local switch stops EU consumption");
    saved = tile.saveWithFullMetadata(h.getLevel().registryAccess());
    tile.loadWithComponents(saved, h.getLevel().registryAccess());
    h.assertTrue(
        !tile.enabled && h.getLevel().getBestNeighborSignal(h.absolutePos(pos)) == 0,
        "saved switch does not power adjacent blocks");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void pipesAndDirectFurnacesReceiveOnlyCompressedAir(GameTestHelper h) {
    var tile = place(h, new BlockPos(0, 1, 1));
    h.setBlock(new BlockPos(1, 1, 1), ExperimentalPipes.BLOCKS.get("bronze_pipe_small"));
    h.setBlock(new BlockPos(2, 1, 1), Ic2Blocks.BLAST_FURNACE);
    var pipe = (ExperimentalFluidPipe) h.getBlockEntity(new BlockPos(1, 1, 1));
    var furnace = (TileEntityBlastFurnace) h.getBlockEntity(new BlockPos(2, 1, 1));
    tile.enabled = false;
    tile.receiver.fillMbUnchecked(Ic2FluidStack.create(Ic2Fluids.AIR.still(), 16000), false);
    var handler =
        h.getLevel()
            .getCapability(
                Capabilities.FluidHandler.BLOCK, h.absolutePos(new BlockPos(0, 1, 1)), null);
    h.assertTrue(
        handler != null
            && handler.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE) == 0,
        "receiver rejects all external filling");
    tile.updateEntityServer();
    pipe.tick();
    h.assertTrue(
        furnace.fluidTank.hasExactFluid(Ic2Fluids.AIR.still())
            && furnace.fluidTank.getFluidAmount() > 0,
        "ordinary pipes deliver air to the blast furnace");
    h.assertTrue(
        tile.receiver.getFluidAmount()
                + pipe.tank.getFluidAmount()
                + furnace.fluidTank.getFluidAmount()
            == 16000,
        "pipe transport conserves air");
    h.setBlock(new BlockPos(1, 1, 1), Ic2Blocks.BLAST_FURNACE);
    var direct = (TileEntityBlastFurnace) h.getBlockEntity(new BlockPos(1, 1, 1));
    tile.updateEntityServer();
    h.assertTrue(
        direct.fluidTank.getFluidAmount() == 32, "direct delivery uses the automatic 32 mB budget");
    var status = MachineStatusData.collect(tile);
    h.assertTrue(
        status.getInt("air") == tile.receiver.getFluidAmount()
            && status.getInt("airCapacity") == 16000,
        "Jade reads the real receiver independently of optional Jade classes");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void nativeUpgradesAndLastMillibucketStayWithinCapacity(GameTestHelper h) {
    var tile = place(h, new BlockPos(1, 1, 1));
    tile.getComponent(Energy.class).addEnergy(100);
    tile.receiver.fillMbUnchecked(Ic2FluidStack.create(Ic2Fluids.AIR.still(), 15999), false);
    tile.updateEntityServer();
    h.assertTrue(
        tile.receiver.getFluidAmount() == 16000 && Math.abs(tile.getEnergy() - 99.4) < 1e-7,
        "last mB charges only its proportional EU cost and never overflows");
    tile.upgradeSlot.put(0, new ItemStack(Ic2Items.OVERCLOCKER_UPGRADE));
    tile.upgradeSlot.put(1, new ItemStack(Ic2Items.TRANSFORMER_UPGRADE));
    tile.upgradeSlot.put(2, new ItemStack(Ic2Items.ENERGY_STORAGE_UPGRADE));
    tile.setChanged();
    h.assertTrue(
        tile.getComponent(Energy.class).getSinkTier() == 2,
        "native transformer increases input tier");
    h.assertTrue(
        tile.getComponent(Energy.class).getCapacity() > 1200,
        "native storage upgrade increases EU capacity");
    tile.receiver.drainMbUnchecked(16000, false);
    tile.getComponent(Energy.class).addEnergy(10000);
    for (int i = 0; i < 20; i++) tile.updateEntityServer();
    h.assertTrue(
        tile.production > 1000.0 / 300 && tile.consumption > 2,
        "native overclockers increase rate and demand");
    h.succeed();
  }
}
