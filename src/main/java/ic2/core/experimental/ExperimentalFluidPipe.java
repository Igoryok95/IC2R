package ic2.core.experimental;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** Original passive equalization and capacity/rate profiles; pump covers pull 640/2560 mB/s. */
public final class ExperimentalFluidPipe extends BlockEntity {
  public final FluidTank tank;
  public final byte[] pumps = new byte[6];
  public int disabled;
  public final PipeFluidVisuals visuals = new PipeFluidVisuals();
  private final int[] pendingFlow = new int[6];
  private final int[] displayFlow = new int[6];
  private boolean renderDirty;
  private long lastRenderUpdate = -5;

  private record Outlet(IFluidHandler handler, Direction side) {}

  private record Neighbor(ExperimentalFluidPipe pipe, Direction side) {}

  public ExperimentalFluidPipe(BlockPos pos, BlockState state) {
    super(ExperimentalPipes.TYPE, pos, state);
    tank =
        new FluidTank(((ExperimentalPipeBlock) state.getBlock()).rate) {
          @Override
          protected void onContentsChanged() {
            setChanged();
            renderDirty = true;
          }
        };
  }

  public boolean enabled(Direction side) {
    return (disabled & (1 << side.ordinal())) == 0;
  }

  @Override
  public void onLoad() {
    super.onLoad();
    if (level != null && level.isClientSide)
      visuals.receive(tank.getFluid(), displayFlow, level.getGameTime(), tank.getCapacity());
  }

  public IFluidHandler handler(Direction side) {
    if (side == null) return tank;
    if (!enabled(side)) return null;
    return new IFluidHandler() {
      @Override
      public int getTanks() {
        return 1;
      }

      @Override
      public FluidStack getFluidInTank(int index) {
        return tank.getFluid();
      }

      @Override
      public int getTankCapacity(int index) {
        return tank.getCapacity();
      }

      @Override
      public boolean isFluidValid(int index, FluidStack fluid) {
        return tank.isFluidValid(fluid);
      }

      @Override
      public int fill(FluidStack fluid, FluidAction action) {
        int accepted = enabled(side) ? tank.fill(fluid, action) : 0;
        if (action.execute()) recordFlow(side, -accepted);
        return accepted;
      }

      @Override
      public FluidStack drain(int amount, FluidAction action) {
        var result =
            enabled(side) && pumps[side.ordinal()] == 0
                ? tank.drain(amount, action)
                : FluidStack.EMPTY;
        if (action.execute()) recordFlow(side, result.getAmount());
        return result;
      }

      @Override
      public FluidStack drain(FluidStack fluid, FluidAction action) {
        var result =
            enabled(side) && pumps[side.ordinal()] == 0
                ? tank.drain(fluid, action)
                : FluidStack.EMPTY;
        if (action.execute()) recordFlow(side, result.getAmount());
        return result;
      }
    };
  }

  public void tick() {
    moveFluid();
    syncRender();
  }

