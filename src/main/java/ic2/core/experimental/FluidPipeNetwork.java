package ic2.core.experimental;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Loaded pipe topology with one reverse receiver search per fluid and world tick. */
final class FluidPipeNetwork {
  private record FluidKey(Fluid fluid, DataComponentPatch components) {
    static FluidKey of(FluidStack stack) {
      return new FluidKey(stack.getFluid(), stack.getComponentsPatch());
    }
  }

  private static final class Node {
    final ExperimentalFluidPipe pipe;
    final ExperimentalFluidPipe[] links = new ExperimentalFluidPipe[6];
    int pipeSides;

    Node(ExperimentalFluidPipe pipe) {
      this.pipe = pipe;
    }
  }

  private record Route(Map<ExperimentalFluidPipe, Integer> distances) {
    int distance(ExperimentalFluidPipe pipe) {
      return distances.getOrDefault(pipe, -1);
    }
  }

  private record Target(IFluidHandler handler, Direction side, ExperimentalFluidPipe pipe) {}

  private final Map<ExperimentalFluidPipe, Node> nodes = new IdentityHashMap<>();
  private final Map<FluidKey, Route> routes = new HashMap<>();
  private boolean valid = true;
  private long routeTick = Long.MIN_VALUE;

  static FluidPipeNetwork build(ExperimentalFluidPipe origin) {
    var network = new FluidPipeNetwork();
    var pending = new ArrayDeque<ExperimentalFluidPipe>();
    network.nodes.put(origin, new Node(origin));
    pending.add(origin);
    while (!pending.isEmpty()) {
      var pipe = pending.removeFirst();
      var node = network.nodes.get(pipe);
      for (var side : Direction.values()) {
        var pos = pipe.getBlockPos().relative(side);
        if (!pipe.getLevel().hasChunkAt(pos)) continue;
        if (!(pipe.getLevel().getBlockEntity(pos) instanceof ExperimentalFluidPipe neighbor))
          continue;
        node.pipeSides |= 1 << side.ordinal();
        if (!pipe.enabled(side)
            || pipe.pumps[side.ordinal()] != 0
            || !neighbor.enabled(side.getOpposite())
            || neighbor.pumps[side.getOpposite().ordinal()] != 0) continue;
        node.links[side.ordinal()] = neighbor;
        if (!network.nodes.containsKey(neighbor)) {
          network.nodes.put(neighbor, new Node(neighbor));
          pending.addLast(neighbor);
        }
      }
    }
    for (var pipe : network.nodes.keySet()) {
      if (pipe.network != null) pipe.network.invalidate();
    }
    for (var pipe : network.nodes.keySet()) pipe.network = network;
    return network;
  }

  boolean valid() {
    return valid;
  }

  void clearRoutes() {
    routes.clear();
  }

  void invalidate() {
    if (!valid) return;
    valid = false;
    for (var pipe : nodes.keySet()) {
      if (pipe.network == this) pipe.network = null;
    }
    routes.clear();
  }

