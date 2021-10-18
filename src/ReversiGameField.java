import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReversiGameField {
    private final char[][] field = new char[8][8]; // b - black, w - white, ' ' - empty
    public Set<Cell> availableCells = Collections.synchronizedSet(new HashSet<>());
    private final AtomicBoolean fieldIsAvailable = new AtomicBoolean(true);
    private static final DirectionStepper toRight = source -> new Cell(source.getX() + 1, source.getY());
    private static final DirectionStepper toLeft = source -> new Cell(source.getX() - 1, source.getY());
    private static final DirectionStepper toDown = source -> new Cell(source.getX(), source.getY() + 1);
    private static final DirectionStepper toUp = source -> new Cell(source.getX(), source.getY() - 1);
    private static final DirectionStepper d1 = source -> new Cell(source.getX() + 1, source.getY() + 1);
    private static final DirectionStepper d2 = source -> new Cell(source.getX() + 1, source.getY() - 1);
    private static final DirectionStepper d3 = source -> new Cell(source.getX() - 1, source.getY() - 1);
    private static final DirectionStepper d4 = source -> new Cell(source.getX() - 1, source.getY() + 1);

    public AtomicBoolean isSkipped = new AtomicBoolean(false);

    boolean isAvailable() {
        return fieldIsAvailable.get();
    }

    public ReversiGameField(ReversiGameField other) {
        clear();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                this.writeChar(other.readPos(i, j), i, j);
            }
        }
        this.availableCells = other.availableCells;
    }

    public ReversiGameField() {
        clear();
        writeBlack(3, 3);
        writeWhite(4, 3);
        writeWhite(3, 4);
        writeBlack(4, 4);
        updateAvailableCells('w');
    }

    private void clear() {
        for (int i = 0; i < 8 * 8; i++) {
            field[i / 8][i % 8] = ' ';
        }
    }

    private void writeBlack(int x, int y) {
        field[y][x] = 'b';
    }

    boolean setBlack(int x, int y) {
        if (availableCells.contains(new Cell(x, y)) && fieldIsAvailable.get()) {
            checkField('b', new Cell(x, y));
            updateAvailableCells('b');
            fieldIsAvailable.set(false);
            new Thread(this::botStepAct).start();
            return true;
        }
        return false;
    }

    void writeWhite(int x, int y) {
        field[y][x] = 'w';
    }

    public char[][] getFieldValue() {
        return field.clone();
    }

    public void updateAvailableCells(char other) {
        HashSet<Cell> preResult = new HashSet<>();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (field[i][j] == ' ' && ((j != 7 && field[i][j + 1] != ' ') || (j != 0 && field[i][j - 1] != ' ')
                        || (i != 7 && field[i + 1][j] != ' ') || (i != 0 && field[i - 1][j] != ' '))) {
                    preResult.add(new Cell(j, i));
                }
            }
        }

        char initiator;
        Integer currCount;
        if (other == 'b') {
            initiator = 'w';
            currCount = this.numberOfBlack();
        } else {
            initiator = 'b';
            currCount = this.numberOfWhite();

        }
        HashSet<Cell> result = new HashSet<>();

        List<ReversiGameField> nextStates = new ArrayList<>();


        preResult.forEach(it -> {
            ReversiGameField nextState = new ReversiGameField(this);
            nextState.checkField(initiator, it);
            nextStates.add(nextState);
            if (other == 'w' && nextState.numberOfWhite() < currCount) {
                result.add(it);
            }
            else if(other == 'b' && nextState.numberOfBlack() < currCount){
                result.add(it);
            }
        });


        this.availableCells = result;
    }

    void botStepAct() {
        if (availableCells.size() != 0) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            List<ReversiGameField> nextStates = new ArrayList<>();
            List<Cell> actions = new ArrayList<>();
            List<Integer> prices = new ArrayList<>();
            availableCells.forEach(it -> {
                ReversiGameField nextState = new ReversiGameField(this);
                nextState.checkField('w', it);
                nextStates.add(nextState);
                actions.add(it);
                prices.add(nextState.numberOfWhite());
            });
            int maxPrice = 0;
            for (Integer price : prices) {
                if (price > maxPrice)
                    maxPrice = price;
            }
            //выполняем над текущим полем самое эффективное действие
            checkField('w', actions.get(prices.indexOf(maxPrice)));
            updateAvailableCells('w');
            if (this.availableCells.size() == 0) {
                updateAvailableCells('b');
                botStepAct();
            }
            isSkipped.set(false);
        } else {
            updateAvailableCells('w');
            isSkipped.set(true);
        }
        fieldIsAvailable.set(true);
    }

    void checkField(char initiator, Cell step) {
        char other;
        if (initiator == 'b') {
            writeBlack(step.getX(), step.getY());
            other = 'w';
        } else {
            writeWhite(step.getX(), step.getY());
            other = 'b';
        }

        checkDir(toRight, initiator, other, step);
        checkDir(toLeft, initiator, other, step);
        checkDir(toDown, initiator, other, step);
        checkDir(toUp, initiator, other, step);
        checkDir(d1, initiator, other, step);
        checkDir(d2, initiator, other, step);
        checkDir(d3, initiator, other, step);
        checkDir(d4, initiator, other, step);
    }

    char readPos(int x, int y) {
        return field[y][x];
    }

    private void writeChar(char value, int x, int y) {
        field[y][x] = value;
    }

    private void checkDir(DirectionStepper stepper, char initiator, char other, Cell source) {
        Cell curr = new Cell(source.getX(), source.getY());
        do {
            curr = stepper.doStep(curr);
        } while (curr.getY() < 8 && curr.getY() > -1 && curr.getX() < 8 && curr.getX() > -1 &&
                readPos(curr.getX(), curr.getY()) == other);

        if (curr.getY() <= 7 && curr.getY() >= 0 && curr.getX() <= 7 && curr.getX() >= 0 &&
                readPos(curr.getX(), curr.getY()) == initiator) {
            Cell curr2 = new Cell(source.getX(), source.getY());
            while (!curr2.equals(curr)) {
                writeChar(initiator, curr2.getX(), curr2.getY());
                curr2 = stepper.doStep(curr2);
            }
        }
    }

    int numberOfWhite() {
        int result = 0;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (readPos(i, j) == 'w')
                    result++;
            }
        }
        return result;
    }

    int numberOfBlack() {
        int result = 0;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (readPos(i, j) == 'b')
                    result++;
            }
        }
        return result;
    }
}


interface DirectionStepper {
    Cell doStep(Cell source);
}

