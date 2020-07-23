/*
 * KeyBindingsHandler
 * Copyright (c) 2018-2019 Federico Berti
 * Last modified: 13/07/19 20:22
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

package org.jpsx.runtime.ui.input;

import org.apache.log4j.Logger;
import org.jpsx.runtime.components.hardware.sio.input.InputProvider.PlayerNumber;
import org.jpsx.runtime.components.hardware.sio.input.JoypadProvider.JoypadButton;
import org.jpsx.runtime.util.FileLoader;
import org.jpsx.runtime.util.SystemProvider.SystemEvent;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.stream.Collectors;

import static javax.swing.KeyStroke.getKeyStroke;

public class KeyBindingsHandler {

    public static final String configFile = String.valueOf(System.getProperty("key.config.file", "key.config"));
    protected static final String DIV = "=";
    protected static final String PLAYER_DIV = "\\.";
    protected static final String PLAYER_LINE_HEAD = "P.";
    private static final Logger LOG = Logger.getLogger(KeyBindingsHandler.class.getSimpleName());
    protected static InputMap keyMap;

    private static KeyBindingsHandler instance;

    public static KeyBindingsHandler getInstance() {
        if (instance == null) {
            instance = new KeyBindingsHandler();
            instance.init();
        }
        return instance;
    }

    private static void loadKeyMap() {
        LOG.info("Loading key config file: " + configFile);
        List<String> l = FileLoader.readFileContent(configFile);
        keyMap = parseConfig(l);
        final List<String> l1 = l;
        Arrays.stream(PlayerNumber.values()).forEach(p -> parsePlayerConfig(l1, p));
    }

    protected static InputMap parseConfig(List<String> str) {
        InputMap m = new InputMap();
        try {
            str.forEach(l -> {
                l = l.trim();
                boolean validLine = !l.isEmpty() && !l.startsWith("#") && !l.startsWith(PLAYER_LINE_HEAD);
                if (validLine) {
                    String[] s = l.split(DIV);
                    Optional<KeyStroke> ksOpt = Optional.ofNullable(getKeyStroke(s[1]));
                    if (ksOpt.isPresent()) {
                        m.put(ksOpt.get(), SystemEvent.valueOf(s[0]));
                    } else {
                        LOG.warn("Unable to parse keyStroke: " + s[1]);
                    }
                }
            });
        } catch (Exception e) {
            return KeyboardInputHelper.DEFAULT_INPUT_MAP;
        }
        return m.size() == 0 ? KeyboardInputHelper.DEFAULT_INPUT_MAP : m;
    }

    protected static void parsePlayerConfig(List<String> str, PlayerNumber number) {
        try {
            Map<String, Integer> playerKeyMap = new HashMap<>();
            str.forEach(l -> {
                l = l.trim();
                boolean validLine = !l.isEmpty() && !l.startsWith("#") && l.startsWith(PLAYER_LINE_HEAD);
                if (validLine) {
                    String[] s = l.split(PLAYER_DIV);
                    PlayerNumber pn = PlayerNumber.valueOf(s[0] + s[1]);
                    if (pn == number) {
                        String[] s1 = s[2].split(DIV);
                        Optional<JoypadButton> btn = getJoypadButton(s1[0]);
                        if (btn.isPresent()) {
                            Optional<KeyStroke> ks = Optional.ofNullable(getKeyStroke(s1[1]));
                            if (ks.isPresent()) {
                                playerKeyMap.put(btn.get().name(), ks.get().getKeyCode());
                            } else {
                                LOG.warn("Unable to parse line: " + l);
                            }
                        }
                    }
                }
            });
            if (!playerKeyMap.isEmpty()) {
                KeyboardInputHelper.updatePlayerMappings(number, playerKeyMap);
            }
        } catch (Exception e) {
            LOG.warn("Unable to parse key config for player: " + number);
        }
    }

    private static Optional<JoypadButton> getJoypadButton(String configToken) {
        JoypadButton btn = null;
        for (JoypadButton b : JoypadButton.values()) {
            if (b.getMnemonic().equalsIgnoreCase(configToken)) {
                btn = b;
            }
        }
        return Optional.ofNullable(btn);
    }

    public static String toConfigString() {
        return toConfigList().stream().collect(Collectors.joining("\n"));
    }

    private static List<String> toConfigList() {
        List<String> l = new ArrayList<>();
        for (KeyStroke ks : keyMap.allKeys()) {
            l.add(keyMap.get(ks).toString() + DIV + ks.toString());
        }
        Collections.sort(l);
        KeyboardInputHelper.keyboardBindings.cellSet().stream().forEach(cell -> {
            String tk = PLAYER_LINE_HEAD + cell.getRowKey().name().substring(1) + ".";
            tk += cell.getColumnKey().getMnemonic() + DIV;
            tk += KeyEvent.getKeyText(cell.getValue()).toUpperCase();
            l.add(tk);
        });
        return l;
    }

    private void init() {
        loadKeyMap();
    }

    public SystemEvent getSystemEventIfAny(KeyStroke keyStroke) {
        return (SystemEvent) Optional.ofNullable(keyMap.get(keyStroke)).orElse(SystemEvent.NONE);
    }

    public KeyStroke getKeyStrokeForEvent(SystemEvent event) {
        return Arrays.stream(keyMap.allKeys()).filter(ks -> event == getSystemEventIfAny(ks)).
                findFirst().orElse(null);
    }
}
