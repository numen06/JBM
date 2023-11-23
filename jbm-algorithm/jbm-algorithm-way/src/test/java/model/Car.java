package model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Car {
    int x;
    int y;
    List<int[]> path;

    Car(int x, int y) {
        this.x = x;
        this.y = y;
        this.path = new ArrayList<>();
        this.path.add(new int[]{x, y});
    }
}
