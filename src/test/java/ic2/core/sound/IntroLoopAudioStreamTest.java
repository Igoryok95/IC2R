package ic2.core.sound;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.AudioStream;
import org.junit.jupiter.api.Test;

class IntroLoopAudioStreamTest {
  @Test
  void joinsBothRecordingsWithinTheBoundaryBufferWithoutSilence() throws IOException {
    var intro = new Sample(new byte[] {1, 2, 3, 4}, false);
    var loop = new Sample(new byte[] {5, 6, 7, 8}, true);
    try (var stream = new IntroLoopAudioStream(intro, loop)) {
      assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6}, bytes(stream.read(6)));
      assertArrayEquals(new byte[] {7, 8, 5, 6, 7, 8}, bytes(stream.read(6)));
      assertEquals(1, intro.closes);
    }
    assertEquals(1, loop.closes);
  }

  @Test
  void exactBoundaryReturnsLoopDataOnTheNextRead() throws IOException {
    var intro = new Sample(new byte[] {1, 2, 3, 4}, false);
    var loop = new Sample(new byte[] {5, 6}, true);
    try (var stream = new IntroLoopAudioStream(intro, loop)) {
      assertArrayEquals(new byte[] {1, 2, 3, 4}, bytes(stream.read(4)));
      assertArrayEquals(new byte[] {5, 6, 5, 6}, bytes(stream.read(4)));
    }
  }

  @Test
  void emptyIntroDoesNotInsertAnEmptyAudioBuffer() throws IOException {
    var intro = new Sample(new byte[0], false);
    var loop = new Sample(new byte[] {5, 6}, true);
    try (var stream = new IntroLoopAudioStream(intro, loop)) {
      assertArrayEquals(new byte[] {5, 6, 5, 6}, bytes(stream.read(4)));
    }
    assertEquals(1, intro.closes);
  }

  @Test
  void interruptionClosesBothStreamsOnce() throws IOException {
    var intro = new Sample(new byte[] {1, 2, 3, 4}, false);
    var loop = new Sample(new byte[] {5, 6}, true);
    var stream = new IntroLoopAudioStream(intro, loop);
    stream.read(2);
    stream.close();
    stream.close();
    assertEquals(1, intro.closes);
    assertEquals(1, loop.closes);
    assertThrows(IOException.class, () -> stream.read(4));
  }

  private static byte[] bytes(ByteBuffer buffer) {
    var bytes = new byte[buffer.remaining()];
    buffer.get(bytes);
    return bytes;
  }

  private static final class Sample implements AudioStream {
    private final byte[] data;
    private final boolean repeat;
    private int offset, closes;

    private Sample(byte[] data, boolean repeat) {
      this.data = data;
      this.repeat = repeat;
    }

    public AudioFormat getFormat() {
      return new AudioFormat(44100, 16, 1, true, false);
    }

    public ByteBuffer read(int size) {
      var result = ByteBuffer.allocateDirect(repeat ? size : Math.min(size, data.length - offset));
      while (result.hasRemaining()) {
        result.put(data[offset++]);
        if (repeat && offset == data.length) offset = 0;
      }
      return result.flip();
    }

    public void close() {
      closes++;
    }
  }
}
