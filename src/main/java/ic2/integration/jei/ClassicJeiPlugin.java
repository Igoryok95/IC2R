package ic2.integration.jei;

import ic2.core.IC2;
import ic2.core.classic.ClassicMachines;
import ic2.core.classic.ClassicProcessRecipe;
import ic2.core.classic.ClassicResources;
import ic2.core.ref.Ic2Blocks;
import ic2.core.ref.Ic2Items;
import java.util.Arrays;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

@JeiPlugin
public final class ClassicJeiPlugin implements IModPlugin {
  private static final RecipeType<ClassicProcessRecipe> RARE_EARTH =
      RecipeType.create("ic2", "rare_earth_extractor", ClassicProcessRecipe.class);
  private static final RecipeType<ClassicProcessRecipe> ALLOY =
      RecipeType.create("ic2", "alloy_smelter", ClassicProcessRecipe.class);
  private static final RecipeType<Electrochemistry> ELECTROCHEMISTRY =
      RecipeType.create("ic2", "electrochemistry", Electrochemistry.class);

  @Override
  public ResourceLocation getPluginUid() {
    return IC2.getIdentifier("classic_processing_plugin");
  }

  @Override
  public void registerCategories(IRecipeCategoryRegistration registration) {
    var helper = registration.getJeiHelpers().getGuiHelper();
    registration.addRecipeCategories(
        new ProcessingCategory(RARE_EARTH, ClassicMachines.RARE_EARTH_EXTRACTOR, helper),
        new ProcessingCategory(ALLOY, ClassicMachines.ALLOY_SMELTER, helper),
        new ElectrochemistryCategory(helper));
  }

  @Override
  public void registerRecipes(IRecipeRegistration registration) {
    var level = Minecraft.getInstance().level;
    if (level == null) return;
    var recipes = level.getRecipeManager().getAllRecipesFor(ClassicMachines.RECIPE_TYPE);
    registration.addRecipes(
        RARE_EARTH,
        recipes.stream()
            .map(h -> h.value())
            .filter(r -> r.machine().equals("rare_earth_extractor"))
            .toList());
    registration.addRecipes(
        ALLOY,
        recipes.stream()
            .map(h -> h.value())
            .filter(r -> r.machine().equals("alloy_smelter"))
            .toList());
    registration.addRecipes(
        ELECTROCHEMISTRY,
        List.of(
            new Electrochemistry(
                new ItemStack(Ic2Items.WATER_CELL),
                new ItemStack(Ic2Items.ELECTROLYZED_WATER_CELL),
                true),
            new Electrochemistry(
                new ItemStack(ClassicResources.DEAD_MAGNET),
                new ItemStack(ClassicResources.MAGNET),
                false)));
  }

  @Override
  public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
    registration.addRecipeCatalyst(new ItemStack(ClassicMachines.RARE_EARTH_EXTRACTOR), RARE_EARTH);
    registration.addRecipeCatalyst(
        new ItemStack(ClassicMachines.RARE_EARTH_CENTRIFUGE), RARE_EARTH);
    registration.addRecipeCatalyst(new ItemStack(ClassicMachines.ALLOY_SMELTER), ALLOY);
    registration.addRecipeCatalyst(new ItemStack(Ic2Blocks.ELECTROLYZER), ELECTROCHEMISTRY);
  }

  private abstract static class Category<T> implements IRecipeCategory<T> {
    final RecipeType<T> type;
    final IDrawable background;
    final IDrawable icon;
    final IDrawable arrow;
    final IGuiHelper helper;
    final Component title;

    Category(RecipeType<T> type, Block block, IGuiHelper helper) {
      this.type = type;
      this.helper = helper;
      this.title = block.getName();
      this.background = helper.createBlankDrawable(160, 84);
      this.icon = helper.createDrawableItemStack(new ItemStack(block));
      this.arrow =
          helper.createDrawable(
              IC2.getIdentifier(
                  "textures/gui_sprites/blocks/machines/lv/gui_rare_earth_extractor.png"),
              176,
              14,
              24,
              16);
    }

    @Override
    public RecipeType<T> getRecipeType() {
      return type;
    }

    @Override
    public Component getTitle() {
      return title;
    }

    @Override
    public IDrawable getBackground() {
      return background;
    }

    @Override
    public IDrawable getIcon() {
      return icon;
    }

    void line(GuiGraphics graphics, int y, String key, Object... args) {
      graphics.drawString(
          Minecraft.getInstance().font, Component.translatable(key, args), 2, y, 0x404040, false);
    }
  }

  private static final class ProcessingCategory extends Category<ClassicProcessRecipe> {
    ProcessingCategory(RecipeType<ClassicProcessRecipe> type, Block block, IGuiHelper helper) {
      super(type, block, helper);
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder, ClassicProcessRecipe recipe, IFocusGroup focuses) {
      for (int i = 0; i < recipe.inputs().size(); i++) {
        var input = recipe.inputs().get(i);
        builder
            .addSlot(RecipeIngredientRole.INPUT, 8 + i * 22, 12)
            .setStandardSlotBackground()
            .addItemStacks(
                Arrays.stream(input.ingredient().getItems())
                    .map(s -> s.copyWithCount(input.count()))
                    .toList());
      }
      builder
          .addSlot(RecipeIngredientRole.OUTPUT, 116, 12)
          .setStandardSlotBackground()
          .addItemStack(recipe.result());
    }

    @Override
    public void draw(
        ClassicProcessRecipe recipe,
        IRecipeSlotsView slots,
        GuiGraphics graphics,
        double x,
        double y) {
      arrow.draw(graphics, 77, 12);
      line(graphics, 38, "jei.ic2.classic.energy", recipe.euPerTick());
      line(graphics, 50, "jei.ic2.classic.time", recipe.ticks());
      if (recipe.materialUnits() < 16000)
        line(graphics, 62, "jei.ic2.classic.material", recipe.materialUnits() / 160D);
    }
  }

  private record Electrochemistry(ItemStack input, ItemStack output, boolean reversible) {}

  private static final class ElectrochemistryCategory extends Category<Electrochemistry> {
    ElectrochemistryCategory(IGuiHelper helper) {
      super(ELECTROCHEMISTRY, Ic2Blocks.ELECTROLYZER, helper);
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder, Electrochemistry recipe, IFocusGroup focuses) {
      builder
          .addSlot(RecipeIngredientRole.INPUT, 16, 12)
          .setStandardSlotBackground()
          .addItemStack(recipe.input());
      builder
          .addSlot(RecipeIngredientRole.OUTPUT, 116, 12)
          .setStandardSlotBackground()
          .addItemStack(recipe.output());
    }

    @Override
    public void draw(
        Electrochemistry recipe, IRecipeSlotsView slots, GuiGraphics graphics, double x, double y) {
      arrow.draw(graphics, 77, 12);
      line(graphics, 38, "jei.ic2.classic.charge_cost", 20000);
      line(graphics, 50, "jei.ic2.classic.charge_threshold");
      if (recipe.reversible()) line(graphics, 62, "jei.ic2.classic.recovery", 16000);
    }
  }
}
