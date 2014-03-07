package org.team1540.periscope;

import ccre.chan.BooleanInput;
import ccre.chan.BooleanStatus;
import ccre.cluck.CluckGlobals;
import ccre.ctrl.Mixing;
import ccre.event.*;
import ccre.log.LogLevel;
import ccre.log.Logger;
import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

public class CVProcessor implements ImageOutput {

    private final DefaultComboBoxModel<String> pointSettings = new DefaultComboBoxModel<String>();
    private ImageOutput out;
    private final BooleanStatus bout = new BooleanStatus();
    private final BooleanStatus waitingForAck = new BooleanStatus();
    private final Event notifyAck = new Event();
    private float width = 640, height = 640;
    private int scanCell = 2, scanThreshold = 128;
    private short[] lastScan = null;
    public boolean showHistogram = false;
    private long takingUntil = 0, lastTook = 0;
    
    public boolean getHistogram() {
        return showHistogram;
    }
    
    public void setHistogram(boolean b) {
        showHistogram = b;
    }
    
    public Color getActiveColor() {
        return bout.readValue() ? Color.YELLOW : Color.RED;
    }

    {
        if (CluckGlobals.node != null) {
            CluckGlobals.node.publish("is-vt-autonomous", (BooleanInput) bout);
            CluckGlobals.node.publish("ack-vt-autonomous", (EventSource) notifyAck);
            CluckGlobals.node.publish("enable-vt-autonomous", Mixing.combine(waitingForAck.getSetTrueEvent(), new EventLogger(LogLevel.INFO, "Got enable!")));
        }
    }

    public ComboBoxModel<String> getPointSettings() {
        return pointSettings;
    }

    public CVProcessor() {
        pointSettings.addElement("rectUL");
        pointSettings.addElement("rectDR");
    }

    public void putPoint(float x, float y) {
        String c = (String) pointSettings.getSelectedItem();
        if ("rectUL".equals(c)) {
            rectUL = new Point((int) (x * width), (int) (y * height));
            Logger.info("RECT1: " + rectUL + " bc " + x + " " + width);
        } else if ("rectDR".equals(c)) {
            rectDR = new Point((int) (x * width), (int) (y * height));
            Logger.info("RECT2: " + rectDR + " bc " + x + " " + width);
        }
        pointSettings.setSelectedItem(pointSettings.getElementAt((pointSettings.getIndexOf(pointSettings.getSelectedItem()) + 1) % pointSettings.getSize()));
    }

    private Point rectUL, rectDR;

    public void setTarget(ImageOutput out) {
        this.out = out;
    }

    @Override
    public void write(BufferedImage newImage) {
        long now = System.currentTimeMillis();
        if (takingUntil > now && lastTook + 500 < now) {
            try {
                ImageIO.write(newImage, "PNG", new File("Photo-" + System.currentTimeMillis()));
            } catch (IOException ex) {
                Logger.log(LogLevel.WARNING, "Could not write PNG", ex);
            }
            lastTook = now;
        }
        this.width = newImage.getWidth();
        this.height = newImage.getHeight();
        Mat fromImage = OpenCVLoader.getFromImage(newImage);
        Imgproc.GaussianBlur(fromImage, fromImage, new Size(9, 9), 0);
        boolean o = false;
        if (rectUL == null) {
            if (rectDR != null) {
                fromImage.put(rectDR.y, rectDR.x, new byte[]{(byte) 255, (byte) 255, (byte) 255});
            }
        } else if (rectDR == null) {
            fromImage.put(rectUL.y, rectUL.x, new byte[]{(byte) 255, (byte) 255, (byte) 255});
        } else {
            Imgproc.cvtColor(fromImage, fromImage, Imgproc.COLOR_BGR2HSV);
            ArrayList<Mat> mats = new ArrayList<Mat>();
            Core.split(new Mat(fromImage, new Rect(new org.opencv.core.Point(rectUL.x, rectUL.y), new org.opencv.core.Point(rectDR.x, rectDR.y))), mats);
            Mat hist = new Mat();
            Imgproc.calcHist(mats, new MatOfInt(0), new Mat(), hist, new MatOfInt(25), new MatOfFloat(0f, 256f));
            Mat target = new Mat(hist.rows(), hist.cols(), CvType.CV_8UC1);
            Core.normalize(hist, hist, 0, 255, Core.NORM_MINMAX, -1, new Mat());
            hist.convertTo(target, CvType.CV_8UC1);
            byte[] extracted = new byte[25];
            target.get(0, 0, extracted);
            o = (extracted[scanCell] & 255) < scanThreshold;
            if (showHistogram) {
                fromImage = target;
            } else {
                fromImage = OpenCVLoader.getFromImage(newImage);
                Core.rectangle(fromImage, new org.opencv.core.Point(rectUL.x, rectUL.y), new org.opencv.core.Point(rectDR.x, rectDR.y), new Scalar(1, 1, 1));
            }
        }
        bout.writeValue(o);
        out.write(OpenCVLoader.getFromMat(fromImage));
        if (waitingForAck.readValue()) {
            takingUntil = System.currentTimeMillis() + 12000;
            waitingForAck.writeValue(false);
            notifyAck.produce();
            Logger.info("Sent ack!");
        }
    }

    public String getCurrentConfig() {
        return "[" + scanCell + "]/" + scanThreshold + "/" + Arrays.toString(lastScan) + "/" + ((lastScan != null && scanCell >= 0 && scanCell < lastScan.length) ? Integer.toString(lastScan[scanCell]) : "<invalid>");
    }

    public void setCurrentConfig(String data) {
        if (data != null && !data.isEmpty()) {
            String[] pts = data.split("/");
            if (pts.length != 2) {
                Logger.log(LogLevel.WARNING, "Bad input - wrong length");
            } else {
                try {
                    scanCell = Integer.parseInt(pts[0]);
                    scanThreshold = Integer.parseInt(pts[1]);
                    Logger.info("Current config: " + scanCell + "/" + scanThreshold);
                } catch (NumberFormatException ex) {
                    Logger.log(LogLevel.WARNING, "Bad input", ex);
                }
            }
        }
    }
}
