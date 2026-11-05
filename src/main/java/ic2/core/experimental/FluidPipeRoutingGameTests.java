package ic2.core.experimental;

import ic2.core.block.comp.Energy;
import ic2.core.block.machine.tileentity.TileEntityMatter;
import ic2.core.block.machine.tileentity.TileEntityReplicator;
import ic2.core.fluid.Ic2FluidStack;
import ic2.core.ref.Ic2Blocks;
import ic2.core.ref.Ic2Fluids;
import ic2.core.ref.Ic2Items;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ic2")
@PrefixGameTestTemplate(false)
public final class FluidPipeRoutingGameTests {
  private static final String EMPTY = "gametest/empty64x8x32";
  private static final IFluidHandler.FluidAction EXECUTE = IFluidHandler.FluidAction.EXECUTE;

  @GameTest(template = EMPTY)
  public static void matterGeneratorRejectsExternalBackflowOnEveryFace(GameTestHelper h) {
    var pos = new BlockPos(2, 2, 2);
    h.setBlock(pos, Ic2Blocks.MATTER_GENERATOR);
    TileEntityMatter generator = h.getBlockEntity(pos);
    generator.fluidTank.fillMbUnchecked(
        Ic2FluidStack.create(Ic2Fluids.UU_MATTER.still(), 17), false);
    for (var side : Direction.values()) {
      var handler =
          h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(pos), side);
      h.assertTrue(handler != null, "the output tank remains available on every face");
      h.assertTrue(
          handler.fill(matter(1), IFluidHandler.FluidAction.SIMULATE) == 0
              && handler.fill(matter(1), EXECUTE) == 0,
          "the generator must not accept external matter: " + side);
      h.assertTrue(
          handler.drain(1, IFluidHandler.FluidAction.SIMULATE).getAmount() == 1,
          "the generator still exposes produced matter for extraction: " + side);
    }
    h.assertTrue(
        generator.fluidTank.getFluidAmount() == 17, "probing ports preserves stored matter");
    h.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 260)
  public static void multipleGeneratorsWithEjectorsFeedRaisedReplicatorWithoutBackflow(
      GameTestHelper h) {
    var pipes = line(h, "bronze_pipe_small", 32, 8);
    for (int y = 2; y <= 5; y++) pipes.add(pipe(h, "bronze_pipe_small", new BlockPos(1, y, 8)));
    h.setBlock(new BlockPos(1, 6, 8), Ic2Blocks.REPLICATOR);
    TileEntityReplicator receiver = h.getBlockEntity(new BlockPos(1, 6, 8));
    var generators = new ArrayList<TileEntityMatter>();
    int total = 0;
    for (int i = 0; i < 8; i++) {
      var pos = new BlockPos(12 + i, 2, 8);
      h.setBlock(pos, Ic2Blocks.MATTER_GENERATOR);
      TileEntityMatter generator = h.getBlockEntity(pos);
      generator.fluidTank.fillMbUnchecked(
          Ic2FluidStack.create(Ic2Fluids.UU_MATTER.still(), 100 + i), false);
      generator.upgradeSlot.put(0, new ItemStack(Ic2Items.FLUID_EJECTOR_UPGRADE));
      var energy = generator.getComponent(Energy.class);
      energy.forceAddEnergy(energy.getCapacity());
      total += 101 + i;
      generators.add(generator);
    }
    int[] savedRemnants = {108, 55, 5, 1, 9, 8, 7, 6, 2, 1};
    for (int i = 0; i < savedRemnants.length; i++) {
      pipes.get(i + 10).tank.fill(matter(savedRemnants[i]), EXECUTE);
      total += savedRemnants[i];
    }
    int expected = total;
    h.runAtTickTime(
        80,
        () ->
            h.assertTrue(
                receiver.fluidTank.getFluidAmount() > 0,
                "real ejectors deliver matter to the raised receiver instead of returning it to generators"));
    h.runAtTickTime(
        200,
        () -> {
          int sourceMatter =
              generators.stream().mapToInt(generator -> generator.fluidTank.getFluidAmount()).sum();
          h.assertTrue(
              receiver.fluidTank.getFluidAmount() == expected
                  && stored(pipes) == 0
                  && sourceMatter == 0,
              "all produced matter and saved pipe remnants reach the receiver exactly once; receiver="
                  + receiver.fluidTank.getFluidAmount()
                  + ", pipes="
                  + stored(pipes)
                  + ", sources="
                  + sourceMatter
                  + ", expected="
                  + expected);
          h.succeed();
        });
  }

  @GameTest(template = EMPTY)
  public static void singleMillibucketTraversesEveryPipeProfileToARealReplicator(GameTestHelper h) {
    var end = new BlockPos(41, 1, 1);
    h.setBlock(end, Ic2Blocks.REPLICATOR);
    TileEntityReplicator replicator = h.getBlockEntity(end);
    int delivered = 0;
    for (String profile : ExperimentalPipes.BLOCKS.keySet()) {
      var line = line(h, profile, 40, 1);
      line.getFirst().tank.fill(matter(1), EXECUTE);
      int loaded = h.getLevel().getChunkSource().getLoadedChunksCount();
      tickReverse(line, 42);
      h.assertTrue(
          replicator.fluidTank.getFluidAmount() == ++delivered && stored(line) == 0,
          "one mB reaches the replicator without residue: " + profile);
      h.assertTrue(
          h.getLevel().getChunkSource().getLoadedChunksCount() == loaded,
          "routing does not load extra chunks: " + profile);
    }
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void savedUnevenMatterBuffersDrainCompletely(GameTestHelper h) {
    var line = line(h, "bronze_pipe_small", 10, 1);
    var sink = tank(h, new BlockPos(11, 1, 1));
    int[] oldAmounts = {108, 55, 5, 1, 9, 8, 7, 6, 2, 1};
    int total = 0;
    for (int i = 0; i < line.size(); i++) {
      var pipe = line.get(i);
      pipe.tank.fill(matter(oldAmounts[i]), EXECUTE);
      total += oldAmounts[i];
      var saved = pipe.saveWithFullMetadata(h.getLevel().registryAccess());
      pipe.loadWithComponents(saved, h.getLevel().registryAccess());
    }
    tickReverse(line, 80);
    h.assertTrue(
        sink.getFluidInTank(0).getAmount() == total && stored(line) == 0,
        "all pre-existing saved matter reaches the receiver exactly once");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void dropletsSplitFairlyWhileLoopsAndDeadEndsDrain(GameTestHelper h) {
    var pipes = new ArrayList<ExperimentalFluidPipe>();
    for (int x = 3; x <= 13; x++) pipes.add(pipe(h, "bronze_pipe_small", new BlockPos(x, 1, 8)));
    // A second route forms a ring; the north branch has no receiving machine.
    for (int z = 4; z <= 7; z++) {
      pipes.add(pipe(h, "bronze_pipe_small", new BlockPos(8, 1, z)));
      pipes.add(pipe(h, "bronze_pipe_small", new BlockPos(12, 1, z)));
    }
    for (int x = 9; x <= 11; x++) pipes.add(pipe(h, "bronze_pipe_small", new BlockPos(x, 1, 4)));
    var deadEnd = pipe(h, "bronze_pipe_small", new BlockPos(8, 1, 9));
    pipes.add(deadEnd);
    deadEnd.tank.fill(matter(3), EXECUTE);
    var west = tank(h, new BlockPos(2, 1, 8));
    var east = tank(h, new BlockPos(14, 1, 8));
    var source = (ExperimentalFluidPipe) h.getBlockEntity(new BlockPos(8, 1, 8));
    for (int i = 0; i < 20; i++) {
      source.tank.fill(matter(1), EXECUTE);
      tickReverse(pipes, 1);
    }
    tickReverse(pipes, 80);
    int a = west.getFluidInTank(0).getAmount(), b = east.getFluidInTank(0).getAmount();
    h.assertTrue(
        a > 0 && b > 0 && a + b == 23 && stored(pipes) == 0,
        "both consumers receive droplets, including fluid initially in a dead end");
    h.assertTrue(Math.abs(a - b) <= 1, "equal routes alternate indivisible droplets fairly");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void fullReceiverAndClosedSidesKeepMatterThenResume(GameTestHelper h) {
    var pipes = line(h, "bronze_pipe_small", 8, 1);
    var sink = tank(h, new BlockPos(9, 1, 1));
    int capacity = sink.getTankCapacity(0);
    sink.fill(matter(capacity), EXECUTE);
    pipes.getFirst().tank.fill(matter(108), EXECUTE);
    tickReverse(pipes, 30);
    h.assertTrue(
        stored(pipes) == 108 && sink.getFluidInTank(0).getAmount() == capacity,
        "a full receiver leaves all matter safely in the pipes");
    sink.drain(capacity, EXECUTE);
    var closed = pipes.get(3);
    closed.disabled |= 1 << Direction.EAST.ordinal();
    closed.connectionsChanged();
    h.getLevel().invalidateCapabilities(closed.getBlockPos());
    tickReverse(pipes, 30);
    int received = sink.getFluidInTank(0).getAmount();
    h.assertTrue(
        received < 108 && received + stored(pipes) == 108,
        "a closed face cannot be bypassed and does not destroy buffered fluid");
    closed.disabled = 0;
    closed.connectionsChanged();
    h.getLevel().invalidateCapabilities(closed.getBlockPos());
    h.runAfterDelay(
        1,
        () -> {
          tickReverse(pipes, 80);
          h.assertTrue(
              sink.getFluidInTank(0).getAmount() == 108 && stored(pipes) == 0,
              "reopening the side and receiver drains every remaining mB");
          h.succeed();
        });
  }

  @GameTest(template = EMPTY)
  public static void pumpCoversKeepBothExtractionRatesAndOneWayInput(GameTestHelper h) {
    var pipe = pipe(h, "bronze_pipe_medium", new BlockPos(2, 1, 2));
    var source = tank(h, new BlockPos(1, 1, 2));
    var sink = tank(h, new BlockPos(3, 1, 2));
    for (int tier = 1; tier <= 2; tier++) {
      source.drain(Integer.MAX_VALUE, EXECUTE);
      sink.drain(Integer.MAX_VALUE, EXECUTE);
      pipe.tank.drain(Integer.MAX_VALUE, EXECUTE);
      source.fill(matter(1000), EXECUTE);
      pipe.pumps[Direction.WEST.ordinal()] = (byte) tier;
      pipe.connectionsChanged();
      h.getLevel().invalidateCapabilities(pipe.getBlockPos());
      pipe.tick();
      int expected = tier == 1 ? 32 : 128;
      h.assertTrue(
          source.getFluidInTank(0).getAmount() == 1000 - expected,
          "pump cover keeps its original extraction rate");
      h.assertTrue(
          pipe.tank.getFluidAmount() + sink.getFluidInTank(0).getAmount() == expected,
          "extracted matter is neither sent back to the source nor lost");
      h.assertTrue(
          pipe.handler(Direction.WEST).drain(1, EXECUTE).isEmpty(),
          "a pump input does not expose fluid for reverse extraction");
    }
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void allProfilesRespectCapacityAndTheirSharedOutputBudget(GameTestHelper h) {
    for (var entry : ExperimentalPipes.BLOCKS.entrySet()) {
      var current = pipe(h, entry.getKey(), new BlockPos(2, 1, 2));
      var east = tank(h, new BlockPos(3, 1, 2));
      var west = tank(h, new BlockPos(1, 1, 2));
      east.drain(Integer.MAX_VALUE, EXECUTE);
      west.drain(Integer.MAX_VALUE, EXECUTE);
      int capacity = current.tank.getCapacity();
      current.tank.fill(matter(capacity), EXECUTE);
      current.tick();
      int received = east.getFluidInTank(0).getAmount() + west.getFluidInTank(0).getAmount();
      h.assertTrue(
          capacity == entry.getValue().rate && received == entry.getValue().rate / 20,
          "capacity and combined output rate are preserved: " + entry.getKey());
      h.assertTrue(
          received + current.tank.getFluidAmount() == capacity,
          "splitting output neither duplicates nor discards fluid");
      current.tank.drain(Integer.MAX_VALUE, EXECUTE);
      east.drain(Integer.MAX_VALUE, EXECUTE);
      west.drain(Integer.MAX_VALUE, EXECUTE);
      west.fill(matter(west.getTankCapacity(0) - 1), EXECUTE);
      int before = west.getFluidInTank(0).getAmount();
      current.tank.fill(matter(capacity), EXECUTE);
      current.tick();
      h.assertTrue(
          east.getFluidInTank(0).getAmount() + west.getFluidInTank(0).getAmount() - before
              == entry.getValue().rate / 20,
          "a nearly full branch does not waste the other branch's output budget");
    }
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void mixedFluidsRemainSeparatedAtAJunction(GameTestHelper h) {
    var matterLine = line(h, "bronze_pipe_small", 8, 5);
    var waterLine = line(h, "bronze_pipe_small", 8, 7);
    var bridge = pipe(h, "bronze_pipe_small", new BlockPos(4, 1, 6));
    var all = new ArrayList<>(matterLine);
    all.addAll(waterLine);
    all.add(bridge);
    h.setBlock(new BlockPos(9, 1, 5), Ic2Blocks.REPLICATOR);
    h.setBlock(new BlockPos(9, 1, 7), Ic2Blocks.ORE_WASHING_PLANT);
    var matterSink =
        h.getLevel()
            .getCapability(
                Capabilities.FluidHandler.BLOCK,
                h.absolutePos(new BlockPos(9, 1, 5)),
                Direction.WEST);
    var waterSink =
        h.getLevel()
            .getCapability(
                Capabilities.FluidHandler.BLOCK,
                h.absolutePos(new BlockPos(9, 1, 7)),
                Direction.WEST);
    matterLine.getFirst().tank.fill(matter(11), EXECUTE);
    waterLine.getFirst().tank.fill(new FluidStack(Fluids.WATER, 17), EXECUTE);
    bridge.tank.fill(new FluidStack(Fluids.WATER, 1), EXECUTE);
    tickReverse(all, 60);
    h.assertTrue(
        matterSink != null
            && waterSink != null
            && matterSink.getFluidInTank(0).is(Ic2Fluids.UU_MATTER.still())
            && matterSink.getFluidInTank(0).getAmount() == 11
            && waterSink.getFluidInTank(0).is(Fluids.WATER)
            && waterSink.getFluidInTank(0).getAmount() == 18
            && stored(all) == 0,
        "water and matter reach only compatible machines without mixing or leaving residue");
    h.succeed();
  }

  private static List<ExperimentalFluidPipe> line(
      GameTestHelper h, String profile, int length, int z) {
    var pipes = new ArrayList<ExperimentalFluidPipe>();
    for (int x = 1; x <= length; x++) pipes.add(pipe(h, profile, new BlockPos(x, 1, z)));
    return pipes;
  }

  private static ExperimentalFluidPipe pipe(GameTestHelper h, String profile, BlockPos pos) {
    h.setBlock(pos, ExperimentalPipes.BLOCKS.get(profile));
    return h.getBlockEntity(pos);
  }

  private static IFluidHandler tank(GameTestHelper h, BlockPos pos) {
    h.setBlock(pos, Ic2Blocks.TANK);
    return h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(pos), null);
  }

  private static FluidStack matter(int amount) {
    return new FluidStack(Ic2Fluids.UU_MATTER.still(), amount);
  }

  private static int stored(List<ExperimentalFluidPipe> pipes) {
    return pipes.stream().mapToInt(pipe -> pipe.tank.getFluidAmount()).sum();
  }

  private static void tickReverse(List<ExperimentalFluidPipe> pipes, int steps) {
    for (int i = 0; i < steps; i++) for (int j = pipes.size() - 1; j >= 0; j--) pipes.get(j).tick();
  }
}
