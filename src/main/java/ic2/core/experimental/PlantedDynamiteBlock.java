package ic2.core.experimental;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class PlantedDynamiteBlock extends Block {
  public static final DirectionProperty FACING = BlockStateProperties.FACING;
  public static final BooleanProperty LINKED = BooleanProperty.create("linked");
  private boolean priming;

  public PlantedDynamiteBlock() {
    super(Properties.of().strength(.5F).noCollission().noOcclusion());
    registerDefaultState(
        stateDefinition.any().setValue(FACING, Direction.UP).setValue(LINKED, false));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(FACING, LINKED);
  }

  @Override
  protected VoxelShape getShape(
      BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return switch (state.getValue(FACING)) {
      case UP -> box(6, 0, 6, 10, 10, 10);
      case DOWN -> box(6, 6, 6, 10, 16, 10);
      case NORTH -> box(6, 6, 6, 10, 10, 16);
      case SOUTH -> box(6, 6, 0, 10, 10, 10);
      case EAST -> box(0, 6, 6, 10, 10, 10);
      case WEST -> box(6, 6, 6, 16, 10, 10);
    };
  }

  @Override
  protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
    Direction side = state.getValue(FACING);
    BlockPos support = pos.relative(side.getOpposite());
    return level.getBlockState(support).isFaceSturdy(level, support, side);
  }

  @Override
  public net.minecraft.world.item.Item asItem() {
    return ExperimentalDynamite.DYNAMITE;
  }

  public void prime(Level level, BlockPos pos, LivingEntity owner, int fuse) {
    if (level.isClientSide || !level.getBlockState(pos).is(this)) return;
    priming = true;
    level.removeBlock(pos, false);
    priming = false;
    var entity = new DynamiteProjectile(ExperimentalDynamite.ENTITY, level);
    entity.setPos(pos.getX() + .5, pos.getY() + .25, pos.getZ() + .5);
    entity.setOwner(owner);
    entity.fuse = fuse;
    entity.setItem(new net.minecraft.world.item.ItemStack(ExperimentalDynamite.STICKY));
    level.addFreshEntity(entity);
  }

  @Override
  protected void neighborChanged(
      BlockState state, Level level, BlockPos pos, Block source, BlockPos other, boolean moving) {
    if (!level.isClientSide && (level.hasNeighborSignal(pos) || !canSurvive(state, level, pos)))
      prime(level, pos, null, 40);
  }

  @Override
  protected void onRemove(
      BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
    if (!priming && !state.is(next.getBlock()) && !level.isClientSide) {
      var entity = new DynamiteProjectile(ExperimentalDynamite.ENTITY, level);
      entity.fuse = 40;
      entity.setPos(pos.getX() + .5, pos.getY() + .25, pos.getZ() + .5);
      level.addFreshEntity(entity);
    }
    super.onRemove(state, level, pos, next, moving);
  }
}
