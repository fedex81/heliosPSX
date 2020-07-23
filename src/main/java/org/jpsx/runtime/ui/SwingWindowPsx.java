/*
 * SwingWindow
 * Copyright (c) 2018-2019 Federico Berti
 * Last modified: 17/10/19 11:55
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

import com.google.common.base.Strings;
import org.apache.log4j.Logger;
import org.jpsx.api.components.hardware.gpu.Display;
import org.jpsx.api.components.hardware.gpu.DisplayManager;
import org.jpsx.bootstrap.JPSXLauncher;
import org.jpsx.runtime.RuntimeConnections;
import org.jpsx.runtime.components.core.R3000Impl;
import org.jpsx.runtime.components.hardware.HardwareComponentConnections;
import org.jpsx.runtime.components.hardware.gpu.GPU;
import org.jpsx.runtime.ui.input.KeyBindingsHandler;
import org.jpsx.runtime.util.SystemProvider;
import org.jpsx.runtime.util.SystemProvider.SystemEvent;

import javax.swing.*;
import java.io.File;

public class SwingWindowPsx extends SwingWindow implements Display {

    private static final Logger LOG = Logger.getLogger(SwingWindowPsx.class.getSimpleName());
    private DisplayManager displayManager;
    private boolean hasRomFile;
    private String filePath;
    private String fileName;

    public SwingWindowPsx() {
        super(null);
        mainEmu = createSystemProvider();
    }

    @Override
    public void init() {
        super.init();
        initTitle();
        HardwareComponentConnections.DISPLAY.set(this);
        RuntimeConnections.KEY_LISTENERS.add(setupFrameKeyListener());
    }

    private void initTitle() {
        hasRomFile = Boolean.valueOf(getProperty("hasRomFile", "false"));
        filePath = getProperty("romFile", "");
        if (hasRomFile) {
            if (!Strings.isNullOrEmpty(filePath)) {
                fileName = new File(filePath).getName();
                setTitle(fileName);
            }
        }
    }

    @Override
    public void refresh() {
        boolean rgb24 = displayManager.getRGB24bit();
        GPU.setVRAMFormat(rgb24);
        int w = displayManager.getPixelWidth();
        int h = displayManager.getPixelHeight();
        int newX = displayManager.getXOrigin();
        int newY = displayManager.getYOrigin();
        updateDimension(false, w, h, newX, newY);
        refreshStrategy(displayManager.getBlanked());
    }

    @Override
    public void resolveConnections() {
        super.resolveConnections();
        displayManager = HardwareComponentConnections.DISPLAY_MANAGER.resolve();
    }

    protected void handleNewRom(File f) {
        super.handleNewRom(f);
        SwingUtilities.invokeLater(() -> JPSXLauncher.launch(f));
    }

    @Override
    public void initDisplay() {
        addKeyListener(RuntimeConnections.KEY_LISTENERS.resolve());
    }

    @Override
    public int[] acquireDisplayBuffer() {
        return renderData;
    }

    @Override
    public void releaseDisplayBuffer() {

    }

    @Override
    protected void handleSystemEvent(SystemEvent event, Object par, String msg) {
        mainEmu.handleSystemEvent(event, par);
        showInfo(event + (Strings.isNullOrEmpty(msg) ? "" : ": " + msg));
    }

    @Override
    protected KeyStroke getAcceleratorKey(SystemEvent event) {
        return KeyBindingsHandler.getInstance().getKeyStrokeForEvent(event);
    }

    private SystemProvider createSystemProvider() {
        return new SystemProvider() {
            @Override
            public void handleSystemEvent(SystemEvent event, Object parameter) {
                switch (event) {
                    case CLOSE_APP:
                        R3000Impl.shutdown();
                        RuntimeConnections.MACHINE.resolve().exit();
                        break;
                    case CLOSE_ROM:
                        R3000Impl.shutdown();
                        RuntimeConnections.MACHINE.resolve().close();
                        resetScreen();
                        reloadSystem(mainEmu);
                        break;
                    case NEW_ROM:
                        handleNewRomDialog();
                        break;
                }
            }

            @Override
            public boolean isRomRunning() {
                return true;
            }

            @Override
            public String getRomName() {
                return Strings.isNullOrEmpty(fileName) ? null : fileName;
            }
        };
    }
}
