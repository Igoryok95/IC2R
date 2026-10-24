package ic2.core.classic;

import ic2.core.IC2;
import ic2.core.Ic2ItemGroupType;
import ic2.core.block.tileentity.Ic2TileEntityBlock;
import ic2.core.item.ItemBlockIc2;
import ic2.core.ref.Ic2Items;
import ic2.core.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ClassicMachines {
  public static final Block RARE_EARTH_EXTRACTOR = machine("rare_earth_extractor");
  public static final Block RARE_EARTH_CENTRIFUGE = machine("centrifugal_rare_earth_extractor");
  public static final Block ALLOY_SMELTER = machine("alloy_smelter");
  public static final BlockEntityType<ClassicProcessMachine> PROCESSOR =
      IC2.envProxy.registerBlockEntity(
          IC2.getIdentifier("classic_processor"),
          ClassicProcessMachine::new,
          RARE_EARTH_EXTRACTOR,
          RARE_EARTH_CENTRIFUGE,
          ALLOY_SMELTER);
  public static final RecipeType<ClassicProcessRecipe> RECIPE_TYPE =
      IC2.envProxy.registerRecipeType(IC2.getIdentifier("classic_processing"));
  public static final ClassicProcessRecipe.Serializer RECIPE_SERIALIZER =
      new ClassicProcessRecipe.Serializer();

  private ClassicMachines() {}

  public static void init() {
    IC2.envProxy.registerRecipeSerializer(
        IC2.getIdentifier("classic_processing"), RECIPE_SERIALIZER);
  }

  private static Block machine(String name) {
    Block block =
        Ic2TileEntityBlock.create(
            BlockBehaviour.Properties.of().strength(3, 10).requiresCorrectToolForDrops(),
            ClassicProcessMachine.class,
            true,
            Ic2TileEntityBlock.DefaultDrop.Self,
            Util.horizontalFacings,
            true);
    IC2.envProxy.registerBlock(IC2.getIdentifier(name), block);
    Item item = new ItemBlockIc2(block, new Item.Properties());
    IC2.envProxy.registerItem(IC2.getIdentifier(name), item);
    Ic2Items.addToCreativeTab(Ic2ItemGroupType.MACHINES, () -> item);
    return block;
  }
}
