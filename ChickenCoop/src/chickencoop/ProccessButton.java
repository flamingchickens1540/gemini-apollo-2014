/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chickencoop;

import janus.engine.graphics.SimplePen;
import janus.engine.graphics.drawables.Button;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author peachg
 */
public class ProccessButton implements Button {

    Process current;
    int x;
    int y;
    int width;
    int height;
    String directory;
    final String file;
    final String name;
    List<String> openinstruction;
    final String search;

    public ProccessButton(int x, int y, int width, int height, final String file, String directory, final String name, List<String> openinstruction) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.directory = directory;
        this.file = file;
        this.search = file.split("[.]")[0];
        this.name = name;
        this.openinstruction = openinstruction;
        if (isRunning(search)) {
            current = new Process() {
                @Override
                public OutputStream getOutputStream() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public InputStream getInputStream() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public InputStream getErrorStream() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public int waitFor() throws InterruptedException {
                    while (true) {
                        Thread.sleep(100);
                        if (isRunning(search)) {
                            continue;
                        }
                        break;
                    }
                    return this.exitValue();
                }

                @Override
                public int exitValue() {
                    if (!isRunning(search)) {
                        return 0;
                    } else {
                        throw new IllegalThreadStateException();
                    }
                }

                @Override
                public void destroy() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

            };
        }
    }

    @Override
    public void draw(SimplePen pen) {
        if (current != null) {
            try {
                current.exitValue();
                pen.setColor(Color.WHITE);
            } catch (IllegalThreadStateException e) {
                pen.setColor(Color.RED);
            }
        } else {
            pen.setColor(Color.WHITE);
        }
        pen.fillRectangle(x, y, width, height);
        pen.setColor(Color.BLACK);
        pen.centerString(name, x + width / 2, y + height / 2, "Monospaced", 9);
    }

    @Override
    public boolean within(MouseEvent e, SimplePen pen) {
        return pen.within(e, x, y, width, height);
    }

    @Override
    public void clicked(MouseEvent e, SimplePen pen) {
        try {
            if (current != null) {
                current.exitValue();
            }
        } catch (IllegalThreadStateException e2) {
            return;
        }
        List<String> command = new ArrayList(Arrays.asList(file));
        command.addAll(0, openinstruction);
        Iterator<String> is = command.iterator();
        while (is.hasNext()) {
            if (is.next().equals("")) {
                is.remove();
            }
        }
        ProcessBuilder pb = new ProcessBuilder(command);
        try {
            pb.directory(new File(directory));
            current = pb.start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static boolean isRunning(String file) {
        try {
            return ProcessAnalysis.searchIds(".*" + file + ".*").size() > 0;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
