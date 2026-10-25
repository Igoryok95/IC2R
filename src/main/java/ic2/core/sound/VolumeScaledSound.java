package ic2.core.sound;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

/** Preserve vanilla playback semantics while scaling unmanaged IC2 one-shots on the client. */
class VolumeScaledSound implements SoundInstance {
  protected final SoundInstance delegate;

  private VolumeScaledSound(SoundInstance delegate) {
    this.delegate = delegate;
  }

  static SoundInstance wrap(SoundInstance sound) {
    return sound instanceof TickableSoundInstance ticking
        ? new Ticking(ticking)
        : new VolumeScaledSound(sound);
  }

  public ResourceLocation getLocation() {
    return delegate.getLocation();
  }

  public WeighedSoundEvents resolve(SoundManager manager) {
    return delegate.resolve(manager);
  }

  public Sound getSound() {
    return delegate.getSound();
  }

  public SoundSource getSource() {
    return delegate.getSource();
  }

  public boolean isLooping() {
    return delegate.isLooping();
  }

  public boolean isRelative() {
    return delegate.isRelative();
  }

  public int getDelay() {
    return delegate.getDelay();
  }

  public float getVolume() {
    return Ic2AudioSettings.scale(delegate.getVolume());
  }

  public float getPitch() {
    return delegate.getPitch();
  }

  public double getX() {
    return delegate.getX();
  }

  public double getY() {
    return delegate.getY();
  }

  public double getZ() {
    return delegate.getZ();
  }

  public Attenuation getAttenuation() {
    return delegate.getAttenuation();
  }

  public boolean canStartSilent() {
    return delegate.canStartSilent();
  }

  public boolean canPlaySound() {
    return delegate.canPlaySound();
  }

  public java.util.concurrent.CompletableFuture<net.minecraft.client.sounds.AudioStream> getStream(
      net.minecraft.client.sounds.SoundBufferLibrary buffers, Sound sound, boolean looping) {
    return delegate.getStream(buffers, sound, looping);
  }

  private static final class Ticking extends VolumeScaledSound implements TickableSoundInstance {
    private final TickableSoundInstance ticking;

    private Ticking(TickableSoundInstance ticking) {
      super(ticking);
      this.ticking = ticking;
    }

    public void tick() {
      ticking.tick();
    }

    public boolean isStopped() {
      return ticking.isStopped();
    }
  }
}
