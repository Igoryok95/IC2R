package ic2.core.experimental;

import ic2.core.IC2;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ExperimentalPipes {
  public static final Map<String, ExperimentalPipeBlock> BLOCKS = new LinkedHashMap<>();
  public static final BlockEntityType<ExperimentalFluidPipe> TYPE;
  public static final Item PUMP_LV = pump("pump_lv", 1);
  public static final Item PUMP_MV = pump("pump_mv", 2);

  static {
    String[] sizes = {"tiny", "small", "medium", "large"};
    float[] multipliers = {1F / 6, 1F / 3, 1, 2};
    double[] widths = {4, 6, 8, 10};
    for (boolean steel : new boolean[] {false, true})
      for (int i = 0; i < sizes.length; i++) {
        String name = (steel ? "steel" : "bronze") + "_pipe_" + sizes[i];
        int rate = (int) ((steel ? 4800 : 2400) * multipliers[i]);
        var block =
            new ExperimentalPipeBlock(rate, widths[i], steel ? 0x808080 : 0xAE5111, sizes[i]);
        BLOCKS.put(name, block);
        IC2.envProxy.registerBlock(IC2.getIdentifier(name), block);
        ExperimentalItems.register(name, new BlockItem(block, new Item.Properties()));
      }
    TYPE =
        IC2.envProxy.registerBlockEntity(
            IC2.getIdentifier("fluid_pipe"),
            ExperimentalFluidPipe::new,
            BLOCKS.values().toArray(Block[]::new));
  }

  private static Item pump(String id, int tier) {
    return ExperimentalItems.register(
        id,
        new Item(new Item.Properties()) {
          @Override
          public InteractionResult useOn(UseOnContext context) {
            var level = context.getLevel();
            var player = context.getPlayer();
            BlockPos pos = context.getClickedPos();
            if (player == null
                || !(level.getBlockEntity(pos) instanceof ExperimentalFluidPipe pipe))
              return InteractionResult.PASS;
            if (!level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand()))
              return InteractionResult.FAIL;
            if (pipe.pumps[context.getClickedFace().ordinal()] != 0) return InteractionResult.FAIL;
            if (!level.isClientSide) {
              pipe.pumps[context.getClickedFace().ordinal()] = (byte) tier;
              pipe.setChanged();
              level.invalidateCapabilities(pos);
              level.sendBlockUpdated(pos, pipe.getBlockState(), pipe.getBlockState(), 3);
              context.getItemInHand().consume(1, player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
          }
        });
  }

  public static void init() {}

  private ExperimentalPipes() {}
}
