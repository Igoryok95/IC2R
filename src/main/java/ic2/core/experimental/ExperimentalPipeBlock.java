package ic2.core.experimental;

import ic2.core.ref.Ic2Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

public final class ExperimentalPipeBlock extends Block implements EntityBlock {
  public static final BooleanProperty[] CONNECTIONS =
      java.util.Arrays.stream(Direction.values())
          .map(d -> BooleanProperty.create(d.getName()))
          .toArray(BooleanProperty[]::new);
  public final int rate, color;
  public final double width;
  public final String size;

  public ExperimentalPipeBlock(int rate, double width, int color, String size) {
    super(Properties.of().strength(1, 5).noOcclusion());
    this.rate = rate;
    this.width = width;
    this.color = color;
    this.size = size;
    var state = stateDefinition.any();
    for (var property : CONNECTIONS) state = state.setValue(property, false);
    registerDefaultState(state);
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(CONNECTIONS);
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new ExperimentalFluidPipe(pos, state);
  }

  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      Level level, BlockState state, BlockEntityType<T> type) {
    return level.isClientSide || type != ExperimentalPipes.TYPE
        ? null
        : (world, pos, block, be) -> ((ExperimentalFluidPipe) be).tick();
  }

  private BlockState connections(BlockState state, LevelAccessor level, BlockPos pos) {
    var own = level.getBlockEntity(pos) instanceof ExperimentalFluidPipe pipe ? pipe : null;
    for (Direction direction : Direction.values()) {
      boolean connected = own == null || own.enabled(direction);
      BlockPos neighbor = pos.relative(direction);
      connected &=
          level.getBlockState(neighbor).getBlock() instanceof ExperimentalPipeBlock
              || level instanceof Level world
                  && world.getCapability(
                          Capabilities.FluidHandler.BLOCK, neighbor, direction.getOpposite())
                      != null;
      state = state.setValue(CONNECTIONS[direction.ordinal()], connected);
    }
    return state;
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    return connections(defaultBlockState(), context.getLevel(), context.getClickedPos());
  }

  @Override
  protected BlockState updateShape(
      BlockState state,
      Direction direction,
      BlockState other,
      LevelAccessor level,
      BlockPos pos,
      BlockPos otherPos) {
    return connections(state, level, pos);
  }

  @Override
  protected VoxelShape getShape(
      BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    double a = (16 - width) / 2, b = 16 - a;
    VoxelShape shape = box(a, a, a, b, b, b);
    for (var dir : Direction.values())
      if (state.getValue(CONNECTIONS[dir.ordinal()])) {
        shape =
            Shapes.or(
                shape,
                box(
                    dir == Direction.WEST ? 0 : a,
                    dir == Direction.DOWN ? 0 : a,
                    dir == Direction.NORTH ? 0 : a,
                    dir == Direction.EAST ? 16 : b,
                    dir == Direction.UP ? 16 : b,
                    dir == Direction.SOUTH ? 16 : b));
      }
    return shape;
  }

  @Override
  protected ItemInteractionResult useItemOn(
      ItemStack stack,
      BlockState state,
      Level level,
      BlockPos pos,
      Player player,
      InteractionHand hand,
      BlockHitResult hit) {
    if (!stack.is(Ic2Items.WRENCH)
        && !stack.is(Ic2Items.ELECTRIC_WRENCH)
        && !stack.is(Ic2Items.CROWBAR))
      return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), stack))
      return ItemInteractionResult.FAIL;
    if (!level.isClientSide && level.getBlockEntity(pos) instanceof ExperimentalFluidPipe pipe) {
      int side = hit.getDirection().ordinal();
      if (stack.is(Ic2Items.CROWBAR) && pipe.pumps[side] != 0) {
        Block.popResource(
            level,
            pos,
            new ItemStack(
                pipe.pumps[side] == 1 ? ExperimentalPipes.PUMP_LV : ExperimentalPipes.PUMP_MV));
        pipe.pumps[side] = 0;
      } else if (player.isShiftKeyDown()) {
        level.destroyBlock(pos, true, player);
        return ItemInteractionResult.SUCCESS;
      } else pipe.disabled ^= 1 << side;
      pipe.setChanged();
      level.invalidateCapabilities(pos);
      level.setBlockAndUpdate(pos, connections(state, level, pos));
      level.sendBlockUpdated(pos, state, level.getBlockState(pos), 3);
    }
    return ItemInteractionResult.sidedSuccess(level.isClientSide);
  }

  @Override
  protected void onRemove(
      BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
    if (!state.is(next.getBlock())
        && level.getBlockEntity(pos) instanceof ExperimentalFluidPipe pipe)
      for (byte pump : pipe.pumps)
        if (pump != 0)
          Block.popResource(
              level,
              pos,
              new ItemStack(pump == 1 ? ExperimentalPipes.PUMP_LV : ExperimentalPipes.PUMP_MV));
    super.onRemove(state, level, pos, next, moving);
  }
}
