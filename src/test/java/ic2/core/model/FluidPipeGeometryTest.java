package ic2.core.model;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.model.BlockModel;
import org.junit.jupiter.api.Test;

class FluidPipeGeometryTest {

  @Test
  void glassKeepsPositionDerivedUvsAcrossAllConnectionPatches() throws Exception {
    for (String size : List.of("tiny", "small", "medium", "large"))
      for (int mask = 0; mask < 64; mask++) {
        String name = size + "_" + mask;
        var json = resource("assets/ic2/models/block/experimental/glass_pipe/" + name + ".json");
        var model = BlockModel.fromString(json.toString());
        int glass = 0;
        for (var element : model.getElements())
          for (var entry : element.faces.entrySet()) {
            var face = entry.getValue();
            if (!face.texture().equals("#glass")) continue;
            glass++;
            assertArrayEquals(
                element.uvsByFace(entry.getKey()),
                face.uv().uvs,
                name + ": parsed UVs must keep native scale and shared model-space alignment");
            assertEquals(
                0, face.uv().rotation, name + ": coplanar glass patches share their orientation");
          }
        assertTrue(glass > 0, name);
      }
  }

  @Test
  void everyMaterialSizeAndConnectionMaskHasNoOverlappingCoplanarFaces() throws Exception {
    for (String size : List.of("tiny", "small", "medium", "large")) {
      for (int mask = 0; mask < 64; mask++) {
        String base = "glass_pipe/" + size + "_" + mask;
        var selected = faces(base);
        for (int i = 0; i < selected.size(); i++)
          for (int j = i + 1; j < selected.size(); j++)
            assertFalse(
                selected.get(i).overlaps(selected.get(j)),
                base
                    + " connections="
                    + mask
                    + ": coincident faces "
                    + selected.get(i)
                    + " and "
                    + selected.get(j));
      }
    }
  }

  @Test
  void everyPipeKeepsItsMetalCostAndAddsOneGlassPane() throws Exception {
    int[] outputs = {6, 3, 2, 1};
    String[] sizes = {"tiny", "small", "medium", "large"};
    for (String material : List.of("bronze", "steel"))
      for (int i = 0; i < sizes.length; i++) {
        String name = material + "_pipe_" + sizes[i];
        var recipe = resource("data/ic2/recipe/shaped/experimental/" + name + ".json");
        String pattern = recipe.getAsJsonArray("pattern").toString();
        assertEquals(6, pattern.chars().filter(c -> c == 'C').count(), name);
        assertEquals(1, pattern.chars().filter(c -> c == 'G').count(), name);
        assertEquals(
            "c:glass_panes/colorless",
            recipe.getAsJsonObject("key").getAsJsonObject("G").get("tag").getAsString());
        assertEquals(outputs[i], recipe.getAsJsonObject("result").get("count").getAsInt());
        var states =
            resource("assets/ic2/blockstates/" + name + ".json").getAsJsonObject("variants");
        assertEquals(64, states.size(), name);
        for (var entry : states.entrySet())
          assertTrue(
              entry
                  .getValue()
                  .getAsJsonObject()
                  .get("model")
                  .getAsString()
                  .startsWith("ic2:block/experimental/glass_pipe/" + sizes[i] + "_"));
      }
  }

  private static JsonObject resource(String resource) throws Exception {
    try (var stream = FluidPipeGeometryTest.class.getClassLoader().getResourceAsStream(resource)) {
      assertNotNull(stream, resource);
      return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
          .getAsJsonObject();
    }
  }

  private static List<Face> faces(String name) throws Exception {
    String resource = "assets/ic2/models/block/experimental/" + name + ".json";
    var result = new ArrayList<Face>();
    try (var stream = FluidPipeGeometryTest.class.getClassLoader().getResourceAsStream(resource)) {
      assertNotNull(stream, resource);
      var model =
          JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
              .getAsJsonObject();
      for (var raw : model.getAsJsonArray("elements")) {
        JsonObject element = raw.getAsJsonObject();
        JsonArray from = element.getAsJsonArray("from"), to = element.getAsJsonArray("to");
        for (String direction : element.getAsJsonObject("faces").keySet()) {
          int axis =
              direction.equals("west") || direction.equals("east")
                  ? 0
                  : direction.equals("down") || direction.equals("up") ? 1 : 2;
          int u = (axis + 1) % 3, v = (axis + 2) % 3;
          boolean positive =
              direction.equals("east") || direction.equals("up") || direction.equals("south");
          result.add(
              new Face(
                  axis,
                  (positive ? to : from).get(axis).getAsDouble(),
                  from.get(u).getAsDouble(),
                  to.get(u).getAsDouble(),
                  from.get(v).getAsDouble(),
                  to.get(v).getAsDouble()));
        }
      }
    }
    return result;
  }

  private record Face(int axis, double plane, double u0, double u1, double v0, double v1) {
    boolean overlaps(Face other) {
      return axis == other.axis
          && plane == other.plane
          && Math.min(u1, other.u1) > Math.max(u0, other.u0)
          && Math.min(v1, other.v1) > Math.max(v0, other.v0);
    }
  }
}
