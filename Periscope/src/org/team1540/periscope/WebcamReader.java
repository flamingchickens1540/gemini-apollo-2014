/*
 * Copyright 2013-2014 Andrew Merrill & Colby Skeggs
 * 
 * This file is part of the CCRE, the Common Chicken Runtime Engine.
 * 
 * The CCRE is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * 
 * The CCRE is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the CCRE.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.team1540.periscope;

import ccre.log.LogLevel;
import ccre.log.Logger;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;

public class WebcamReader {

    private final BufferedInputStream in;
    private final ImageOutput output;

    public WebcamReader(BufferedInputStream instr, ImageOutput output) {
        this.in = instr;
        this.output = output;
    }

    public void loop() throws IOException {
        String boundary = null;
        assertLine("HTTP/1.0 200 OK");
        for (String line : readLinesUntilEmptyLine()) {
            if (line.startsWith("Content-Type:")) {
                int i = line.indexOf("boundary=");
                boundary = line.substring(i + 9);
            }
        }
        if (boundary == null) {
            throw new IOException("No boundary!");
        }

        while (!Thread.interrupted()) {
            String header = nextNonemptyLine();
            if (!header.endsWith(boundary)) {
                throw new IOException("Not a boundary: " + header);
            }
            int contentLength = -1;
            for (String line : readLinesUntilEmptyLine()) {
                if (line.startsWith("Content-Length: ")) {
                    String contentLengthString = line.substring(line.indexOf(": ") + 2);
                    try {
                        contentLength = Integer.parseInt(contentLengthString);
                    } catch (NumberFormatException nfe) {
                        throw new IOException("Invalid content length: '" + contentLengthString + "'");
                    }
                }
            }
            if (contentLength < 1 || contentLength > 300000) {
                throw new IOException("content length out of range: " + contentLength);
            }
            BufferedImage readImage = readImage(contentLength);
            try {
                output.write(readImage);
            } catch (Throwable thr) {
                Logger.log(LogLevel.WARNING, "Could not handle image", thr);
            }
        }
    }

    private void assertLine(String require) throws IOException {
        String line = readLine();
        if (!line.equals(require)) {
            throw new IOException("Expected '" + require + "', got '" + line + "'");
        }
    }

    private String nextNonemptyLine() throws IOException {
        while (true) {
            String line = readLine();
            if (!line.isEmpty()) {
                return line;
            }
        }
    }

    private Iterable<String> readLinesUntilEmptyLine() throws IOException {
        ArrayList<String> out = new ArrayList<String>();
        while (true) {
            String str = readLine();
            if (str.isEmpty()) {
                break;
            }
            out.add(str);
        }
        return out;
    }

    private String readLine() throws IOException {
        byte[] buffer = new byte[1024];
        for (int i = 0; i < buffer.length; i++) {
            int b = in.read();
            if (b == -1) {
                throw new EOFException();
            }
            buffer[i] = (byte) b;
            if (i > 0 && b == 10 && buffer[i - 1] == 13) {
                return new String(buffer, 0, i - 1);
            }
        }
        return null;
    }

    private BufferedImage readImage(int contentLength) throws IOException {
        byte[] imageBytes = new byte[contentLength];
        int bytesRemaining = contentLength;
        int offset = 0;
        while (bytesRemaining > 0) {
            int bytesRead = in.read(imageBytes, offset, bytesRemaining);
            if (bytesRead == -1) {
                throw new EOFException();
            }
            bytesRemaining -= bytesRead;
            offset += bytesRead;
        }
        return ImageIO.read(new ByteArrayInputStream(imageBytes));
    }
}
