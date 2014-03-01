package janus.engine.graphics;

import janus.engine.graphics.drawables.Addable;
import janus.engine.graphics.drawables.Animation;
import janus.engine.graphics.drawables.Clickable;
import janus.engine.graphics.drawables.Drawable;
import janus.engine.graphics.drawables.Inputable;
import janus.engine.graphics.drawables.Updateable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("Convert2Diamond")
public class AddingManager {

    public void clearAll() {
        d.clear();
        u.clear();
        c.clear();
        i.clear();
        a.clear();
    }
    public List<List<Drawable>> d = new ArrayList<List<Drawable>>();
    public List<List<Updateable>> u = new ArrayList<List<Updateable>>();
    public List<List<Clickable>> c = new ArrayList<List<Clickable>>();
    public List<List<Inputable>> i = new ArrayList<List<Inputable>>();
    public List<List<Animation>> a = new ArrayList<List<Animation>>();

    public void add(Addable o) {
        if (o instanceof Animation) {
            a.add(new ArrayList<Animation>(Arrays.asList((Animation) o)));
        }
        if (o instanceof Drawable) {
            d.add(new ArrayList<Drawable>(Arrays.asList((Drawable) o)));
        }
        if (o instanceof Updateable) {
            u.add(new ArrayList<Updateable>(Arrays.asList((Updateable) o)));
        }
        if (o instanceof Clickable) {
            c.add(new ArrayList<Clickable>(Arrays.asList((Clickable) o)));
        }
        if (o instanceof Inputable) {
            i.add(new ArrayList<Inputable>(Arrays.asList((Inputable) o)));
        }
    }

    public void remove(Addable o) {
        if (o instanceof Animation) {
            a.remove(new ArrayList<Animation>(Arrays.asList((Animation) o)));
        }
        if (o instanceof Drawable) {
            d.remove(new ArrayList<Drawable>(Arrays.asList((Drawable) o)));
        }
        if (o instanceof Updateable) {
            u.remove(new ArrayList<Updateable>(Arrays.asList((Updateable) o)));
        }
        if (o instanceof Clickable) {
            c.remove(new ArrayList<Clickable>(Arrays.asList((Clickable) o)));
        }
        if (o instanceof Inputable) {
            i.remove(new ArrayList<Inputable>(Arrays.asList((Inputable) o)));
        }
    }

    public void deepRemove() {

    }

    public void addDrawList(List<? extends Drawable> dl) {
        d.add((List<Drawable>) dl);
    }

    public void addUpdateList(List<? extends Updateable> ul) {
        u.add((List<Updateable>) ul);
    }

    public void addClickList(List<? extends Clickable> cl) {
        c.add((List<Clickable>) cl);
    }

    public void addInputableList(List<? extends Inputable> il) {
        i.add((List<Inputable>) il);
    }

    public void addAnimationList(List<? extends Animation> il) {
        a.add((List<Animation>) il);
    }

    public void removeDrawList(List<? extends Drawable> dl) {
        d.remove((List<Drawable>) dl);
    }

    public void removeUpdateList(List<? extends Updateable> ul) {
        u.remove((List<Updateable>) ul);
    }

    public void removeClickList(List<? extends Clickable> cl) {
        c.remove((List<Clickable>) cl);
    }

    public void removeInputableList(List<? extends Inputable> il) {
        i.remove((List<Inputable>) il);
    }

    public void removeAnimationList(List<? extends Animation> il) {
        a.remove((List<Animation>) il);
    }
}
