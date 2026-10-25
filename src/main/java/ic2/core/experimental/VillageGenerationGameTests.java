package ic2.core.experimental;

import ic2.core.block.tileentity.Ic2TileEntity;
import ic2.core.ref.Ic2Blocks;
import ic2.mixin.StructureTemplatePoolAccessor;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ic2")
@PrefixGameTestTemplate(false)
public final class VillageGenerationGameTests {
  private static final String EMPTY = "gametest/empty7x7x7";

  @GameTest(template = EMPTY, batch = "village_generation")
  public static void poolsKeepVanillaWeightsAndAddSixBalancedWorkshopsOnce(GameTestHelper h) {
    var registry = h.getLevel().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
    int[] vanillaWeights = {87, 72, 81, 76, 68};
    for (int i = 0; i < RestoredVillageGeneration.BIOMES.size(); i++) {
      String biome = RestoredVillageGeneration.BIOMES.get(i);
      var pool =
          registry.get(ResourceLocation.withDefaultNamespace("village/" + biome + "/houses"));
      h.assertTrue(
          pool != null && pool.size() == vanillaWeights[i] + 10,
          "vanilla pool retains its total weight with ten new workshop weight units");
      var extra =
          registry.get(
              ResourceLocation.fromNamespaceAndPath("ic2", "village/" + biome + "/workshops"));
      var entries = ((StructureTemplatePoolAccessor) extra).ic2$getEntries();
      h.assertTrue(
          entries.size() == 6
              && entries.stream()
                  .map(pair -> pair.getSecond())
                  .toList()
                  .equals(List.of(2, 2, 2, 2, 1, 1)),
          "approved relative frequency for all six professions");
      RestoredVillageGeneration.inject(h.getLevel().registryAccess());
      h.assertTrue(
          pool.size() == vanillaWeights[i] + 10,
          "repeated initialization does not duplicate workshops");
    }
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "village_generation")
  public static void everyBiomeOverlayPlacesARealEmptyWorkstation(GameTestHelper h) {
    String[] roles = {"electric", "crop", "brewing", "demo", "nuclear", "greg"};
    Block[] blocks = {
      Ic2Blocks.IRON_FURNACE,
      Ic2Blocks.CROPMATRON,
      ExperimentalBrewing.BARREL,
      Ic2Blocks.ITNT,
      Ic2Blocks.NUCLEAR_REACTOR,
      Ic2Blocks.INDUSTRIAL_WORKBENCH
    };
    var origin = h.absolutePos(BlockPos.ZERO);
    var level = h.getLevel();
    for (String biome : RestoredVillageGeneration.BIOMES)
      for (int role = 0; role < roles.length; role++) {
        var template =
            level
                .getStructureManager()
                .getOrCreate(
                    ResourceLocation.fromNamespaceAndPath(
                        "ic2", "village/" + biome + "/workstations/" + roles[role]));
        h.assertTrue(
            template.placeInWorld(
                level, origin, origin, new StructurePlaceSettings(), level.random, 3),
            "workstation overlay is placeable");
        int found = 0;
        for (var pos : BlockPos.betweenClosed(origin, origin.offset(6, 7, 8))) {
          if (!level.getBlockState(pos).is(blocks[role])) continue;
          found++;
          if (level.getBlockEntity(pos) instanceof Ic2TileEntity tile)
            h.assertTrue(!tile.getActive(), "generated machines have no supplied fuel or energy");
          level.removeBlock(pos, false);
        }
        h.assertTrue(found == 1, "overlay adds exactly one real workstation");
      }
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "village_generation", timeoutTicks = 100)
  public static void realJigsawWorkshopGeneratesWithItsNativeVillageResident(GameTestHelper h) {
    var level = h.getLevel();
    var pools = level.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
    var pool =
        pools
            .getHolder(ResourceLocation.fromNamespaceAndPath("ic2", "village/plains/workshops"))
            .orElseThrow();
    var origin = h.absolutePos(new BlockPos(2, 2, 2));
    h.assertTrue(
        JigsawPlacement.generateJigsaw(
            level,
            pool,
            ResourceLocation.withDefaultNamespace("building_entrance"),
            2,
            origin,
            false),
        "vanilla jigsaw system accepts the additive workshop pool");
    h.succeedWhen(
        () ->
            h.assertTrue(
                !level.getEntitiesOfClass(Villager.class, new AABB(origin).inflate(24)).isEmpty(),
                "village resident is generated through vanilla villager jigsaw pieces"));
  }
}
