package com.mojang.mojam;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.IOException;
import java.net.*;
import java.util.*;

import javax.swing.*;

import com.mojang.mojam.entity.Player;
import com.mojang.mojam.entity.building.Base;
import com.mojang.mojam.entity.mob.Team;
import com.mojang.mojam.gui.*;
import com.mojang.mojam.gui.Button;
import com.mojang.mojam.gui.Font;
import com.mojang.mojam.level.Level;
import com.mojang.mojam.level.tile.Tile;
import com.mojang.mojam.network.*;
import com.mojang.mojam.network.packet.*;
import com.mojang.mojam.screen.Screen;
import com.mojang.mojam.sound.SoundPlayer;

public class MojamComponent extends Canvas implements Runnable, MouseMotionListener, CommandListener, PacketListener, MouseListener, ButtonListener, KeyListener {

    private static final long serialVersionUID = 1L;
    public static final int GAME_WIDTH = 512;
    public static final int GAME_HEIGHT = GAME_WIDTH * 3 / 4;
    public static final int SCALE = 2;
    public static final int SCALE_WIDTH = GAME_WIDTH * SCALE;
    public static final int SCALE_HEIGHT = GAME_HEIGHT * SCALE;
    
    public static final int MIDDLEX = SCALE_WIDTH / 2;
    public static final int MIDDLEY = (SCALE_HEIGHT - 64) / 2;
    
    private boolean running = true;
    private double framerate = 60;
    private int fps;
    private Screen screen = new Screen(GAME_WIDTH, GAME_HEIGHT);
    private Level level;

    private Stack<GuiMenu> menuStack = new Stack<GuiMenu>();

    public MouseButtons mouseButtons = new MouseButtons();
    public Controls controls = new Controls();
    public Controls[] synchedControls = {
            new Controls(), new Controls()
    };
    public Player[] players = new Player[2];
    public Player player;
    public TurnSynchronizer synchronizer;
    private PacketLink packetLink;
    private ServerSocket serverSocket;
    private boolean isMultiplayer;
    private boolean isServer;
    private int localId;
    private Thread hostThread;
    public static SoundPlayer soundPlayer;

    private int createServerState = 0;

    public MojamComponent() {
        this.setPreferredSize(new Dimension(SCALE_WIDTH, SCALE_HEIGHT));
        this.setMinimumSize(new Dimension(SCALE_WIDTH, SCALE_HEIGHT));
        this.setMaximumSize(new Dimension(SCALE_WIDTH, SCALE_HEIGHT));
        
        InputHandler inHandler = new InputHandler(controls);
        this.addKeyListener(inHandler);
        this.addMouseListener(inHandler);
        this.addMouseMotionListener(inHandler);
        
        this.addMouseMotionListener(this);
        this.addMouseListener(this);
        this.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        TitleMenu menu = new TitleMenu(GAME_WIDTH, GAME_HEIGHT);
        addMenu(menu);
        addKeyListener(this);
    }

    public void mouseDragged(MouseEvent arg0) {
    }

