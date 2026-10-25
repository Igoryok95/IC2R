package ic2.core.experimental;

import ic2.core.IC2;
import ic2.core.Ic2Gui;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class ContainmentScreen extends Ic2Gui<ContainmentMenu> {
  public ContainmentScreen(ContainmentMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title, 166);
  }

  @Override
  protected ResourceLocation getTextureLocation() {
    return IC2.getIdentifier("textures/gui/experimental/containment_box.png");
  }
}
