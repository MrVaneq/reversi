
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    public static final int HEIGHT = 400;
    public static final int WIDTH = 400;

    public static void main(String[] args) {
        JLabel stepCounterLabel = new AdvancedLabel("Сделано ходов: 0");
        JLabel whiteCounterLabel = new AdvancedLabel("Белых фишек: 2");
        JLabel blackCounterLabel = new AdvancedLabel("Черных фишек: 2");
        JPanel panel = new JPanel();

        JPanel scorePanel = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(panel.getWidth() / 3, panel.getHeight());
            }
        };

        JPanel game = new GameField() {
            @Override
            protected void updateScore(int blackNum, int whiteNum, int steps) {
                stepCounterLabel.setText("Сделано ходов: " + steps);
                whiteCounterLabel.setText("Белых фишек: " + whiteNum);
                blackCounterLabel.setText("Черных фишек: " + blackNum);
                scorePanel.repaint();
            }
        };
        panel.setPreferredSize(new Dimension(WIDTH * 3 / 2, HEIGHT + 2));
        panel.setLayout(new BorderLayout());
        panel.add(game, BorderLayout.CENTER);

        scorePanel.setLayout(new GridLayout(4, 1));
        scorePanel.add(stepCounterLabel);
        scorePanel.add(whiteCounterLabel);
        scorePanel.add(blackCounterLabel);
        panel.add(scorePanel, BorderLayout.EAST);

        JFrame frame = new JFrame();
        frame.setContentPane(panel);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setVisible(true);
        game.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        frame.pack();
        frame.setLocation(screenSize.width / 2 - frame.getWidth() / 2, screenSize.height / 2 - frame.getHeight() / 2);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }
}

class AdvancedLabel extends JLabel {
    AdvancedLabel(String text) {
        super(text);
        setFont(new Font("Dialog", Font.BOLD, 14));
    }
}

enum GameStatement {LOSE, WIN, DRAW, IN_GAME};

class GameField extends JPanel {
    private GameStatement state = GameStatement.IN_GAME;
    private int currCellY = -1;
    private int currCellX = -1;
    private ReversiGameField rgm = new ReversiGameField();
    private final Label mainLabel = new Label(" Подождите, ваш противник ходит ");
    private final String loseStr = " Вы проиграли! ";
    private final String winStr = " Вы выиграли! ";
    private final String drawStr = " Ничья?! 0_0 ";

    private final AtomicBoolean awaitingIsEnded = new AtomicBoolean(true);
    private int stepCounter = 0;

    void doAwait() {
        awaitingIsEnded.set(false);
        mainLabel.setLocation(this.getWidth() / 2 - mainLabel.getWidth() / 2, this.getHeight() / 2 - mainLabel.getHeight() / 2);
        mainLabel.setVisible(true);
        doLayout();
        new Thread(() -> {
            try {
                Thread.sleep(500);
                while (!rgm.isAvailable()) {
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            SwingUtilities.invokeLater(() -> {
                mainLabel.setVisible(false);
                repaint();
            });
            awaitingIsEnded.set(true);
            stepCounter++;
            int black = rgm.numberOfBlack();
            int white = rgm.numberOfWhite();
            updateScore(black, white, stepCounter);
            if (black + white == 64 || (rgm.availableCells.size() == 0 && rgm.isSkipped.get())) {
                if (black > white) {
                    state = GameStatement.WIN;
                    mainLabel.setText(winStr);
                } else if (white == black) {
                    state = GameStatement.DRAW;
                    mainLabel.setText(drawStr);
                } else {
                    state = GameStatement.LOSE;
                    mainLabel.setText(loseStr);
                }
                SwingUtilities.invokeLater(() -> {
                    mainLabel.setFont(new Font("", Font.BOLD, 16));
                    mainLabel.setVisible(true);
                    doLayout();
                    repaint();
                });
            }
        }).start();
    }

    public GameField() {
        super();
        this.setLayout(new GridBagLayout());
        this.add(mainLabel);
        mainLabel.setFont(new Font("", Font.BOLD, 14));
        mainLabel.setBackground(Color.GRAY);
        mainLabel.setForeground(Color.CYAN);
        mainLabel.setVisible(false);

        this.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent mouseEvent) {
                int newCurrCellX = (int) Math.ceil(mouseEvent.getX() / (((float)getWidth() / 8)) - 1);
                int newCurrCellY = (int) Math.ceil(mouseEvent.getY() / (((float)getHeight()) / 8) - 1);
                if (newCurrCellX != currCellX || newCurrCellY != currCellY) {
                    currCellX = newCurrCellX;
                    currCellY = newCurrCellY;
                    repaint();
                }
            }
        });
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                int x = (int) Math.ceil(mouseEvent.getX() / (((float)getWidth() / 8)) - 1);
                int y = (int) Math.ceil(mouseEvent.getY() / (((float)getHeight() / 8)) - 1);
                if (rgm.setBlack(x, y) && awaitingIsEnded.get()) {
                    doAwait();
                }
                repaint();
                super.mousePressed(mouseEvent);
            }
        });
    }

    @Override
    protected void paintComponent(Graphics graphics) {

        super.paintComponent(graphics);

        Graphics2D g2 = (Graphics2D) graphics;
        g2.setStroke(new BasicStroke(1));

        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        if (state == GameStatement.IN_GAME) {
            int w = (this.getWidth() - 1) / 8;
            int h = (this.getHeight() - 1) / 8;
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    g2.setColor(Color.BLACK);
                    g2.draw(new Rectangle(j * w, i * h, w, h));
                    if (rgm.availableCells.contains(new Cell(j, i))) {
                        g2.setColor(new Color(123, 23, 122, 50));
                        g2.fill(new Rectangle(j * w + 1, i * h + 1, w - 1, h - 1));
                        g2.setColor(Color.BLACK);
                    }
                    if (i == currCellY && j == currCellX) {
                        g2.setColor(new Color(68, 75, 64, 100));
                        g2.fill(new Rectangle(j * w + 1, i * h + 1, w - 1, h - 1));
                        g2.setColor(Color.BLACK);
                    }
                    char currCell = rgm.getFieldValue()[i][j];

                    if (currCell == 'b') {
                        g2.setColor(Color.BLACK);
                        g2.setStroke(new BasicStroke(5));
                        g2.drawOval(j * w + 5, i * h + 5, w - 10, h - 10);
                        g2.setStroke(new BasicStroke(1));
                        g2.setColor(Color.BLACK);
                        g2.fillOval(j * w + 5, i * h + 5, w - 10, h - 10);
                    }


                    if (currCell == 'w') {
                        g2.setColor(Color.BLACK);
                        g2.setStroke(new BasicStroke(5));
                        g2.drawOval(j * w + 5, i * h + 5, w - 10, h - 10);

                        g2.setStroke(new BasicStroke(1));
                        g2.setColor(Color.WHITE);
                        g2.fillOval(j * w + 5, i * h + 5, w - 10, h - 10);
                        g2.setColor(Color.BLACK);

                    }

                }
            }
        }

    }

    protected void updateScore(int blackNum, int whiteNum, int step) {
    }
}