    public void mouseMoved(MouseEvent arg0) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
        mouseButtons.releaseAll();
    }

    public void mousePressed(MouseEvent e) {
        mouseButtons.setNextState(e.getButton(), true);
    }

    public void mouseReleased(MouseEvent e) {
        mouseButtons.setNextState(e.getButton(), false);
    }

    public void paint(Graphics g) {
    }

    public void update(Graphics g) {
    }

    public void start() {
        running = true;
        Thread thread = new Thread(this);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    public void stop() {
        running = false;
        soundPlayer.shutdown();
    }

    private void init() {
        soundPlayer = new SoundPlayer();
        soundPlayer.startBackgroundMusic();

        setFocusTraversalKeysEnabled(false);
        requestFocus();

    }

    private synchronized void createLevel() {
        try {
            level = Level.fromFile("/levels/level1.bmp");
        } catch (Exception ex) {
            throw new RuntimeException("Unable to load level", ex);
        }

        level.init();

        players[0] = new Player(synchedControls[0], level.width * Tile.WIDTH / 2 - 16, (level.height - 5 - 1) * Tile.HEIGHT - 16, Team.Team1);
        players[0].setFacing(4);
        level.addEntity(players[0]);
        level.addEntity(new Base(34 * Tile.WIDTH, 7 * Tile.WIDTH, Team.Team1));
        if (isMultiplayer) {
            players[1] = new Player(synchedControls[1], level.width * Tile.WIDTH / 2 - 16, 7 * Tile.HEIGHT - 16, Team.Team2);
            level.addEntity(players[1]);
            level.addEntity(new Base(32 * Tile.WIDTH - 20, 32 * Tile.WIDTH - 20, Team.Team2));
        }
        player = players[localId];
        player.setCanSee(true);
    }

    public void run() {
        long lastTime = System.nanoTime();
        double unprocessed = 0;
        int frames = 0;
        long lastTimer1 = System.currentTimeMillis();

        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

//        if (!isMultiplayer) {
//            createLevel();
//        }

        int toTick = 0;

        long lastRenderTime = System.nanoTime();
        int min = 999999999;
        int max = 0;

        while (running) {
            if (!this.hasFocus()) {
                controls.release();
            }

            double nsPerTick = 1000000000.0 / framerate;
            boolean shouldRender = false;
            while (unprocessed >= 1) {
                toTick++;
                unprocessed -= 1;
            }

            int tickCount = toTick;
            if (toTick > 0 && toTick < 3) {
                tickCount = 1;
            }
            if (toTick > 20) {
                toTick = 20;
            }

            for (int i = 0; i < tickCount; i++) {
                toTick--;
//                long before = System.nanoTime();
                tick();
//                long after = System.nanoTime();
//                System.out.println("Tick time took " + (after - before) * 100.0 / nsPerTick + "% of the max time");
                shouldRender = true;
            }
//            shouldRender = true;

            BufferStrategy bs = getBufferStrategy();
            if (bs == null) {
                createBufferStrategy(3);
                continue;
            }
            if (shouldRender) {
                frames++;
                Graphics g = bs.getDrawGraphics();

                Random lastRandom = TurnSynchronizer.synchedRandom;
                TurnSynchronizer.synchedRandom = null;

                render(g);

                TurnSynchronizer.synchedRandom = lastRandom;

                long renderTime = System.nanoTime();
                int timePassed = (int) (renderTime - lastRenderTime);
                if (timePassed < min) {
                    min = timePassed;
                }
                if (timePassed > max) {
                    max = timePassed;
                }
                lastRenderTime = renderTime;
            }

            long now = System.nanoTime();
            unprocessed += (now - lastTime) / nsPerTick;
            lastTime = now;

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (shouldRender) {
                if (bs != null) {
                    bs.show();
                }
            }

            if (System.currentTimeMillis() - lastTimer1 > 1000) {
                lastTimer1 += 1000;
                fps = frames;
                frames = 0;
            }
        }
    }

    private synchronized void render(Graphics g) {
        if (level != null) {
            int xScroll = (int) (player.pos.x - screen.w / 2);
            int yScroll = (int) (player.pos.y - (screen.h - 24) / 2);
            soundPlayer.setListenerPosition((float) player.pos.x, (float) player.pos.y);
            level.render(screen, xScroll, yScroll);
        }
        if (!menuStack.isEmpty()) {
            menuStack.peek().render(screen);
        }

        Font.draw(screen, "FPS: " + fps, 10, 10);
//        for (int p = 0; p < players.length; p++) {
//            if (players[p] != null) {
//                String msg = "P" + (p + 1) + ": " + players[p].getScore();
//                Font.draw(screen, msg, 320, screen.h - 24 + p * 8);
//            }
//        }
        if (player != null && menuStack.size() == 0) {
            Font.draw(screen, player.health + " / 10", 340, screen.h - 19);
            Font.draw(screen, "" + player.score, 340, screen.h - 33);
        }

        g.setColor(Color.BLACK);

        g.fillRect(0, 0, getWidth(), getHeight());
        g.translate((getWidth() - GAME_WIDTH * SCALE) / 2, (getHeight() - GAME_HEIGHT * SCALE) / 2);
        g.clipRect(0, 0, GAME_WIDTH * SCALE, GAME_HEIGHT * SCALE);

        if (!menuStack.isEmpty() || level != null) {
            g.drawImage(screen.image, 0, 0, GAME_WIDTH * SCALE, GAME_HEIGHT * SCALE, null);
        }

//        String msg = "FPS: " + fps;
//        g.setColor(Color.LIGHT_GRAY);
//        g.drawString(msg, 11, 11);
//        g.setColor(Color.WHITE);
//        g.drawString(msg, 10, 10);

    }

    private void tick() {
        if (level != null) {
            if (level.player1Score >= Level.TARGET_SCORE) {
                addMenu(new WinMenu(GAME_WIDTH, GAME_HEIGHT, 1));
                level = null;
                return;
            }
            if (level.player2Score >= Level.TARGET_SCORE) {
                addMenu(new WinMenu(GAME_WIDTH, GAME_HEIGHT, 2));
                level = null;
                return;
            }
        }
        if (packetLink != null) {
            packetLink.tick();
        }
        if (level != null) {
            if (synchronizer.preTurn()) {
                synchronizer.postTurn();
                for (int index = 0; index < controls.getAll().size(); index++) {
                    Controls.Key key = controls.getAll().get(index);
                    boolean nextState = key.nextState;
                    if (key.isDown != nextState) {
                        synchronizer.addCommand(new ChangeKeyCommand(index, nextState));
                    }
                }
                synchronizer.addCommand(new MouseMoveCommand(controls.mouseX, controls.mouseY));
                controls.tick();
                for (Controls skeys : synchedControls) {
                    skeys.tick();
                }
                level.tick();
            }
        }
        mouseButtons.setPosition(getMousePosition());
        if (!menuStack.isEmpty()) {
            menuStack.peek().tick(mouseButtons);
        }
        
        mouseButtons.tick();

        if (createServerState == 1) {
            createServerState = 2;

            synchronizer = new TurnSynchronizer(MojamComponent.this, packetLink, localId, 2);

            clearMenus();
            createLevel();

            synchronizer.setStarted(true);
            packetLink.sendPacket(new StartGamePacket(TurnSynchronizer.synchedSeed));
            packetLink.setPacketListener(MojamComponent.this);

        }
    }

    public static void main(String[] args) {
        MojamComponent mc = new MojamComponent();
        JFrame frame = new JFrame();
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(mc);
        frame.setContentPane(panel);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        mc.start();
    }

    public void handle(int playerId, NetworkCommand packet) {

        if (packet instanceof ChangeKeyCommand) {
            ChangeKeyCommand ckc = (ChangeKeyCommand) packet;
            synchedControls[playerId].getAll().get(ckc.getKey()).nextState = ckc.getNextState();
        } else if (packet instanceof MouseMoveCommand) {
        	MouseMoveCommand mmc = (MouseMoveCommand) packet;
        	synchedControls[playerId].mouseX = mmc.getX();
        	synchedControls[playerId].mouseY = mmc.getY();
        }
    }

    public void handle(Packet packet) {
        if (packet instanceof StartGamePacket) {
            if (!isServer) {
                synchronizer.onStartGamePacket((StartGamePacket) packet);
                createLevel();
            }
        } else if (packet instanceof TurnPacket) {
            synchronizer.onTurnPacket((TurnPacket) packet);
        }
    }

    public void buttonPressed(Button button) {
        if (button.getId() == TitleMenu.RESTART_GAME_ID) {
            clearMenus();
            TitleMenu menu = new TitleMenu(GAME_WIDTH, GAME_HEIGHT);
            addMenu(menu);


        } else if (button.getId() == TitleMenu.START_GAME_ID) {
            clearMenus();
            isMultiplayer = false;

            localId = 0;
            synchronizer = new TurnSynchronizer(this, null, 0, 1);
            synchronizer.setStarted(true);

            createLevel();
        } else if (button.getId() == TitleMenu.HOST_GAME_ID) {
            addMenu(new HostingWaitMenu());
            isMultiplayer = true;
            isServer = true;
            try {
                if (isServer) {
                    localId = 0;
                    serverSocket = new ServerSocket(3000);
                    serverSocket.setSoTimeout(1000);

                    hostThread = new Thread() {

                        public void run() {
                            boolean fail = true;
                            try {
                                while (!isInterrupted()) {
                                    Socket socket = null;
                                    try {
                                        socket = serverSocket.accept();
                                    } catch (SocketTimeoutException e) {

                                    }
                                    if (socket == null) {
                                        System.out.println("Waiting for connection");
                                        continue;
                                    }
                                    fail = false;

                                    packetLink = new NetworkPacketLink(socket);

                                    createServerState = 1;
                                    break;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (fail) {
                                try {
                                    serverSocket.close();
                                } catch (IOException e) {
                                }
                            }
                        };
                    };
                    hostThread.start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (button.getId() == TitleMenu.JOIN_GAME_ID) {
            addMenu(new JoinGameMenu());
        } else if (button.getId() == TitleMenu.CANCEL_JOIN_ID) {
            popMenu();
            if (hostThread != null) {
                hostThread.interrupt();
                hostThread = null;
            }
        } else if (button.getId() == TitleMenu.PERFORM_JOIN_ID) {
            menuStack.clear();
            isMultiplayer = true;
            isServer = false;

            try {
                localId = 1;
                packetLink = new ClientSidePacketLink(TitleMenu.ip, 3000);
                synchronizer = new TurnSynchronizer(this, packetLink, localId, 2);
                packetLink.setPacketListener(this);
            } catch (Exception e) {
                e.printStackTrace();
//                System.exit(1);
                addMenu(new TitleMenu(GAME_WIDTH, GAME_HEIGHT));
            }
        } else if (button.getId() == TitleMenu.EXIT_GAME_ID) {
            System.exit(0);
        }
    }

    private void clearMenus() {
        while (!menuStack.isEmpty()) {
            menuStack.pop();
        }
    }

    private void addMenu(GuiMenu menu) {
        menuStack.add(menu);
        menu.addButtonListener(this);
    }

    private void popMenu() {
        if (!menuStack.isEmpty()) {
            menuStack.pop();
        }
    }

    public void keyPressed(KeyEvent e) {
        if (!menuStack.isEmpty()) {
            menuStack.peek().keyPressed(e);
        }
    }

    public void keyReleased(KeyEvent e) {
        if (!menuStack.isEmpty()) {
            menuStack.peek().keyReleased(e);
        }
    }

    public void keyTyped(KeyEvent e) {
        if (!menuStack.isEmpty()) {
            menuStack.peek().keyTyped(e);
        }
    }

}