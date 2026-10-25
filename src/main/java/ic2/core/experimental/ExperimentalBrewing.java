package ic2.core.experimental;

import ic2.core.IC2;
import ic2.core.block.tileentity.Ic2TileEntityBlock;
import ic2.core.item.ItemBlockIc2;
import ic2.core.util.StackUtil;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ExperimentalBrewing {
  public static final Block BARREL =
      Ic2TileEntityBlock.create(
          Block.Properties.of().strength(2, 6).sound(SoundType.WOOD),
          ExperimentalBarrel.class,
          true,
          Ic2TileEntityBlock.DefaultDrop.Self,
          Set.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST),
          false);
  public static final BlockEntityType<ExperimentalBarrel> TYPE =
      IC2.envProxy.registerBlockEntity(
          IC2.getIdentifier("barrel"), ExperimentalBarrel::new, BARREL);
  public static final Item BARREL_ITEM;
  public static final Item BOOZE_MUG =
      ExperimentalItems.register("booze_mug", new ExperimentalDrink());

  static {
    IC2.envProxy.registerBlock(IC2.getIdentifier("barrel"), BARREL);
    BARREL_ITEM =
        ExperimentalItems.register(
            "barrel",
            new ItemBlockIc2(BARREL, new Item.Properties().stacksTo(1)) {
              @Override
              public InteractionResult useOn(UseOnContext context) {
                var level = context.getLevel();
                var pos = context.getClickedPos();
                var player = context.getPlayer();
                if (player == null || !level.getBlockState(pos).is(Blocks.SCAFFOLDING))
                  return InteractionResult.PASS;
                if (!level.mayInteract(player, pos)
                    || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand()))
                  return InteractionResult.FAIL;
                if (!level.isClientSide) {
                  if (!level.setBlockAndUpdate(pos, BARREL.defaultBlockState()))
                    return InteractionResult.FAIL;
                  var barrel = (ExperimentalBarrel) level.getBlockEntity(pos);
                  barrel.brew.load(
                      StackUtil.getOrCreateNbtData(context.getItemInHand()).getCompound("brew"));
                  barrel.face(player.getDirection().getOpposite());
                  barrel.setChanged();
                  context.getItemInHand().consume(1, player);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
              }
            });
  }

  public static ItemStack drink(int value) {
    value &= ~124;
    var stack = new ItemStack(BOOZE_MUG);
    var data = StackUtil.getOrCreateNbtData(stack);
    data.putInt("value", value);
    StackUtil.setTag(stack, data);
    int type = value & 3;
    stack.set(
        net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,
        new net.minecraft.world.item.component.CustomModelData(
            type == 2 ? 7 : Math.min(5, value >>> 13 & 7) + 1));
    return stack;
  }

  public static void init() {}

  private ExperimentalBrewing() {}
}