  private void moveFluid() {
    var outputs = new ArrayList<Outlet>();
    var neighbors = new ArrayList<Neighbor>();
    long total = tank.getFluidAmount();
    for (var side : Direction.values()) {
      if (!enabled(side) || !level.hasChunkAt(worldPosition.relative(side))) continue;
      var handler =
          level.getCapability(
              Capabilities.FluidHandler.BLOCK, worldPosition.relative(side), side.getOpposite());
      if (handler == null) continue;
      if (pumps[side.ordinal()] != 0) {
        recordFlow(side, -transfer(handler, tank, pumps[side.ordinal()] == 1 ? 32 : 128));
        continue;
      }
      if (tank.isEmpty()) continue;
      if (level.getBlockEntity(worldPosition.relative(side))
          instanceof ExperimentalFluidPipe pipe) {
        if (pipe.enabled(side.getOpposite())
            && pipe.pumps[side.getOpposite().ordinal()] == 0
            && (pipe.tank.isEmpty()
                || FluidStack.isSameFluidSameComponents(pipe.tank.getFluid(), tank.getFluid()))
            && pipe.tank.getFluidAmount() <= tank.getFluidAmount()) {
          neighbors.add(new Neighbor(pipe, side));
          total += pipe.tank.getFluidAmount();
        }
      } else if (handler.fill(tank.getFluid(), IFluidHandler.FluidAction.SIMULATE) > 0)
        outputs.add(new Outlet(handler, side));
    }
    if (tank.isEmpty()) return;
    total = tank.getFluidAmount();
    for (var neighbor : neighbors) total += neighbor.pipe.tank.getFluidAmount();
    int share = (int) (total / (neighbors.size() + 1));
    for (var neighbor : neighbors) {
      int sent =
          transfer(
              tank, neighbor.pipe.tank, Math.max(0, share - neighbor.pipe.tank.getFluidAmount()));
      recordFlow(neighbor.side, sent);
      neighbor.pipe.recordFlow(neighbor.side.getOpposite(), -sent);
    }
    int budget =
        Math.min(
            tank.getFluidAmount(), ((ExperimentalPipeBlock) getBlockState().getBlock()).rate / 20);
    int left = outputs.size();
    for (var output : outputs) {
      int sent = transfer(tank, output.handler, (budget + left - 1) / left);
      recordFlow(output.side, sent);
      budget -= sent;
      left--;
    }
  }

  private void recordFlow(Direction side, int amount) {
    pendingFlow[side.ordinal()] += amount;
  }

  private void syncRender() {
    if (level == null || level.isClientSide || level.getGameTime() - lastRenderUpdate < 5) return;
    boolean moving = java.util.Arrays.stream(pendingFlow).anyMatch(value -> value != 0);
    boolean stopping = java.util.Arrays.stream(displayFlow).anyMatch(value -> value != 0);
    if (!renderDirty && !moving && !stopping) return;
    System.arraycopy(pendingFlow, 0, displayFlow, 0, 6);
    java.util.Arrays.fill(pendingFlow, 0);
    renderDirty = false;
    lastRenderUpdate = level.getGameTime();
    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
  }

  public static int transfer(IFluidHandler source, IFluidHandler target, int amount) {
    if (amount <= 0) return 0;
    var fluid = source.drain(amount, IFluidHandler.FluidAction.SIMULATE);
    if (fluid.isEmpty()) return 0;
    int accepted = target.fill(fluid, IFluidHandler.FluidAction.SIMULATE);
    if (accepted <= 0) return 0;
    var drained = source.drain(fluid.copyWithAmount(accepted), IFluidHandler.FluidAction.EXECUTE);
    int sent = target.fill(drained, IFluidHandler.FluidAction.EXECUTE);
    if (sent < drained.getAmount())
      source.fill(
          drained.copyWithAmount(drained.getAmount() - sent), IFluidHandler.FluidAction.EXECUTE);
    return sent;
  }

  @Override
  protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registry) {
    super.saveAdditional(nbt, registry);
    nbt.put("tank", tank.writeToNBT(registry, new CompoundTag()));
    nbt.putByteArray("pumps", pumps);
    nbt.putInt("disabled", disabled);
  }

  @Override
  public CompoundTag getUpdateTag(HolderLookup.Provider registry) {
    var tag = saveWithoutMetadata(registry);
    tag.putIntArray("visualFlow", displayFlow);
    return tag;
  }

  @Override
  public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
    return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
  }

  @Override
  protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registry) {
    super.loadAdditional(nbt, registry);
    tank.readFromNBT(registry, nbt.getCompound("tank"));
    if (level != null && level.isClientSide)
      visuals.receive(
          tank.getFluid(), nbt.getIntArray("visualFlow"), level.getGameTime(), tank.getCapacity());
    disabled = nbt.getInt("disabled") & 63;
    byte[] saved = nbt.getByteArray("pumps");
    for (int i = 0; i < Math.min(saved.length, 6); i++)
      pumps[i] = (byte) Math.clamp(saved[i], 0, 2);
  }
}
