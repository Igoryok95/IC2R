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

  public ExperimentalFluidPipe(BlockPos pos, BlockState state) {
    super(ExperimentalPipes.TYPE, pos, state);
    tank =
        new FluidTank(((ExperimentalPipeBlock) state.getBlock()).rate) {
          @Override
          protected void onContentsChanged() {
            setChanged();
          }
        };
  }

  public boolean enabled(Direction side) {
    return (disabled & (1 << side.ordinal())) == 0;
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
        return enabled(side) ? tank.fill(fluid, action) : 0;
      }

      @Override
      public FluidStack drain(int amount, FluidAction action) {
        return enabled(side) && pumps[side.ordinal()] == 0
            ? tank.drain(amount, action)
            : FluidStack.EMPTY;
      }

      @Override
      public FluidStack drain(FluidStack fluid, FluidAction action) {
        return enabled(side) && pumps[side.ordinal()] == 0
            ? tank.drain(fluid, action)
            : FluidStack.EMPTY;
      }
    };
  }

  public void tick() {
    var outputs = new ArrayList<IFluidHandler>();
    var neighbors = new ArrayList<ExperimentalFluidPipe>();
    long total = tank.getFluidAmount();
    for (var side : Direction.values()) {
      if (!enabled(side) || !level.hasChunkAt(worldPosition.relative(side))) continue;
      var handler =
          level.getCapability(
              Capabilities.FluidHandler.BLOCK, worldPosition.relative(side), side.getOpposite());
      if (handler == null) continue;
      if (pumps[side.ordinal()] != 0) {
        transfer(handler, tank, pumps[side.ordinal()] == 1 ? 32 : 128);
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
          neighbors.add(pipe);
          total += pipe.tank.getFluidAmount();
        }
      } else if (handler.fill(tank.getFluid(), IFluidHandler.FluidAction.SIMULATE) > 0)
        outputs.add(handler);
    }
    if (tank.isEmpty()) return;
    total = tank.getFluidAmount();
    for (var pipe : neighbors) total += pipe.tank.getFluidAmount();
    int share = (int) (total / (neighbors.size() + 1));
    for (var pipe : neighbors)
      transfer(tank, pipe.tank, Math.max(0, share - pipe.tank.getFluidAmount()));
    int budget =
        Math.min(
            tank.getFluidAmount(), ((ExperimentalPipeBlock) getBlockState().getBlock()).rate / 20);
    int left = outputs.size();
    for (var handler : outputs) {
      int sent = transfer(tank, handler, (budget + left - 1) / left);
      budget -= sent;
      left--;
    }
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
    return saveWithoutMetadata(registry);
  }

  @Override
  public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
    return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
  }

  @Override
  protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registry) {
    super.loadAdditional(nbt, registry);
    tank.readFromNBT(registry, nbt.getCompound("tank"));
    disabled = nbt.getInt("disabled") & 63;
    byte[] saved = nbt.getByteArray("pumps");
    for (int i = 0; i < Math.min(saved.length, 6); i++)
      pumps[i] = (byte) Math.clamp(saved[i], 0, 2);
  }
}
