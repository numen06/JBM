import java.util.Scanner;

public class SnakesGame {
    public static void main(String[] args) {
        int[][] matrix = new int[10][10];
        int snakeX = 5, snakeY = 5; // 蛇的初始位置
        int foodX = 0, foodY = 0; // 食物的初始位置
        int snakeLength = 1; // 蛇的长度
        int score = 0; // 分数
        boolean isGameRunning = true; // 游戏是否在运行

        Scanner scanner = new Scanner(System.in);
        while (isGameRunning) {
            System.out.println("**********");
            printMatrix(matrix, snakeX, snakeY, snakeLength, foodX, foodY);
            System.out.println("Score: " + score);
            System.out.print("Enter your choice (up/down/left/right): ");
            String direction = scanner.nextLine();

            // 根据玩家的选择，更新蛇的位置
            if (direction.equals("up")) {
                snakeY--;
            } else if (direction.equals("down")) {
                snakeY++;
            } else if (direction.equals("left")) {
                snakeX--;
            } else if (direction.equals("right")) {
                snakeX++;
            }

            // 检查蛇是否与边界碰撞
            if (snakeX < 0 || snakeX >= 10 || snakeY < 0 || snakeY >= 10) {
                isGameRunning = false;
            }

            // 检查蛇是否撞到食物
            if (snakeX == foodX && snakeY == foodY) {
                snakeLength++;
                foodX = (int) ((foodX + Math.random() % 2) % 10);
                foodY = (int) ((foodY + Math.random() % 2) % 10);
            }

            // 检查蛇是否撞到自己的身体
            for (int i = 1; i < snakeLength; i++) {
                if (snakeX == matrix[i][snakeY] && snakeY == matrix[i][foodY]) {
                    isGameRunning = false;
                    break;
                }
            }

            // 更新蛇的位置
            matrix[snakeX][snakeY] = snakeLength;

            // 清除掉旧的蛇身体部分
            for (int i = 1; i < snakeLength; i++) {
                matrix[snakeX - i][snakeY] = 0;
                matrix[snakeX + i][snakeY] = 0;
                matrix[snakeX][snakeY - i] = 0;
                matrix[snakeX][snakeY + i] = 0;
            }

            try {
                Thread.sleep(500); // 控制游戏速度
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Game over!");
    }

    public static void printMatrix(int[][] matrix, int snakeX, int snakeY, int snakeLength, int foodX, int foodY) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                if (matrix[i][j] == 0) {
                    System.out.print("+");
                } else if (i == snakeX && j == snakeY) {
                    System.out.print("* ");
                } else if (i == foodX && j == foodY) {
                    System.out.print("Food ");
                } else {
                    int tail = snakeLength - 1;
                    if (i == snakeX - tail && j == snakeY) {
                        System.out.print(".Head ");
                    } else if (i == snakeX + tail && j == snakeY) {
                        System.out.print(".Tail ");
                    } else if (i == snakeX && j == snakeY - tail) {
                        System.out.print("^ . ");
                    } else if (i == snakeX && j == snakeY + tail) {
                        System.out.print("v . ");
                    } else {
                        System.out.print(". ");
                    }
                }
            }
            System.out.println();
        }
    }
}
