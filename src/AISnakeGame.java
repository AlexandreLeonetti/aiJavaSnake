import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.LinkedList;
import java.util.Random;

public class AISnakeGame extends JPanel implements ActionListener, KeyListener {
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int GRID_SIZE = 16;
    private static final int GRID_DIM = 600;
    private static final int CELL_SIZE = GRID_DIM / GRID_SIZE;
    private static final int DELAY = 20; // Adjust for speed control

    private enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    private static class Apple {
        int x, y, score, topScore;
    }

    private static class SnakeSegment {
        int x, y;
        Direction dir;
        SnakeSegment next;
    }

    private Apple apple;
    private SnakeSegment head, tail;
    private boolean quit;
    private Timer timer;

    public AISnakeGame() {
        initGame();
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        timer = new Timer(DELAY, this);
        timer.start();
    }

    private void initGame() {
        apple = new Apple();
        initSnake();
        generateApple();
        apple.score = 0;
    }

    private void initSnake() {
        head = new SnakeSegment();
        head.x = GRID_SIZE / 2;
        head.y = GRID_SIZE / 2;
        head.dir = Direction.UP;
        head.next = null;
        tail = head;

        for (int i = 0; i < 3; i++) {
            increaseSnake();
        }
    }

    private void increaseSnake() {
        SnakeSegment newSegment = new SnakeSegment();
        switch (tail.dir) {
            case UP -> {
                newSegment.x = tail.x;
                newSegment.y = tail.y + 1;
            }
            case DOWN -> {
                newSegment.x = tail.x;
                newSegment.y = tail.y - 1;
            }
            case LEFT -> {
                newSegment.x = tail.x + 1;
                newSegment.y = tail.y;
            }
            case RIGHT -> {
                newSegment.x = tail.x - 1;
                newSegment.y = tail.y;
            }
        }
        newSegment.dir = tail.dir;
        newSegment.next = null;
        tail.next = newSegment;
        tail = newSegment;
    }

    private void moveSnake() {
        int prevX = head.x;
        int prevY = head.y;
        Direction prevDir = head.dir;

        switch (head.dir) {
            case UP -> head.y--;
            case DOWN -> head.y++;
            case LEFT -> head.x--;
            case RIGHT -> head.x++;
        }

        SnakeSegment track = head.next;
        while (track != null) {
            int saveX = track.x;
            int saveY = track.y;
            Direction saveDir = track.dir;

            track.x = prevX;
            track.y = prevY;
            track.dir = prevDir;

            prevX = saveX;
            prevY = saveY;
            prevDir = saveDir;

            track = track.next;
        }
    }

    private void resetSnake() {
        head = null;
        tail = null;
        initSnake();
        if (apple.score > apple.topScore) {
            apple.topScore = apple.score;
        }
        apple.score = 0;
    }

    private void generateApple() {
        Random rand = new Random();
        boolean inSnake;
        do {
            inSnake = false;
            apple.x = rand.nextInt(GRID_SIZE);
            apple.y = rand.nextInt(GRID_SIZE);

            SnakeSegment track = head;
            while (track != null) {
                if (track.x == apple.x && track.y == apple.y) {
                    inSnake = true;
                    break;
                }
                track = track.next;
            }
        } while (inSnake);
    }

    private void detectApple() {
        if (head.x == apple.x && head.y == apple.y) {
            generateApple();
            increaseSnake();
            apple.score++;
        }
    }

    private void detectCrash() {
        if (head.x < 0 || head.x >= GRID_SIZE || head.y < 0 || head.y >= GRID_SIZE) {
            resetSnake();
        }

        SnakeSegment track = head.next;
        while (track != null) {
            if (track.x == head.x && track.y == head.y) {
                resetSnake();
                break;
            }
            track = track.next;
        }
    }

    private void aiControl() {
        // Basic AI: Random direction change (improve this with actual logic)
        int tryForward = state(Direction.UP);
        int tryLeft = state(Direction.LEFT);
        int tryRight = state(Direction.RIGHT);

        if (tryForward >= tryLeft && tryForward >= tryRight) {
            // Continue forward
        } else {
            if (tryLeft > tryRight) {
                turnLeft();
            } else {
                turnRight();
            }
        }
    }

