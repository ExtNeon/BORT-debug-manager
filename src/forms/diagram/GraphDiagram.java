package forms.diagram;

import forms.diagram.exceptions.EmptyListException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;

/**
 * Created by Кирилл on 08.08.2018.
 */
public class GraphDiagram implements ComponentListener {
    /*
    Так. С графиком у меня возникают трудности. Никогда раньше такого не делал.
    Что нам нужно получить: панелька, на которой рендерится поле в клетку и линия. И клетка и  линия сдвигают....
    Зачем сдвигать клетку? Клетка - единица времени по оси ординат, а по оси абсцисс - опорные значения. Двигать её не надо.
    Уже легче.
    Теперь об отображении линии. Можно сделать множество линий для одного типа данных. Это интересно и показательно.
    Что у нас можно будет посмотреть:
        * Напряжения. Шкала измерения вольты. Будет три линии, у каждой свой цвет.
        * Обороты. Всего одна линия. Шкала измерения - оборотов/мин
        * Уровень температуры
        * Уровень топлива
    Как это должно выглядеть в итоге:
        Поле с едва видимой клеткой, нулевая точка осей - левый нижний угол, так как отрицательных значений у нас нет.
        Слева около каждой горизонтальной линии у нас подписаны опорные значения.
        Снизу пока что ничего не будем писать.
        Над линией, отображаемой на графике будет её фактическое значение.
        Снизу справа будет описание и цвет каждой линии
        В правом нижнем углу будет масштаб по оси х

    Порядок отрисовки:
        1) Рисуем клетку
        2) Находим минимальное и максимальное значение в массиве, расширяем их на определённое количество единиц
        2) Разбиваем шкалу на значения и рисуем сбоку с подписью типа шкалы
        3) Рисуем каждую из линий
        4) Рисуем доп. инфу.


        Фух

     */

  /*  private void drawLine(double[] input_array, double maxInputValueToDraw, int lengthOfGraph, Graphics graphics) {

    }*/

    private static final double DIAGRAM_RESOLUTION_VERTICAL_SHIFT_MULTIPLIER = 0.1; //Коэффициент дополнительного расширения графика по вертикали относительно его максимальной и минимальной части
    private final ArrayList<GraphLine> lines = new ArrayList<>();
    private final JComponent graphComponent;
    private final Color infoBackgroundColor = new Color(128, 128, 128, 76);
    private String unitCaption = "";
    private int gridCellSize = 20;
    private int renderValuesAmount = 200;
    private int maximumDrawThreshold = -1;
    private int minimumDrawThreshold = -1;
    private Color backgroundColor;
    private Color captionColor = Color.black;
    private Color gridColor = new Color(0xCACACA);
    private boolean redrawIfResized = true;


    public GraphDiagram(JComponent graphComponent, String unitCaption, int gridCellSize, int renderValuesAmount) {
        this(graphComponent);
        this.unitCaption = unitCaption;
        this.gridCellSize = gridCellSize;
        this.renderValuesAmount = renderValuesAmount;

    }

    private GraphDiagram(JComponent graphComponent) {
        this.graphComponent = graphComponent;
        this.backgroundColor = graphComponent.getBackground();
        graphComponent.setEnabled(false);
        graphComponent.addComponentListener(this);
    }

    private static double[] castDoubleBasedArrayListToBasicArray(ArrayList<Double> input) {
        double[] result = new double[input.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = input.get(i);
        }
        return result;
    }

    private static int getYPosForValue(int fieldHeight, int gridCellSize, double maxValue, double minValue, double currentValue) {
        int bottomLine = (fieldHeight / gridCellSize - 1) * gridCellSize;
        double unitsPerPixel = Math.abs((maxValue - minValue) / (bottomLine - gridCellSize));
        return (int) ((maxValue - currentValue) / unitsPerPixel + gridCellSize);
    }

