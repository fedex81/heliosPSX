/*
 * JinputGamepadMapping
 * Copyright (c) 2018-2019 Federico Berti
 * Last modified: 13/10/19 15:20
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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.java.games.input.Component;
import net.java.games.input.Component.Identifier.Button;
import org.apache.log4j.Logger;
import org.jpsx.runtime.components.hardware.sio.input.JoypadProvider.JoypadButton;
import org.jpsx.runtime.components.hardware.sio.input.JoypadProvider.JoypadDirection;

import static net.java.games.input.Component.Identifier.Button.Axis;

public class JinputGamepadMapping {

    public static final String SONY_PSX_CLASSIC_PAD_NAME = "Sony Interactive Entertainment Controller";
    public static final String XBOX360_COMPAT_PAD_NAME = "Microsoft X-Box 360 pad";
    public static final String GAMESIR_G3S_PAD_NAME = "xiaoji Gamesir-G3s 1.02";

    public static Table<String, Component.Identifier, Object> deviceMappings = HashBasedTable.create();
    private static Logger LOG = Logger.getLogger(JinputGamepadMapping.class);

    static {
        deviceMappings.put(SONY_PSX_CLASSIC_PAD_NAME, Axis.Y, JoypadDirection.UP_DOWN);
        deviceMappings.put(SONY_PSX_CLASSIC_PAD_NAME, Axis.X, JoypadDirection.LEFT_RIGHT);

        deviceMappings.put(SONY_PSX_CLASSIC_PAD_NAME, Button.X, JoypadButton.SQUARE);
        deviceMappings.put(SONY_PSX_CLASSIC_PAD_NAME, Button.C, JoypadButton.CROSS);
        deviceMappings.put(SONY_PSX_CLASSIC_PAD_NAME, Button.B, JoypadButton.CIRCLE);
        deviceMappings.put(SONY_PSX_CLASSIC_PAD_NAME, Button.A, JoypadButton.TRIANGLE);
        deviceMappings.put(SONY_PSX_CLASSIC_PAD_NAME, Button.LEFT_THUMB, JoypadButton.L1);
        deviceMappings.put(SONY_PSX_CLASSIC_PAD_NAME, Button.RIGHT_THUMB, JoypadButton.R1);
        deviceMappings.put(SONY_PSX_CLASSIC_PAD_NAME, Button.Y, JoypadButton.L2);
        deviceMappings.put(SONY_PSX_CLASSIC_PAD_NAME, Button.Z, JoypadButton.R2);
        deviceMappings.put(SONY_PSX_CLASSIC_PAD_NAME, Button.RIGHT_THUMB2, JoypadButton.S);
        deviceMappings.put(SONY_PSX_CLASSIC_PAD_NAME, Button.LEFT_THUMB2, JoypadButton.SELECT);

        //TODO
        deviceMappings.put(XBOX360_COMPAT_PAD_NAME, Axis.Y, JoypadDirection.UP_DOWN);
        deviceMappings.put(XBOX360_COMPAT_PAD_NAME, Axis.X, JoypadDirection.LEFT_RIGHT);

        deviceMappings.put(XBOX360_COMPAT_PAD_NAME, Button.A, JoypadButton.SQUARE);
        deviceMappings.put(XBOX360_COMPAT_PAD_NAME, Button.B, JoypadButton.CROSS);
        deviceMappings.put(XBOX360_COMPAT_PAD_NAME, Button.LEFT_THUMB, JoypadButton.L1);
        deviceMappings.put(XBOX360_COMPAT_PAD_NAME, Button.X, JoypadButton.CIRCLE);
        deviceMappings.put(XBOX360_COMPAT_PAD_NAME, Button.Y, JoypadButton.TRIANGLE);
        deviceMappings.put(XBOX360_COMPAT_PAD_NAME, Button.RIGHT_THUMB, JoypadButton.R1);

        deviceMappings.put(XBOX360_COMPAT_PAD_NAME, Button.START, JoypadButton.S);
        deviceMappings.put(XBOX360_COMPAT_PAD_NAME, Button.SELECT, JoypadButton.SELECT);

        deviceMappings.put(GAMESIR_G3S_PAD_NAME, Axis.POV, JoypadDirection.UP_DOWN);
        deviceMappings.put(GAMESIR_G3S_PAD_NAME, Axis.POV, JoypadDirection.LEFT_RIGHT);

        deviceMappings.put(GAMESIR_G3S_PAD_NAME, Button.X, JoypadButton.SQUARE);
        deviceMappings.put(GAMESIR_G3S_PAD_NAME, Button.A, JoypadButton.CROSS);
        deviceMappings.put(GAMESIR_G3S_PAD_NAME, Button.B, JoypadButton.CIRCLE);
        deviceMappings.put(GAMESIR_G3S_PAD_NAME, Button.LEFT_THUMB, JoypadButton.L1);
        deviceMappings.put(GAMESIR_G3S_PAD_NAME, Button.RIGHT_THUMB, JoypadButton.R1);
        deviceMappings.put(GAMESIR_G3S_PAD_NAME, Button.Y, JoypadButton.TRIANGLE);

        deviceMappings.put(GAMESIR_G3S_PAD_NAME, Button.START, JoypadButton.S);
        deviceMappings.put(GAMESIR_G3S_PAD_NAME, Button.SELECT, JoypadButton.SELECT);
    }
}
