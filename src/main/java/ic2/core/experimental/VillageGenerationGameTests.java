package ic2.core.experimental;

import ic2.core.block.tileentity.Ic2TileEntity;
import ic2.core.ref.Ic2Blocks;
import ic2.mixin.StructureTemplatePoolAccessor;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
  public static void workshopsOfferOneJobPerBlockAndKeepVanillaJobs(GameTestHelper h) {
    var pois = h.getLevel().getPoiManager();
    var pos = h.absolutePos(new BlockPos(3, 1, 3));
    Block[] workstations = {
      Ic2Blocks.IRON_FURNACE,
      Ic2Blocks.CROPMATRON,
      ExperimentalBrewing.BARREL,
      Ic2Blocks.ITNT,
      Ic2Blocks.NUCLEAR_REACTOR,
      Ic2Blocks.INDUSTRIAL_WORKBENCH,
      Blocks.COMPOSTER,
      Blocks.BLAST_FURNACE,
      Blocks.BARREL,
      Blocks.BREWING_STAND,
      Blocks.SMITHING_TABLE
    };
    for (Block block : workstations) {
      var type = PoiTypes.forState(block.defaultBlockState()).orElseThrow();
      h.assertTrue(type.value().maxTickets() == 1, "one worker per job block: " + block);
      h.assertTrue(type.value().validRange() == 1, "native job reach range: " + block);
      pois.add(pos, type);
      h.assertTrue(
          pois.take(
                  holder -> holder.equals(type),
                  (holder, candidate) -> candidate.equals(pos),
                  pos,
                  1)
              .isPresent(),
          "first worker can reserve " + block);
      h.assertTrue(
          pois.take(
                  holder -> holder.equals(type),
                  (holder, candidate) -> candidate.equals(pos),
                  pos,
                  1)
              .isEmpty(),
          "second worker cannot reserve the same " + block);
      h.assertTrue(pois.release(pos), "the worker can release " + block);
      h.assertTrue(pois.getFreeTickets(pos) == 1, "release restores one vacancy");
      pois.remove(pos);
    }
    for (var entry : RestoredVillagers.ALL.entrySet()) {
      var own =
          h.getLevel()
              .registryAccess()
              .registryOrThrow(Registries.POINT_OF_INTEREST_TYPE)
              .getHolder(ResourceLocation.fromNamespaceAndPath("ic2", entry.getKey()))
              .orElseThrow();
      h.assertTrue(
          entry.getValue().get().heldJobSite().test(own), "profession recognizes its own job");
      for (Block block :
          List.of(
              Blocks.COMPOSTER,
              Blocks.BLAST_FURNACE,
              Blocks.BARREL,
              Blocks.BREWING_STAND,
              Blocks.SMITHING_TABLE))
        h.assertFalse(
            entry
                .getValue()
                .get()
                .heldJobSite()
                .test(PoiTypes.forState(block.defaultBlockState()).orElseThrow()),
            "IC2 professions cannot claim vanilla job blocks");
    }
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "village_generation")
  public static void savedIc2JobCapacityMigratesOnceWithoutChangingVanillaRecords(
      GameTestHelper h) {
    var ops = RegistryOps.create(NbtOps.INSTANCE, h.getLevel().registryAccess());
    var codec = PoiRecord.codec(() -> {});
    for (var role : RestoredVillagers.ALL.keySet()) {
      var type =
          h.getLevel()
              .registryAccess()
              .registryOrThrow(Registries.POINT_OF_INTEREST_TYPE)
              .getHolder(ResourceLocation.fromNamespaceAndPath("ic2", role))
              .orElseThrow();
      for (int free = 0; free <= 2; free++) {
        var record = new PoiRecord(BlockPos.ZERO, type, () -> {});
        CompoundTag legacy = ((CompoundTag) codec.encodeStart(ops, record).getOrThrow()).copy();
        legacy.remove(VillagePoiCapacity.CAPACITY_KEY);
        legacy.putInt("free_tickets", free);
        PoiRecord migrated = codec.parse(ops, legacy).getOrThrow();
        int expected = Math.max(0, free - 1);
        h.assertTrue(
            migrated.getFreeTickets() == expected, "legacy occupied reservations survive " + role);
        CompoundTag saved = (CompoundTag) codec.encodeStart(ops, migrated).getOrThrow();
        h.assertTrue(
            saved.getInt(VillagePoiCapacity.CAPACITY_KEY) == 1,
            "capacity marker is saved for IC2 only");
        h.assertTrue(
            codec.parse(ops, saved).getOrThrow().getFreeTickets() == expected,
            "reloading must not apply the capacity reduction twice");
      }
    }
    var vanilla =
        new PoiRecord(
            BlockPos.ZERO,
            PoiTypes.forState(Blocks.COMPOSTER.defaultBlockState()).orElseThrow(),
            () -> {});
    CompoundTag saved = (CompoundTag) codec.encodeStart(ops, vanilla).getOrThrow();
    h.assertFalse(
        saved.contains(VillagePoiCapacity.CAPACITY_KEY),
        "vanilla records keep their native format");
    h.assertTrue(
        codec.parse(ops, saved).getOrThrow().getFreeTickets() == 1,
        "vanilla vacancies remain unchanged");
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
