package ic2.core.gametest.client;

import ic2.core.experimental.ExperimentalFluidPipe;
import ic2.core.experimental.ExperimentalPipes;
import ic2.core.ref.Ic2Blocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.slf4j.LoggerFactory;

/** One opt-in world pass: all eight shells, turns, vertical joins, real fluids and side flow. */
@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public final class PipeClientSmoke {
  private static final List<BlockPos> DISPLAY = new ArrayList<>();
  private static final BlockPos FLOW = new BlockPos(7, 129, -3);
  private static final BlockPos ELBOW = new BlockPos(12, 129, -3);
  private static int ticks;

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    if (!Boolean.getBoolean("ic2.pipeSmoke")) return;
    var mc = Minecraft.getInstance();
    var server = mc.getSingleplayerServer();
    if (mc.player == null || mc.level == null || server == null || mc.getOverlay() != null) return;
    mc.options.pauseOnLostFocus = false;
    int phase = ++ticks;
    var id = mc.player.getUUID();
    if (phase == 1) {
      mc.options.hideGui = true;
      server.execute(
          () -> {
            var player = server.getPlayerList().getPlayer(id);
            var level = player.serverLevel();
            player.setGameMode(GameType.CREATIVE);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.getInventory().clearContent();
            level.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS).set(false, server);
            level.setDayTime(6000);
            level.setWeatherParameters(10000, 0, false, false);
            for (int x = -3; x <= 19; x++)
              for (int z = -5; z <= 10; z++) {
                level.setBlockAndUpdate(
                    new BlockPos(x, 127, z), Blocks.SMOOTH_STONE.defaultBlockState());
                for (int y = 128; y <= 133; y++) {
                  var pos = new BlockPos(x, y, z);
                  if (level.getBlockEntity(pos) instanceof net.minecraft.world.Container container)
                    container.clearContent();
                  level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
              }
            level
                .getEntitiesOfClass(
                    net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(-3, 127, -5, 20, 135, 11))
                .forEach(net.minecraft.world.entity.Entity::discard);
            String[] sizes = {"tiny", "small", "medium", "large"};
            for (int material = 0; material < 2; material++)
              for (int size = 0; size < sizes.length; size++) {
                var block =
                    ExperimentalPipes.BLOCKS.get(
                        (material == 0 ? "bronze" : "steel") + "_pipe_" + sizes[size]);
                var start = new BlockPos(size * 5, 129, material * 5);
                var positions =
                    List.of(
                        start, start.east(), start.east().south(), start.east().south().above());
                for (var pos : positions) level.setBlockAndUpdate(pos, block.defaultBlockState());
                for (var pos : positions) {
                  var pipe = (ExperimentalFluidPipe) level.getBlockEntity(pos);
                  pipe.tank.fill(
                      new FluidStack(
                          material == 0 ? Fluids.WATER : Fluids.LAVA,
                          pipe.tank.getCapacity() * 3 / 5),
                      IFluidHandler.FluidAction.EXECUTE);
                  DISPLAY.add(pos);
                }
              }
            var elbow = ExperimentalPipes.BLOCKS.get("bronze_pipe_medium");
            var elbowPositions = List.of(ELBOW, ELBOW.east(), ELBOW.below());
            for (var pos : elbowPositions) level.setBlockAndUpdate(pos, elbow.defaultBlockState());
            for (var pos : elbowPositions) {
              var pipe = (ExperimentalFluidPipe) level.getBlockEntity(pos);
              pipe.tank.fill(
                  new FluidStack(Fluids.WATER, pipe.tank.getCapacity() * 3 / 5),
                  IFluidHandler.FluidAction.EXECUTE);
              DISPLAY.add(pos);
            }
            level.setBlockAndUpdate(FLOW.west(), Ic2Blocks.TANK.defaultBlockState());
            level.setBlockAndUpdate(
                FLOW, ExperimentalPipes.BLOCKS.get("bronze_pipe_medium").defaultBlockState());
            level.setBlockAndUpdate(
                FLOW.east(),
                ExperimentalPipes.BLOCKS.get("bronze_pipe_medium").defaultBlockState());
            level.setBlockAndUpdate(FLOW.east(2), Ic2Blocks.TANK.defaultBlockState());
            var pipe = (ExperimentalFluidPipe) level.getBlockEntity(FLOW);
            pipe.pumps[Direction.WEST.ordinal()] = 1;
            level
                .getCapability(Capabilities.FluidHandler.BLOCK, FLOW.west(), null)
                .fill(new FluidStack(Fluids.WATER, 8000), IFluidHandler.FluidAction.EXECUTE);
            player.connection.teleport(8, 134, -9, 0, 29);
          });
    }
    if (phase == 100) {
      for (var pos : DISPLAY) {
        if (!(mc.level.getBlockEntity(pos) instanceof ExperimentalFluidPipe pipe)
            || pipe.visuals.fluid().isEmpty()
            || Math.abs(
                    pipe.visuals.amount(mc.level.getGameTime()) - pipe.tank.getCapacity() * 3 / 5)
                > 1)
          throw new IllegalStateException("Pipe contents did not reach client at " + pos);
      }
      if (!(mc.level.getBlockEntity(FLOW) instanceof ExperimentalFluidPipe pipe)
          || pipe.visuals.offset(Direction.EAST, mc.level.getGameTime() + 1).lengthSqr() == 0)
        throw new IllegalStateException("Measured pipe flow did not reach client");
      capture(mc, "glass-pipes-overview.png");
      mc.options.fov().set(30);
      server.execute(
          () -> server.getPlayerList().getPlayer(id).connection.teleport(14.2, 129, -5.2, 26, 28));
    }
    if (phase == 140) {
      capture(mc, "glass-pipes-bronze-close.png");
      server.execute(
          () -> server.getPlayerList().getPlayer(id).connection.teleport(12.5, 131, 8.5, 150, 15));
    }
    if (phase == 180) {
      capture(mc, "glass-pipes-steel-reverse.png");
      server.execute(
          () -> {
            var player = server.getPlayerList().getPlayer(id);
            var level = player.serverLevel();
            for (var pos : DISPLAY) {
              var pipe = (ExperimentalFluidPipe) level.getBlockEntity(pos);
              pipe.tank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            }
            player.connection.teleport(14.2, 129, -5.2, 26, 28);
          });
    }
    if (phase == 230) {
      for (var pos : DISPLAY) {
        var pipe = (ExperimentalFluidPipe) mc.level.getBlockEntity(pos);
        if (pipe.visuals.amount(mc.level.getGameTime()) > .01)
          throw new IllegalStateException("Empty pipe still renders liquid at " + pos);
      }
      capture(mc, "glass-pipes-empty.png");
    }
    if (phase == 250) {
      mc.options.hideGui = false;
      mc.options.fov().set(70);
      LoggerFactory.getLogger("IC2R-Glass-Pipes")
          .info(
              "IC2R_GLASS_PIPE_CLIENT_PASS: all eight sizes/materials, real water/lava, emptying and synchronized measured flow");
      mc.stop();
    }
  }

  private static void capture(Minecraft mc, String name) {
    Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), text -> {});
  }

  private PipeClientSmoke() {}
}