    private int state(Direction tryDir) {
        int reward = 0;

        int tryX = head.x;
        int tryY = head.y;

        // Calculate the potential new position based on the tryDir
        switch (head.dir) {
            case UP -> {
                if (tryDir == Direction.UP) tryY--;
                if (tryDir == Direction.LEFT) tryX--;
                if (tryDir == Direction.RIGHT) tryX++;
            }
            case DOWN -> {
                if (tryDir == Direction.DOWN) tryY++;
                if (tryDir == Direction.LEFT) tryX++;
                if (tryDir == Direction.RIGHT) tryX--;
            }
            case LEFT -> {
                if (tryDir == Direction.LEFT) tryX--;
                if (tryDir == Direction.UP) tryY--;
                if (tryDir == Direction.DOWN) tryY++;
            }
            case RIGHT -> {
                if (tryDir == Direction.RIGHT) tryX++;
                if (tryDir == Direction.UP) tryY--;
                if (tryDir == Direction.DOWN) tryY++;
            }
        }

        // Detect wall crash
        if (tryX < 0 || tryX >= GRID_SIZE || tryY < 0 || tryY >= GRID_SIZE) {
            reward -= 300;
        }

        // Detect apple
        if (tryX == apple.x && tryY == apple.y) {
            reward += 1000;
        }

        // Move towards apple
        int diffX = Math.abs(head.x - apple.x);
        int diffY = Math.abs(head.y - apple.y);
        int tryDiffX = Math.abs(tryX - apple.x);
        int tryDiffY = Math.abs(tryY - apple.y );

        if (tryDiffX < diffX) reward += 3;
        if (tryDiffY < diffY) reward += 3;

        // Detect tail
        SnakeSegment track = head.next;
        while (track != null) {
            if (tryX == track.x && tryY == track.y) {
                reward -= 500;
            }
            track = track.next;
        }

        return reward;
    }


    private void turnLeft() {
        switch (head.dir) {
            case UP -> head.dir = Direction.LEFT;
            case DOWN -> head.dir = Direction.RIGHT;
            case LEFT -> head.dir = Direction.DOWN;
            case RIGHT -> head.dir = Direction.UP;
        }
    }

    private void turnRight() {
        switch (head.dir) {
            case UP -> head.dir = Direction.RIGHT;
            case DOWN -> head.dir = Direction.LEFT;
            case LEFT -> head.dir = Direction.UP;
            case RIGHT -> head.dir = Direction.DOWN;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!quit) {
            moveSnake();
            detectApple();
            detectCrash();
            aiControl();
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Render grid
        g.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i < GRID_SIZE; i++) {
            g.drawLine(i * CELL_SIZE, 0, i * CELL_SIZE, GRID_DIM);
            g.drawLine(0, i * CELL_SIZE, GRID_DIM, i * CELL_SIZE);
        }

        // Render snake
        g.setColor(Color.GREEN);
        SnakeSegment track = head;
        while (track != null) {
            g.fillRect(track.x * CELL_SIZE, track.y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            track = track.next;
        }

        // Render apple
        g.setColor(Color.RED);
        g.fillRect(apple.x * CELL_SIZE, apple.y * CELL_SIZE, CELL_SIZE, CELL_SIZE);

        // Render score
        g.setColor(Color.WHITE);
        g.drawString("Score: " + apple.score, GRID_DIM + 10, 20);
        g.drawString("Top Score: " + apple.topScore, GRID_DIM + 10, 40);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE -> quit = true;
            case KeyEvent.VK_UP -> head.dir = Direction.UP;
            case KeyEvent.VK_DOWN -> head.dir = Direction.DOWN;
            case KeyEvent.VK_LEFT -> head.dir = Direction.LEFT;
            case KeyEvent.VK_RIGHT -> head.dir = Direction.RIGHT;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("AI Snake Game");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            AISnakeGame gamePanel = new AISnakeGame();
            frame.add(gamePanel);
            frame.pack();
            frame.setVisible(true);
            frame.setLocationRelativeTo(null);
        });
    }
}

