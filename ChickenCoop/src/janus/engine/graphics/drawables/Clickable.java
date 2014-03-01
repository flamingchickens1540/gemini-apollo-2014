package janus.engine.graphics.drawables;

import janus.engine.graphics.SimplePen;

import java.awt.event.MouseEvent;

public interface Clickable extends Addable {

    public boolean within(MouseEvent e, SimplePen pen);

    public void clicked(MouseEvent e, SimplePen pen);
}
