package ic2.core.classic;

import ic2.core.IC2;
import ic2.core.Ic2ItemGroupType;
import ic2.core.item.ItemBlockIc2;
import ic2.core.ref.Ic2Items;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Resources restored alongside the existing Experimental processing chains. */
public final class ClassicResources {
  public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
  public static final Map<String, Item> ITEMS = new LinkedHashMap<>();

  public static final Block SILVER_ORE = ore("silver_ore", false);
  public static final Block DEEPSLATE_SILVER_ORE = ore("deepslate_silver_ore", true);
  public static final Block SILVER_ORE_NETHER = ore("silver_ore_nether", false);
  public static final Block ALUMINIUM_ORE_NETHER = ore("aluminium_ore_nether", false);
  public static final Block RAW_SILVER_BLOCK = resourceBlock("raw_silver_block");
  public static final Block RAW_ALUMINIUM_BLOCK = resourceBlock("raw_aluminium_block");
  public static final Block ALUMINIUM_BLOCK = resourceBlock("aluminium_block");
  public static final Block REFINED_IRON_BLOCK = resourceBlock("refined_iron_block");
  public static final Block STABILIZED_MACHINE = resourceBlock("stabilized_machine_block");
  public static final Block COLOSSAL_CASING = resourceBlock("colossal_base_block");
  public static final Block IRIDIUM_STONE = resourceBlock("iridium_stone");

  public static final Item RAW_SILVER = material("raw_silver");
  public static final Item RAW_ALUMINIUM = material("raw_aluminium");
  public static final Item ALUMINIUM_DUST = material("aluminium_dust");
  public static final Item ALUMINIUM_INGOT = material("aluminium_ingot");
  public static final Item REFINED_IRON_INGOT = material("refined_iron_ingot");
  public static final Item ADVANCED_ALLOY_INGOT = material("advanced_alloy_ingot");
  public static final Item POLISHED_GOLD_INGOT = material("polished_gold_ingot");
  public static final Item UNCOMPRESSED_SUPERCONDUCTOR = material("uncompressed_superconductor");
  public static final Item RARE_EARTH_DUST = material("rare_earth_dust");
  public static final Item RARE_EARTH_CHUNK = material("rare_earth_chunk");
  public static final Item DEAD_MAGNET = material("dead_magnet");
  public static final Item MAGNET = material("magnet");
  public static final Item PULSATING_QUARTZ = material("pulsating_quartz");
  public static final Item COMPLEX_CIRCUIT = material("complex_circuit");
  public static final Item TURBINE_BLADE = material("turbine_blade");
  public static final Item PLASMA_CORE = material("plasma_core");
  public static final Item ELECTROLYZED_WATER_CELL = Ic2Items.ELECTROLYZED_WATER_CELL;
  public static final Item SOLAR_HELMET = equipment("solar_helmet", new ClassicChargingArmor(true));
  public static final Item STATIC_BOOTS =
      equipment("static_boots", new ClassicChargingArmor(false));

  private ClassicResources() {}

  public static void init() {}

  private static Block ore(String name, boolean deepslate) {
    return block(
        name,
        new Block(
            BlockBehaviour.Properties.of()
                .strength(4, 5)
                .requiresCorrectToolForDrops()
                .sound(deepslate ? SoundType.DEEPSLATE : SoundType.STONE)));
  }

  private static Block resourceBlock(String name) {
    return block(
        name,
        new Block(BlockBehaviour.Properties.of().strength(4, 5).requiresCorrectToolForDrops()));
  }

  private static Block block(String name, Block block) {
    IC2.envProxy.registerBlock(IC2.getIdentifier(name), block);
    BLOCKS.put(name, block);
    Item item = new ItemBlockIc2(block, new Item.Properties());
    IC2.envProxy.registerItem(IC2.getIdentifier(name), item);
    ITEMS.put(name, item);
    Ic2Items.addToCreativeTab(Ic2ItemGroupType.GENERAL, () -> item);
    return block;
  }

  private static Item material(String name) {
    Item item = new Item(new Item.Properties());
    IC2.envProxy.registerItem(IC2.getIdentifier(name), item);
    ITEMS.put(name, item);
    Ic2Items.addToCreativeTab(Ic2ItemGroupType.MATERIALS, () -> item);
    return item;
  }

  private static Item equipment(String name, Item item) {
    IC2.envProxy.registerItem(IC2.getIdentifier(name), item);
    ITEMS.put(name, item);
    Ic2Items.addToCreativeTab(Ic2ItemGroupType.COMBAT, () -> item);
    return item;
  }
}
