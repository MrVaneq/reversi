import org.junit.jupiter.api.Assertions;

import java.util.Arrays;
import java.util.HashSet;

class ReversiGameFieldTest {

    @org.junit.jupiter.api.Test
    void updateAvailableCellsTest() {
        ReversiGameField testRGM = new ReversiGameField();
        Assertions.assertEquals(new HashSet<>(Arrays.asList(
                new Cell(4,2),
                new Cell(5,3),
                new Cell(2,4),
                new Cell(3,5)
                )), testRGM.availableCells);
    }


    @org.junit.jupiter.api.Test
    void readPosTest() {
        ReversiGameField testRGM = new ReversiGameField();
        Assertions.assertEquals('b', testRGM.readPos(3, 3));
        Assertions.assertEquals('w', testRGM.readPos(3, 4));
        Assertions.assertEquals('b', testRGM.readPos(4, 4));
        Assertions.assertEquals('w', testRGM.readPos(4, 3));
    }

    @org.junit.jupiter.api.Test
    void numberTest() {
        ReversiGameField testRGM = new ReversiGameField();
        Assertions.assertEquals(2, testRGM.numberOfWhite());
        Assertions.assertEquals(2, testRGM.numberOfBlack());
    }


}
