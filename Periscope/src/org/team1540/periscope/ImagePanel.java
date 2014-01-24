package org.team1540.periscope;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

public class ImagePanel extends JPanel implements ImageOutput {

    private BufferedImage img;

    @Override
    public void write(BufferedImage newImage) {
        img = newImage;
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        if (img == null) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
    }
    
    public BufferedImage getActiveImage() {
        return img;
    }
}
