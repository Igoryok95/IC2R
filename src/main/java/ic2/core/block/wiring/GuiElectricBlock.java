package ic2.core.block.wiring;

import ic2.core.Ic2Gui;
import ic2.core.gui.EnergyGauge;
import ic2.core.gui.TextLabel;
import ic2.core.gui.VanillaButton;
import ic2.core.gui.dynamic.TextProvider;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class GuiElectricBlock extends Ic2Gui<ContainerElectricBlock> {
  private static final ResourceLocation background =
      ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/guielectricblock.png");
  private final DecimalFormat energyFormat = createEnergyFormat();

  public GuiElectricBlock(
      ContainerElectricBlock container, Inventory playerInventory, Component title) {
    super(container, playerInventory, title, 196);
    this.addElement(
        new EnergyGauge(this, 79, 38, container.base, EnergyGauge.EnergyGaugeStyle.Bar) {
          @Override
          protected List<Component> getToolTip() {
            return List.of(getEnergyTooltip());
          }
        });
    int[] rows = {24, 61, 73};
    for (int i = 0; i < rows.length; i++) {
      int line = i;
      this.addElement(
          TextLabel.create(
                  this,
                  79,
                  rows[i],
                  90,
                  9,
                  TextProvider.of(() -> getReadingLines().get(line).getString()),
                  4210752,
                  false,
                  false,
                  false)
              .withTooltip(
                  () ->
                      line == 0
                          ? getEnergyTooltip().getString()
                          : Component.translatable(
                                  "ic2.EUStorage.gui.info.output",
                                  energyFormat.format(container.base.getOutput()))
                              .getString()));
    }
    this.addElement(
        new VanillaButton(this, 152, 4, 20, 20, this.createEventSender(0))
            .withIcon((Supplier<ItemStack>) () -> new ItemStack(Items.REDSTONE))
            .withTooltip((Supplier<String>) container.base::getRedstoneMode));
  }

  @Override
  protected void drawForegroundLayer(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    int color = 4207152;
    super.drawForegroundLayer(guiGraphics, mouseX, mouseY);
    this.drawString(
        guiGraphics,
        8,
        74,
        Component.translatable("ic2.EUStorage.gui.info.armor").getString(),
        color);
  }

  public List<Component> getReadingLines() {
    return List.of(
        Component.translatable("gui.ic2.storage.tier", this.menu.base.energy.getSourceTier()),
        Component.translatable("gui.ic2.storage.output_label"),
        Component.translatable(
            "gui.ic2.storage.output_value", energyFormat.format(this.menu.base.getOutput())));
  }

  private static DecimalFormat createEnergyFormat() {
    var symbols = DecimalFormatSymbols.getInstance(Locale.ROOT);
    symbols.setGroupingSeparator(' ');
    return new DecimalFormat("#,##0", symbols);
  }

  public Component getEnergyTooltip() {
    return Component.translatable(
        "gui.ic2.storage.exact",
        energyFormat.format(this.menu.base.getStoredLong()),
        energyFormat.format(this.menu.base.getCapacityLong()));
  }

  @Override
  protected ResourceLocation getTextureLocation() {
    return background;
  }
}
