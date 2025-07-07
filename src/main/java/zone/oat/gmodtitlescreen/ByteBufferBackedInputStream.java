package zone.oat.gmodtitlescreen;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * A simple InputStream implementation that reads bytes from a ByteBuffer.
 * This is useful for feeding raw pixel data from a ByteBuffer into methods
 * that expect an InputStream, like NativeImage.read().
 */
public class ByteBufferBackedInputStream extends InputStream {
    private final ByteBuffer buf;

    /**
     * Constructs a new ByteBufferBackedInputStream.
     * The ByteBuffer's position will be used as the starting point for reading.
     *
     * @param buf The ByteBuffer to read from.
     */
    public ByteBufferBackedInputStream(ByteBuffer buf) {
        this.buf = buf;
    }

    /**
     * Reads the next byte of data from the input stream.
     * The value byte is returned as an int in the range 0 to 255.
     * If no byte is available because the end of the stream has been reached, the value -1 is returned.
     *
     * @return The next byte of data, or -1 if the end of the stream is reached.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public int read() throws IOException {
        if (!buf.hasRemaining()) {
            return -1; // End of buffer
        }
        return buf.get() & 0xFF; // Read a byte and convert to unsigned int
    }

    /**
     * Reads up to len bytes of data from the input stream into an array of bytes.
     *
     * @param bytes The buffer into which the data is read.
     * @param off The start offset in the destination array bytes at which the data is written.
     * @param len The maximum number of bytes read.
     * @return The total number of bytes read into the buffer, or -1 if there is no more data because the end of the stream has been reached.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public int read(byte[] bytes, int off, int len) throws IOException {
        if (!buf.hasRemaining()) {
            return -1; // End of buffer
        }
        len = Math.min(len, buf.remaining()); // Don't read more than available
        buf.get(bytes, off, len); // Read bytes into the array
        return len; // Return number of bytes read
    }
}
