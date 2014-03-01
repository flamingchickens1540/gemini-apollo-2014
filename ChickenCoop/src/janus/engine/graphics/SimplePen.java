package janus.engine.graphics;

import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;

public class SimplePen {

    public Graphics2D mine;

    public void updateGraphics() {
        mine = (Graphics2D) window.simpleCanvas.getBufferStrategy().getDrawGraphics();
    }
    public final AddingManager adder;
    protected final SimpleGraphics window;

    public void centerRectangle(int x, int y, int width, int height) {
        this.fillRectangle(x - width / 2, y - height / 2, width, height);
    }

    public void centerString(String text, int x, int y, String font, int size) {
        final double[] sizer = this.sizeString(text, font, size);
        this.drawString(text, (int) (x - sizer[0] / 2), (int) (y + sizer[1] / 2), font, size);
    }

    public void clearAll() {
        window.adder.clearAll();
    }

    public void drawArc(int x, int y, int width, int height, int startAngle,
            int arcAngle) {
        x -= window.xOff;
        y -= window.yOff;
        mine.drawArc(x, y, width, height, startAngle, arcAngle);
    }

    public void drawCircle(int x, int y, int radius) {
        x -= window.xOff;
        y -= window.yOff;
        mine.drawOval(x - radius, y - radius, radius * 2, radius * 2);
    }

    public void drawEllipse(int x, int y, int width, int height) {
        x -= window.xOff;
        y -= window.yOff;
        mine.drawOval(x - width, y - height, width, height);
    }

    public void drawImage(String name, int x, int y, int width, int height) {
        x -= window.xOff;
        y -= window.yOff;
        BufferedImage img = null;
        if (window.imgs.containsKey(name)) {
            img = window.imgs.get(name);
        } else {
            try {
                img = ImageIO.read(window.getClass().getResource(name));
                window.imgs.put(name, img);
            } catch (final IOException e) {
                throw new RuntimeException("Resource is not loadable!");
            }
        }
        mine.drawImage(img, x, y, width, height, null);
    }

    public void drawLine(int startX, int startY, int endX, int endY) {
        startX -= window.xOff;
        startY -= window.yOff;
        endX -= window.xOff;
        endY -= window.yOff;
        mine.drawLine(startX, startY, endX, endY);
    }

    public void drawLine(int startX, int startY, int endX, int endY,
            int thickness) {
        Stroke s = mine.getStroke();
        mine.setStroke(new BasicStroke(thickness));
        mine.drawLine(startX, startY, endX, endY);
        mine.setStroke(s);
    }

    public void drawPoint(int x, int y) {
        x -= window.xOff;
        y -= window.yOff;
        mine.drawRect(x, y, 1, 1);
    }

    public void drawPolygon(Point[] ps) {
        final int[] xs = new int[ps.length];
        final int[] ys = new int[ps.length];
        int index = -1;
        for (final Point p : ps) {
            index++;
            xs[index] = p.x - window.xOff;
            ys[index] = p.y - window.yOff;
        }
        mine.drawPolygon(xs, ys, xs.length);
    }

    public void drawRectangle(int x, int y, int width, int height) {
        x -= window.xOff;
        y -= window.yOff;
        mine.drawRect(x, y, width, height);
    }

    public void drawString(String text, int x, int y, String font, int size) {
        x -= window.xOff;
        y -= window.yOff;
        mine.setFont(this.getFont(font, size));
        mine.drawString(text, x, y);
    }

    public void endGraphics() {
        window.mainFrame.dispose();
    }

    public void endProgramOnClose(boolean bool) {
        window.endProgramOnClose = bool;
    }

    public void fillArc(int x, int y, int width, int height, int startAngle,
            int arcAngle) {
        x -= window.xOff;
        y -= window.yOff;
        mine.fillArc(x, y, width, height, startAngle, arcAngle);
    }

    public void fillCircle(int x, int y, int radius) {
        x -= window.xOff;
        y -= window.yOff;
        mine.fillOval(x - radius, y - radius, radius * 2, radius * 2);
    }

    public void fillEllipse(int x, int y, int width, int height) {
        x -= window.xOff;
        y -= window.yOff;
        mine.fillOval(x - width, y - height, width, height);
    }

    public void fillPolygon(Point[] ps) {
        final int[] xs = new int[ps.length];
        final int[] ys = new int[ps.length];
        int index = -1;
        for (final Point p : ps) {
            index++;
            xs[index] = p.x - window.xOff;
            ys[index] = p.y - window.yOff;
        }
        mine.fillPolygon(xs, ys, xs.length);
    }

    public void fillRectangle(int x, int y, int width, int height) {
        x -= window.xOff;
        y -= window.yOff;
        mine.fillRect(x, y, width, height);
    }

    public int getActualFrameRate() {
        return (int) (((System.nanoTime() - window.startTime) / 1e9) / window.passedFrames);
    }

    public Point getCameraPosition() {
        return new Point(window.xOff, window.yOff);
    }

    public Font getFont(String fontname, int fontsize) {
        if (window.fonts.containsKey(fontname + fontsize)) {
            return window.fonts.get(fontname + fontsize);
        } else {
            final Font tbr = new Font(fontname, 0, fontsize);
            window.fonts.put(fontname + fontsize, tbr);
            return tbr;
        }
    }

