package forms.diagram;

import java.awt.*;
import java.util.ArrayList;

/**
 * Created by Кирилл on 08.08.2018.
 */
public class GraphLine {
    private Color color;
    private ArrayList<Double> values = new ArrayList<>();
    private String name;
    private int maxSize = 0;

    public GraphLine(Color color, String name) {
        this.color = color;
        this.name = name;
        char a = 3;
        int d = a;
    }

    public GraphLine(Color color, String name, int maxSize) {
        this.color = color;
        this.name = name;
        this.maxSize = maxSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public ArrayList<Double> getValues() {
        return values;
    }

    public void setValues(ArrayList<Double> values) {
        this.values = values;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
