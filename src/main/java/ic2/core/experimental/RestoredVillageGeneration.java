package ic2.core.experimental;

import com.mojang.datafixers.util.Pair;
import ic2.mixin.StructureTemplatePoolAccessor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/** Additive village pools: existing relative weights remain unchanged, workshops are rare. */
@EventBusSubscriber(modid = "ic2")
public final class RestoredVillageGeneration {
  public static final List<String> BIOMES =
      List.of("plains", "desert", "savanna", "taiga", "snowy");

  @SubscribeEvent
  public static void starting(ServerAboutToStartEvent event) {
    inject(event.getServer().registryAccess());
  }

  public static void inject(RegistryAccess access) {
    var pools = access.registryOrThrow(Registries.TEMPLATE_POOL);
    for (var biome : BIOMES) {
      var target = pools.get(ResourceLocation.withDefaultNamespace("village/" + biome + "/houses"));
      var additions =
          pools.get(
              ResourceLocation.fromNamespaceAndPath("ic2", "village/" + biome + "/workshops"));
      if (target == null || additions == null) continue;
      var destination = (StructureTemplatePoolAccessor) target;
      var extra = ((StructureTemplatePoolAccessor) additions).ic2$getEntries();
      if (extra.isEmpty()
          || destination.ic2$getEntries().stream()
              .anyMatch(pair -> pair.getFirst() == extra.getFirst().getFirst())) continue;
      var merged = new ArrayList<Pair<StructurePoolElement, Integer>>();
      merged.addAll(destination.ic2$getEntries());
      merged.addAll(extra);
      destination.ic2$setEntries(merged);
      var weighted = destination.ic2$getWeightedTemplates();
      weighted.clear();
      for (var pair : merged)
        for (int i = 0; i < pair.getSecond(); i++) weighted.add(pair.getFirst());
      destination.ic2$setMaxSize(Integer.MIN_VALUE);
    }
  }

  private RestoredVillageGeneration() {}
}
