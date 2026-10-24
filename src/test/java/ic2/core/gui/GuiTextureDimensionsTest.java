package ic2.core.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class GuiTextureDimensionsTest {
  @Test
  void nonPowerOfTwoGuiImagesUseTheirActualTextureSize() {
    var texture = new MetadataTexture(34, 18);
    assertEquals(34, texture.getWidth());
    assertEquals(18, texture.getHeight());
    assertEquals(34, texture.getCanvasWidth());
    assertEquals(18, texture.getCanvasHeight());
    texture.close();
  }

  @Test
  void onePixelTexturesHaveValidNonzeroUvDimensionsWithoutAnOpenGlContext() {
    var texture = new MetadataTexture(1, 1);
    assertEquals(1, texture.getCanvasWidth());
    assertEquals(1, texture.getCanvasHeight());
    texture.close();
  }

  private static final class MetadataTexture extends GlTexture {
    MetadataTexture(int width, int height) {
      super(ResourceLocation.fromNamespaceAndPath("ic2", "test/gui.png"));
      load(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));
    }
  }
}
