import java.util.*;

public class SnakeGame2 {
    public static void main(String[] args) {
        int matrixSize = 20;
        int snakeX = 5;
        int snakeY = 5;
        int foodX = 10;
        int foodY = 10;
        int obstacleX = 7;
        int obstacleY = 7;
        int snakeLength = 1;
        int score = 0;
        boolean isGameRunning = true;

        while (isGameRunning) {
            printMatrix(matrixSize, snakeX, snakeY, snakeLength, foodX, foodY, obstacleX, obstacleY);
            System.out.println("Score: " + score);

            Scanner scanner = new Scanner(System.in);
            int direction = scanner.nextInt();
            switch (direction) {
                case 0: // up
                    snakeY--;
                    break;
                case 1: // right
                    snakeX++;
                    break;
                case 2: // down
                    snakeY++;
                    break;
                case 3: // left
                    snakeX--;
                    break;
            }

            if (snakeX < 0 || snakeX >= matrixSize || snakeY < 0 || snakeY >= matrixSize) {
                isGameRunning = false;
            } else if (snakeX == foodX && snakeY == foodY) {
                snakeLength++;
                foodX = (int) ((foodX + Math.random() % 2) % matrixSize);
                foodY = (int) ((foodY + Math.random() % 2) % matrixSize);
            } else if (snakeX == obstacleX && snakeY == obstacleY) {
                isGameRunning = false;
            }

            score++;
        }

        System.out.println("Game over!");
    }

    public static void printMatrix(int matrixSize, int snakeX, int snakeY, int snakeLength, int foodX, int foodY, int obstacleX, int obstacleY) {
        for (int i = 0; i < matrixSize; i++) {
            for (int j = 0; j < matrixSize; j++) {
                if (j == snakeX && i == snakeY) {
                    System.out.print(" * ");
                } else if (j == foodX && i == foodY) {
                    System.out.print(" F ");
                } else if (j == obstacleX && i == obstacleY) {
                    System.out.print(" O ");
                } else {
                    int tail = snakeLength - 1;
                    if (j == snakeX - tail && i == snakeY) {
                        System.out.print(" . ");
                    } else if (j == snakeX + tail && i == snakeY) {
                        System.out.print(" . ");
                    } else if (j == snakeX && i == snakeY - tail) {
                        System.out.print(" . ");
                    } else if (j == snakeX && i == snakeY + tail) {
                        System.out.print(" . ");
                    } else {
                        System.out.print(" # ");
                    }
                }
            }
            System.out.println();
        }
    }
}
