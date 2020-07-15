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
import org.jpsx.runtime.SingletonJPSXComponent;
import org.jpsx.runtime.components.hardware.HardwareComponentConnections;
import org.jpsx.runtime.components.hardware.gpu.GPU;
import org.jpsx.runtime.components.hardware.sio.input.InputProvider;
import org.jpsx.runtime.components.hardware.sio.input.InputProvider.PlayerNumber;
import org.jpsx.runtime.ui.SystemProvider.SystemEvent;
import org.jpsx.runtime.util.FileLoader;
import org.jpsx.runtime.util.MiscUtil;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.stream.IntStream;

import static org.jpsx.runtime.ui.ScreenSizeHelper.*;
import static org.jpsx.runtime.ui.SystemProvider.SystemEvent.*;
import static org.jpsx.runtime.util.FileLoader.QUICK_SAVE_PATH;

public abstract class SwingWindow extends SingletonJPSXComponent implements DisplayWindow, Display {

    private static final Logger LOG = Logger.getLogger(SwingWindow.class.getSimpleName());

    private Dimension fullScreenSize;
    //when scaling is slow set this to FALSE
    private static final boolean UI_SCALE_ON_EDT
            = Boolean.valueOf(System.getProperty("ui.scale.on.edt", "true"));
    private Dimension outputNonScaledScreenSize = DEFAULT_SCALED_SCREEN_SIZE;
    private Dimension outputScreenSize = DEFAULT_SCALED_SCREEN_SIZE;

    private BufferedImage baseImage;
    private Image destImage;
    private double scale = DEFAULT_SCALE_FACTOR;

    private final JLabel perfLabel = new JLabel("");
    private Canvas screenCanvas;
    private BufferStrategy strategy;
    private Graphics2D graphics;

    private JFrame jFrame;
    protected SystemProvider mainEmu;

    private List<JCheckBoxMenuItem> regionItems;
    private JCheckBoxMenuItem fullScreenItem;
    private JCheckBoxMenuItem muteItem;
    private JMenu recentFilesMenu;
    private JMenuItem[] recentFilesItems;
    private Map<PlayerNumber, JMenu> inputMenusMap;
    private boolean showDebug = false;
    private Dimension nativeScreenSize = DEFAULT_BASE_SCREEN_SIZE;
    private Map<SystemEvent, AbstractAction> actionMap = new HashMap<>();

    // Transparent 16 x 16 pixel cursor image.
    private BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