    public void hideCursor() {
        final BufferedImage cursorImg = new BufferedImage(16, 16,
                BufferedImage.TYPE_INT_ARGB);
        final Cursor blankCursor = Toolkit.getDefaultToolkit()
                .createCustomCursor(cursorImg, new java.awt.Point(0, 0),
                        "blank cursor");
        window.mainFrame.getContentPane().setCursor(blankCursor);
    }

    public boolean isKeyPressed(char c) {
        final int code = KeyEvent.getExtendedKeyCodeForChar(c);
        boolean tbr = false;
        if (window.ispresed.containsKey(code)) {
            tbr = window.ispresed.get(code);
        }
        return tbr;
    }

    public boolean isKeyPressed(int i) {
        final int code = i;
        boolean tbr = false;
        if (window.ispresed.containsKey(code)) {
            tbr = window.ispresed.get(code);
        }
        return tbr;
    }

    public boolean isMousePressed() {
        return window.mousePressed.mine;
    }

    public Point mouseLocation() {
        return new Point(-window.mainFrame.getLocation().x
                + MouseInfo.getPointerInfo().getLocation().x,
                -window.mainFrame.getLocation().y
                + MouseInfo.getPointerInfo().getLocation().y);
    }

    public void playSound(String name) {
        // specify the sound to play
        // (assuming the sound can be played by the audio system)
        Clip clip = null;
        AudioInputStream sound = null;
        try {
            if (window.clips.containsKey(name)) {
                clip = window.clips.get(name);
                sound = window.sounds.get(name);
            } else {
                final File soundFile = new File(window.getClass()
                        .getResource(name).getFile());
                sound = AudioSystem.getAudioInputStream(soundFile);
                // load the sound into memory (a Clip)
                final DataLine.Info info = new DataLine.Info(Clip.class,
                        sound.getFormat());
                clip = (Clip) AudioSystem.getLine(info);
                window.clips.put(name, clip);
                window.sounds.put(name, sound);
            }
            clip.open(sound);
        } catch (final Exception e) {
            throw new RuntimeException("Could not read sound");
        }
        // play the sound clip
        clip.start();
    }

    public void preloadImage(String name) {
        BufferedImage img = null;
        if (window.imgs.containsKey(name)) {
            img = window.imgs.get(name);
        } else {
            try {
                img = ImageIO.read(window.getClass().getResource(name));
                window.imgs.put(name, img);
            } catch (final IOException e) {
                throw new RuntimeException("Resource is not loadable!");
            }
        }
    }

    public void preloadSound(String name) {
        Clip clip = null;
        AudioInputStream sound = null;
        if (window.clips.containsKey(name)) {
        } else {
            try {
                final File soundFile = new File(window.getClass()
                        .getResource(name).getFile());
                sound = AudioSystem.getAudioInputStream(soundFile);
                // load the sound into memory (a Clip)
                final DataLine.Info info = new DataLine.Info(Clip.class,
                        sound.getFormat());
                clip = (Clip) AudioSystem.getLine(info);
                window.clips.put(name, clip);
                window.sounds.put(name, sound);
            } catch (final Exception e) {
                throw new RuntimeException("Resource in notloadable");
            }
        }
    }

    // Object related functions
    public void restart() {
        this.clearAll();
        window.start(this);
    }

    public Point screenSize() {
        return new Point(window.panelWidth, window.panelHeight);
    }

    public void setBackground(Color c) {
        window.background = c;
    }

    public void setCameraPosition(int x, int y) {
        window.xOff = x - window.panelWidth / 2;
        window.yOff = y - window.panelHeight / 2;
    }

    public void pauseCamera() {
        if (!window.camPause) {
            window.storedXOff = window.xOff;
            window.storedYOff = window.yOff;
            window.camPause = true;
            window.xOff = window.panelWidth / 2;
            window.yOff = window.panelHeight / 2;
        } else {
            window.camPause = false;
            window.xOff = window.storedXOff;
            window.yOff = window.storedYOff;
        }
    }

    public void setColor(Color c) {
        mine.setColor(c);
    }

    public void showCursor() {
        window.mainFrame.getContentPane().setCursor(
                new Cursor(Cursor.DEFAULT_CURSOR));
    }

    public double[] sizeString(String text, String font, int size) {
        final Rectangle2D sizer = this.getStringSize(text, font, size);
        return new double[]{sizer.getWidth(), sizer.getHeight()};
    }

    public boolean within(MouseEvent e, int x, int y, int width, int height) {
        if (e.getX() > x && e.getX() < x + width && e.getY() > y
                && e.getY() < y + height) {
            return true;
        }
        return false;
    }

    private Rectangle2D getStringSize(String str, String fontname, int fontsize) {
        mine.setFont(this.getFont(fontname, fontsize));
        return mine.getFontMetrics().getStringBounds(str, mine);
    }

    public BufferedImage getScreen() throws AWTException {
        Rectangle r = new Rectangle(this.window.simpleCanvas.getLocationOnScreen().x, this.window.simpleCanvas.getLocationOnScreen().y, this.window.panelWidth, this.window.panelHeight);
        Robot robot = new Robot();
        return robot.createScreenCapture(r);
    }

    public SimplePen(SimpleGraphics graph) {
        window = graph;
        adder = window.adder;
    }
}
