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
import org.jpsx.runtime.RuntimeConnections;
import org.jpsx.runtime.components.core.R3000Impl;
import org.jpsx.runtime.ui.SystemProvider.SystemEvent;

import javax.swing.*;

public class SwingWindowPsx extends SwingWindow {

    private static final Logger LOG = Logger.getLogger(SwingWindowPsx.class.getSimpleName());

    public SwingWindowPsx() {
        super(null);
        mainEmu = createSystemProvider();
    }

    @Override
    protected void handleSystemEvent(SystemEvent event, Object par, String msg) {
        mainEmu.handleSystemEvent(event, par);
        showInfo(event + (Strings.isNullOrEmpty(msg) ? "" : ": " + msg));
    }

    //TODO
    @Override
    protected KeyStroke getAcceleratorKey(SystemEvent event) {
        return null;
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
                return null;
            }
        };
    }
}
