package suanfa;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AStarPathPlanning {

    private static int size = 10;

    public static void main(String[] args) {

        int[][] grid = generateGrid(size);
        int startX = (int) (Math.random() * size);
        int startY = (int) (Math.random() * size);
        int endX = (int) (Math.random() * size);
        int endY = (int) (Math.random() * size);
        List<Point> path = findPath(grid, startX, startY, endX, endY);
        for (Point point : path) {
            System.out.println("(" + point.x + "," + point.y + ")");
        }
    }

    public static int[][] generateGrid(int size) {
        int[][] grid = new int[size][size];
        Random random = new Random();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                grid[i][j] = random.nextInt(2);
            }
        }
        return grid;
    }

    public static List<Point> findPath(int[][] grid, int startX, int startY, int endX, int endY) {
        List<Point> openList = new ArrayList<>();
        List<Point> closedList = new ArrayList<>();
        openList.add(new Point(startX, startY));
        int[][] cameFrom = new int[grid.length][grid[0].length];
        int[][] gScore = new int[grid.length][grid[0].length];
        int[][] fScore = new int[grid.length][grid[0].length];
        for (int i = 0; i < gScore.length; i++) {
            for (int j = 0; j < gScore[0].length; j++) {
                gScore[i][j] = Integer.MAX_VALUE;
                fScore[i][j] = Integer.MAX_VALUE;
            }
        }
        fScore[startX][startY] = 0;

        while (!openList.isEmpty()) {
            Point current = findMinimumf(openList, fScore);
            if (current.x == endX && current.y == endY) {
                break;
            }
            openList.remove(current);
            closedList.add(current);

            for (Point neighbor : getNeighbors(grid, current.x, current.y)) {
                int newG = gScore[current.x][current.y] + 1;
                if (isObstacle(grid, neighbor.x, neighbor.y)) {
                    newG = Integer.MAX_VALUE;
                }
                if (inBounds(neighbor.x, neighbor.y, grid.length, grid[0].length) && (!closedList.contains(neighbor) || newG < gScore[neighbor.x][neighbor.y])) {
                    cameFrom[neighbor.x][neighbor.y] = current.x * size + current.y;
                    gScore[neighbor.x][neighbor.y] = newG;
                    fScore[neighbor.x][neighbor.y] = newG + heuristic(neighbor.x, neighbor.y, endX, endY);
                    if (!openList.contains(neighbor)) {
                        openList.add(neighbor);
                    }
                }
            }
        }

        List<Point> path = new ArrayList<>();
        Point current = new Point(endX, endY);
        while (current.x != startX || current.y != startY) {
            path.add(current);
            current = new Point((int) ((current.x - cameFrom[current.x][current.y]) / size) + cameFrom[current.x][current.y] / size, (int) (current.y - cameFrom[current.x][current.y] % size) + cameFrom[current.x][current.y] % size);
        }
        path.add(new Point(startX, startY));
        return path;
    }

    public static Point findMinimumf(List<Point> openList, int[][] fScore) {
        Point minimum = openList.get(0);
        for (Point current : openList) {
            if (fScore[current.x][current.y] < fScore[minimum.x][minimum.y]) {
                minimum = current;
            }
        }
        return minimum;
    }

    public static List<Point> getNeighbors(int[][] grid, int x, int y) {
        List<Point> neighbors = new ArrayList<>();
        for (Point neighbor : new Point[]{new Point(x - 1, y), new Point(x + 1, y), new Point(x, y - 1), new Point(x, y + 1)}) {
            if (inBounds(neighbor.x, neighbor.y, grid.length, grid[0].length) && grid[neighbor.x][neighbor.y] != 1 && !isObstacle(grid, neighbor.x, neighbor.y)) {
                neighbors.add(neighbor);
            }
        }
        return neighbors;
    }

    public static boolean inBounds(int x, int y, int rows, int columns) {
        return x >= 0 && x < rows && y >= 0 && y < columns;
    }

    public static boolean isObstacle(int[][] grid, int x, int y) {
        return grid[x][y] == 1;
    }

    public static int heuristic(int x1, int y1, int x2, int y2) {
        return Math.abs(x2 - x1) + Math.abs(y2 - y1);
    }

    public static class Point {
        int x;
        int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "(" + x + "," + y + ")";
        }
    }
}
