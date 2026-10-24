package ic2.core.classic;

import ic2.core.IC2;
import ic2.core.Ic2ItemGroupType;
import ic2.core.block.tileentity.Ic2TileEntityBlock;
import ic2.core.block.wiring.CableBlock;
import ic2.core.block.wiring.CableType;
import ic2.core.block.wiring.FoamCableBlock;
import ic2.core.block.wiring.tileentity.TileEntityTransformer;
import ic2.core.item.ItemBlockIc2;
import ic2.core.item.ItemCable;
import ic2.core.ref.Ic2Items;
import ic2.core.util.Util;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class ClassicWiring {
  public static final Map<String, CableBlock> CABLES = new LinkedHashMap<>();
  public static final CableBlock BRONZE = cable("bronze_cable", CableType.bronze, 0);
  public static final CableBlock INSULATED_BRONZE =
      cable("insulated_bronze_cable", CableType.bronze, 1);
  public static final CableBlock DOUBLE_INSULATED_BRONZE =
      cable("double_insulated_bronze_cable", CableType.bronze, 2);
  public static final CableBlock ALUMINIUM = cable("aluminium_cable", CableType.aluminium, 0);
  public static final CableBlock INSULATED_ALUMINIUM =
      cable("insulated_aluminium_cable", CableType.aluminium, 1);
  public static final CableBlock DOUBLE_INSULATED_ALUMINIUM =
      cable("double_insulated_aluminium_cable", CableType.aluminium, 2);
  public static final CableBlock TRIPLE_INSULATED_ALUMINIUM =
      cable("triple_insulated_aluminium_cable", CableType.aluminium, 3);
  public static final CableBlock QUADRUPLE_INSULATED_ALUMINIUM =
      cable("quadruple_insulated_aluminium_cable", CableType.aluminium, 4);
  public static final CableBlock SUPERCONDUCTOR =
      cable("superconductor_cable", CableType.superconductor, 0);
  public static final CableBlock PLASMA = cable("plasma_cable", CableType.plasma, 0);
  public static final Block IV_TRANSFORMER = transformer();
  public static final BlockEntityType<IvTransformer> TRANSFORMER =
      IC2.envProxy.registerBlockEntity(
          IC2.getIdentifier("classic_iv_transformer"), IvTransformer::new, IV_TRANSFORMER);

  private ClassicWiring() {}

  public static void init() {}

  private static CableBlock cable(String name, CableType type, int insulation) {
    FoamCableBlock foam =
        FoamCableBlock.create(BlockBehaviour.Properties.of().strength(.5F, 5F), type, insulation);
    IC2.envProxy.registerBlock(IC2.getIdentifier(name + "_foam"), foam);
    CableBlock block =
        CableBlock.create(
            BlockBehaviour.Properties.of()
                .strength(.5F, 5F)
                .sound(insulation > 0 ? SoundType.WOOL : SoundType.METAL),
            type,
            insulation,
            foam);
    IC2.envProxy.registerBlock(IC2.getIdentifier(name), block);
    Item item = new ItemCable(block, new Item.Properties());
    IC2.envProxy.registerItem(IC2.getIdentifier(name), item);
    Ic2Items.addToCreativeTab(Ic2ItemGroupType.GENERATORS_AND_WIRING, () -> item);
    CABLES.put(name, block);
    return block;
  }

  private static Block transformer() {
    Block block =
        Ic2TileEntityBlock.create(
            BlockBehaviour.Properties.of().strength(3, 10).requiresCorrectToolForDrops(),
            IvTransformer.class,
            true,
            Ic2TileEntityBlock.DefaultDrop.Self,
            Util.allFacings,
            true);
    IC2.envProxy.registerBlock(IC2.getIdentifier("iv_transformer"), block);
    Item item = new ItemBlockIc2(block, new Item.Properties());
    IC2.envProxy.registerItem(IC2.getIdentifier("iv_transformer"), item);
    Ic2Items.addToCreativeTab(Ic2ItemGroupType.GENERATORS_AND_WIRING, () -> item);
    return block;
  }

  public static final class IvTransformer extends TileEntityTransformer {
    public IvTransformer(BlockPos pos, BlockState state) {
      super(TRANSFORMER, pos, state, 5);
    }
  }
}
