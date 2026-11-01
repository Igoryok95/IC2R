package ic2.core.gui;

import ic2.core.Ic2Gui;
import ic2.core.block.machine.tileentity.TileEntityAirCompressor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Native IC2 button chrome with vanilla lit/unlit torch artwork. */
public final class AirCompressorButton extends VanillaButton {
  private final TileEntityAirCompressor compressor;

  public AirCompressorButton(
      Ic2Gui<?> gui,
      int x,
      int y,
      int width,
      int height,
      IClickHandler handler,
      TileEntityAirCompressor compressor) {
    super(gui, x, y, width, height, handler);
    this.compressor = compressor;
    withTooltip(
        () ->
            Component.translatable(
                    compressor.enabled
                        ? "ic2.air_compressor.gui.disable"
                        : "ic2.air_compressor.gui.enable")
                .getString());
  }

  @Override
  public void drawBackground(GuiGraphics graphics, int mouseX, int mouseY) {
    super.drawBackground(graphics, mouseX, mouseY);
    graphics.blit(
        ResourceLocation.withDefaultNamespace(
            compressor.enabled
                ? "textures/block/redstone_torch.png"
                : "textures/block/redstone_torch_off.png"),
        gui.getX() + x + (width - 16) / 2,
        gui.getY() + y + (height - 16) / 2,
        0,
        0,
        16,
        16,
        16,
        16);
  }
}
