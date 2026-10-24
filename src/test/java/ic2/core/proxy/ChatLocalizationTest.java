package ic2.core.proxy;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.Test;

class ChatLocalizationTest {
  @Test
  void argumentsPreserveComponentsStylesAndNumericTypes() {
    var mode =
        Component.translatable("item.ic2.mining_laser.tooltip.mode.normal")
            .withStyle(ChatFormatting.AQUA);
    var arguments =
        SideProxyServer.getMessageComponents(mode, 42, "Igoryok95", "ic2.tooltip.mode.single");
    assertSame(mode, arguments[0]);
    assertEquals(42, arguments[1]);
    assertEquals("Igoryok95", arguments[2]);
    assertEquals(
        "ic2.tooltip.mode.single",
        ((TranslatableContents) ((Component) arguments[3]).getContents()).getKey());
  }

  @Test
  void serializedChatUsesTheReceivingClientsLanguage() throws Exception {
    Language original = Language.getInstance();
    try {
      Language.inject(locale("en_us"));
      Component message =
          Component.translatable(
              "item.ic2.mining_laser.tooltip.mode",
              SideProxyServer.getMessageComponents(
                  Component.translatable("item.ic2.mining_laser.tooltip.mode.no_shear")));
      assertEquals("Mode: No Shearing", message.getString());
      var registries = HolderLookup.Provider.create(Stream.empty());
      String packet = Component.Serializer.toJson(message, registries);
      assertTrue(packet.contains("item.ic2.mining_laser.tooltip.mode.no_shear"));
      Language.inject(locale("ru_ru"));
      Component received = Component.Serializer.fromJson(packet, registries);
      assertNotNull(received);
      assertEquals("Режим: Без стрижки", received.getString());
      Language.inject(locale("en_us"));
      assertEquals("Mode: No Shearing", received.getString());
    } finally {
      Language.inject(original);
    }
  }

  @Test
  void formattedNamesAndCoordinatesRemainData() {
    var values = SideProxyServer.getMessageComponents("Player ic2.literal", -12, 64, 123.5);
    assertEquals("Player ic2.literal", values[0]);
    assertEquals(-12, values[1]);
    assertEquals(64, values[2]);
    assertEquals(123.5, values[3]);
  }

  private static Language locale(String name) throws Exception {
    Map<String, String> translations = new HashMap<>();
    try (var stream =
        ChatLocalizationTest.class
            .getClassLoader()
            .getResourceAsStream("assets/ic2/lang/" + name + ".json")) {
      assertNotNull(stream);
      var json =
          JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
              .getAsJsonObject();
      json.entrySet()
          .forEach(entry -> translations.put(entry.getKey(), entry.getValue().getAsString()));
    }
    return new Language() {
      public String getOrDefault(String key, String fallback) {
        return translations.getOrDefault(key, fallback);
      }

      public boolean has(String key) {
        return translations.containsKey(key);
      }

      public boolean isDefaultRightToLeft() {
        return false;
      }

      public FormattedCharSequence getVisualOrder(FormattedText text) {
        return FormattedCharSequence.forward(text.getString(), Style.EMPTY);
      }
    };
  }
}
