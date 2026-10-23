package ic2.core.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class RussianLocalizationTest {
  private static final Pattern FORMAT_ARGUMENT =
      Pattern.compile("%(?:\\d+\\$)?[-#+ 0,(<]*\\d*(?:\\.\\d+)?[a-zA-Z%]");

  @Test
  void russianCoversEveryEnglishKey() throws IOException {
    assertEquals(readLocale("en_us").keySet(), readLocale("ru_ru").keySet());
  }

  @Test
  void russianPreservesFormatArguments() throws IOException {
    JsonObject english = readLocale("en_us");
    JsonObject russian = readLocale("ru_ru");

    for (String key : english.keySet()) {
      assertEquals(
          arguments(english.get(key).getAsString()),
          arguments(russian.get(key).getAsString()),
          "format arguments for " + key);
    }
  }

  @Test
  void russianContainsValidReadableText() throws IOException {
    JsonObject russian = readLocale("ru_ru");
    for (String key : russian.keySet()) {
      String value = russian.get(key).getAsString();
      assertFalse(value.isBlank(), "empty translation for " + key);
      assertTrue(value.codePoints().noneMatch(c -> c == 0xfffd), "replacement character in " + key);
      assertTrue(
          value.codePoints().noneMatch(c -> c >= 0x4e00 && c <= 0x9fff),
          "untranslated Chinese text in " + key);
      assertFalse(value.contains("Ã") || value.contains("Ð"), "broken UTF-8 in " + key);
    }

    assertEquals("Дробитель", russian.get("block.ic2.macerator").getAsString());
    assertEquals("Железная печь", russian.get("block.ic2.iron_furnace").getAsString());
    assertEquals("Материя", russian.get("fluid_type.ic2.uu_matter").getAsString());
  }

  private static Map<String, Integer> arguments(String text) {
    Map<String, Integer> result = new HashMap<>();
    Matcher matcher = FORMAT_ARGUMENT.matcher(text);
    while (matcher.find()) {
      result.merge(matcher.group(), 1, Integer::sum);
    }
    return result;
  }

  private static JsonObject readLocale(String name) throws IOException {
    String path = "assets/ic2/lang/" + name + ".json";
    try (InputStream stream =
        RussianLocalizationTest.class.getClassLoader().getResourceAsStream(path)) {
      assertNotNull(stream, "missing locale " + path);
      try (InputStreamReader reader =
          new InputStreamReader(
              stream,
              StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT))) {
        return JsonParser.parseReader(reader).getAsJsonObject();
      }
    }
  }
}
