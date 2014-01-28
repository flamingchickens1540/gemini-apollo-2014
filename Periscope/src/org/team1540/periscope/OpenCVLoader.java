package org.team1540.periscope;

import ccre.log.Logger;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;

public class OpenCVLoader {

    static {
        String arch = System.getProperty("os.arch");
        if (arch.equals("amd64") || arch.equals("x86_64")) {
            System.load(new File("opencv_java248_x64.dll").getAbsolutePath());
        } else {
            if (!arch.equals("x86")) {
                Logger.warning("Cannot detect system type! Defaulting to 32-bit libraries.");
            }
            System.load(new File("opencv_java248_x86.dll").getAbsolutePath());
        }
    }

    static void test(BufferedImage activeImage) {
        Mat x = getFromImage(activeImage);
        Highgui.imwrite("test.png", x);
    }

    public static Mat getFromImage(BufferedImage img) {
        long start = System.nanoTime();
        if (img.getType() != BufferedImage.TYPE_3BYTE_BGR) {
            throw new IllegalArgumentException("Bad type!");
        }
        long mid1 = System.nanoTime();
        Mat cvi = new Mat(img.getHeight(), img.getWidth(), CvType.CV_8UC3);
        long mid2 = System.nanoTime();
        byte[] dat = ((DataBufferByte) img.getTile(0, 0).getDataBuffer()).getData();
        long mid3 = System.nanoTime();
        cvi.put(0, 0, dat);
        long end = System.nanoTime();
        //Logger.info("Mid1 " + (mid1 - start) / 1000000.0 + " ms");
        //Logger.info("Mid2 " + (mid2 - mid1) / 1000000.0 + " ms");
        //Logger.info("Mid3 " + (mid3 - mid2) / 1000000.0 + " ms");
        //Logger.info("End " + (end - mid3) / 1000000.0 + " ms");
        //Logger.info("Total " + (end - start) / 1000000.0 + " ms");
        return cvi;
    }

    /*public static Mat getFromImage2(BufferedImage img) {
     if (img.getType() != BufferedImage.TYPE_3BYTE_BGR) {
     throw new IllegalArgumentException("Bad type!");
     }
     Mat cvi = new Mat(img.getHeight(), img.getWidth(), CvType.CV_8UC3);
     cvi.
     cvi.put(0, 0, ((DataBufferByte) img.getData().getDataBuffer()).getData());
     return cvi;
     }*/
    public static void load() {
        // Forces class initialization.
    }

    public static void main(String[] args) {
        Mat m = new Mat(3, 3, CvType.CV_8UC1);
        System.out.println(m.dump());
    }

    public static BufferedImage getFromMat(Mat fromImage) {
        int type = fromImage.type();
        if (type == CvType.CV_8UC3) { // Default
            type = BufferedImage.TYPE_3BYTE_BGR;
        } else if (type == CvType.CV_8UC1) {
            type = BufferedImage.TYPE_BYTE_GRAY;
        } else {
            throw new IllegalArgumentException("Bad kind of Mat: " + CvType.typeToString(type));
        }
        BufferedImage out = new BufferedImage(fromImage.cols(), fromImage.rows(), type);
        fromImage.get(0, 0, ((DataBufferByte) out.getTile(0, 0).getDataBuffer()).getData());
        return out;
    }
}
