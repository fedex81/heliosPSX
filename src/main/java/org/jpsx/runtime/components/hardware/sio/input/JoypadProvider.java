/*
 * JoypadProvider
 * Copyright (c) 2018-2019 Federico Berti
 * Last modified: 13/10/19 16:20
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


import static org.jpsx.runtime.components.hardware.sio.input.JoypadProvider.JoypadButton.*;

public interface JoypadProvider {

    JoypadButton[] directionButton = {D, L, R, U};

    enum JoypadAction {
        PRESSED,
        RELEASED
    }

    enum JoypadType {
        BUTTON_2,
        BUTTON_3,
        BUTTON_6
    }

    enum JoypadButton {
        U("UP"), D("DOWN"), L("LEFT"), R("RIGHT"), S("START"),
        SQUARE, CIRCLE, TRIANGLE, CROSS, L1, L2, R1, R2, SELECT;

        String mnemonic;

        JoypadButton() {
            this.mnemonic = name();
        }

        JoypadButton(String s) {
            this.mnemonic = s;
        }

        public String getMnemonic() {
            return mnemonic;
        }
    }

    enum JoypadDirection {
        UP_DOWN(U, D),
        LEFT_RIGHT(L, R);

        JoypadButton b1, b2;

        JoypadDirection(JoypadButton b1, JoypadButton b2) {
            this.b1 = b1;
            this.b2 = b2;
        }

        public JoypadButton getB1() {
            return b1;
        }

        public JoypadButton getB2() {
            return b2;
        }
    }
}
