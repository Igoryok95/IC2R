package ic2.integration.jei;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IIngredientAliasRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Translation keys let JEI rebuild its search index for the selected language. */
final class IngredientSearchAliases {
  static void register(IIngredientAliasRegistration registration) {
    try (var stream =
        IngredientSearchAliases.class.getResourceAsStream("/assets/ic2/search_aliases.json")) {
      if (stream == null) throw new IllegalStateException("Missing IC2 ingredient search aliases");
      var aliases =
          JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
              .getAsJsonObject();
      for (var entry : aliases.entrySet()) {
        var id = ResourceLocation.parse(entry.getKey());
        if (!BuiltInRegistries.ITEM.containsKey(id))
          throw new IllegalStateException("Unknown IC2 search alias item: " + id);
        var stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
        for (var alias : entry.getValue().getAsJsonArray()) {
          registration.addAlias(VanillaTypes.ITEM_STACK, stack, alias.getAsString());
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read IC2 ingredient search aliases", e);
    }
  }

  private IngredientSearchAliases() {}
}
