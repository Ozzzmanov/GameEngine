package org.example;

import static org.lwjgl.opengl.GL11.*;

/**
 * Клас для відображення координатної сітки в 3D просторі
 * Сітка відображається тільки на площині XZ (по координаті Z),
 * а по XY відображаються тільки вісі
 */
public class Grid {
    // Налаштування сітки
    private float gridSize = 0.1f;        // Відстань між лініями сітки
    private float gridExtent = 10.0f;     // Розмір сітки в обидва боки від початку координат

    // Кольори осей
    private float[] xAxisColor = {1.0f, 0.2f, 0.2f};  // Червоний для осі X
    private float[] yAxisColor = {0.2f, 1.0f, 0.2f};  // Зелений для осі Y
    private float[] zAxisColor = {0.2f, 0.2f, 1.0f};  // Синій для осі Z

    // Налаштування ліній
    private float majorLineWidth = 1.5f;  // Ширина основних ліній
    private float minorLineWidth = 0.8f;  // Ширина другорядних ліній
    private float axisLineWidth = 2.0f;   // Ширина осей

    // Кольори ліній сітки (тільки для Z площини)
    private float[] majorGridColor = {0.5f, 0.5f, 0.5f};  // Колір основних ліній сітки
    private float[] minorGridColor = {0.3f, 0.3f, 0.3f};  // Колір другорядних ліній сітки

    // Опції відображення
    private boolean showLabels = true;     // Показувати мітки на осях

    /**
     * Малювання 3D сітки
     */
    public void draw() {
        // Відображаємо сітку тільки по осі Z (площина XZ)
        drawZGrid();

        // Відображаємо осі координат
        drawAxes();
    }

    /**
     * Малювання сітки на площині XZ (по Z координаті)
     */
    private void drawZGrid() {
        // Малювання другорядних ліній сітки
        drawZGridLines(gridSize, minorGridColor, minorLineWidth);

        // Малювання основних ліній сітки (кожні 5 другорядних ліній)
        drawZGridLines(gridSize * 5, majorGridColor, majorLineWidth);
    }

    /**
     * Малювання ліній сітки на площині XZ
     *
     * @param spacing відстань між лініями
     * @param color колір ліній
     * @param lineWidth товщина ліній
     */
    private void drawZGridLines(float spacing, float[] color, float lineWidth) {
        glLineWidth(lineWidth);
        glBegin(GL_LINES);
        glColor3f(color[0], color[1], color[2]);

        // Лінії вздовж осі X (паралельні осі X)
        for (float z = -gridExtent; z <= gridExtent; z += spacing) {
            // Пропуск лінії осі, оскільки вона буде намальована окремо
            if (Math.abs(z) < 0.001f) continue;

            glVertex3f(-gridExtent, 0.0f, z);
            glVertex3f(gridExtent, 0.0f, z);
        }

        // Лінії вздовж осі Z (паралельні осі Z)
        for (float x = -gridExtent; x <= gridExtent; x += spacing) {
            // Пропуск лінії осі
            if (Math.abs(x) < 0.001f) continue;

            glVertex3f(x, 0.0f, -gridExtent);
            glVertex3f(x, 0.0f, gridExtent);
        }

        glEnd();
    }

    /**
     * Малювання осей координат (тільки X та Y)
     */
    private void drawAxes() {
        // Малювання осі X (червона)
        glLineWidth(axisLineWidth);
        glBegin(GL_LINES);
        glColor3f(xAxisColor[0], xAxisColor[1], xAxisColor[2]);
        glVertex3f(-gridExtent, 0.0f, 0.0f);
        glVertex3f(gridExtent, 0.0f, 0.0f);
        glEnd();

        // Малювання осі Y (зелена)
        glLineWidth(axisLineWidth);
        glBegin(GL_LINES);
        glColor3f(yAxisColor[0], yAxisColor[1], yAxisColor[2]);
        glVertex3f(0.0f, -gridExtent, 0.0f);
        glVertex3f(0.0f, gridExtent, 0.0f);
        glEnd();

        // Малювання осі Z (синя)
        glLineWidth(axisLineWidth);
        glBegin(GL_LINES);
        glColor3f(zAxisColor[0], zAxisColor[1], zAxisColor[2]);
        glVertex3f(0.0f, 0.0f, -gridExtent);
        glVertex3f(0.0f, 0.0f, gridExtent);
        glEnd();

        // Малювання міток на осях
        if (showLabels) {
            drawAxisMarkers();
        }
    }

    /**
     * Малювання міток на осях
     */
    private void drawAxisMarkers() {
        float markerSize = 0.05f;
        float markerStep = gridSize * 5;  // Крок міток (кожні 5 ліній сітки)

        // Мітки на осі X
        for (float x = markerStep; x <= gridExtent; x += markerStep) {
            glLineWidth(majorLineWidth);

            // Мітка по осі X
            glBegin(GL_LINES);
            glColor3f(xAxisColor[0], xAxisColor[1], xAxisColor[2]);
            glVertex3f(x, 0.0f, -markerSize);
            glVertex3f(x, 0.0f, markerSize);
            glEnd();
        }

        // Мітки на осі Y
        for (float y = markerStep; y <= gridExtent; y += markerStep) {
            glLineWidth(majorLineWidth);

            // Мітка по осі Y
            glBegin(GL_LINES);
            glColor3f(yAxisColor[0], yAxisColor[1], yAxisColor[2]);
            glVertex3f(-markerSize, y, 0.0f);
            glVertex3f(markerSize, y, 0.0f);
            glEnd();
        }

        // Мітки на осі Z
        for (float z = markerStep; z <= gridExtent; z += markerStep) {
            glLineWidth(majorLineWidth);

            // Мітка по осі Z
            glBegin(GL_LINES);
            glColor3f(zAxisColor[0], zAxisColor[1], zAxisColor[2]);
            glVertex3f(-markerSize, 0.0f, z);
            glVertex3f(markerSize, 0.0f, z);
            glEnd();
        }
    }

    // Геттери та сеттери
    public float getGridSize() {
        return gridSize;
    }

    public void setGridSize(float gridSize) {
        this.gridSize = gridSize;
    }

    public float getGridExtent() {
        return gridExtent;
    }

    public void setGridExtent(float gridExtent) {
        this.gridExtent = gridExtent;
    }

    public boolean isShowLabels() {
        return showLabels;
    }

    public void setShowLabels(boolean showLabels) {
        this.showLabels = showLabels;
    }
}