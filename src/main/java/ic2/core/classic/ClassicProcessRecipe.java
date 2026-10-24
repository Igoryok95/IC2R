package ic2.core.classic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record ClassicProcessRecipe(
    String machine,
    List<Input> inputs,
    ItemStack result,
    int ticks,
    int euPerTick,
    int materialUnits)
    implements Recipe<RecipeInput> {
  public static final MapCodec<ClassicProcessRecipe> CODEC =
      RecordCodecBuilder.mapCodec(
          i ->
              i.group(
                      Codec.STRING.fieldOf("machine").forGetter(ClassicProcessRecipe::machine),
                      Input.CODEC
                          .listOf()
                          .fieldOf("inputs")
                          .forGetter(ClassicProcessRecipe::inputs),
                      ItemStack.STRICT_CODEC
                          .fieldOf("result")
                          .forGetter(ClassicProcessRecipe::result),
                      Codec.intRange(1, 10000000)
                          .fieldOf("ticks")
                          .forGetter(ClassicProcessRecipe::ticks),
                      Codec.intRange(1, 10000000)
                          .fieldOf("eu_per_tick")
                          .forGetter(ClassicProcessRecipe::euPerTick),
                      Codec.intRange(1, 16000)
                          .optionalFieldOf("material_units", 16000)
                          .forGetter(ClassicProcessRecipe::materialUnits))
                  .apply(i, ClassicProcessRecipe::new));

  public record Input(Ingredient ingredient, int count) {
    public static final Codec<Input> CODEC =
        RecordCodecBuilder.create(
            i ->
                i.group(
                        Ingredient.CODEC_NONEMPTY
                            .fieldOf("ingredient")
                            .forGetter(Input::ingredient),
                        Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(Input::count))
                    .apply(i, Input::new));
  }

  /** Returns an exact consumption plan; backtracking handles overlapping item tags. */
  public int[] plan(ItemStack[] slots) {
    int[] used = new int[slots.length];
    return match(0, slots, used) ? used : null;
  }

  private boolean match(int inputIndex, ItemStack[] slots, int[] used) {
    if (inputIndex == inputs.size()) return true;
    return allocate(inputIndex, 0, inputs.get(inputIndex).count(), slots, used);
  }

  private boolean allocate(int inputIndex, int slot, int remaining, ItemStack[] slots, int[] used) {
    if (remaining == 0) return match(inputIndex + 1, slots, used);
    if (slot == slots.length) return false;
    Input input = inputs.get(inputIndex);
    int available = input.ingredient().test(slots[slot]) ? slots[slot].getCount() - used[slot] : 0;
    for (int take = Math.min(available, remaining); take >= 0; take--) {
      used[slot] += take;
      if (allocate(inputIndex, slot + 1, remaining - take, slots, used)) return true;
      used[slot] -= take;
    }
    return false;
  }

  @Override
  public boolean matches(RecipeInput input, Level level) {
    ItemStack[] slots = new ItemStack[input.size()];
    for (int i = 0; i < slots.length; i++) slots[i] = input.getItem(i);
    return plan(slots) != null;
  }

  @Override
  public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
    return result.copy();
  }

  @Override
  public ItemStack getResultItem(HolderLookup.Provider registries) {
    return result;
  }

  @Override
  public NonNullList<Ingredient> getIngredients() {
    NonNullList<Ingredient> list = NonNullList.create();
    for (Input input : inputs) list.add(input.ingredient());
    return list;
  }

  @Override
  public boolean canCraftInDimensions(int width, int height) {
    return width * height >= inputs.size();
  }

  @Override
  public boolean isSpecial() {
    return true;
  }

  @Override
  public RecipeType<?> getType() {
    return ClassicMachines.RECIPE_TYPE;
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return ClassicMachines.RECIPE_SERIALIZER;
  }

  public static final class Serializer implements RecipeSerializer<ClassicProcessRecipe> {
    @Override
    public MapCodec<ClassicProcessRecipe> codec() {
      return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, ClassicProcessRecipe> streamCodec() {
      return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
    }
  }
}
