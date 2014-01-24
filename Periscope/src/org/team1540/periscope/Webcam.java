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

import ccre.concurrency.ReporterThread;
import ccre.log.LogLevel;
import ccre.log.Logger;
import java.io.*;
import java.net.*;

public final class Webcam extends ReporterThread {

    private ImageOutput output;
    private String address;
    private boolean keepRunning, enabled = true;
    private int frameCount;
    private long startTime;

    public Webcam() {
        super("Webcam");
        this.keepRunning = true;
        this.start();
    }
    
    public Webcam(String address, ImageOutput output) {
        this();
        setOutput(output);
        setAddress(address);
    }

    public void setOutput(ImageOutput output) {
        this.output = output;
    }

    public ImageOutput getOutput() {
        return output;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void setWebcamEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isWebcamEnabled() {
        return enabled;
    }

    private Socket curclose = null;

    public void reconnect() {
        if (curclose != null) {
            try {
                curclose.close();
            } catch (IOException ex) {
                Logger.log(LogLevel.WARNING, "Got error while closing webcam socket!", ex);
            }
        }
        this.interrupt();
    }

    @Override
    protected void threadBody() {
        while (keepRunning) {
            System.out.println("Try connect at " + System.currentTimeMillis());
            try {
                if (address != null && output != null) {
                    connectToWebcam(address);
                }
                Thread.sleep(1000);
            } catch (IOException ioe) {
                Logger.log(LogLevel.WARNING, "IO Exception", ioe);
            } catch (InterruptedException inte) {
                Logger.log(LogLevel.WARNING, "Interrupted", inte);
            }
        }
        Logger.info("Webcam thread ended.");
    }

    public void connectToWebcam(String address) throws IOException {
        String requestString = "GET /mjpg/video.mjpg HTTP/1.1\n"
                + "User-Agent: HTTPStreamClient\n"
                + "Connection: Keep-Alive\n"
                + "Cache-Control: no-cache\n"
                + "Authorization: Basic RlJDOkZSQw==\n\n";

        Socket socket = new Socket(address, 80);
        curclose = socket;
        try {
            BufferedInputStream socketInputStream = new BufferedInputStream(socket.getInputStream());
            BufferedOutputStream socketOutputStream = new BufferedOutputStream(socket.getOutputStream());
            socketOutputStream.write(requestString.getBytes());
            socketOutputStream.flush();
            if (output == null) {
                Logger.warning("Attempt to send to null!");
                return;
            }
            new WebcamReader(socketInputStream, output).loop();
            Logger.config("Webcam disconnected.");
        } finally {
            curclose = null;
        }
    }

    public double getFPS() {
        long currentTime = System.currentTimeMillis();
        return 1000d * frameCount / (currentTime - startTime);
    }

    public void end() {
        keepRunning = false;
        reconnect();
    }
}
