package org.example;

import org.example.Render.DefaultRenderStrategy;
import org.example.Render.Grid.GridRenderStrategy;
import org.example.Render.RenderStrategy;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Клас для відображення координатної сітки в 3D просторі використовуючи VAO/VBO/EBO
 * Сітка відображається на площині XZ, а по XY відображаються тільки вісі
 */
public class Grid {
    // Налаштування сітки
    private float gridSize = 0.1f;        // Відстань між лініями сітки
    private float gridExtent = 10.0f;     // Розмір сітки в обидва боки від початку координат

    // Кольори осей
    private float[] xAxisColor = {1.0f, 0.2f, 0.2f, 1.0f};  // Червоний для осі X
    private float[] yAxisColor = {0.2f, 1.0f, 0.2f, 1.0f};  // Зелений для осі Y
    private float[] zAxisColor = {0.2f, 0.2f, 1.0f, 1.0f};  // Синій для осі Z

    // Кольори ліній сітки
    private float[] majorGridColor = {0.5f, 0.5f, 0.5f, 1.0f};  // Колір основних ліній сітки
    private float[] minorGridColor = {0.3f, 0.3f, 0.3f, 1.0f};  // Колір другорядних ліній сітки

    // VAO та VBO для сітки
    private int gridVAO;
    private int gridVBO;
    private int gridEBO;
    private int gridVertexCount;

    // VAO та VBO для осей
    private int axesVAO;
    private int axesVBO;
    private int axesVertexCount;

    // Чи ініціалізовано буфери
    private boolean initialized = false;

    private RenderStrategy renderStrategy;

    public Grid() {
        this.renderStrategy = new GridRenderStrategy();
    }

    public void init() {
        if (initialized) {
            return;
        }

        // Створюємо дані для сітки
        createGridBuffers();

        // Створюємо дані для осей
        createAxesBuffers();

        initialized = true;
    }

    /**
     * Створення та заповнення буферів для сітки
     */
    private void createGridBuffers() {
        // Розрахунок кількості ліній в обох напрямках
        int linesCount = (int)(gridExtent * 2 / gridSize) + 1;

        // Створюємо буфери для вершин
        FloatBuffer verticesBuffer = null;
        IntBuffer indicesBuffer = null;

        try {
            // Вершини містять позицію (xyz) та колір (rgba)
            int verticesSize = linesCount * 4 * 7; // 4 вершини на лінію, 7 елементів на вершину
            verticesBuffer = MemoryUtil.memAllocFloat(verticesSize);

            int vertexCount = 0;

            // Створюємо вершини для ліній сітки
            for (float x = -gridExtent; x <= gridExtent; x += gridSize) {
                // Пропускаємо лінію осі X
                if (Math.abs(x) < 0.001f) continue;

                boolean isMajor = Math.abs(x % (gridSize * 5)) < 0.001f;
                float[] color = isMajor ? majorGridColor : minorGridColor;

                // Лінія вздовж Z
                addVertex(verticesBuffer, x, 0.0f, -gridExtent, color);
                addVertex(verticesBuffer, x, 0.0f, gridExtent, color);
                vertexCount += 2;
            }

            for (float z = -gridExtent; z <= gridExtent; z += gridSize) {
                // Пропускаємо лінію осі Z
                if (Math.abs(z) < 0.001f) continue;

                boolean isMajor = Math.abs(z % (gridSize * 5)) < 0.001f;
                float[] color = isMajor ? majorGridColor : minorGridColor;

                // Лінія вздовж X
                addVertex(verticesBuffer, -gridExtent, 0.0f, z, color);
                addVertex(verticesBuffer, gridExtent, 0.0f, z, color);
                vertexCount += 2;
            }

            gridVertexCount = vertexCount;

            // Створюємо індекси (для GL_LINES кожна пара індексів створює лінію)
            indicesBuffer = MemoryUtil.memAllocInt(vertexCount);
            for (int i = 0; i < vertexCount; i++) {
                indicesBuffer.put(i);
            }

            verticesBuffer.flip();
            indicesBuffer.flip();

            // Створюємо об'єкти VAO, VBO та EBO
            gridVAO = glGenVertexArrays();
            gridVBO = glGenBuffers();
            gridEBO = glGenBuffers();

            // Прив'язуємо VAO
            glBindVertexArray(gridVAO);

            // Прив'язуємо та заповнюємо VBO
            glBindBuffer(GL_ARRAY_BUFFER, gridVBO);
            glBufferData(GL_ARRAY_BUFFER, verticesBuffer, GL_STATIC_DRAW);

            // Прив'язуємо та заповнюємо EBO
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, gridEBO);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);

