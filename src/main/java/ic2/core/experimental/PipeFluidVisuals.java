package ic2.core.experimental;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Interpolated, client-owned presentation of authoritative tank contents and measured side flow.
 */
public final class PipeFluidVisuals {
  private FluidStack fluid = FluidStack.EMPTY;
  private double fromAmount, toAmount;
  private long received;
  private boolean initialized;
  private final double[] anchors = new double[6];
  private final double[] rates = new double[6];

  public void receive(FluidStack next, int[] flows, long time, int capacity) {
    double current = amount(time);
    for (int i = 0; i < 6; i++) anchors[i] = phase(i, time) % 1;
    boolean same = next.isEmpty() || FluidStack.isSameFluidSameComponents(fluid, next);
    fromAmount = !initialized ? next.getAmount() : same ? current : 0;
    toAmount = next.getAmount();
    if (!next.isEmpty()) fluid = next.copy();
    for (int i = 0; i < 6; i++)
      rates[i] =
          i < flows.length
              ? Math.clamp(flows[i] / (double) Math.max(1, capacity) / 5, -.08, .08)
              : 0;
    received = time;
    initialized = true;
  }

  public FluidStack fluid() {
    return fluid;
  }

  public double amount(double time) {
    return fromAmount + (toAmount - fromAmount) * Math.clamp((time - received) / 5, 0, 1);
  }

  private double phase(int side, double time) {
    return anchors[side] + rates[side] * Math.clamp(time - received, 0, 10);
  }

  public Vec3 offset(Direction side, double time) {
    if (side != null)
      return Vec3.atLowerCornerOf(side.getNormal()).scale(-phase(side.ordinal(), time));
    Vec3 result = Vec3.ZERO;
    for (Direction direction : Direction.values()) result = result.add(offset(direction, time));
    return result.scale(.5);
  }
}
