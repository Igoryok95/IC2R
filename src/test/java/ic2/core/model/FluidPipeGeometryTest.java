package ic2.core.model;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class FluidPipeGeometryTest {

  @Test
  void glassKeepsPositionDerivedUvsAcrossAllConnectionPatches() throws Exception {
    for (String size : List.of("tiny", "small", "medium", "large"))
      for (int mask = 0; mask < 64; mask++) {
        String name = size + "_" + mask;
        var json = resource("assets/ic2/models/block/experimental/glass_pipe/" + name + ".json");
        assertEquals("neoforge:composite", json.get("loader").getAsString(), name);
        var frame = json.getAsJsonObject("children").getAsJsonObject("frame");
        var glazing = json.getAsJsonObject("children").getAsJsonObject("glazing");
        assertEquals("minecraft:solid", frame.get("render_type").getAsString(), name);
        assertEquals("minecraft:translucent", glazing.get("render_type").getAsString(), name);
        var models =
            List.of(
                BlockModel.fromString(frame.toString()), BlockModel.fromString(glazing.toString()));
        int glass = 0, highlights = 0;
        boolean hasHighlightSpace = false;
        for (var model : models)
          for (var element : model.getElements())
            for (var entry : element.faces.entrySet()) {
              var face = entry.getValue();
              if (face.texture().equals("#shine")) {
                highlights++;
                int axis = entry.getKey().getAxis().ordinal();
                for (int coordinate = 0; coordinate < 3; coordinate++)
                  if (coordinate != axis)
                    assertEquals(
                        .5f,
                        element.to.get(coordinate) - element.from.get(coordinate),
                        name
                            + ": individual highlight pixels keep their 32px density without a background quad");
                assertArrayEquals(new float[] {0, 0, 1, 1}, face.uv().uvs, name);
                assertEquals(
                    255,
                    face.faceData().color() >>> 24,
                    name + ": reflection pixels do not use a translucent decal");
              }
              if (!face.texture().equals("#glass")) continue;
              glass++;
              int planeAxis = entry.getKey().getAxis().ordinal();
              hasHighlightSpace |=
                  element.to.get((planeAxis + 1) % 3) - element.from.get((planeAxis + 1) % 3) >= 2
                      && element.to.get((planeAxis + 2) % 3) - element.from.get((planeAxis + 2) % 3)
                          >= 2;
              int alpha = face.faceData().color() >>> 24;
              assertTrue(alpha > 0 && alpha < 128, name + ": glass has a subtle visible tint");
              assertArrayEquals(
                  element.uvsByFace(entry.getKey()),
                  face.uv().uvs,
                  name + ": parsed UVs must keep native scale and shared model-space alignment");
              assertEquals(
                  0, face.uv().rotation, name + ": coplanar glass patches share their orientation");
            }
        assertTrue(glass > 0, name);
        if (hasHighlightSpace) assertTrue(highlights > 0, name);
      }
  }

  @Test
  void everyElbowKeepsItsConcaveCornerMetalPixel() throws Exception {
    int[] widths = {4, 6, 8, 10};
    String[] sizes = {"tiny", "small", "medium", "large"};
    for (int size = 0; size < sizes.length; size++)
      for (var first : Direction.values())
        for (var second : Direction.values()) {
          if (first.ordinal() >= second.ordinal() || first.getAxis() == second.getAxis()) continue;
          int mask = (1 << first.ordinal()) | (1 << second.ordinal());
          String name = "glass_pipe/" + sizes[size] + "_" + mask;
          var frame = faces(name).stream().filter(face -> face.texture.equals("#metal")).toList();
          double a = (16 - widths[size]) / 2.0, b = 16 - a;
          double[] corner = {a, a, a};
          corner[first.getAxis().ordinal()] =
              first.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? a : b - 1;
          corner[second.getAxis().ordinal()] =
              second.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? a : b - 1;
          int axis = 3 - first.getAxis().ordinal() - second.getAxis().ordinal();
          int u = (axis + 1) % 3, v = (axis + 2) % 3;
          for (double plane : new double[] {a, b})
            assertTrue(
                frame.stream()
                    .anyMatch(
                        face ->
                            face.axis == axis
                                && face.plane == plane
                                && face.u0 <= corner[u]
                                && face.u1 >= corner[u] + 1
                                && face.v0 <= corner[v]
                                && face.v1 >= corner[v] + 1),
                name + ": complete metal corner at " + plane);
        }
  }

  @Test
  void structuralFacesHaveNoUnmatchedVerticesAlongSharedEdges() throws Exception {
    for (String size : List.of("tiny", "small", "medium", "large"))
      for (int mask = 0; mask < 64; mask++) {
        String name = "glass_pipe/" + size + "_" + mask;
        var faces = faces(name).stream().filter(face -> !face.texture.equals("#shine")).toList();
        var lines = new HashMap<Line, TreeSet<Double>>();
        for (var face : faces)
          for (var point : face.corners())
            for (int axis = 0; axis < 3; axis++)
              lines
                  .computeIfAbsent(Line.through(axis, point), ignored -> new TreeSet<>())
                  .add(point[axis]);
        for (var face : faces) {
          var corners = face.corners();
          for (int i = 0; i < 4; i++) {
            var start = corners[i];
            var end = corners[(i + 1) % 4];
            for (int axis = 0; axis < 3; axis++) {
              if (start[axis] == end[axis]) continue;
              double low = Math.min(start[axis], end[axis]),
                  high = Math.max(start[axis], end[axis]);
              var interior = lines.get(Line.through(axis, start)).higher(low);
              assertTrue(
                  interior == null || interior >= high,
                  name + ": unmatched vertex " + interior + " inside edge " + low + ".." + high);
            }
          }
        }
      }
  }

  private record Line(int axis, double u, double v) {
    static Line through(int axis, double[] point) {
      return new Line(axis, point[(axis + 1) % 3], point[(axis + 2) % 3]);
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
      var elements = new JsonArray();
      if (model.has("children")) {
        for (var part : model.getAsJsonObject("children").entrySet())
          elements.addAll(part.getValue().getAsJsonObject().getAsJsonArray("elements"));
      } else elements = model.getAsJsonArray("elements");
      for (var raw : elements) {
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
                  to.get(v).getAsDouble(),
                  element
                      .getAsJsonObject("faces")
                      .getAsJsonObject(direction)
                      .get("texture")
                      .getAsString()));
        }
      }
    }
    return result;
  }

  private record Face(
      int axis, double plane, double u0, double u1, double v0, double v1, String texture) {
    double[][] corners() {
      double[][] points = new double[4][3];
      double[] us = {u0, u1, u1, u0}, vs = {v0, v0, v1, v1};
      for (int i = 0; i < 4; i++) {
        points[i][axis] = plane;
        points[i][(axis + 1) % 3] = us[i];
        points[i][(axis + 2) % 3] = vs[i];
      }
      return points;
    }

    boolean overlaps(Face other) {
      return axis == other.axis
          && plane == other.plane
          && Math.min(u1, other.u1) > Math.max(u0, other.u0)
          && Math.min(v1, other.v1) > Math.max(v0, other.v0);
    }
  }
}
