package janus.engine.graphics.drawables;

import janus.engine.graphics.SimplePen;

public interface Animation extends Addable {

    public boolean update(SimplePen pen);

    public void draw(SimplePen pen);
}
