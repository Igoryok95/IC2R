package ic2.core.sound;

import ic2.core.init.IC2ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;

/** Client-local IC2 volume, exposed through the native scrollable audio options list. */
public final class Ic2AudioSettings {
  public static float scale(float volume) {
    return IC2ClientConfig.audio.enabled.get()
        ? volume * IC2ClientConfig.audio.volume.get().floatValue()
        : 0;
  }

  public static void setVolume(double volume) {
    double previous = IC2ClientConfig.audio.volume.get();
    IC2ClientConfig.audio.volume.set(Math.clamp(volume, 0, 1));
    IC2ClientConfig.audio.volume.save();
    var mc = Minecraft.getInstance();
    mc.getSoundManager()
        .updateSourceVolume(
            SoundSource.BLOCKS, mc.options.getSoundSourceVolume(SoundSource.BLOCKS));
    if (previous == 0
        && volume > 0
        && ic2.core.IC2.soundManager instanceof SoundManagerClient manager)
      manager.resumeMutedLoops();
  }

  public static void addSlider(
      Screen screen,
      java.util.List<net.minecraft.client.gui.components.events.GuiEventListener> widgets) {
    if (!(screen instanceof SoundOptionsScreen)) return;
    for (var widget : widgets) {
      if (!(widget instanceof OptionsList list)) continue;
      var option =
          new OptionInstance<Double>(
              "ic2.audio.volume",
              OptionInstance.cachedConstantTooltip(
                  Component.translatable("ic2.audio.volume.tooltip")),
              (label, value) ->
                  value == 0
                      ? Component.translatable(
                          "options.generic_value", label, Component.translatable("options.off"))
                      : Component.translatable(
                          "options.percent_value", label, (int) Math.round(value * 100)),
              OptionInstance.UnitDouble.INSTANCE,
              IC2ClientConfig.audio.volume.get(),
              Ic2AudioSettings::setVolume);
      var voice = Minecraft.getInstance().options.getSoundSourceOptionInstance(SoundSource.VOICE);
      var voiceWidget = list.findOption(voice);
      int voiceRow = -1;
      for (int i = 0; i < list.children().size(); i++) {
        Object row = list.children().get(i);
        if (row instanceof net.minecraft.client.gui.components.events.ContainerEventHandler entry
            && entry.children().size() == 1
            && entry.children().contains(voiceWidget)) {
          voiceRow = i;
          break;
        }
      }
      if (voiceRow >= 0) {
        list.children().remove(voiceRow);
        list.addSmall(voice, option);
        java.util.Collections.rotate(list.children().subList(voiceRow, list.children().size()), 1);
      } else {
        list.addSmall(option);
      }
      return;
    }
  }

  private Ic2AudioSettings() {}
}
