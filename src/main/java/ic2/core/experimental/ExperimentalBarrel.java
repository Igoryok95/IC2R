package ic2.core.experimental;

import ic2.core.block.tileentity.Ic2TileEntity;
import ic2.core.ref.Ic2Items;
import ic2.core.util.LiquidUtil;
import ic2.core.util.StackUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

public final class ExperimentalBarrel extends Ic2TileEntity {
  public final ExperimentalBrewState brew = new ExperimentalBrewState();

  public ExperimentalBarrel(BlockPos pos, BlockState state) {
    super(ExperimentalBrewing.TYPE, pos, state);
  }

  public void face(Direction direction) {
    setFacing(level, direction);
  }

  @Override
  protected void updateEntityServer() {
    super.updateEntityServer();
    brew.tick(getActive());
    if (level.getGameTime() % 20 == 0) setChanged();
  }

  @Override
  public void saveAdditional(CompoundTag nbt, HolderLookup.Provider registry) {
    super.saveAdditional(nbt, registry);
    nbt.put("brew", brew.save());
  }

  @Override
  protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registry) {
    super.loadAdditional(nbt, registry);
    brew.load(nbt.getCompound("brew"));
  }

  @Override
  public ItemStack adjustDrop(ItemStack stack, boolean wrench) {
    brew.open();
    var data = StackUtil.getOrCreateNbtData(stack);
    data.put("brew", brew.save());
    StackUtil.setTag(stack, data);
    return stack;
  }

  @Override
  protected InteractionResult onClicked(Player player) {
    if (level.isClientSide) return InteractionResult.SUCCESS;
    if (getActive()) {
      Block.popResource(level, worldPosition, new ItemStack(Ic2Items.TREETAP));
      setActive(false);
      brew.drain();
      setChanged();
    } else {
      Block.popResource(
          level, worldPosition, adjustDrop(new ItemStack(ExperimentalBrewing.BARREL_ITEM), false));
      level.setBlockAndUpdate(worldPosition, Blocks.SCAFFOLDING.defaultBlockState());
    }
    return InteractionResult.SUCCESS;
  }

  @Override
  protected InteractionResult onActivated(
      Player player, InteractionHand hand, Direction side, Vec3 hit) {
    var stack = player.getItemInHand(hand);
    if (level.isClientSide) return InteractionResult.SUCCESS;
    if (!getActive() && side.getAxis() != Direction.Axis.Y && stack.is(Ic2Items.TREETAP)) {
      setActive(true);
      setFacing(level, side);
      stack.consume(1, player);
      setChanged();
      return InteractionResult.SUCCESS;
    }
    if (getActive() && stack.is(Ic2Items.EMPTY_MUG) && brew.amount > 0) {
      var drink = ExperimentalBrewing.drink(brew.value());
      if (brew.drain()) {
        stack.consume(1, player);
        if (!player.getInventory().add(drink)) player.drop(drink, false);
        setChanged();
        return InteractionResult.SUCCESS;
      }
    }
    if (brew.opened) return InteractionResult.PASS;
    if ((brew.type == 0 || brew.type == 1) && brew.amount < 32) {
      int limit = (player.isShiftKeyDown() ? 1 : 32 - brew.amount) * 1000;
      var simulated =
          LiquidUtil.drainContainer(
              player,
              hand,
              Fluids.WATER,
              limit,
              ic2.api.util.FluidContainerOutputMode.InPlacePreferred,
              true);
      if (simulated != null && simulated.getAmountMb() >= 1000) {
        int amount = simulated.getAmountMb() / 1000;
        var drained =
            LiquidUtil.drainContainer(
                player,
                hand,
                Fluids.WATER,
                amount * 1000,
                ic2.api.util.FluidContainerOutputMode.InPlacePreferred,
                false);
        if (drained != null
            && drained.getAmountMb() == amount * 1000
            && brew.add(0, amount, level.random)) {
          setChanged();
          return InteractionResult.SUCCESS;
        }
      }
    }
    int ingredient =
        stack.is(Items.WHEAT)
            ? 1
            : stack.is(Ic2Items.HOPS) ? 2 : stack.is(Items.SUGAR_CANE) ? 3 : -1;
    int space =
        ingredient == 1 ? 64 - brew.wheat : ingredient == 2 ? 64 - brew.hops : 32 - brew.amount;
    int amount = Math.min(space, player.isShiftKeyDown() ? 1 : stack.getCount());
    if (ingredient >= 0 && brew.add(ingredient, amount, level.random)) {
      stack.consume(amount, player);
      setChanged();
      return InteractionResult.SUCCESS;
    }
    return InteractionResult.PASS;
  }
}
