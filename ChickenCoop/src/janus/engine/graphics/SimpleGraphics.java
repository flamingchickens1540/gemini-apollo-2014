package janus.engine.graphics;

import janus.engine.graphics.drawables.Animation;
import janus.engine.graphics.drawables.Drawable;
import janus.engine.graphics.drawables.Updateable;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Clip;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

public abstract class SimpleGraphics extends ListenFunctionizer {

    protected final JFrame mainFrame;
    protected final long startTime = System.nanoTime();
    protected final HashMap<String, Clip> clips = new HashMap<String, Clip>();
    protected final HashMap<String, Font> fonts = new HashMap<String, Font>();
    protected final HashMap<String, BufferedImage> imgs = new HashMap<String, BufferedImage>();
    protected final HashMap<String, AudioInputStream> sounds = new HashMap<String, AudioInputStream>();
    private boolean first = true;
    public Color background = Color.BLACK;
    public boolean camPause = false;
    public boolean endProgramOnClose = true;
    public int passedFrames = 0;
    public int panelHeight, panelWidth;
    public int xOff, yOff;
    public int storedXOff, storedYOff;

    protected SimpleGraphics(int width, int height, String name) {
        this(width, height, name, 60);
    }

    protected SimpleGraphics(int width, int height, String name, int frames) {
        init(new SimpleCanvas(frames), null);
        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        panelWidth = width;
        panelHeight = height;
        mainPanel.add(simpleCanvas, BorderLayout.CENTER);
        mainFrame = new JFrame();
        mainFrame.setLocation(new java.awt.Point(0, 0));
        mainFrame.setName(name);
        mainFrame.setTitle(name);
        mainFrame.getContentPane().add(mainPanel);
        mainFrame.setVisible(true);
        mainFrame.setResizable(false);
        mainFrame.setSize(width + (simpleCanvas.getLocationOnScreen().x - mainFrame.getX()), height + (simpleCanvas.getLocationOnScreen().y - mainFrame.getY()));
        simpleCanvas.setSize(width, height);
        simpleCanvas.createBufferStrategy(2);
        myPen = new SimplePen(this);
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (endProgramOnClose) {
                    System.exit(0);
                }
                SimpleGraphics.this.onClose();
                SimpleGraphics.this.mainFrame.dispose();
                SimpleGraphics.this.mainFrame.setVisible(false);
            }
        });
    }

    public final class SimpleCanvas extends Canvas {

        private BufferedImage previous;

        private SimpleCanvas(final int frames) {
            new Timer((int) 1000f / frames, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (mainFrame != null) {
                        if (mainFrame.isVisible()) {
                            if (SimpleCanvas.this.getBufferStrategy() != null && myPen != null) {
                                if (SimpleCanvas.this.getBufferStrategy().getDrawGraphics() != null && myPen.adder != null) {
                                    final BufferedImage toUseImage = new BufferedImage(SimpleCanvas.this.getWidth(), SimpleCanvas.this.getHeight(), BufferedImage.TYPE_INT_ARGB);
                                    final Graphics2D toUseImageGraphics = (Graphics2D) toUseImage.getGraphics();
                                    toUseImageGraphics.setColor(background);
                                    toUseImageGraphics.drawRect(0, 0, toUseImage.getWidth(), toUseImage.getHeight());
                                    if (previous == null) {
                                        previous = new BufferedImage(SimpleCanvas.this.getWidth(), SimpleCanvas.this.getHeight(), BufferedImage.TYPE_INT_ARGB);
                                    }
                                    Thread t;
                                    (t = new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            SimplePen touse = new SimplePen(SimpleGraphics.this);
                                            touse.mine = toUseImageGraphics;
                                            runCanvas(touse);
                                        }
                                    })).start();

                                    do {
                                        myPen.updateGraphics();
                                        if (previous != null) {
                                            myPen.mine.drawImage(previous, 0, 0, panelWidth, panelHeight, null);
                                            runAnimations();
                                            try {
                                                Thread.sleep(((int) 1000f / frames) / 2);
                                            } catch (InterruptedException e1) {
                                                e1.printStackTrace();
                                                break;
                                            }
                                        }
                                    } while (t.isAlive());
                                    previous = toUseImage;
                                    myPen.mine.drawImage(toUseImage, 0, 0, toUseImage.getWidth(), toUseImage.getHeight(), null);
                                }
                            }
                        }
                    }
                }
            }).start();
            new Timer((int) 1000f / 70, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final BufferStrategy b = SimpleCanvas.this.getBufferStrategy();
                    if (b != null) {
                        try {
                            b.show();
                        } catch (IllegalStateException e1) {
                        }
                    }
                }
            }).start();
            this.setFocusable(true);
        }

        protected void runCanvas(SimplePen sg) {
            if (this.getBufferStrategy() != null && sg != null) {
                if (this.getBufferStrategy().getDrawGraphics() != null && sg.adder != null) {
                    sg.updateGraphics();
                    if (first) {
                        first = false;
                        this.startCanvas(sg);
                    }
                    this.updateCanvas(sg);
                    this.drawCanvas(sg);
                }
            }
            passedFrames++;
        }

        protected void runAnimations() {
            for (List<Animation> anList : adder.a) {
                Iterator<Animation> aIt = anList.iterator();
                while (aIt.hasNext()) {
                    Animation current = aIt.next();
                    if (current.update(myPen)) {
                        aIt.remove();
                        break;
                    }
                    current.draw(myPen);
                }
            }
        }

        protected void startCanvas(SimplePen sg) {
            try {
                SimpleGraphics.this.start(sg);
            } catch (final Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        protected void updateCanvas(SimplePen sg) {
            try {
                for (final List<Updateable> c : adder.u) {
                    for (final Updateable c2 : c) {
                        c2.update(sg);
                    }
                }
                SimpleGraphics.this.update(sg);
            } catch (final Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        protected void drawCanvas(SimplePen sg) {
            boolean org = SimpleGraphics.this.camPause;
            SimpleGraphics.this.camPause = true;
            sg.setColor(background);
            sg.fillRectangle(0, 0, panelWidth, panelHeight);
            SimpleGraphics.this.camPause = org;
            sg.setColor(Color.BLACK);
            try {
                for (final List<Drawable> d : adder.d) {
                    for (final Drawable d2 : d) {
                        d2.draw(sg);
                    }
                }
                SimpleGraphics.this.draw(sg);
            } catch (final Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    public abstract void start(SimplePen pen);

    public abstract void update(SimplePen pen);

    public abstract void draw(SimplePen pen);
}
