package org.openjfx.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

/**
 * Handles TCP frame reading/writing using newline-delimited JSON.
 * 
 * Since TCP doesn't preserve message boundaries, we use newlines to delimit
 * JSON frames. This allows sending multiple JSON objects over a single stream.
 */
public class Framing {
    private BufferedReader reader;
    private PrintWriter writer;

    /**
     * Initialize the Framing handler with input/output streams.
     * 
     * @param inputStream  Input stream from socket
     * @param outputStream Output stream from socket
     * @throws UnsupportedEncodingException if UTF-8 is not supported
     */
    public Framing(InputStream inputStream, OutputStream outputStream) throws UnsupportedEncodingException {
        this.reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        this.writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);
    }

    /**
     * Read a single JSON frame (one line) from the stream.
     * Blocks until a complete frame is available or EOF is reached.
     * 
     * @return The JSON string, or null if EOF reached
     * @throws IOException if an I/O error occurs
     */
    public String readFrame() throws IOException {
        String frame = reader.readLine();
        if (frame != null) {
            frame = frame.trim();
        }
        return frame;
    }

    /**
     * Write a JSON frame (one line) to the stream.
     * Automatically appends a newline as a frame delimiter.
     * 
     * @param jsonFrame The JSON string to send
     */
    public void writeFrame(String jsonFrame) {
        if (jsonFrame != null) {
            writer.println(jsonFrame);
            writer.flush();
        }
    }

    /**
     * Close the reader and writer, releasing resources.
     */
    public void close() {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException ignored) {
        }

        try {
            if (writer != null) {
                writer.close();
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Check if the reader is ready (has data available).
     * 
     * @return true if data is available to read
     * @throws IOException if an I/O error occurs
     */
    public boolean isReady() throws IOException {
        return reader.ready();
    }
}
