/*
 * GamepadTest
 * Copyright (c) 2018-2019 Federico Berti
 * Last modified: 13/10/19 17:33
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

package org.jpsx.runtime.components.hardware.sio.input.jinput;

import net.java.games.input.Controller;
import org.jpsx.runtime.components.hardware.sio.input.InputProvider;

import java.io.File;
import java.util.List;

public class GamepadTest {

    private static Controller controller;
    public static float ON = 1.0f;

    public static void main(String[] args) {
        String lib = new File(".").getAbsolutePath() + File.separator + "native"
                + File.separator + "linux";
        System.out.println(lib);
        System.setProperty("log4j.configurationFile", "./res/log4j2-info.properties");
        System.setProperty("jinput.enable", "true");
        System.setProperty("jinput.native.location", "native");
        System.setProperty("java.library.path", "native");
        System.setProperty("jinput.detect.debug", "true");
        System.setProperty("net.java.games.input.librarypath", lib);
        InputProvider inputProvider = new JinputGamepadInputProvider();
        List<String> l = inputProvider.getAvailableControllers();
        if (l.size() > 1) {
            inputProvider.setPlayerController(InputProvider.PlayerNumber.P1, inputProvider.getAvailableControllers().get(2));
            System.out.println("Waiting for a button press ...");
            pollInputs(inputProvider);
        }
    }

    private static void pollInputs(InputProvider inputProvider) {
        while (true) {
            inputProvider.handleEvents();
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
