package com.github.davidcanboni.jenkins.xml;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

/**
 * A Writer implementation that can mask out a particular string - typically a password stored in a Jenkins job configuration.
 */
public class FilteredWriter extends Writer {

    private Writer writer;
    char[] filtered;
    char[] buffer = new char[0];

    public FilteredWriter(Writer writer, String filtered) {
        this.writer = writer;
        this.filtered = filtered.toCharArray();
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {

        // Add the data onto the internal buffer
        char[] buffer = ArrayUtils.addAll(this.buffer, ArrayUtils.subarray(cbuf, off, off + len));

        if (buffer.length >= filtered.length) {
            // Mask out any ocurrences of the filtered value
            replace(buffer);

            // Write out most of the buffer, but retain enough to find a partial ocurrence of the filtered value on the next write
            writer.write(ArrayUtils.subarray(buffer, 0, buffer.length - filtered.length));
            this.buffer = ArrayUtils.subarray(buffer, buffer.length - filtered.length, buffer.length);
        } else {
            this.buffer = buffer;
        }
    }

    void replace(char[] buffer) {
        for (int i = 0; i < buffer.length - filtered.length + 1; i++) {
            //System.out.println(ArrayUtils.toString(ArrayUtils.subarray(buffer, i, i + filtered.length)));
            if (Objects.deepEquals(filtered, ArrayUtils.subarray(buffer, i, i + filtered.length))) {
                for (int c = i; c < i + filtered.length; c++) {
                    buffer[c] = '*';
                }
            }
        }
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        // Write any remaining characters:
        writer.write(buffer, 0, buffer.length);
        writer.close();
    }
}