  void move(ExperimentalFluidPipe pipe) {
    if (pipe.tank.isEmpty()) return;
    var node = nodes.get(pipe);
    long tick = pipe.getLevel().getGameTime();
    if (routeTick != tick) {
      routes.clear();
      routeTick = tick;
    }
    var fluid = pipe.tank.getFluid();
    var key = FluidKey.of(fluid);
    var route = routes.get(key);
    if (route == null) {
      route = findReceivers(fluid);
      routes.put(key, route);
    }
    int distance = route.distance(pipe);
    int budget =
        Math.min(
            pipe.tank.getFluidAmount(),
            ((ExperimentalPipeBlock) pipe.getBlockState().getBlock()).rate / 20);
    if (distance < 0) {
      equalize(node, route, budget);
      return;
    }
    var targets = new ArrayList<Target>();
    for (var side : Direction.values()) {
      var neighbor = node.links[side.ordinal()];
      if (neighbor != null
          && route.distance(neighbor) == distance - 1
          && compatible(neighbor, fluid)) {
        if (neighbor.tank.fill(fluid, IFluidHandler.FluidAction.SIMULATE) > 0)
          targets.add(new Target(neighbor.tank, side, neighbor));
      } else if (distance == 0 && outputSide(node, side)) {
        var handler = pipe.neighborHandler(side);
        if (handler != null && handler.fill(fluid, IFluidHandler.FluidAction.SIMULATE) > 0)
          targets.add(new Target(handler, side, null));
      }
    }
    if (targets.isEmpty()) return;
    int offset = Math.floorMod(pipe.outputCursor++, targets.size());
    int left = targets.size();
    for (int i = 0; i < targets.size() && budget > 0; i++) {
      var target = targets.get((offset + i) % targets.size());
      budget -= send(pipe, target, (budget + left - 1) / left);
      left--;
    }
    // Redistribute unused shares when a destination accepted only part of its quota.
    for (int i = 0; i < targets.size() && budget > 0; i++)
      budget -= send(pipe, targets.get((offset + i) % targets.size()), budget);
  }

  private Route findReceivers(FluidStack fluid) {
    var distances = new IdentityHashMap<ExperimentalFluidPipe, Integer>();
    var pending = new ArrayDeque<ExperimentalFluidPipe>();
    var probe = fluid.copyWithAmount(1);
    for (var node : nodes.values()) {
      if (!compatible(node.pipe, fluid)) continue;
      for (var side : Direction.values()) {
        if (!outputSide(node, side)) continue;
        var handler = node.pipe.neighborHandler(side);
        if (handler != null && handler.fill(probe, IFluidHandler.FluidAction.SIMULATE) > 0) {
          distances.put(node.pipe, 0);
          pending.addLast(node.pipe);
          break;
        }
      }
    }
    while (!pending.isEmpty()) {
      var pipe = pending.removeFirst();
      int distance = distances.get(pipe) + 1;
      for (var neighbor : nodes.get(pipe).links) {
        if (neighbor == null || distances.containsKey(neighbor) || !compatible(neighbor, fluid))
          continue;
        distances.put(neighbor, distance);
        pending.addLast(neighbor);
      }
    }
    return new Route(distances);
  }

  private static boolean outputSide(Node node, Direction side) {
    return (node.pipeSides & (1 << side.ordinal())) == 0
        && node.pipe.enabled(side)
        && node.pipe.pumps[side.ordinal()] == 0;
  }

  private static boolean compatible(ExperimentalFluidPipe pipe, FluidStack fluid) {
    return pipe.tank.isFluidValid(fluid)
        && (pipe.tank.isEmpty()
            || FluidStack.isSameFluidSameComponents(pipe.tank.getFluid(), fluid));
  }

  private static int send(ExperimentalFluidPipe pipe, Target target, int amount) {
    int sent = ExperimentalFluidPipe.transfer(pipe.tank, target.handler, amount);
    pipe.recordFlow(target.side, sent);
    if (target.pipe != null) target.pipe.recordFlow(target.side.getOpposite(), -sent);
    return sent;
  }

  private static void equalize(Node node, Route route, int budget) {
    var neighbors = new ArrayList<Target>();
    long total = node.pipe.tank.getFluidAmount();
    for (var side : Direction.values()) {
      var other = node.links[side.ordinal()];
      if (other != null
          && route.distance(other) < 0
          && compatible(other, node.pipe.tank.getFluid())
          && other.tank.getFluidAmount() <= node.pipe.tank.getFluidAmount()) {
        neighbors.add(new Target(other.tank, side, other));
        total += other.tank.getFluidAmount();
      }
    }
    int share = (int) (total / (neighbors.size() + 1));
    for (var neighbor : neighbors) {
      int amount = Math.min(budget, Math.max(0, share - neighbor.pipe.tank.getFluidAmount()));
      budget -= send(node.pipe, neighbor, amount);
    }
  }
}
