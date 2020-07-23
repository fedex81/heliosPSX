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
package org.jpsx.runtime.components.hardware.sio;

import org.jpsx.bootstrap.util.CollectionsFactory;
import org.jpsx.runtime.RuntimeConnections;
import org.jpsx.runtime.components.hardware.HardwareComponentConnections;
import org.jpsx.runtime.components.hardware.sio.input.InputProvider;
import org.jpsx.runtime.components.hardware.sio.input.JoypadProvider.JoypadButton;
import org.jpsx.runtime.ui.input.KeyboardInputHelper;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Map;

import static org.jpsx.runtime.components.hardware.sio.input.JoypadProvider.JoypadButton.*;
import static org.jpsx.runtime.ui.input.KeyboardInputHelper.keyboardBindings;

// todo allow different mappings
public class AWTKeyboardController extends StandardController implements KeyListener {
    protected KeyMapping mapping;

    protected static final KeyMapping DEF_CONTROLLER_0_MAPPING;

    static {
        DEF_CONTROLLER_0_MAPPING = new KeyMapping();
        KeyboardInputHelper.init();
        Map<JoypadButton, Integer> p1Map = keyboardBindings.row(InputProvider.PlayerNumber.P1);
        DEF_CONTROLLER_0_MAPPING.put(PADstart, p1Map.get(S));
        DEF_CONTROLLER_0_MAPPING.put(PADselect, p1Map.get(SELECT));
        DEF_CONTROLLER_0_MAPPING.put(PADLup, p1Map.get(U));
        DEF_CONTROLLER_0_MAPPING.put(PADLleft, p1Map.get(L));
        DEF_CONTROLLER_0_MAPPING.put(PADLright, p1Map.get(R));
        DEF_CONTROLLER_0_MAPPING.put(PADLdown, p1Map.get(D));
        DEF_CONTROLLER_0_MAPPING.put(PADRup, p1Map.get(TRIANGLE));
        DEF_CONTROLLER_0_MAPPING.put(PADRdown, p1Map.get(CROSS));
        DEF_CONTROLLER_0_MAPPING.put(PADRleft, p1Map.get(SQUARE));
        DEF_CONTROLLER_0_MAPPING.put(PADRright, p1Map.get(CIRCLE));
        DEF_CONTROLLER_0_MAPPING.put(PADRdown, KeyEvent.VK_KP_DOWN); //TODO
        DEF_CONTROLLER_0_MAPPING.put(PADRleft, KeyEvent.VK_U); //TODO
        DEF_CONTROLLER_0_MAPPING.put(PADL1, p1Map.get(L1));
        DEF_CONTROLLER_0_MAPPING.put(PADL2, p1Map.get(R2));
        DEF_CONTROLLER_0_MAPPING.put(PADR1, p1Map.get(R1));
        DEF_CONTROLLER_0_MAPPING.put(PADR2, p1Map.get(R2));
    }

    public static class KeyMapping {
        private Map<Integer, Integer> map = CollectionsFactory.newHashMap();

        public int get(int vkey) {
            return map.getOrDefault(vkey, 0);
        }

        public void put(int mask, int vkey) {
            map.put(vkey, mask);
        }
    }

    public AWTKeyboardController() {
        this("JPSX AWT Keyboard Controller", DEF_CONTROLLER_0_MAPPING);
    }

    public AWTKeyboardController(String name) {
        this(name, DEF_CONTROLLER_0_MAPPING);
    }

    public AWTKeyboardController(String name, KeyMapping mapping) {
        super(name);
        this.mapping = mapping;
    }

    @Override
    public void init() {
        super.init();
        RuntimeConnections.KEY_LISTENERS.add(this);
    }

    public void resolveConnections() {
        // for now just connect to the left serial port
        HardwareComponentConnections.LEFT_PORT_INSTANCE.resolve().connect(this);
    }

    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
        int vkey = e.getKeyCode();
        pressed(mapping.get(vkey));
    }

    public void keyReleased(KeyEvent e) {
        int vkey = e.getKeyCode();
        released(mapping.get(vkey));
    }
}
