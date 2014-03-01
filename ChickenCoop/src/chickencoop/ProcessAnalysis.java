// Copyright (C) 2014 Colby Skeggs. All rights reserved unless otherwise specified.
// May be freely used for Team 1540 software projects only.
// All redistributions must contain this copyright.
package chickencoop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class ProcessAnalysis {

    public static Map<Integer, String> searchIds(String pattern) throws IOException {
        Process p = Runtime.getRuntime().exec("wmic PROCESS get ProcessID, CommandLine /FORMAT:LIST");
        BufferedReader bread = new BufferedReader(new InputStreamReader(p.getInputStream()));
        HashMap<Integer, String> map = new HashMap<Integer, String>();
        String active = null;
        String line;
        while ((line = bread.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] spt = line.split("=", 2);
            if (active == null) {
                if (!"CommandLine".equalsIgnoreCase(spt[0])) {
                    throw new IOException("Bad format: " + spt[0]);
                }
                active = spt[1];
            } else {
                if (!"ProcessId".equalsIgnoreCase(spt[0])) {
                    throw new IOException("Bad format: " + spt[0]);
                }
                if (active.matches(pattern)) {
                    try {
                        map.put(Integer.parseInt(spt[1]), active);
                    } catch (NumberFormatException ex) {
                        throw new IOException("Bad format", ex);
                    }
                }
                active = null;
            }
        }
        if (active != null) {
            throw new IOException("Bad format!");
        }
        return map;
    }
}