            // Налаштовуємо атрибути вершин
            // Позиція (xyz)
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 7 * Float.BYTES, 0);
            glEnableVertexAttribArray(0);

            // Колір (rgba)
            glVertexAttribPointer(1, 4, GL_FLOAT, false, 7 * Float.BYTES, 3 * Float.BYTES);
            glEnableVertexAttribArray(1);

            // Відв'язуємо VAO
            glBindVertexArray(0);
        } finally {
            // Звільняємо пам'ять буферів
            if (verticesBuffer != null) {
                MemoryUtil.memFree(verticesBuffer);
            }
            if (indicesBuffer != null) {
                MemoryUtil.memFree(indicesBuffer);
            }
        }
    }

    /**
     * Створення та заповнення буферів для осей координат
     */
    private void createAxesBuffers() {
        FloatBuffer verticesBuffer = null;

        try {
            // 6 вершин (2 для кожної осі), 7 елементів на вершину (xyz + rgba)
            verticesBuffer = MemoryUtil.memAllocFloat(6 * 7);

            // Вісь X (червона)
            addVertex(verticesBuffer, -gridExtent, 0.0f, 0.0f, xAxisColor);
            addVertex(verticesBuffer, gridExtent, 0.0f, 0.0f, xAxisColor);

            // Вісь Y (зелена)
            addVertex(verticesBuffer, 0.0f, -gridExtent, 0.0f, yAxisColor);
            addVertex(verticesBuffer, 0.0f, gridExtent, 0.0f, yAxisColor);

            // Вісь Z (синя)
            addVertex(verticesBuffer, 0.0f, 0.0f, -gridExtent, zAxisColor);
            addVertex(verticesBuffer, 0.0f, 0.0f, gridExtent, zAxisColor);

            axesVertexCount = 6;

            verticesBuffer.flip();

            // Створюємо об'єкти VAO та VBO
            axesVAO = glGenVertexArrays();
            axesVBO = glGenBuffers();

            // Прив'язуємо VAO
            glBindVertexArray(axesVAO);

            // Прив'язуємо та заповнюємо VBO
            glBindBuffer(GL_ARRAY_BUFFER, axesVBO);
            glBufferData(GL_ARRAY_BUFFER, verticesBuffer, GL_STATIC_DRAW);

            // Налаштовуємо атрибути вершин
            // Позиція (xyz)
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 7 * Float.BYTES, 0);
            glEnableVertexAttribArray(0);

            // Колір (rgba)
            glVertexAttribPointer(1, 4, GL_FLOAT, false, 7 * Float.BYTES, 3 * Float.BYTES);
            glEnableVertexAttribArray(1);

            // Відв'язуємо VAO
            glBindVertexArray(0);
        } finally {
            // Звільняємо пам'ять буфера
            if (verticesBuffer != null) {
                MemoryUtil.memFree(verticesBuffer);
            }
        }
    }

    /**
     * Додавання вершини до буфера
     *
     * @param buffer буфер для додавання вершини
     * @param x координата x
     * @param y координата y
     * @param z координата z
     * @param color колір вершини (rgba)
     */
    private void addVertex(FloatBuffer buffer, float x, float y, float z, float[] color) {
        buffer.put(x).put(y).put(z);
        buffer.put(color[0]).put(color[1]).put(color[2]).put(color[3]);
    }

    /**
     * Малювання сітки та осей з урахуванням специфіки шейдерів
     *
     * @param shaderProgram ідентифікатор шейдерної програми
     * @param viewMatrix матриця виду
     * @param projectionMatrix матриця проекції
     */
    public void render(int shaderProgram, Matrix4f viewMatrix, Matrix4f projectionMatrix) {
        if (!initialized) {
            init();
        }
        renderStrategy.render(this, shaderProgram, viewMatrix, projectionMatrix);
    }
