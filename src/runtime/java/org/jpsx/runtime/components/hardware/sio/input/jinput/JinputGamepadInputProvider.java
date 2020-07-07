/*
 * JinputGamepadInputProvider
 * Copyright (c) 2018-2019 Federico Berti
 * Last modified: 14/10/19 15:19
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.java.games.input.*;
import org.apache.log4j.Logger;
import org.jpsx.runtime.components.hardware.HardwareComponentConnections;
import org.jpsx.runtime.components.hardware.sio.StandardController;
import org.jpsx.runtime.components.hardware.sio.input.InputProvider;
import org.jpsx.runtime.components.hardware.sio.input.JoypadProvider;
import org.jpsx.runtime.components.hardware.sio.input.JoypadProvider.JoypadAction;
import org.jpsx.runtime.components.hardware.sio.input.JoypadProvider.JoypadButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;
import static net.java.games.input.Component.Identifier.Button.Axis;

public class JinputGamepadInputProvider extends StandardController implements InputProvider {

    private static Logger LOG = Logger.getLogger(JinputGamepadInputProvider.class);

    private static ExecutorService executorService = Executors.newSingleThreadExecutor();
    private long POLLING_INTERVAL_MS = Long.valueOf(System.getProperty("jinput.polling.interval.ms", "5"));

    private volatile JoypadProvider joypadProvider;
    private volatile boolean stop = false;
    private String pov = Axis.POV.getName();

    private static InputProvider INSTANCE = NO_OP;
    private static final int AXIS_p1 = 1;
    private static final int AXIS_0 = 0;
    private static final int AXIS_m1 = -1;

    private List<String> controllerNames;
    private List<Controller> controllers;

    private Map<PlayerNumber, String> playerControllerMap = Maps.newHashMap(
            ImmutableMap.of(PlayerNumber.P1, KEYBOARD_CONTROLLER,
                    PlayerNumber.P2, KEYBOARD_CONTROLLER
            ));

    public JinputGamepadInputProvider() {
        super("JPSX JInput Pad Controller");
        controllerNames = new ArrayList<>(DEFAULT_CONTROLLERS);
        controllers = new ArrayList<>();
        initProvider();
    }

    private void initProvider() {
        InputProvider.bootstrap();
        List<Controller> list = detectControllers();
        InputProvider provider = NO_OP;
        if (!list.isEmpty()) {
            provider = createOrGetInstance(this, list);
        } else {
            LOG.info("Unable to find a controller");
        }
    }

    private static InputProvider createOrGetInstance(JinputGamepadInputProvider g, List<Controller> controllers) {
        if (INSTANCE == NO_OP) {
            g.controllerNames.addAll(controllers.stream().map(Controller::getName).collect(Collectors.toList()));
            g.controllers = controllers;
            executorService.submit(g.inputRunnable());
            INSTANCE = g;
        }
        return INSTANCE;
    }

    @Override
    public void init() {
        super.init();
    }

    public void resolveConnections() {
        // for now just connect to the left serial port
        HardwareComponentConnections.LEFT_PORT_INSTANCE.resolve().connect(this);
    }

    static List<Controller> detectControllers() {
        Controller[] ca = ControllerEnvironment.getDefaultEnvironment().getControllers();
        List<Controller> l = Arrays.stream(ca).filter(c -> c.getType() == Controller.Type.GAMEPAD).collect(Collectors.toList());
        if (DEBUG_DETECTION || l.isEmpty()) {
            LOG.info("Controller detection: " + detectControllerVerbose());
        }
        return l;
    }

    private Runnable inputRunnable() {
        return () -> {
            LOG.info("Starting controller polling, interval (ms): " + POLLING_INTERVAL_MS);
            do {
                handleEvents();
                try {
                    sleep(POLLING_INTERVAL_MS);
                } catch (InterruptedException e) {
                }
            } while (!stop);
            LOG.info("Controller polling stopped");
        };
    }

    private boolean resetDirections = false;

    @Override
    public void handleEvents() {
        for (Controller controller : controllers) {
            String ctrlName = controller.getName();
            boolean ok = controller.poll();
            if (!ok) {
                return;
            }
            int count = 0;
            for (Map.Entry<PlayerNumber, String> entry : playerControllerMap.entrySet()) {
                PlayerNumber player = entry.getKey();
                if (player == PlayerNumber.P2) { //hack
                    if (!ctrlName.equalsIgnoreCase(entry.getValue())) {
                        continue;
                    }
                }
                resetDirections = true; //joypadProvider.hasDirectionPressed(player);
                EventQueue eventQueue = controller.getEventQueue();

                boolean hasEvents;
                do {
                    Event event = new Event();
                    hasEvents = eventQueue.getNextEvent(event);
                    if (player != null && hasEvents) {
                        handleEvent(player, ctrlName, event);
                        count++;
                    }
                } while (hasEvents);
            }
        }
    }

    @Override
    public void setPlayerController(PlayerNumber player, String controllerName) {
        playerControllerMap.put(player, controllerName);
    }

    @Override
    public void reset() {
//        stop = true;
    }

    @Override
    public List<String> getAvailableControllers() {
        return controllerNames;
    }

    private void setDirectionOff(PlayerNumber playerNumber) {
//        released(PADRdown);
//        released(PADRup);
//        released(PADRleft);
//        released(PADRright);
    }

    /**
     * up Controller state now: efff
     * down Controller state now: bfff
     * l Controller state now: 7fff
     * r Controller state now: dfff
     * triangle Controller state now: ffef
     * circle  Controller state now: ffdf
     * cross Controller state now: ffbf
     * square Controller state now: ff7f
     * l1 Controller state now: fffb
     * l2 Controller state now: fffe
     * r1 Controller state now: fff7
     * r2 Controller state now: fffd
     * start Controller state now: f7ff
     * select
     * <p>
     * public static final int PADRup = (1 << 4);    //triangle
     * public static final int PADRdown = (1 << 6);  //cross
     * public static final int PADRleft = (1 << 7);  //square
     * public static final int PADRright = (1 << 5); //circle
     */
    static Map<JoypadButton, Integer> pad1Mapping = ImmutableMap.<JoypadButton, Integer>builder()
            .put(JoypadButton.U, PADLup)
            .put(JoypadButton.D, PADLdown)
            .put(JoypadButton.L, PADLleft)
            .put(JoypadButton.R, PADLright)
            .put(JoypadButton.TRIANGLE, PADRup)    //triangle
            .put(JoypadButton.CIRCLE, PADRright) //circle
            .put(JoypadButton.CROSS, PADRdown)  //cross
            .put(JoypadButton.SQUARE, PADRleft)  //square
            .put(JoypadButton.S, PADh)
            .put(JoypadButton.SELECT, PADselect)
            .build();

    private void setButtonAction(InputProvider.PlayerNumber number, JoypadButton button, JoypadAction action) {
        if (action == JoypadAction.PRESSED) {
            pressed(pad1Mapping.getOrDefault(button, 0));
        } else {
            released(pad1Mapping.getOrDefault(button, 0));
        }
    }

    private void handleEvent(PlayerNumber playerNumber, String ctrlName, Event event) {
        Component.Identifier id = event.getComponent().getIdentifier();
        double value = event.getValue();
        JoypadAction action = value == ON ? JoypadAction.PRESSED : JoypadAction.RELEASED;
        if (InputProvider.DEBUG_DETECTION) {
            LOG.info(id + ": " + value);
            System.out.println(id + ": " + value);
        }
        Object res = JinputGamepadMapping.deviceMappings.row(ctrlName).getOrDefault(id, null);
        if (res != null && res instanceof JoypadButton) {
            setButtonAction(playerNumber, (JoypadButton) res, action);
        } else if (res != null && res instanceof JoypadProvider.JoypadDirection) {
            handleDPad(playerNumber, id, value);
        } else {
            LOG.debug("Unhandled event: " + event);
        }
    }

    private void handleDPad(PlayerNumber playerNumber, Component.Identifier id, double value) {
        if (Axis.X == id) {
            int ival = (int) value;
            switch (ival) {
                case AXIS_0:
                    setButtonAction(playerNumber, JoypadButton.R, JoypadAction.RELEASED);
                    setButtonAction(playerNumber, JoypadButton.L, JoypadAction.RELEASED);
                    break;
                case AXIS_m1:
                    setButtonAction(playerNumber, JoypadButton.L, JoypadAction.PRESSED);
                    break;
                case AXIS_p1:
                    setButtonAction(playerNumber, JoypadButton.R, JoypadAction.PRESSED);
                    break;
            }
        }
        if (Axis.Y == id) {
            int ival = (int) value;
            switch (ival) {
                case AXIS_0:
                    setButtonAction(playerNumber, JoypadButton.U, JoypadAction.RELEASED);
                    setButtonAction(playerNumber, JoypadButton.D, JoypadAction.RELEASED);
                    break;
                case AXIS_m1:
                    setButtonAction(playerNumber, JoypadButton.U, JoypadAction.PRESSED);
                    break;
                case AXIS_p1:
                    setButtonAction(playerNumber, JoypadButton.D, JoypadAction.PRESSED);
                    break;
            }
        }

        if (pov.equals(id.getName())) {
            JoypadAction action = JoypadAction.PRESSED;
            //release directions previously pressed - only on the first event
            boolean off = resetDirections || value == Component.POV.OFF;
            if (off) {
                setDirectionOff(playerNumber);
                if (resetDirections) {
                    resetDirections = false;
                }
            }
            if (value == Component.POV.DOWN) {
                setButtonAction(playerNumber, JoypadButton.D, action);
            }
            if (value == Component.POV.UP) {
                setButtonAction(playerNumber, JoypadButton.U, action);
            }
            if (value == Component.POV.LEFT) {
                setButtonAction(playerNumber, JoypadButton.L, action);
            }
            if (value == Component.POV.RIGHT) {
                setButtonAction(playerNumber, JoypadButton.R, action);
            }
            if (value == Component.POV.DOWN_LEFT) {
                setButtonAction(playerNumber, JoypadButton.D, action);
                setButtonAction(playerNumber, JoypadButton.L, action);
            }
            if (value == Component.POV.DOWN_RIGHT) {
                setButtonAction(playerNumber, JoypadButton.D, action);
                setButtonAction(playerNumber, JoypadButton.R, action);
            }
            if (value == Component.POV.UP_LEFT) {
                setButtonAction(playerNumber, JoypadButton.U, action);
                setButtonAction(playerNumber, JoypadButton.L, action);
            }
            if (value == Component.POV.UP_RIGHT) {
                setButtonAction(playerNumber, JoypadButton.U, action);
                setButtonAction(playerNumber, JoypadButton.R, action);
            }
        }
    }

    private static String detectControllerVerbose() {
        Controller[] ca = ControllerEnvironment.getDefaultEnvironment().getControllers();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < ca.length; i++) {
            /* Get the name of the controller */
            sb.append("\n" + ca[i].getName() + "\n");
            sb.append("Position: [" + i + "]\n");
            sb.append("Type: " + ca[i].getType().toString() + "\n");

            /* Get this controllers components (buttons and axis) */
            Component[] components = ca[i].getComponents();
            sb.append("Component Count: " + components.length + "\n");
            for (int j = 0; j < components.length; j++) {

                /* Get the components name */
                sb.append("Component " + j + ": " + components[j].getName() + "\n");
                sb.append("    Identifier: " + components[j].getIdentifier().getName() + "\n");
                sb.append("    ComponentType: ");
                if (components[j].isRelative()) {
                    sb.append("Relative");
                } else {
                    sb.append("Absolute");
                }
                if (components[j].isAnalog()) {
                    sb.append(" Analog");
                } else {
                    sb.append(" Digital");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
