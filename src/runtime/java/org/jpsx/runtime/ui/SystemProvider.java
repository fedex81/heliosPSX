/*
 * SystemProvider
 * Copyright (c) 2018-2019 Federico Berti
 * Last modified: 13/10/19 15:41
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jpsx.runtime.ui;

import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.util.EnumSet;

public interface SystemProvider {
//    extends Device {

    void handleSystemEvent(SystemEvent event, Object parameter);

    /**
     * STATE
     **/

    boolean isRomRunning();

//    RegionDetector.Region getRegion();

//    default long getRegionCode() {
//        return getRegion().getVersionCode();
//    }

    String getRomName();

    default SystemType getSystemType() {
        return SystemType.PSX;
    }

    default void reset() {
    }

    //TODO
    static class VideoMode {
        Dimension d = new Dimension(320, 256);

        Dimension getDimension() {
            return d;
        }
    }

    public enum SystemType {
        NONE(""),
        PSX("PSX");

        private String shortName;

        SystemType(String s) {
            this.shortName = s;
        }

        public String getShortName() {
            return shortName;
        }
    }

    static class FileLoader {
        public static String basePath = System.getProperty("user.home") + File.separatorChar + "roms";

        private static String SNAPSHOT_VERSION = "SNAPSHOT";
        private static String MANIFEST_RELATIVE_PATH = "/META-INF/MANIFEST.MF";
        private static String BIOS_JAR_PATH = ".";
        public static String QUICK_SAVE_FILENAME = "quick_save";
        public static String QUICK_SAVE_PATH = System.getProperty("quick.save.path", ".");
        public static FileFilter ROM_FILTER = null;
        public static FileFilter SAVE_STATE_FILTER = null;

        static public String readFileContentAsString(String str) {
            return "";
        }
    }

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
    //TODO

    enum SystemEvent {
        NONE,
        NEW_ROM,
        CLOSE_ROM,
        CLOSE_APP,
        RESET,
        LOAD_STATE,
        SAVE_STATE,
        QUICK_SAVE,
        QUICK_LOAD,
        TOGGLE_PAUSE,
        TOGGLE_MUTE,
        TOGGLE_FULL_SCREEN,
        TOGGLE_THROTTLE,
        CONTROLLER_CHANGE,
        TOGGLE_DEBUG_LOGGING,
        SET_DEBUG_UI,
        TOGGLE_SOUND_RECORD,
        SOFT_RESET
    }
}
