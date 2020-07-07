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
package org.jpsx.api.components.hardware.cd;

import java.io.Closeable;

public interface CDMedia extends Closeable {

    int SECTOR_SIZE_BYTES = 2352;

    public enum TrackType {
        UNKNOWN, MODE2_2352, MODE1_2352, AUDIO;

        static TrackType[] values = TrackType.values();

        public static TrackType getTrackType(String name) {
            name = name.replace("/", "_"); //MODE2/2352
            for (int i = 0; i < values.length; i++) {
                TrackType tt = values[i];
                if (name.equalsIgnoreCase(tt.name())) {
                    return tt;
                }
            }
            return null;
        }
    }

    int getFirstTrack();

    int getLastTrack();

    int getTrackMSF(int track);

    TrackType getTrackType(int track);

    void readSector(int sectorNumber, byte[] buffer) throws MediaException;

    /**
     * @param num
     * @param buffer
     * @throws MediaException
     * @deprecated just for use of old cd stuff
     */
    void readSector(int num, int[] buffer) throws MediaException;
}
