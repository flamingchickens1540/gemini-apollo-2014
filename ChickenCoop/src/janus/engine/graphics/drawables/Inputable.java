package janus.engine.graphics.drawables;

import janus.engine.graphics.SimplePen;

import java.awt.event.KeyEvent;

public interface Inputable extends Addable {

    public boolean receving(SimplePen pen);

    public void keyPressed(KeyEvent e, SimplePen pen);

    public void keyTyped(KeyEvent e, SimplePen pen);
}
