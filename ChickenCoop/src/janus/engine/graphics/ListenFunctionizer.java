package janus.engine.graphics;

import janus.engine.graphics.drawables.Clickable;
import janus.engine.graphics.drawables.Inputable;

import java.awt.Canvas;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.List;

public abstract class ListenFunctionizer {

    public Canvas simpleCanvas;
    public SimplePen myPen;
    public HashMap<Integer, Boolean> ispresed = new HashMap<Integer, Boolean>();
    public BooleanTracker mousePressed = new BooleanTracker();
    public AddingManager adder = new AddingManager();

    public void init(Canvas c, SimplePen sp) {
        simpleCanvas = c;
        myPen = sp;
        simpleCanvas.addMouseListener(new MouseListner());
        simpleCanvas.addKeyListener(new KeyListner());
    }

    public final class KeyListner implements KeyListener {

        @Override
        public void keyPressed(KeyEvent e) {
            ispresed.put(e.getKeyCode(), true);
            for (List<Inputable> i : adder.i) {
                for (Inputable i2 : i) {
                    if (i2.receving(myPen)) {
                        i2.keyPressed(e, myPen);
                    }
                }
            }
            onKeyPressed(e, myPen);
        }

        @Override
        public void keyReleased(KeyEvent e) {
            ispresed.put(e.getKeyCode(), false);
            onKeyReleased(e, myPen);
        }

        @Override
        public void keyTyped(KeyEvent e) {
            for (List<Inputable> i : adder.i) {
                for (Inputable i2 : i) {
                    if (i2.receving(myPen)) {
                        i2.keyTyped(e, myPen);
                    }
                }
            }
            onKeyTyped(e, myPen);
        }

    }

    public final class MouseListner implements MouseListener {

        @Override
        public void mouseClicked(MouseEvent event) {
            onMouseClicked(event, myPen);
        }

        @Override
        public void mouseEntered(MouseEvent event) {
            onMouseEntered(event, myPen);
        }

        @Override
        public void mouseExited(MouseEvent event) {
            onMouseExited(event, myPen);
        }

        @Override
        public void mousePressed(MouseEvent event) {
            for (List<Clickable> c : adder.c) {
                for (Clickable c2 : c) {
                    if (c2.within(event, myPen)) {
                        c2.clicked(event, myPen);
                    }
                }
            }
            mousePressed.mine = true;
            onMousePressed(event, myPen);
        }

        @Override
        public void mouseReleased(MouseEvent event) {
            mousePressed.mine = false;
            onMouseReleased(event, myPen);
        }
    }

    public void onClose() {
    }

    public void onKeyPressed(KeyEvent event, SimplePen pen) {
    }

    public void onKeyReleased(KeyEvent event, SimplePen pen) {
    }

    public void onKeyTyped(KeyEvent event, SimplePen pen) {
    }

    public void onMouseClicked(MouseEvent event, SimplePen pen) {
    }

    public void onMouseEntered(MouseEvent event, SimplePen pen) {
    }

    public void onMouseExited(MouseEvent event, SimplePen pen) {
    }

    public void onMousePressed(MouseEvent event, SimplePen pen) {
    }

    public void onMouseReleased(MouseEvent event, SimplePen pen) {
    }

    public class BooleanTracker {

        public boolean mine;
    }
}