    // Create a new blank cursor.
    private Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
            cursorImg, new Point(0, 0), "blank cursor");
    private int showInfoCount = SHOW_INFO_FRAMES_DELAY;
    private Optional<String> actionInfo = Optional.empty();
    private DisplayManager displayManager;
    private boolean hasRomFile;

    private int[] renderData;
    private Dimension baseD;

    public SwingWindow() {
        this(null);
    }

    public SwingWindow(SystemProvider mainEmu) {
        super("SwingWindow");
        this.mainEmu = mainEmu;
        this.inputMenusMap = new LinkedHashMap<>();
        Arrays.stream(PlayerNumber.values()).
                forEach(pn -> inputMenusMap.put(pn, new JMenu(pn.name())));
    }

    protected abstract void handleSystemEvent(SystemEvent event, Object par, String msg);

    protected abstract KeyStroke getAcceleratorKey(SystemEvent event);

    @Override
    public void init() {
        super.init();
        HardwareComponentConnections.DISPLAY.set(this);
        hasRomFile = Boolean.valueOf(getProperty("hasRomFile", "false"));
        initSwing();
    }

    @Override
    public void resolveConnections() {
        super.resolveConnections();
        displayManager = HardwareComponentConnections.DISPLAY_MANAGER.resolve();
        if (!hasRomFile) {
            RuntimeConnections.MAIN.set(MiscUtil.sleeper());
        }
    }

    public void setTitle(String title) {
        jFrame.setTitle(APP_NAME + mainEmu.getSystemType().getShortName() + " " + VERSION + " - " + title);
        reloadRecentFiles();
    }


    private void addKeyAction(JMenuItem component, SystemEvent event, ActionListener l) {
        AbstractAction action = toAbstractAction(component.getText(), l);
        if (event != NONE) {
            action.putValue(Action.ACCELERATOR_KEY, getAcceleratorKey(event));
            actionMap.put(event, action);
        }
        component.setAction(action);
    }

    private void addAction(JMenuItem component, ActionListener act) {
        addKeyAction(component, NONE, act);
    }

    private AbstractAction toAbstractAction(String name, ActionListener listener) {
        return new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent e) {
                listener.actionPerformed(e);
            }

            @Override
            public void setEnabled(boolean newValue) {
                super.setEnabled(true);
            }
        };
    }

    private void showHelpMessage(String title, String msg) {
        JTextArea area = new JTextArea(msg);
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setPreferredSize(jFrame.getPreferredSize());
        JOptionPane.showMessageDialog(this.jFrame,
                scrollPane, "Help: " + title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void fullScreenAction(ActionEvent doToggle) {
        if (doToggle == null) {
            setFullScreen(!fullScreenItem.getState());
        }
    }

    private GraphicsDevice getGraphicsDevice() {
        return jFrame.getGraphicsConfiguration().getDevice();
    }

    private BufferedImage createImage(GraphicsDevice gd, Dimension d) {
        BufferedImage bi = gd.getDefaultConfiguration().createCompatibleImage(d.width, d.height);
        if (bi.getType() != BufferedImage.TYPE_INT_RGB) {
            //mmh we need INT_RGB here
            bi = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
        }
        return bi;
    }

    private void showDebugInfo(boolean showDebug) {
        this.showDebug = showDebug;
        SwingUtilities.invokeLater(() -> {
            if (fullScreenItem.getState()) {
                jFrame.getJMenuBar().setVisible(showDebug);
            }
            perfLabel.setVisible(showDebug);
        });
    }

    public void resetScreen() {
//        Util.sleep(250);
        SwingUtilities.invokeLater(() -> {
            screenCanvas.invalidate();
            screenCanvas.repaint();
            perfLabel.setText("");
            jFrame.setTitle(FRAME_TITLE_HEAD);
            LOG.info("Blanking screen");
        });
    }

    @Override
    public void setFullScreen(boolean value) {
        SwingUtilities.invokeLater(() -> {
            fullScreenItem.setState(value);
            LOG.info("Full screen: " + fullScreenItem.isSelected());
        });
    }

    @Override
    public String getRegionOverride() {
        return regionItems.stream().filter(i -> i.isSelected()).
                map(JCheckBoxMenuItem::getText).findFirst().orElse(null);
    }

    public void initSwing() {
//        Util.registerJmx(this);
        GraphicsDevice gd = SwingScreenSupport.setupScreens();
        fullScreenSize = gd.getDefaultConfiguration().getBounds().getSize();
        LOG.info("Full screen size: " + fullScreenSize);
        LOG.info("Emulation viewport size: " + DEFAULT_SCALED_SCREEN_SIZE);
        LOG.info("Application size: " + DEFAULT_FRAME_SIZE);

        jFrame = new JFrame(FRAME_TITLE_HEAD, gd.getDefaultConfiguration());
        jFrame.getContentPane().setBackground(Color.BLACK);
        jFrame.getContentPane().setForeground(Color.BLACK);

        baseD = new Dimension(1024, 513);
        baseImage = createImage(gd, baseD);
        destImage = createImage(gd, outputNonScaledScreenSize);
        renderData = getPixels(baseImage);

        JMenuBar bar = new JMenuBar();

        JMenu menu = new JMenu("File");
        bar.add(menu);

        JMenu setting = new JMenu("Setting");
        bar.add(setting);

        JMenuItem pauseItem = new JMenuItem("Pause");
        addKeyAction(pauseItem, TOGGLE_PAUSE, e -> handleSystemEvent(TOGGLE_PAUSE, null, null));
        setting.add(pauseItem);

        JMenuItem resetItem = new JMenuItem("Hard Reset");
        addKeyAction(resetItem, RESET, e -> mainEmu.reset());
        setting.add(resetItem);

        JMenuItem softResetItem = new JMenuItem("Soft Reset");
        addKeyAction(softResetItem, SOFT_RESET, e -> handleSystemEvent(SOFT_RESET, null, null));
        setting.add(softResetItem);

        JMenu regionMenu = new JMenu("Region");
        setting.add(regionMenu);

        JMenu screensMenu = new JMenu("Screens");
        createAddScreenItems(screensMenu);
        setting.add(screensMenu);

        JMenu inputMenu = new JMenu("Input");
        reloadControllers(InputProvider.DEFAULT_CONTROLLERS);
        inputMenusMap.values().forEach(inputMenu::add);
        setting.add(inputMenu);

        JMenu menuView = new JMenu("View");
        bar.add(menuView);

        regionItems = createRegionItems();
        regionItems.forEach(regionMenu::add);

        fullScreenItem = new JCheckBoxMenuItem("Full Screen", false);
        addKeyAction(fullScreenItem, TOGGLE_FULL_SCREEN, e -> fullScreenAction(e));
        menuView.add(fullScreenItem);

        muteItem = new JCheckBoxMenuItem("Enable Sound", true);
        addKeyAction(muteItem, TOGGLE_MUTE, e -> handleSystemEvent(TOGGLE_MUTE, null, null));
        menuView.add(muteItem);

        JMenu helpMenu = new JMenu("Help");
        bar.add(helpMenu);
        bar.add(Box.createHorizontalGlue());
        bar.add(perfLabel);

        JMenuItem loadRomItem = new JMenuItem("Load ROM");
        addKeyAction(loadRomItem, NEW_ROM, e -> handleNewRom());

        recentFilesMenu = new JMenu("Recent Files");
        recentFilesItems = new JMenuItem[PrefStore.recentFileTotal];
        IntStream.range(0, recentFilesItems.length).forEach(i -> {
            recentFilesItems[i] = new JMenuItem();
            addKeyAction(recentFilesItems[i], NONE, e -> handleNewRom(recentFilesItems[i].getToolTipText()));
            recentFilesMenu.add(recentFilesItems[i]);
        });
        reloadRecentFiles();

        JMenuItem closeRomItem = new JMenuItem("Close ROM");
        addKeyAction(closeRomItem, CLOSE_ROM, e -> handleSystemEvent(CLOSE_ROM, null, null));

        JMenuItem loadStateItem = new JMenuItem("Load State");
        addKeyAction(loadStateItem, LOAD_STATE, e -> handleLoadState());

        JMenuItem saveStateItem = new JMenuItem("Save State");
        addKeyAction(saveStateItem, SAVE_STATE, e -> handleSaveState());

        JMenuItem quickSaveStateItem = new JMenuItem("Quick Save State");
        addKeyAction(quickSaveStateItem, QUICK_SAVE, e -> handleQuickSaveState());

        JMenuItem quickLoadStateItem = new JMenuItem("Quick Load State");
        addKeyAction(quickLoadStateItem, QUICK_LOAD, e -> handleQuickLoadState());

        JMenuItem exitItem = new JMenuItem("Exit");
        addKeyAction(exitItem, CLOSE_APP, e -> {
            handleSystemEvent(CLOSE_APP, null, null);
            System.exit(0);
        });

        JMenuItem aboutItem = new JMenuItem("About");
        addAction(aboutItem, e -> showHelpMessage(aboutItem.getText(), getAboutString()));

        JMenuItem creditsItem = new JMenuItem("Credits");
        addAction(creditsItem, e -> showHelpMessage(creditsItem.getText(),
                FileLoader.readFileContentAsString("CREDITS.md")
        ));

        JMenuItem keyBindingsItem = new JMenuItem("Key Bindings");
        addAction(keyBindingsItem, e -> showHelpMessage(keyBindingsItem.getText(),
//                KeyBindingsHandler.toConfigString()
                ""
        ));

        JMenuItem readmeItem = new JMenuItem("Readme");
        addAction(readmeItem, e -> showHelpMessage(readmeItem.getText(),
                FileLoader.readFileContentAsString("README.md")
        ));

        JMenuItem licenseItem = new JMenuItem("License");
        addAction(licenseItem, e -> showHelpMessage(licenseItem.getText(),
                FileLoader.readFileContentAsString("LICENSE.md")
        ));

        JMenuItem historyItem = new JMenuItem("History");
        addAction(historyItem, e -> showHelpMessage(historyItem.getText(),
                FileLoader.readFileContentAsString("HISTORY.md")));

        menu.add(loadRomItem);
        menu.add(recentFilesMenu);
        menu.add(closeRomItem);
        menu.add(loadStateItem);
        menu.add(saveStateItem);
        menu.add(quickLoadStateItem);
        menu.add(quickSaveStateItem);
        menu.add(exitItem);
        helpMenu.add(aboutItem);
        helpMenu.add(keyBindingsItem);
        helpMenu.add(readmeItem);
        helpMenu.add(creditsItem);
        helpMenu.add(historyItem);
        helpMenu.add(licenseItem);

        AbstractAction debugUiAction = toAbstractAction("debugUI", e -> showDebugInfo(!showDebug));
        actionMap.put(SET_DEBUG_UI, debugUiAction);

        setupFrameKeyListener();

        jFrame.setMinimumSize(DEFAULT_FRAME_SIZE);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setResizable(true);
        jFrame.setJMenuBar(bar);
        jFrame.setVisible(true);

        screenCanvas = new Canvas(gd.getDefaultConfiguration());
        screenCanvas.setIgnoreRepaint(true);
        jFrame.add(screenCanvas, -1);
        screenCanvas.createBufferStrategy(2);
        do {
            strategy = screenCanvas.getBufferStrategy();
        } while (strategy == null);
        jFrame.setVisible(false);
        jFrame.pack();

        //get the center location and then reset it
        jFrame.setLocationRelativeTo(null);
        Point centerPoint = jFrame.getLocation();
        jFrame.setLocation(gd.getDefaultConfiguration().getBounds().x + centerPoint.x,
                gd.getDefaultConfiguration().getBounds().y + centerPoint.y);
        jFrame.setVisible(true);
    }

    private void showLabel(String label) {
        showInfoCount--;
        if (actionInfo.isPresent()) {
            label += " - " + actionInfo.get();
        }
        if (!label.equalsIgnoreCase(perfLabel.getText())) {
            perfLabel.setText(label);
        }
        if (showInfoCount <= 0) {
            actionInfo = Optional.empty();
        }
    }

    private Optional<File> loadFileDialog(Component parent, FileFilter filter) {
        return fileDialog(parent, filter, true);
    }

    private Optional<File> fileDialog(Component parent, FileFilter filter, boolean load) {
        int dialogType = load ? JFileChooser.OPEN_DIALOG : JFileChooser.SAVE_DIALOG;
        Optional<File> res = Optional.empty();
        JFileChooser fileChooser = new JFileChooser(FileLoader.basePath);
        fileChooser.setFileFilter(filter);
        fileChooser.setDialogType(dialogType);
        int result = fileChooser.showDialog(parent, null);
        if (result == JFileChooser.APPROVE_OPTION) {
            res = Optional.ofNullable(fileChooser.getSelectedFile());
        }
        return res;
    }

    @Override
    public void showInfo(String info) {
        actionInfo = Optional.of(info);
        showInfoCount = SHOW_INFO_FRAMES_DELAY;
    }

    private Optional<File> loadRomDialog(Component parent) {
        return loadFileDialog(parent, FileLoader.ROM_FILTER); //TODO
    }

    private Optional<File> loadStateFileDialog(Component parent) {
        return loadFileDialog(parent, FileLoader.SAVE_STATE_FILTER);
    }

    private void handleLoadState() {
        Optional<File> optFile = loadStateFileDialog(jFrame);
        if (optFile.isPresent()) {
            Path file = optFile.get().toPath();
            handleSystemEvent(LOAD_STATE, file, file.getFileName().toString());
        }
    }

    private void handleQuickLoadState() {
        Path file = Paths.get(QUICK_SAVE_PATH, FileLoader.QUICK_SAVE_FILENAME);
        handleSystemEvent(QUICK_LOAD, file, file.getFileName().toString());
    }

    private void handleQuickSaveState() {
        Path p = Paths.get(QUICK_SAVE_PATH, FileLoader.QUICK_SAVE_FILENAME);
        handleSystemEvent(QUICK_SAVE, p, p.getFileName().toString());
    }

    private void handleSaveState() {
        Optional<File> optFile = fileDialog(jFrame, FileLoader.SAVE_STATE_FILTER, false);
        if (optFile.isPresent()) {
            handleSystemEvent(SAVE_STATE, optFile.get().toPath(), optFile.get().getName());
        }
    }

    protected void handleNewRom() {
        Optional<File> optFile = loadRomDialog(jFrame);
        if (optFile.isPresent()) {
            handleSystemEvent(CLOSE_ROM, null, null);
            Path file = optFile.get().toPath();
//            mainEmu.handleSystemEvent(NEW_ROM, file);
            reloadRecentFiles();
            showInfo(NEW_ROM + ": " + file.getFileName());
            resetScreen();
            SwingUtilities.invokeLater(() -> {
                jFrame.removeAll();
                jFrame.repaint();
                jFrame.dispose();
            });
            SwingUtilities.invokeLater(() -> JPSXLauncher.launch(file.toFile()));
        }
    }

    private void handleNewRom(String path) {
        Path p = Paths.get(path);
        showInfo(NEW_ROM + ": " + p.getFileName());
        mainEmu.handleSystemEvent(NEW_ROM, p);
    }

    private SystemProvider getMainEmu() {
        return mainEmu;
    }

    @Override
    public void reloadSystem(SystemProvider systemProvider) {
        Optional.ofNullable(mainEmu).ifPresent(sys -> sys.handleSystemEvent(CLOSE_ROM, null));
        this.mainEmu = systemProvider;

        Arrays.stream(jFrame.getKeyListeners()).forEach(jFrame::removeKeyListener);
        setupFrameKeyListener();
        Optional.ofNullable(mainEmu).ifPresent(sp -> setTitle(""));
    }

    @Override
    public void addKeyListener(KeyListener keyAdapter) {
        jFrame.addKeyListener(keyAdapter);
    }

    private JLabel getPerfLabel() {
        return perfLabel;
    }

    private int[] getPixels(BufferedImage img) {
        return ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
    }

    //TODO this is necessary in fullScreenMode
    private void setupFrameKeyListener() {
        jFrame.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                SystemProvider mainEmu = getMainEmu();
                KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);
//                LOG.info(keyStroke.toString());
//                SystemProvider.SystemEvent event = KeyBindingsHandler.getInstance().getSystemEventIfAny(keyStroke);
//                if (event != null && event != NONE) {
//                    //if the menuBar is visible it will handle the event, otherwise we need to perform the action here
//                    boolean menuVisible = jFrame.getJMenuBar().isVisible();
//                    if (!menuVisible) {
//                        Optional.ofNullable(actionMap.get(event)).ifPresent(act -> act.actionPerformed(null));
//                    }
//                }
            }
        });
    }

    private List<JCheckBoxMenuItem> createRegionItems() {
        List<JCheckBoxMenuItem> l = new ArrayList<>();
        l.add(new JCheckBoxMenuItem("AutoDetect", true));
        Arrays.stream(Region.values()).sorted().
                forEach(r -> l.add(new JCheckBoxMenuItem(r.name(), false)));
        //only allow one selection
        final List<JCheckBoxMenuItem> list1 = new ArrayList<>(l);
        l.stream().forEach(i -> i.addItemListener(e -> {
            if (ItemEvent.SELECTED == e.getStateChange()) {
                list1.stream().filter(i1 -> !i.getText().equals(i1.getText())).forEach(i1 -> i1.setSelected(false));
            }
        }));
        return l;
    }

    private List<JCheckBoxMenuItem> createAddScreenItems(JMenu screensMenu) {
        List<String> l = SwingScreenSupport.detectScreens();
        List<JCheckBoxMenuItem> items = new ArrayList<>();
        for (int i = 0; i < l.size(); i++) {
            String s = l.get(i);
            JCheckBoxMenuItem it = new JCheckBoxMenuItem(s);
            it.setState(i == SwingScreenSupport.getCurrentScreen());
            items.add(it);
            screensMenu.add(it);
        }
        for (int i = 0; i < items.size(); i++) {
            final int num = i;
            addKeyAction(items.get(i), NONE, e -> handleScreenChange(items, num));
        }
        return items;
    }

    private void handleScreenChange(List<JCheckBoxMenuItem> items, int newScreen) {
        int cs = SwingScreenSupport.getCurrentScreen();
        if (cs != newScreen) {
            SwingScreenSupport.showOnScreen(newScreen, jFrame);
        }
        for (int i = 0; i < items.size(); i++) {
            if (i != newScreen) {
                items.get(i).setSelected(false);
            }
        }
    }

    private void reloadRecentFiles() {
        List<String> l = PrefStore.getRecentFilesList();
        IntStream.range(0, recentFilesItems.length).forEach(i -> {
            String val = i < l.size() ? l.get(i) : "<none>";
            val = Strings.isNullOrEmpty(val) ? "<none>" : val;
            int idx = val.lastIndexOf(File.separatorChar);
            String text = i + ". " + (idx > 0 ? val.substring(idx + 1) : val);
            recentFilesItems[i].setVisible(true);
            recentFilesItems[i].setEnabled(!Strings.isNullOrEmpty(val));
            recentFilesItems[i].setText(text);
            recentFilesItems[i].setToolTipText(val);
        });
    }

    @Override
    public void reloadControllers(Collection<String> list) {
        for (PlayerNumber pn : inputMenusMap.keySet()) {
            JMenu menu = inputMenusMap.get(pn);
            menu.removeAll();
            List<JCheckBoxMenuItem> l = new ArrayList<>();
            list.forEach(c -> {
                JCheckBoxMenuItem item = new JCheckBoxMenuItem(c, InputProvider.KEYBOARD_CONTROLLER.equalsIgnoreCase(c));
                addAction(item, e -> handleSystemEvent(CONTROLLER_CHANGE, pn.name() + ":" + c,
                        pn.name() + ":" + c));
                l.add(item);
            });
            //only allow one selection
            final List<JCheckBoxMenuItem> list1 = new ArrayList<>(l);
            l.stream().forEach(i -> i.addItemListener(e -> {
                if (ItemEvent.SELECTED == e.getStateChange()) {
                    list1.stream().filter(i1 -> !i.getText().equals(i1.getText())).forEach(i1 -> i1.setSelected(false));
                }
            }));
            l.stream().forEach(menu::add);
            //fudgePlayer1Using1stController
            if (list.size() > 2 && pn == PlayerNumber.P1) {
                LOG.info("Auto-selecting " + pn + " using Controller: " + l.get(2).getText());
                l.get(2).doClick();
            }
        }
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

    private Dimension dimension = new Dimension();
    private int x, y;

    private Graphics2D getBuffer() {
        if (graphics == null) {
            try {
                graphics = (Graphics2D) strategy.getDrawGraphics();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return null;
            }
        }
        return graphics;
    }


    @Override
    public void refresh() {
        refreshStrategy();
    }

    private void refreshStrategy() {
        // Update Graphics
        Graphics2D bg = null;
        boolean rgb24 = displayManager.getRGB24bit();
        GPU.setVRAMFormat(rgb24);
        updateDimension();
        double scaleW = 1, scaleH = 1;
        if (dimension.width != DEFAULT_X || dimension.height != DEFAULT_Y) {
            scaleW = scale * DEFAULT_X / dimension.width;
            scaleH = scale * DEFAULT_Y / dimension.height;
        }
        int w = (int) (dimension.width * scaleW);
        int h = (int) (dimension.height * scaleH);
        do {
            try {
                bg = getBuffer();
                if (displayManager.getBlanked()) {
                    bg.setColor(Color.BLACK);
                    bg.fillRect(0, 0, w, h);
                } else {
                    bg.drawImage(destImage, 0, 0, w, h
                            , 0, 0, dimension.width, dimension.height, null);
                }
            } finally {
                Optional.ofNullable(bg).ifPresent(Graphics2D::dispose);
            }
        } while (!updateScreen());
    }

    private boolean updateScreen() {
        graphics.dispose();
        graphics = null;
        try {
            strategy.show();
            Toolkit.getDefaultToolkit().sync();
            return (!strategy.contentsLost());
        } catch (Exception e) {
            return true;
        }
    }

    private boolean updateDimension() {
        int w = displayManager.getPixelWidth();
        int h = displayManager.getPixelHeight();
        int newX = displayManager.getXOrigin();
        int newY = displayManager.getYOrigin();

        if (w * h == 0) {
            return false;
        }
        boolean change = false;
        if (dimension.width != w || dimension.height != h) {
            dimension.width = w;
            dimension.height = h;
//            LOG.info(dimension.toString() + " -> " + dimension);
            change = true;
        }
        if (x != newX || y != newY) {
            change = true;
            x = newX;
            y = newY;
        }
        if (change) {
            destImage = baseImage.getSubimage(x, y, w, h);
        }
        return change;
    }
}
