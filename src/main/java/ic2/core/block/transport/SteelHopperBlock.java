package ic2.core.block.transport;

import com.mojang.serialization.MapCodec;
import ic2.core.ref.Ic2BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class SteelHopperBlock extends HopperBlock {
  public static final MapCodec<HopperBlock> CODEC = simpleCodec(SteelHopperBlock::new);

  public SteelHopperBlock(Properties properties) {
    super(properties);
  }

  @Override
  public MapCodec<HopperBlock> codec() {
    return CODEC;
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new SteelHopperBlockEntity(pos, state);
  }

  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      Level level, BlockState state, BlockEntityType<T> type) {
    return level.isClientSide
        ? null
        : createTickerHelper(type, Ic2BlockEntities.STEEL_HOPPER, HopperBlockEntity::pushItemsTick);
  }
}