    @SuppressWarnings("SameParameterValue")
    private static double roundTo(double input, int digitsAfterPoint) {
        long decimalMLT = (long) Math.pow(10, digitsAfterPoint);
        long multipliedResult = Math.round((decimalMLT * input));
        return multipliedResult / (double) decimalMLT;
    }

    private static int getMinValueIndex(ArrayList<Double> input_array) {
        int result = 0;
        for (int i = 1; i < input_array.size(); i++) {
            if (input_array.get(i) < input_array.get(result)) {
                result = i;
            }
        }
        return result;
    }

    private static int getMaxValueIndex(ArrayList<Double> input_array) {
        int result = 0;
        for (int i = 1; i < input_array.size(); i++) {
            if (input_array.get(i) > input_array.get(result)) {
                result = i;
            }
        }
        return result;
    }

    public int getMaximumDrawThreshold() {
        return maximumDrawThreshold;
    }

    public void setMaximumDrawThreshold(int maximumDrawThreshold) {
        this.maximumDrawThreshold = maximumDrawThreshold;
    }

    public int getMinimumDrawThreshold() {
        return minimumDrawThreshold;
    }

    public void setMinimumDrawThreshold(int minimumDrawThreshold) {
        this.minimumDrawThreshold = minimumDrawThreshold;
    }

