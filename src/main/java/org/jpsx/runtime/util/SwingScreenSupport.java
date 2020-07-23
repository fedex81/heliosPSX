package org.jpsx.runtime.util;

import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SwingScreenSupport
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2020
 */
public class SwingScreenSupport {

    public static final int DEFAULT_SCREEN = 0;
    public static final double FOUR_BY_THREE = 4.0 / 3.0;

    public final static int DEFAULT_X = 640;
    public final static int DEFAULT_Y = 480;

    public static final int DEFAULT_SCALE_FACTOR =
            Integer.valueOf(System.getProperty("helios.ui.scale", "1"));
    public static final double FULL_SCREEN_WITH_TITLE_BAR_FACTOR =
            Double.valueOf(System.getProperty("helios.ui.fsTitle.factor", "1"));

    public static final boolean FIX_ASPECT_RATIO =
            Boolean.valueOf(System.getProperty("ui.fix.aspect.ratio", "true"));

    public static Dimension DEFAULT_SCALED_SCREEN_SIZE = new Dimension(DEFAULT_X * DEFAULT_SCALE_FACTOR,
            DEFAULT_Y * DEFAULT_SCALE_FACTOR);
    public static Dimension DEFAULT_BASE_SCREEN_SIZE = new Dimension(DEFAULT_X,
            DEFAULT_Y);
    public static Dimension DEFAULT_FRAME_SIZE = new Dimension((int) (DEFAULT_SCALED_SCREEN_SIZE.width * 1.02),
            (int) (DEFAULT_SCALED_SCREEN_SIZE.height * 1.10));

    private static final Logger LOG = Logger.getLogger(SwingScreenSupport.class.getSimpleName());

    private static GraphicsDevice[] graphicsDevices =
            GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();

    private static int currentScreen = graphicsDevices.length > 1 ? DEFAULT_SCREEN : 0;

    public static GraphicsDevice setupScreens() {
        LOG.info("Screen detected: #" + graphicsDevices.length);
        GraphicsDevice gd = graphicsDevices[currentScreen];
        LOG.info("Initial screen: " + gd.getIDstring());
        return gd;
    }

    public static GraphicsDevice getGraphicsDevice() {
        return graphicsDevices[currentScreen];
    }

    public static List<String> detectScreens() {
        return Arrays.stream(graphicsDevices).map(GraphicsDevice::toString).collect(Collectors.toList());
    }

    public static int getCurrentScreen() {
        return currentScreen;
    }

    public static void showOnScreen(int screen, JFrame frame) {
        GraphicsDevice[] gd = graphicsDevices;
        int width = 0, height = 0;
        if (screen > -1 && screen < gd.length) {
            Rectangle bounds = gd[screen].getDefaultConfiguration().getBounds();
            width = bounds.width;
            height = bounds.height;
            frame.setLocation(
                    ((width / 2) - (frame.getSize().width / 2)) + bounds.x,
                    ((height / 2) - (frame.getSize().height / 2)) + bounds.y
            );
            frame.setVisible(true);
            LOG.info("Showing on screen: " + screen);
            currentScreen = screen;
        } else {
            LOG.error("Unable to set screen: " + screen);
        }
    }

    public static Dimension getScreenSize(Dimension src, double multiplier, boolean mantainAspectRatio) {
        Dimension dim = src;
        if (mantainAspectRatio || multiplier != 1.0) {
            double w = src.width * multiplier;
            double h = w / FOUR_BY_THREE;
            dim = new Dimension((int) w, (int) h);
        }
        return dim;
    }

    public static double getFullScreenScaleFactor(Dimension fullScreenSize, Dimension nativeScreenSize) {
        double scaleW = fullScreenSize.getWidth() / nativeScreenSize.getWidth();
        double baseH = nativeScreenSize.getHeight();
        baseH = FIX_ASPECT_RATIO ? nativeScreenSize.getWidth() / FOUR_BY_THREE : baseH;
        double scaleH = fullScreenSize.getHeight() * FULL_SCREEN_WITH_TITLE_BAR_FACTOR / baseH;
        double scale = Math.min(scaleW, scaleH);
        return scale;
    }
}
