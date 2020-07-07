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
package org.jpsx.runtime.components.hardware.media;

import org.apache.log4j.Logger;
import org.digitalmediaserver.cuelib.CueParser;
import org.digitalmediaserver.cuelib.CueSheet;
import org.digitalmediaserver.cuelib.FileData;
import org.digitalmediaserver.cuelib.TrackData;
import org.jpsx.api.components.hardware.cd.CDDrive;
import org.jpsx.api.components.hardware.cd.CDMedia;
import org.jpsx.api.components.hardware.cd.MediaException;
import org.jpsx.runtime.SingletonJPSXComponent;
import org.jpsx.runtime.components.hardware.HardwareComponentConnections;
import org.jpsx.runtime.util.CDUtil;
import org.jpsx.runtime.util.MiscUtil;

import java.io.*;
import java.util.List;

import static org.jpsx.runtime.util.CDUtil.toMSF;

public class CueBinImageDrive extends SingletonJPSXComponent implements CDDrive {
    public static final String PROPERTY_IMAGE_FILE = "image";

    private static final String CATEGORY = "CDImage";
    private static final Logger log = Logger.getLogger(CATEGORY);
    private static final String DEFAULT_CUE_FILE =
//            "rips/wipeoutxl.cue";
            "rips/granturismo.cue";
    //            "rips/microv3.cue";
    private static final int MAX_TRACKS = 100; //0-99
    private CDMedia currentMedia;

    private boolean refreshed = false;

    public CueBinImageDrive() {
        super("JPSX CUE/BIN Image CD Drive");
    }

    public void init() {
        super.init();
        HardwareComponentConnections.CD_DRIVE.set(this);
    }

    @Override
    public void close() throws IOException {
        MiscUtil.closeQuietly(currentMedia, true);
    }

    public CDMedia getCurrentMedia() {
        if (!refreshed) {
            refreshMedia();
            refreshed = true;
        }
        return currentMedia;
    }

    public boolean isDriveOpen() {
        return false;
    }

    public void refreshMedia() {
        String cueFilename = getProperty(PROPERTY_IMAGE_FILE, DEFAULT_CUE_FILE);
        currentMedia = CueBinImageMedia.create(cueFilename);
    }

    public void refreshMedia(String cueFile) {
        MiscUtil.closeQuietly(currentMedia, true);
        currentMedia = CueBinImageMedia.create(cueFile);
    }

    private static class CueBinImageMedia implements CDMedia {
        int first;
        int last;
        CueSheet cueSheet;
        int[] msfList = new int[MAX_TRACKS];
        TrackType[] trackTypeList = new TrackType[MAX_TRACKS];
        byte[] byteBuf = new byte[SECTOR_SIZE_BYTES];
        RandomAccessFile binFile;

        private CueBinImageMedia() {
        }

        public TrackType getTrackType(int track) {
            return trackTypeList[track];
        }


        public int getFirstTrack() {
            return first;
        }

        public int getLastTrack() {
            return last;
        }

        public int getTrackMSF(int trackIndex) {
            return msfList[trackIndex];
        }

        public static CueBinImageMedia create(String cueFilename) {
            CueBinImageMedia rc = new CueBinImageMedia();
            if (!rc.parse(cueFilename)) {
                return null;
            }
            return rc;
        }

        private boolean parse(String cueFilename) {
            File cueFile = new File(cueFilename);
            File dataFile;
            int offset = 150;
            try (
                    FileReader fr = new FileReader(cueFile);
                    LineNumberReader reader = new LineNumberReader(fr);
            ) {
                cueSheet = CueParser.parse(reader);
                dataFile = getFirstDataFile(cueFile, cueSheet);
                List<TrackData> l = cueSheet.getAllTrackData();
                last = l.size();
                first = -1;
                for (int i = 0; i < l.size(); i++) {
                    TrackData td = l.get(i);
                    TrackType tt = TrackType.getTrackType(td.getDataType());
                    if (tt != null) {
                        trackTypeList[td.getNumber()] = tt;
                        int sectorVal = td.getIndex(1).getPosition().getTotalFrames() + offset;
                        msfList[td.getNumber()] = toMSF(sectorVal);
                        first = first < 0 ? td.getNumber() : first;
                    } else {
                        log.warn("Unable to parse track: " + td);
                    }
                }
//                logTracks(cueFile, this);
                binFile = new RandomAccessFile(dataFile, "r");
                msfList[0] = toMSF(offset + (int) (binFile.length() / (long) SECTOR_SIZE_BYTES));
            } catch (IOException e) {
                log.warn("Unable to open BIN/CUE file " + cueFilename + ": " + e.getMessage());
                return false;
            }
            return true;
        }

        private File getFirstDataFile(File cueFile, CueSheet cueSheet) {
            List<FileData> l = cueSheet.getFileData();
            return new File(cueFile.getParent(), l.get(0).getFile());
        }

        public void readSector(int num, byte[] buffer) throws MediaException {
            try {
                // note findbugs complains about this, but we know that the value can't overflow
                binFile.seek(num * SECTOR_SIZE_BYTES);
                binFile.readFully(buffer, 0, SECTOR_SIZE_BYTES);
            } catch (IOException e) {
                throw new MediaException("readSector failed", e);
            }
        }

        public void readSector(int num, int[] buffer) throws MediaException {
            try {
                // note findbugs complains about this, but we know that the value can't overflow
                binFile.seek(num * SECTOR_SIZE_BYTES);
                binFile.readFully(byteBuf);
                for (int i = 0; i < SECTOR_SIZE_BYTES / 4; i++) {
                    buffer[i] = ((((int) byteBuf[i * 4 + 3]) & 0xff) << 24) |
                            ((((int) byteBuf[i * 4 + 2]) & 0xff) << 16) |
                            ((((int) byteBuf[i * 4 + 1]) & 0xff) << 8) |
                            ((((int) byteBuf[i * 4]) & 0xff));
                }
            } catch (IOException e) {
                throw new MediaException("readSector failed", e);
            }
        }

        private static void logTracks(File cueFile, CDMedia media) {
            for (int i = media.getFirstTrack(); i <= media.getLastTrack(); i++) {
                System.out.printf("Track %2d, %10s, MSF: %s\n", i, media.getTrackType(i),
                        CDUtil.printMSF(media.getTrackMSF(i)));
            }
            System.out.println("End MSF: " + CDUtil.printMSF(media.getTrackMSF(0)));
        }

        @Override
        public void close() throws IOException {
            MiscUtil.closeQuietly(binFile, true);
        }
    }
}
