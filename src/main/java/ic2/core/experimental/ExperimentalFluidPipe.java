package ic2.core.experimental;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** Receiver-directed transport with original capacity/rate profiles and pump-cover extraction. */
public final class ExperimentalFluidPipe extends BlockEntity {
  public final FluidTank tank;
  public final byte[] pumps = new byte[6];
  public int disabled;
  public final PipeFluidVisuals visuals = new PipeFluidVisuals();
  private final int[] pendingFlow = new int[6];
  private final int[] displayFlow = new int[6];
  private boolean renderDirty;
  private long lastRenderUpdate = -5;

  FluidPipeNetwork network;
  int outputCursor;
  private final Map<Direction, BlockCapabilityCache<IFluidHandler, Direction>> neighborHandlers =
      new EnumMap<>(Direction.class);

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
    else connectionsChanged();
  }

  @Override
  public void onChunkUnloaded() {
    connectionsChanged();
    neighborHandlers.clear();
    super.onChunkUnloaded();
  }

  @Override
  public void setRemoved() {
    connectionsChanged();
    neighborHandlers.clear();
    super.setRemoved();
  }

  public void connectionsChanged() {
    if (level == null || level.isClientSide) return;
    if (network != null) network.invalidate();
    for (var side : Direction.values()) {
      var pos = worldPosition.relative(side);
      if (level.hasChunkAt(pos)
          && level.getBlockEntity(pos) instanceof ExperimentalFluidPipe pipe
          && pipe.network != null) pipe.network.invalidate();
    }
  }

  IFluidHandler neighborHandler(Direction side) {
    if (!(level instanceof ServerLevel world) || !enabled(side)) return null;
    return neighborHandlers
        .computeIfAbsent(
            side,
            direction ->
                BlockCapabilityCache.create(
                    Capabilities.FluidHandler.BLOCK,
                    world,
                    worldPosition.relative(direction),
                    direction.getOpposite(),
                    () -> !isRemoved(),
                    () -> {
                      if (network != null) network.clearRoutes();
                    }))
        .getCapability();
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
    for (var side : Direction.values()) {
      if (!enabled(side) || pumps[side.ordinal()] == 0) continue;
      var handler = neighborHandler(side);
      if (handler == null) continue;
      recordFlow(side, -transfer(handler, tank, pumps[side.ordinal()] == 1 ? 32 : 128));
    }
    if (tank.isEmpty()) return;
    if (network == null || !network.valid()) FluidPipeNetwork.build(this);
    network.move(this);
  }

  void recordFlow(Direction side, int amount) {
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
    connectionsChanged();
  }
}
