package ic2.mixin;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StructureTemplatePool.class)
public interface StructureTemplatePoolAccessor {
  @Accessor("rawTemplates")
  List<Pair<StructurePoolElement, Integer>> ic2$getEntries();

  @Mutable
  @Accessor("rawTemplates")
  void ic2$setEntries(List<Pair<StructurePoolElement, Integer>> entries);

  @Accessor("templates")
  ObjectArrayList<StructurePoolElement> ic2$getWeightedTemplates();

  @Accessor("maxSize")
  void ic2$setMaxSize(int size);
}
