package org.example.Render.Grid;

import org.example.Grid;
import org.example.Render.RenderStrategy;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

public class GridRenderStrategy implements RenderStrategy {
    @Override
    public void render(Grid grid, int shaderProgram, Matrix4f viewMatrix, Matrix4f projectionMatrix) {
        // Використовуємо шейдерну програму
        glUseProgram(shaderProgram);

        // Створюємо модельну матрицю (для сітки це одинична матриця)
        Matrix4f modelMatrix = new Matrix4f().identity();

        // Створюємо комбіновану матрицю MVP (Model-View-Projection)
        Matrix4f mvpMatrix = new Matrix4f();
        projectionMatrix.mul(viewMatrix, mvpMatrix);   // mvp = projection * view
        mvpMatrix.mul(modelMatrix);                    // mvp = projection * view * model

        // Передаємо MVP матрицю в шейдер (за допомогою uniform mvp)
        int mvpLoc = glGetUniformLocation(shaderProgram, "mvp");

        // Створюємо буфер для матриці MVP
        FloatBuffer mvpBuffer = MemoryUtil.memAllocFloat(16);

        try {
            // Заповнюємо буфер даними з матриці MVP
            mvpMatrix.get(mvpBuffer);

            // Передаємо матрицю MVP в шейдер
            glUniformMatrix4fv(mvpLoc, false, mvpBuffer);

            // Встановлюємо власні значення матеріалу для сітки
            int ambientLoc = glGetUniformLocation(shaderProgram, "material.ambient");
            int diffuseLoc = glGetUniformLocation(shaderProgram, "material.diffuse");
            int specularLoc = glGetUniformLocation(shaderProgram, "material.specular");
            int shininessLoc = glGetUniformLocation(shaderProgram, "material.shininess");

            // Встановлюємо значення за замовчуванням для матеріалу сітки
            glUniform3f(ambientLoc, 0.5f, 0.5f, 0.5f);
            glUniform3f(diffuseLoc, 0.5f, 0.5f, 0.5f);
            glUniform3f(specularLoc, 0.0f, 0.0f, 0.0f);
            glUniform1f(shininessLoc, 1.0f);

            // Малюємо сітку
            glBindVertexArray(grid.getGridVAO());

            // Встановлюємо колір для сітки через uniform lightColor
            int lightColorLoc = glGetUniformLocation(shaderProgram, "lightColor");
            glUniform3f(lightColorLoc, 0.3f, 0.3f, 0.3f);

            glLineWidth(1.0f);
            glDrawElements(GL_LINES, grid.getGridVertexCount(), GL_UNSIGNED_INT, 0);

            // Малюємо осі з різними кольорами
            glBindVertexArray(grid.getAxesVAO());
            glLineWidth(2.0f);

            float[] xAxisColor = grid.getxAxisColor();
            float[] yAxisColor = grid.getyAxisColor();
            float[] zAxisColor = grid.getzAxisColor();

            // Малюємо вісь X (червона)
            glUniform3f(lightColorLoc, xAxisColor[0], xAxisColor[1], xAxisColor[2]);
            glDrawArrays(GL_LINES, 0, 2);

            // Малюємо вісь Y (зелена)
            glUniform3f(lightColorLoc, yAxisColor[0], yAxisColor[1], yAxisColor[2]);
            glDrawArrays(GL_LINES, 2, 2);

            // Малюємо вісь Z (синя)
            glUniform3f(lightColorLoc, zAxisColor[0], zAxisColor[1], zAxisColor[2]);
            glDrawArrays(GL_LINES, 4, 2);

            // Відв'язуємо VAO
            glBindVertexArray(0);
        } finally {
            // Звільняємо пам'ять буфера
            MemoryUtil.memFree(mvpBuffer);
        }
    }
}

