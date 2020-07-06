/*
 * Copyright (C) 2003, 2014 Graham Sanderson
 *
 * This file is part of JPSX.
 * 
 * JPSX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPSX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JPSX.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jpsx.runtime.util;

import org.jpsx.api.components.hardware.cd.CDMedia;
import org.jpsx.api.components.hardware.cd.MediaException;

import static org.jpsx.api.components.hardware.cd.CDMedia.SECTOR_SIZE_BYTES;

public class CDUtil {
    // flags ORed together
    public static final int MEDIA_PRESENT = 0x0001;
    public static final int MEDIA_HAS_DATA = 0x0002;
    public static final int MEDIA_HAS_AUDIO = 0x0004;
    public static final int MEDIA_PLAYSTATION = 0x0008;
    public static final int MEDIA_REGION_EUROPE = 0x0010;
    public static final int MEDIA_REGION_USA = 0x0020;
    public static final int MEDIA_REGION_JAPAN = 0x0040;

    public static int toBCD(int x) {
        return (x % 10) + ((x / 10) << 4);
    }

    public static int fromBCD(int x) {
        return (x >> 4) * 10 + (x & 15);
    }

    // todo fix up this method to return all types
    public static int getMediaType(CDMedia media) {
        int rc = 0;
        if (media != null) {
            rc |= MEDIA_PRESENT;
            for (int i = media.getFirstTrack(); i <= media.getLastTrack(); i++) {
                CDMedia.TrackType tt = media.getTrackType(i);
                if (tt == CDMedia.TrackType.AUDIO) {
                    rc |= MEDIA_HAS_AUDIO;
                } else if (tt == CDMedia.TrackType.MODE2_2352 || tt == CDMedia.TrackType.MODE1_2352) {
                    rc |= MEDIA_HAS_DATA;
                }
            }
            if (0 != (rc & MEDIA_HAS_DATA)) {
                try {
                    int[] sector = new int[SECTOR_SIZE_BYTES / 4];
                    media.readSector(4, sector);
                    int countryCode = (sector[0x15] & 0xff);
                    if (countryCode == 'E') {
                        rc |= MEDIA_REGION_EUROPE;
                    } else {
                    }
                } catch (MediaException e) {
                }
            }
        }
        return rc;
    }


    public static int toMSF(int sector) {
        int f = sector % 75;
        sector /= 75;
        int s = sector % 60;
        sector /= 60;
        int m = sector;
        return (CDUtil.toBCD(m) << 16) | (CDUtil.toBCD(s) << 8) | CDUtil.toBCD(f);
    }

    public static String printMSF(int msf) {
        int m = (msf & 0xff0000) >> 16;
        int s = (msf & 0xff00) >> 8;
        int f = msf & 0xff;
        return MiscUtil.toHex(m, 2) + ":" + MiscUtil.toHex(s, 2) + ":" + MiscUtil.toHex(f, 2);
    }

    public static int toMSF(int m, int s, int f) {
        return (toBCD(m) << 16) | (toBCD(s) << 8) | toBCD(f);
    }

    public static int toSector(int m, int s, int f) {
        return (m * 60 + s) * 75 + f - 150;
    }
}
