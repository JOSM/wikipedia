// License: GPL. For details, see LICENSE file.
package org.wikipedia.tools;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A {@link FilterInputStream} that redirects all calls to the contained {@link InputStream}
 * and whenever a {@code read} method is called, the read bytes are recorded.
 * You can receive all bytes that were read so far from the stream by calling the method {@link #getCapturedBytes()}.
 */
public class CapturingInputStream extends FilterInputStream {
    private final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

    public CapturingInputStream(final InputStream in) {
        super(in);
    }

    /**
     * @return the bytes that the {@link InputStream} has captured so far.
     */
    public byte[] getCapturedBytes() {
        return byteStream.toByteArray();
    }

    @Override
    public int read() throws IOException {
        final int result = super.read();
        if (result >= 0) {
            byteStream.write(result);
        }
        return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        final int result = super.read(b, off, len);
        byteStream.write(b, off, result);
        return result;
    }
}
