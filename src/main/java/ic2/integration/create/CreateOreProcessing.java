package ic2.integration.create;

import ic2.core.IC2;
import ic2.core.Ic2ItemGroupType;
import ic2.core.ref.Ic2Items;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmeltingRecipe;

/** Optional, data-driven ore compatibility; no classes from Create are linked. */
public final class CreateOreProcessing {
  public static void addCompatibilityPack(net.neoforged.neoforge.event.AddPackFindersEvent event) {
    if (net.neoforged.fml.ModList.get().isLoaded("create")) {
      event.addPackFinders(
          IC2.getIdentifier("packs/create_compat"),
          net.minecraft.server.packs.PackType.SERVER_DATA,
          net.minecraft.network.chat.Component.translatable("ic2.pack.create_compat"),
          net.minecraft.server.packs.repository.PackSource.BUILT_IN,
          true,
          net.minecraft.server.packs.repository.Pack.Position.TOP);
    }
  }

  public static void init() {
    if (!net.neoforged.fml.ModList.get().isLoaded("create")) return;
    for (String name : List.of("purified_zinc", "zinc_dust", "small_zinc_dust")) {
      Item item = new Item(new Item.Properties());
      IC2.envProxy.registerItem(IC2.getIdentifier(name), item);
      Ic2Items.CREATIVE_TAB_ITEMS
          .computeIfAbsent(Ic2ItemGroupType.MATERIALS, key -> new java.util.ArrayList<>())
          .add(() -> item);
    }
  }

  /** Respect custom datapack recipes; the IC2 adapter replaces only Create's base smelting rule. */
  public static RecipeHolder<SmeltingRecipe> selectSmelting(
      RecipeManager manager, ItemStack input, RecipeHolder<SmeltingRecipe> selected) {
    if (selected == null || !selected.id().getNamespace().equals("create")) return selected;
    var item = BuiltInRegistries.ITEM.getKey(input.getItem());
    if (!item.getNamespace().equals("create") || !item.getPath().startsWith("crushed_raw_"))
      return selected;
    String metal = item.getPath().substring("crushed_raw_".length());
    var alternative =
        manager.byKey(IC2.getIdentifier("smelting/compat/create/" + metal + "_ingot_from_crushed"));
    if (alternative.isPresent()
        && alternative.get().value() instanceof SmeltingRecipe recipe
        && recipe.getGroup().equals("ic2_create_processing")
        && recipe.getIngredients().getFirst().test(input))
      return new RecipeHolder<>(alternative.get().id(), recipe);
    return selected;
  }

  private CreateOreProcessing() {}
}
