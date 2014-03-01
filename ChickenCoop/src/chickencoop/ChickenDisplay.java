/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chickencoop;

import janus.engine.graphics.SimpleGraphics;
import janus.engine.graphics.SimplePen;
import janus.engine.graphics.drawables.Clickable;
import janus.engine.graphics.drawables.Button;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author peachg
 */
public class ChickenDisplay extends SimpleGraphics {

    public ChickenDisplay() {
        super(340, 180, "ChickenCoop");
    }

    @Override
    public void start(SimplePen pen) {
        this.mainFrame.setLocation(683, 384);
        pen.adder.add(new ProccessButton(10, 10, 100, 100, "Poultry.bat", "\\", "PoultryInspector", Arrays.asList("")));
        pen.adder.add(new ProccessButton(120, 10, 100, 100, "Periscope.bat", "\\", "Periscope", Arrays.asList("")));
        pen.adder.add(new ProccessButton(230, 10, 100, 100, "Driver.bat", "\\", "Drivers Station", Arrays.asList("")));
        pen.adder.add(new Button() {
            @Override
            public void draw(SimplePen pen) {
                pen.setColor(Color.BLUE);
                pen.fillRectangle(10, 120, 320, 20);
            }

            @Override
            public boolean within(MouseEvent e, SimplePen pen) {
                return pen.within(e, 10, 110, 320, 20);
            }

            @Override
            public void clicked(MouseEvent e, SimplePen pen) {
                for (List<Clickable> cl : pen.adder.c) {
                    for (Clickable c : cl) {
                        if (!c.equals(this)) {
                            c.clicked(e, pen);
                        }
                    }
                }
            }

        });
    }

    @Override
    public void update(SimplePen pen) {
        this.mainFrame.setVisible(true);
        for (List<Clickable> cl : pen.adder.c) {
            for (Clickable c : cl) {
                if (c instanceof ProccessButton) {
                    if (ProccessButton.isRunning(((ProccessButton) c).search)) {
                    } else {
                        return;
                    }
                }
            }
        }
        this.mainFrame.setVisible(false);
    }

    @Override
    public void draw(SimplePen pen) {
    }

}
