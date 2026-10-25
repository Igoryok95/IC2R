package ic2.core.model;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class SteelHopperResourcesTest {
  @Test
  void allHopperDirectionsUseVanillaGeometryAndNative32pxSteelTextures() throws Exception {
    var loader = getClass().getClassLoader();
    for (String model : new String[] {"steel_hopper", "steel_hopper_side"}) {
      try (var stream =
          loader.getResourceAsStream("assets/ic2/models/block/transport/" + model + ".json")) {
        assertNotNull(stream);
        var json =
            JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                .getAsJsonObject();
        assertEquals(
            model.endsWith("_side") ? "minecraft:block/hopper_side" : "minecraft:block/hopper",
            json.get("parent").getAsString());
        for (String key : new String[] {"particle", "top", "side", "inside"}) {
          String texture = json.getAsJsonObject("textures").get(key).getAsString();
          try (var pixels =
              loader.getResourceAsStream(
                  "assets/"
                      + texture.replace(':', '/').replace("ic2/", "ic2/textures/")
                      + ".png")) {
            assertNotNull(pixels, texture);
            var image = ImageIO.read(pixels);
            assertEquals(32, image.getWidth(), texture);
            assertEquals(32, image.getHeight(), texture);
            boolean nativeDetail = false;
            for (int y = 0; y < 32; y += 2) {
              for (int x = 0; x < 32; x += 2) {
                assertEquals(255, image.getRGB(x, y) >>> 24, texture);
                nativeDetail |=
                    image.getRGB(x, y) != image.getRGB(x + 1, y)
                        || image.getRGB(x, y) != image.getRGB(x, y + 1);
              }
            }
            assertTrue(
                nativeDetail, "texture must have detail beyond an enlarged 16px grid: " + texture);
          }
        }
      }
    }
    try (var stream = loader.getResourceAsStream("assets/ic2/models/item/steel_hopper.json")) {
      assertNotNull(stream);
      var item =
          JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
              .getAsJsonObject();
      assertEquals(
          "minecraft:item/hopper",
          item.get("parent").getAsString(),
          "held item must inherit the vanilla item transforms, not an unscaled world model");
    }
    try (var stream = loader.getResourceAsStream("assets/ic2/textures/item/steel_hopper.png")) {
      assertNotNull(stream);
      var item = ImageIO.read(stream);
      assertEquals(32, item.getWidth());
      assertEquals(32, item.getHeight());
      assertEquals(0, item.getRGB(0, 0) >>> 24, "sprite background remains transparent");
    }
  }
}
