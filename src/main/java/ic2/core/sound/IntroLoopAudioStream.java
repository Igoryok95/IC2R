package ic2.core.sound;

import java.io.IOException;
import java.nio.ByteBuffer;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.AudioStream;

/** Concatenates decoded startup and repeating PCM in the same queued audio stream. */
public final class IntroLoopAudioStream implements AudioStream {
  private AudioStream intro;
  private final AudioStream loop;
  private final AudioFormat format;
  private boolean closed;

  public IntroLoopAudioStream(AudioStream intro, AudioStream loop) {
    this.format = intro.getFormat();
    if (!this.format.matches(loop.getFormat()))
      throw new IllegalArgumentException("Startup and loop audio formats must match");
    this.intro = intro;
    this.loop = loop;
  }

  @Override
  public AudioFormat getFormat() {
    return this.format;
  }

  @Override
  public ByteBuffer read(int size) throws IOException {
    if (this.closed) throw new IOException("Audio stream is closed");
    if (this.intro == null) return this.loop.read(size);
    ByteBuffer prefix = this.intro.read(size);
    if (prefix.remaining() >= size) return prefix;
    this.intro.close();
    this.intro = null;
    ByteBuffer following = this.loop.read(size - prefix.remaining());
    ByteBuffer joined = ByteBuffer.allocateDirect(prefix.remaining() + following.remaining());
    return joined.put(prefix).put(following).flip();
  }

  @Override
  public void close() throws IOException {
    if (this.closed) return;
    this.closed = true;
    try {
      if (this.intro != null) this.intro.close();
    } finally {
      this.intro = null;
      this.loop.close();
    }
  }
}
