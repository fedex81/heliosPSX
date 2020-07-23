package org.jpsx.runtime.util;

import java.util.EnumSet;

/**
 * Federico Berti
 * <p>
 * Copyright 2020
 */
public enum Region {

    JAPAN('J', 2, 0x00, 60),
    USA('U', 0, 0x80, 60),
    EUROPE('E', 1, 0xC0, 50);

    private static EnumSet<Region> values = EnumSet.allOf(Region.class);

    private char region;
    private long versionCode;
    private int fps;
    private int order;
    private double frameIntervalMs;

    Region(char region, int order, long versionCode, int fps) {
        this.region = region;
        this.versionCode = versionCode;
        this.fps = fps;
        this.order = order;
        this.frameIntervalMs = 1000d / fps;
    }

    public static Region getRegion(char region) {
        Region res = null;
        for (Region r : Region.values) {
            res = r.region == region ? r : res;
        }
        return res;
    }

    public int getFps() {
        return fps;
    }

    public double getFrameIntervalMs() {
        return frameIntervalMs;
    }

    public long getVersionCode() {
        return versionCode;
    }
}
