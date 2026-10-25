package ic2.core.gui;

import ic2.core.Ic2Gui;
import ic2.core.block.machine.IWarmupControl;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Uses the original vanilla lit/unlit torch sprites and the server-synchronized local flag. */
public final class WarmupButton extends VanillaButton {
  private final IWarmupControl control;
  private static final ResourceLocation LIT =
      ResourceLocation.withDefaultNamespace("textures/block/redstone_torch.png");
  private static final ResourceLocation UNLIT =
      ResourceLocation.withDefaultNamespace("textures/block/redstone_torch_off.png");

  public WarmupButton(
      Ic2Gui<?> gui,
      int x,
      int y,
      int width,
      int height,
      IClickHandler handler,
      IWarmupControl control) {
    super(gui, x, y, width, height, handler);
    this.control = control;
    withTooltip(
        () ->
            Component.translatable(
                    control.isManualWarmup()
                        ? "ic2.gui.warmup.manual.on"
                        : "ic2.gui.warmup.manual.off")
                .getString());
  }

  @Override
  public void drawBackground(GuiGraphics graphics, int mouseX, int mouseY) {
    super.drawBackground(graphics, mouseX, mouseY);
    graphics.blit(
        control.isManualWarmup() ? LIT : UNLIT,
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
