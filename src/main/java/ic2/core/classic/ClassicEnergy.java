package ic2.core.classic;

import ic2.core.IC2;
import ic2.core.Ic2ItemGroupType;
import ic2.core.block.tileentity.Ic2TileEntityBlock;
import ic2.core.block.wiring.tileentity.TileEntityElectricBlock;
import ic2.core.item.ItemBattery;
import ic2.core.item.ItemBlockIc2;
import ic2.core.ref.Ic2Items;
import ic2.core.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class ClassicEnergy {
  public static final Item GLOWTRONIC_CRYSTAL =
      battery("glowtronic_crystal", 75_000_000, 25_000, 4);
  public static final Item UESC = battery("uesc", 175_000_000, 95_000, 5);
  public static final Item PESD = battery("pesd", 500_000_000, 250_000, 5);
  public static final Block ESU = storage("esu");
  public static final Block ISU = storage("isu");
  public static final Block PESU = storage("pesu");
  public static final BlockEntityType<Storage> STORAGE =
      IC2.envProxy.registerBlockEntity(
          IC2.getIdentifier("classic_energy_storage"), Storage::new, ESU, ISU, PESU);

  private ClassicEnergy() {}

  public static void init() {}

  private static Item battery(String name, double capacity, double transfer, int tier) {
    Item item =
        new ItemBattery(
            new Item.Properties().stacksTo(1).rarity(Rarity.RARE), capacity, transfer, tier);
    IC2.envProxy.registerItem(IC2.getIdentifier(name), item);
    Ic2Items.addToCreativeTab(Ic2ItemGroupType.TOOLS_AND_UTILITIES, () -> item);
    return item;
  }

  private static Block storage(String name) {
    Block block =
        Ic2TileEntityBlock.create(
            BlockBehaviour.Properties.of().strength(3, 10).requiresCorrectToolForDrops(),
            Storage.class,
            false,
            Ic2TileEntityBlock.DefaultDrop.Self,
            Util.allFacings,
            true);
    IC2.envProxy.registerBlock(IC2.getIdentifier(name), block);
    Item item = new ItemBlockIc2(block, new Item.Properties());
    IC2.envProxy.registerItem(IC2.getIdentifier(name), item);
    Ic2Items.addToCreativeTab(Ic2ItemGroupType.GENERATORS_AND_WIRING, () -> item);
    return block;
  }

  public static final class Storage extends TileEntityElectricBlock {
    public Storage(BlockPos pos, BlockState state) {
      super(
          STORAGE,
          pos,
          state,
          state.is(PESU) ? 6 : state.is(ISU) ? 5 : 4,
          state.is(PESU) ? 32768 : state.is(ISU) ? 8192 : 2048,
          state.is(PESU) ? 4_000_000_000D : state.is(ISU) ? 2_000_000_000D : 300_000_000D);
    }
  }
}