//    public void render(int shaderProgram, Matrix4f viewMatrix, Matrix4f projectionMatrix) {
//        if (!initialized) {
//            init();
//        }
//
//        // Використовуємо шейдерну програму
//        glUseProgram(shaderProgram);
//
//        // Створюємо модельну матрицю (для сітки це одинична матриця)
//        Matrix4f modelMatrix = new Matrix4f().identity();
//
//        // Створюємо комбіновану матрицю MVP (Model-View-Projection)
//        Matrix4f mvpMatrix = new Matrix4f();
//        projectionMatrix.mul(viewMatrix, mvpMatrix);   // mvp = projection * view
//        mvpMatrix.mul(modelMatrix);                    // mvp = projection * view * model
//
//        // Передаємо MVP матрицю в шейдер (за допомогою uniform mvp)
//        int mvpLoc = glGetUniformLocation(shaderProgram, "mvp");
//
//        // Створюємо буфер для матриці MVP
//        FloatBuffer mvpBuffer = MemoryUtil.memAllocFloat(16);
//
//        try {
//            // Заповнюємо буфер даними з матриці MVP
//            mvpMatrix.get(mvpBuffer);
//
//            // Передаємо матрицю MVP в шейдер
//            glUniformMatrix4fv(mvpLoc, false, mvpBuffer);
//
//            // Встановлюємо власні значення матеріалу для сітки
//            int ambientLoc = glGetUniformLocation(shaderProgram, "material.ambient");
//            int diffuseLoc = glGetUniformLocation(shaderProgram, "material.diffuse");
//            int specularLoc = glGetUniformLocation(shaderProgram, "material.specular");
//            int shininessLoc = glGetUniformLocation(shaderProgram, "material.shininess");
//
//            // Встановлюємо значення за замовчуванням для матеріалу сітки
//            glUniform3f(ambientLoc, 0.5f, 0.5f, 0.5f);
//            glUniform3f(diffuseLoc, 0.5f, 0.5f, 0.5f);
//            glUniform3f(specularLoc, 0.0f, 0.0f, 0.0f);
//            glUniform1f(shininessLoc, 1.0f);
//
//            // Малюємо сітку
//            glBindVertexArray(gridVAO);
//
//            // Встановлюємо колір для сітки через uniform lightColor
//            int lightColorLoc = glGetUniformLocation(shaderProgram, "lightColor");
//            glUniform3f(lightColorLoc, 0.3f, 0.3f, 0.3f);
//
//
//            glLineWidth(1.0f);
//            glDrawElements(GL_LINES, gridVertexCount, GL_UNSIGNED_INT, 0);
//
//            // Малюємо осі з різними кольорами
//            glBindVertexArray(axesVAO);
//            glLineWidth(2.0f);
//
//            // Малюємо вісь X (червона)
//            glUniform3f(lightColorLoc, xAxisColor[0], xAxisColor[1], xAxisColor[2]);
//            glDrawArrays(GL_LINES, 0, 2);
//
//            // Малюємо вісь Y (зелена)
//            glUniform3f(lightColorLoc, yAxisColor[0], yAxisColor[1], yAxisColor[2]);
//            glDrawArrays(GL_LINES, 2, 2);
//
//            // Малюємо вісь Z (синя)
//            glUniform3f(lightColorLoc, zAxisColor[0], zAxisColor[1], zAxisColor[2]);
//            glDrawArrays(GL_LINES, 4, 2);
//
//            // Відв'язуємо VAO
//            glBindVertexArray(0);
//        } finally {
//            // Звільняємо пам'ять буфера
//            MemoryUtil.memFree(mvpBuffer);
//        }
//    }

    /**
     * Очищення ресурсів OpenGL
     */
    public void cleanup() {
        if (initialized) {
            // Видаляємо VAO, VBO та EBO для сітки
            glDeleteVertexArrays(gridVAO);
            glDeleteBuffers(gridVBO);
            glDeleteBuffers(gridEBO);

            // Видаляємо VAO та VBO для осей
            glDeleteVertexArrays(axesVAO);
            glDeleteBuffers(axesVBO);

            initialized = false;
        }
    }

    // Геттери та сеттери
    public float getGridSize() {
        return gridSize;
    }

    public void setGridSize(float gridSize) {
        this.gridSize = gridSize;
        initialized = false; // Потрібна реініціалізація
    }

    public float getGridExtent() {
        return gridExtent;
    }

    public void setGridExtent(float gridExtent) {
        this.gridExtent = gridExtent;
        initialized = false; // Потрібна реініціалізація
    }

    public int getGridVAO() {
        return gridVAO;
    }

    public int getGridVertexCount() {
        return gridVertexCount;
    }

    public int getAxesVAO() {
        return axesVAO;
    }

    public float[] getxAxisColor() {
        return xAxisColor;
    }

    public float[] getyAxisColor() {
        return yAxisColor;
    }

    public float[] getzAxisColor() {
        return zAxisColor;
    }

    public boolean isInitialized() {
        return initialized;
    }
}