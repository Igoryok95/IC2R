package ic2.core.sound;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** Keeps the machine's startup and working loop on one cancellable OpenAL channel. */
public final class IntroLoopSoundInstance extends RepeatablePositionedSoundInstance {
  private final SoundEvent introEvent;
  private Sound introSound;

  public IntroLoopSoundInstance(
      SoundEvent intro,
      SoundEvent loop,
      SoundSource category,
      float volume,
      float pitch,
      BlockPos pos) {
    super(loop, category, volume, pitch, pos);
    this.introEvent = intro;
  }

  @Override
  public WeighedSoundEvents resolve(SoundManager manager) {
    var event = super.resolve(manager);
    var intro = manager.getSoundEvent(this.introEvent.getLocation());
    this.introSound = intro == null ? SoundManager.EMPTY_SOUND : intro.getSound(this.random);
    if (this.sound != SoundManager.EMPTY_SOUND && this.introSound != SoundManager.EMPTY_SOUND) {
      var loop = this.sound;
      this.sound =
          new Sound(
              loop.getLocation(),
              loop.getVolume(),
              loop.getPitch(),
              loop.getWeight(),
              Sound.Type.FILE,
              true,
              loop.shouldPreload(),
              loop.getAttenuationDistance());
    }
    return event;
  }

  @Override
  public CompletableFuture<AudioStream> getStream(
      SoundBufferLibrary buffers, Sound loop, boolean looping) {
    if (this.introSound == null || this.introSound == SoundManager.EMPTY_SOUND)
      return buffers.getStream(loop.getPath(), true);
    var intro = buffers.getStream(this.introSound.getPath(), false);
    var repeating = buffers.getStream(loop.getPath(), true);
    return intro
        .thenCombine(
            repeating,
            (prefix, cycle) -> {
              AudioStream stream;
              if (!prefix.getFormat().matches(cycle.getFormat())) {
                close(prefix);
                stream = cycle;
              } else {
                stream = new IntroLoopAudioStream(prefix, cycle);
              }
              if (this.isStopped()) {
                AudioFormat format = stream.getFormat();
                close(stream);
                return silent(format);
              }
              return stream;
            })
        .whenComplete(
            (stream, failure) -> {
              if (failure != null) {
                intro.thenAccept(IntroLoopSoundInstance::close);
                repeating.thenAccept(IntroLoopSoundInstance::close);
              }
            });
  }

  private static AudioStream silent(AudioFormat format) {
    return new AudioStream() {
      public AudioFormat getFormat() {
        return format;
      }

      public ByteBuffer read(int size) {
        return ByteBuffer.allocateDirect(0);
      }

      public void close() {}
    };
  }

  private static void close(AudioStream stream) {
    try {
      stream.close();
    } catch (IOException exception) {
      org.slf4j.LoggerFactory.getLogger(IntroLoopSoundInstance.class)
          .warn("Could not close a machine audio stream", exception);
    }
  }
}
