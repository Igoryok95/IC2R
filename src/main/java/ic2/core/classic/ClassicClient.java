package ic2.core.classic;

import ic2.core.gui.Gauge;
import net.minecraft.resources.ResourceLocation;

public final class ClassicClient {
  private ClassicClient() {}

  public static void init() {
    var texture =
        ResourceLocation.fromNamespaceAndPath(
            "ic2", "textures/gui_sprites/blocks/machines/lv/gui_rare_earth_extractor.png");
    var material =
        new Gauge.GaugePropertyBuilder(
                7, 168, 162, 2, Gauge.GaugePropertyBuilder.GaugeOrientation.Right)
            .withTexture(texture)
            .withHover(0, -2, 162, 6)
            .build();
    Gauge.GaugeStyle.addStyle("classicmaterial", () -> material);
    var progress =
        new Gauge.GaugePropertyBuilder(
                176, 14, 24, 16, Gauge.GaugePropertyBuilder.GaugeOrientation.Right)
            .withTexture(texture)
            .build();
    Gauge.GaugeStyle.addStyle("classicprogress", () -> progress);
  }
}
