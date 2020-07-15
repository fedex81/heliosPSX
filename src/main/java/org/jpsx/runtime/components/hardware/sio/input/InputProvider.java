/*
 * InputProvider
 * Copyright (c) 2018-2019 Federico Berti
 * Last modified: 14/10/19 15:26
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

package org.jpsx.runtime.components.hardware.sio.input;

import com.google.common.collect.ImmutableList;
import org.apache.log4j.Logger;
import org.jpsx.runtime.util.MiscUtil;

import java.io.File;
import java.util.List;

import static org.jpsx.runtime.util.MiscUtil.NATIVE_LIB_PATH;

public interface InputProvider {

    Logger LOG = Logger.getLogger(InputProvider.class);

    String KEYBOARD_CONTROLLER = "Default (Keyboard)";
    String NO_CONTROLLER = "Disable";

    List<String> DEFAULT_CONTROLLERS = ImmutableList.of(NO_CONTROLLER, KEYBOARD_CONTROLLER);

    boolean DEBUG_DETECTION = Boolean.valueOf(System.getProperty("jinput.detect.debug", "false"));
    boolean JINPUT_ENABLE = Boolean.valueOf(System.getProperty("jinput.enable", "false"));

    enum PlayerNumber {
        P1, P2
    }

    InputProvider NO_OP = new InputProvider() {
        @Override
        public void handleEvents() {

        }

        @Override
        public void setPlayerController(PlayerNumber player, String controllerName) {

        }

        @Override
        public void reset() {

        }

        @Override
        public List<String> getAvailableControllers() {
            return DEFAULT_CONTROLLERS;
        }
    };

    float ON = 1.0f;

    static void bootstrap() {
        String lib = new File(".").getAbsolutePath() + File.separator + NATIVE_LIB_PATH
                + File.separator + MiscUtil.NATIVE_SUBDIR;
//        System.out.println(lib);
        System.setProperty("net.java.games.input.librarypath", lib);
        LOG.info("Loading system library from: " + lib);
        //disable java.util.logging
        java.util.logging.LogManager.getLogManager().reset();
        LOG.info("Disabling java.util.logging");
    }


    void handleEvents();

    void setPlayerController(PlayerNumber player, String controllerName);

    void reset();

    List<String> getAvailableControllers();
}
