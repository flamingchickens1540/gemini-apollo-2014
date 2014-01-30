package org.team1540.periscope;

import ccre.chan.BooleanInput;
import ccre.chan.BooleanStatus;
import ccre.cluck.CluckGlobals;
import ccre.ctrl.Mixing;
import ccre.event.*;
import ccre.log.LogLevel;
import ccre.log.Logger;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
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

    {
        CluckGlobals.node.publish("is-vt-autonomous", (BooleanInput) bout);
        CluckGlobals.node.publish("ack-vt-autonomous", (EventSource) notifyAck);
        CluckGlobals.node.publish("enable-vt-autonomous", Mixing.combine(waitingForAck.getSetTrueEvent(), new EventLogger(LogLevel.INFO, "Got enable!")));
    }

    public ComboBoxModel<String> getPointSettings() {
        return pointSettings;
    }

    public CVProcessor() {
        pointSettings.addElement("rectUL");
        pointSettings.addElement("rectDR");
    }

    public void putPoint(Point p) {
        String c = (String) pointSettings.getSelectedItem();
        if ("rectUL".equals(c)) {
            rectUL = p;
        } else if ("rectDR".equals(c)) {
            rectDR = p;
        }
        pointSettings.setSelectedItem(pointSettings.getElementAt((pointSettings.getIndexOf(pointSettings.getSelectedItem()) + 1) % pointSettings.getSize()));
    }

    private Point rectUL, rectDR;

    public void setTarget(ImageOutput out) {
        this.out = out;
    }

    @Override
    public void write(BufferedImage newImage) {
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
            ArrayList<Mat> mats = new ArrayList();
            Core.split(new Mat(fromImage, new Rect(new org.opencv.core.Point(rectUL.x, rectUL.y), new org.opencv.core.Point(rectDR.x, rectDR.y))), mats);
            Mat hist = new Mat();
            Imgproc.calcHist(mats, new MatOfInt(0), new Mat(), hist, new MatOfInt(25), new MatOfFloat(0f, 256f));
            Mat target = new Mat(hist.rows(), hist.cols(), CvType.CV_8UC1);
            Core.normalize(hist, hist, 0, 255, Core.NORM_MINMAX, -1, new Mat());
            hist.convertTo(target, CvType.CV_8UC1);
            byte[] extracted = new byte[25];
            target.get(0, 0, extracted);
            if ((extracted[16] & 255) < 128) {
                fromImage = target;
            } else {
                o = true;
                fromImage = OpenCVLoader.getFromImage(newImage);
                //fromImage = mats.get((int) (System.currentTimeMillis() / 1000 % 4) % 3);
                Core.rectangle(fromImage, new org.opencv.core.Point(rectUL.x, rectUL.y), new org.opencv.core.Point(rectDR.x, rectDR.y), new Scalar(1, 1, 1));
            }
        }
        bout.writeValue(o);
        out.write(OpenCVLoader.getFromMat(fromImage));
        if (waitingForAck.readValue()) {
            waitingForAck.writeValue(false);
            notifyAck.produce();
            Logger.info("Sent ack!");
        }
    }
}