    public ArrayList<GraphLine> getLines() {
        return lines;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public Color getCaptionColor() {
        return captionColor;
    }

    public void setCaptionColor(Color captionColor) {
        this.captionColor = captionColor;
    }

    public Color getGridColor() {
        return gridColor;
    }

    public void setGridColor(Color gridColor) {
        this.gridColor = gridColor;
    }

    public String getUnitCaption() {
        return unitCaption;
    }

    public void setUnitCaption(String unitCaption) {
        this.unitCaption = unitCaption;
    }

    public int getGridCellSize() {
        return gridCellSize;
    }

    public void setGridCellSize(int gridCellSize) {
        this.gridCellSize = gridCellSize;
    }

    public int getRenderValuesAmount() {
        return renderValuesAmount;
    }

    public void setRenderValuesAmount(int renderValuesAmount) {
        this.renderValuesAmount = renderValuesAmount;
    }

    private int drawGrid(double maxValue, double minValue) {
        Graphics graphics = graphComponent.getGraphics();
        Dimension field = graphComponent.getSize();

        char measuredText[] = ((((int) maxValue) + 0.11) + unitCaption).toCharArray();
        int gridXOffset = graphics.getFontMetrics().charsWidth(measuredText, 0, measuredText.length) + 5;

        int fieldWidth = field.width;
        int fieldHeight = field.height;
        int stepsCount = fieldHeight / gridCellSize - 2;
        double stepOfMeasurement = Math.abs(maxValue - minValue) / stepsCount;

        //Clear the field
        graphics.setColor(backgroundColor);
        graphics.fillRect(0, 0, fieldWidth, fieldHeight);

        //Draw the text
        graphics.setColor(captionColor);
        int currentYPos = graphics.getFontMetrics().getAscent() / 2;
        for (int step = 0; step <= stepsCount; step++) {
            char[] text = (roundTo(maxValue - stepOfMeasurement * step, 2) + unitCaption).toCharArray();
            graphics.drawChars(text, 0, text.length, 0, currentYPos += gridCellSize);
        }

        //Draw the cross-lines
        graphics.setColor(gridColor);
        for (int xPixel = gridCellSize + gridXOffset; xPixel < fieldWidth; xPixel += gridCellSize) {
            graphics.drawLine(xPixel, 0, xPixel, fieldHeight);
        }
        for (int yPixel = gridCellSize; yPixel < fieldHeight - gridCellSize; yPixel += gridCellSize) {
            graphics.drawLine(gridXOffset, yPixel, fieldWidth, yPixel);
        }

        return gridXOffset;
    }

    public void draw() {
        if (graphComponent.isShowing()) {
            try {
                double maxVal = 0, minVal = 0;
                for (GraphLine line : lines) {
                    if (line.getValues() == null || line.getValues().size() == 0) {
                        throw new NullPointerException("Line values is null");
                    }
                    if (maximumDrawThreshold < 0) {
                        int maxValIndexForCurrentLine = getMaxValueIndex(line.getValues());
                        if (line.getValues().get(maxValIndexForCurrentLine) > maxVal) {
                            maxVal = line.getValues().get(maxValIndexForCurrentLine);
                        }
                    } else {
                        maxVal = maximumDrawThreshold;
                    }
                    if (minimumDrawThreshold < 0) {
                        int minValIndexForCurrentLine = getMinValueIndex(line.getValues());

                        if (line.getValues().get(minValIndexForCurrentLine) < minVal) {
                            minVal = line.getValues().get(minValIndexForCurrentLine);
                        }
                    } else {
                        minVal = minimumDrawThreshold;
                    }

                }
                double graphResolutionShift = DIAGRAM_RESOLUTION_VERTICAL_SHIFT_MULTIPLIER * (maxVal - minVal);
                if (graphResolutionShift == 0) { //В случае, если у нас минимум и максимум равны.
                    graphResolutionShift = 1;
                }
                maxVal += graphResolutionShift;
                minVal -= graphResolutionShift;
                int gridXOffset = drawGrid(maxVal, minVal);
                for (GraphLine currentLine : lines) {
                    drawLine(currentLine, maxVal, minVal, gridXOffset);
                }
                drawLinesInfo(gridXOffset);
            } catch (NullPointerException | EmptyListException e) {
                drawGrid(0, 0);
            }
        }
    }

    private void drawLinesInfo(int gridXOffset) throws EmptyListException {
        Graphics graphics = graphComponent.getGraphics();
        Dimension field = graphComponent.getSize();
        int maxHeight = graphics.getFontMetrics().getHeight() * (lines.size() + 1);
        int yPos = field.height - maxHeight;
        if (lines.size() == 0) {
            throw new EmptyListException("This graph has no lines");
        }
        int maxLengthTextIndex = 0;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).getName().length() > lines.get(maxLengthTextIndex).getName().length()) {
                maxLengthTextIndex = i;
            }
        }
        char measuredText[] = (" - " + lines.get(maxLengthTextIndex).getName()).toCharArray();
        int maximumTextLength = graphics.getFontMetrics().charsWidth(measuredText, 0, measuredText.length);
        int textOffset = 5;
        int maxWidth = maximumTextLength + gridCellSize * 2 + textOffset * 2;
        int infoPanelOffset = 10;
        int xPos = field.width - (maxWidth + infoPanelOffset);

        graphics.setColor(infoBackgroundColor);
        graphics.fillRect(xPos, yPos, maxWidth, maxHeight);
        int currentYPos = yPos;
        int shiftLineDownPixels = (graphics.getFontMetrics().getDescent() + graphics.getFontMetrics().getAscent()) / 2;
        for (GraphLine currentLine : lines) {
            graphics.setColor(currentLine.getColor());
            graphics.drawLine(xPos + textOffset, currentYPos + shiftLineDownPixels, xPos + gridCellSize * 2 - textOffset, currentYPos + shiftLineDownPixels);
            char text[] = (" - " + currentLine.getName()).toCharArray();
            graphics.setColor(captionColor);
            graphics.drawChars(text, 0, text.length, xPos + gridCellSize * 2, currentYPos + graphics.getFontMetrics().getAscent());
            currentYPos += graphics.getFontMetrics().getHeight();
        }
        char text[] = ("← " + ((field.width - gridXOffset) / renderValuesAmount) + " →").toCharArray();
        graphics.drawChars(text, 0, text.length, xPos + gridCellSize * 2, currentYPos + graphics.getFontMetrics().getAscent());


       /* char text[] = (" - " + currentLine.getName()).toCharArray();
        graphics.setColor(captionColor);
        graphics.drawChars(text, 0, text.length, xPos + gridCellSize * 2, currentYPos + graphics.getFontMetrics().getAscent());*/
    }

    private void drawLine(GraphLine line, double maxVal, double minVal, int gridXOffset) {
        Graphics graphics = graphComponent.getGraphics();
        Dimension field = graphComponent.getSize();

        ArrayList<Double> values = line.getValues();
        Color colorOfLine = line.getColor();
        if (line.getMaxSize() > 0 && line.getValues().size() > line.getMaxSize()) {
//            ArrayList<Double> newVal = new ArrayList<>(line.getValues().subList(line.getValues().size() - line.getMaxSize(), line.getValues().size()));
            line.getValues().clear();
            //line.getValues().addAll(newVal);
        }
        int maxFieldWidth = field.width - gridXOffset - gridCellSize;
        double pixelsForOneMeasurement = maxFieldWidth / (double) renderValuesAmount;
        double[] valuesForRender;
        if (pixelsForOneMeasurement < 1) {
            int skipN = (int) Math.floor((double) renderValuesAmount / (double) maxFieldWidth) + 1;
            valuesForRender = new double[maxFieldWidth];
            int counter = 0;
            int i;
            for (i = 0; i < values.size() && counter < valuesForRender.length; i += skipN) {
                if (i + skipN < values.size()) {
                    double valuesSum = 0;
                    for (int j = i; j < i + skipN; j++) {
                        valuesSum += values.get(j);
                    }
                    valuesForRender[counter++] = valuesSum / skipN;
                } else {
                    valuesForRender[counter++] = values.get(i);
                }
            }
            pixelsForOneMeasurement = 1;
        } else {
            if (values.size() > renderValuesAmount) {
                valuesForRender = new double[renderValuesAmount];
                int counter = 0;
                int valuesSize = values.size(); //По всей видимости, он изменяется, так что вот.
                for (int i = valuesSize - renderValuesAmount; i < valuesSize; i++) {
                    valuesForRender[counter++] = values.get(i);
                }
            } else {
                valuesForRender = castDoubleBasedArrayListToBasicArray(values);
            }
        }

        int countOfPoints = valuesForRender.length;

        int[] xCoords = new int[countOfPoints];
        int[] yCoords = new int[countOfPoints];
        int index = 0;
        int xPos;
        for (xPos = gridXOffset; xPos < field.width - 1 && index < countOfPoints; xPos += pixelsForOneMeasurement) {
            xCoords[index] = xPos;
            yCoords[index] = getYPosForValue(field.height, gridCellSize, maxVal, minVal, valuesForRender[index]);
            index++;
        }

        graphics.setColor(colorOfLine);
        graphics.drawPolyline(xCoords, yCoords, countOfPoints);
        if (countOfPoints > 0) {
            char[] text = (roundTo(valuesForRender[countOfPoints - 1], 2) + unitCaption).toCharArray();
            graphics.drawChars(text, 0, text.length, xCoords[xCoords.length - 1] - (graphics.getFontMetrics().charsWidth(text, 0, text.length) / 2), yCoords[yCoords.length - 1] - (graphics.getFontMetrics().getHeight() / 2));
        }
    }

    public boolean isRedrawIfResized() {
        return redrawIfResized;
    }

    public void setRedrawIfResized(boolean redrawIfResized) {
        this.redrawIfResized = redrawIfResized;
    }

    /**
     * Invoked when the component's size changes.
     *
     * @param e
     */
    @Override
    public void componentResized(ComponentEvent e) {
        if (redrawIfResized) {
            draw();
        }
    }

    /**
     * Invoked when the component's position changes.
     *
     * @param e
     */
    @Override
    public void componentMoved(ComponentEvent e) {
        draw();
    }

    /**
     * Invoked when the component has been made visible.
     *
     * @param e
     */
    @Override
    public void componentShown(ComponentEvent e) {
        draw();
    }

    /**
     * Invoked when the component has been made invisible.
     *
     * @param e
     */
    @Override
    public void componentHidden(ComponentEvent e) {
    }
}
