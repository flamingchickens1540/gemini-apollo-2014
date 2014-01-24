package org.team1540.periscope;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

public class CVProcessor implements Serializable, ImageOutput {

    private transient final DefaultComboBoxModel<String> pointSettings = new DefaultComboBoxModel<String>();
    private transient final HashMap<String, PropertyDescriptor> pointMap = new HashMap<String, PropertyDescriptor>();
    private ImageOutput out;

    public ComboBoxModel<String> getPointSettings() {
        return pointSettings;
    }

    public CVProcessor() {
        try {
            for (PropertyDescriptor pd : Introspector.getBeanInfo(this.getClass()).getPropertyDescriptors()) {
                if (pd.getPropertyType() == Point.class) {
                    pointSettings.addElement(pd.getName());
                    pointMap.put(pd.getName(), pd);
                }
            }
        } catch (IntrospectionException ex) {
            Logger.getLogger(CVProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void putPoint(Point p) {
        try {
            PropertyDescriptor pm = pointMap.get((String) pointSettings.getSelectedItem());
            if (pm == null) {
                return;
            }
            pm.getWriteMethod().invoke(this, p);
            pointSettings.setSelectedItem(pointSettings.getElementAt((pointSettings.getIndexOf(pointSettings.getSelectedItem()) + 1) % pointSettings.getSize()));
        } catch (IllegalAccessException ex) {
            Logger.getLogger(CVProcessor.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(CVProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private Point rectUL, rectDR;

    public Point getRectUL() {
        return rectUL;
    }

    public void setRectUL(Point p) {
        rectUL = p;
    }

    public Point getRectDR() {
        return rectDR;
    }

    public void setRectDR(Point p) {
        rectDR = p;
    }

    public void setTarget(ImageOutput out) {
        this.out = out;
    }

    @Override
    public void write(BufferedImage newImage) {
        Mat fromImage = OpenCVLoader.getFromImage(newImage);
        if (rectUL == null) {
            if (rectDR != null) {
                fromImage.put(rectDR.y, rectDR.x, new byte[]{(byte) 255, (byte) 255, (byte) 255});
            }
        } else if (rectDR == null) {
            fromImage.put(rectUL.y, rectUL.x, new byte[]{(byte) 255, (byte) 255, (byte) 255});
        } else {
            Core.rectangle(fromImage, new org.opencv.core.Point(rectUL.x, rectUL.y), new org.opencv.core.Point(rectDR.x, rectDR.y), new Scalar(1, 1, 1));
        }
        out.write(OpenCVLoader.getFromMat(fromImage));
    }

}
