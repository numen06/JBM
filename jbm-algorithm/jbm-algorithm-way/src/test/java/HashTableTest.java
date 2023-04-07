
import org.junit.Test;
import java.util.Random;

public class HashTableTest {

    @Test
    public void test() {
        int[][] matrix = new int[3][3];
        Random rand = new Random();

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                matrix[i][j] = rand.nextInt(10); // generate random integer between 0 and 9
                System.out.print(matrix[i][j] + " ");
            }
            System.out.println();
        }
    }

    @Test
    public void test2() {
        AStarTest.image(new int[8][8], new Point(0, 0), "S");
    }
}
