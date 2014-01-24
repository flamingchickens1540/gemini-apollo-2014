package org.team1540.periscope;

import java.util.Objects;

public class WebcamPanel extends ImagePanel {

    private Webcam web;
    private String address;
    private boolean wenable;

    public synchronized void setAddress(String address) {
        if (Objects.equals(address, this.address)) {
            return;
        }
        this.address = address;
        updateWebcam();
    }

    public String getAddress() {
        return address;
    }

    private void updateWebcam() {
        if (web != null) {
            web.end();
            web = null;
        }
        if (wenable) {
            this.web = new Webcam(address, this);
        }
    }

    public void setWebcamEnabled(boolean enable) {
        if (this.wenable != enable) {
            this.wenable = enable;
            updateWebcam();
        }
    }

}
