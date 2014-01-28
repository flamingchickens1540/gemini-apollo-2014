package org.team1540.periscope;

import ccre.cluck.CluckGlobals;

public class CluckConnector {
    private String address;
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
        if (address == null) {
            if (CluckGlobals.cli != null) {
                CluckGlobals.cli.setRemote("1.2.3.4");
            }
        } else {
            if (CluckGlobals.cli == null) {
                CluckGlobals.setupClient(address, "peer", "periscope");
            } else {
                CluckGlobals.cli.setRemote(address);
            }
        }
    }
}
