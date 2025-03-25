package org.example.Render;

import org.example.Mesh;
import org.example.Node;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

import static org.lwjgl.opengl.GL30.*;

public class DefaultRenderStrategy implements RenderStrategy {
    @Override
    public void render(Mesh mesh, int shaderProgram, Matrix4f viewMatrix, Matrix4f projectionMatrix,
                       Vector3f cameraPosition, List<Node> lightNodes) {
        glUseProgram(shaderProgram);
        glBindVertexArray(mesh.getVaoID());

        // Налаштування джерел світла
        if (!lightNodes.isEmpty()) {
            Node lightNode = lightNodes.get(0);
            Vector3f lightPos = lightNode.getPosition();
            int lightPosLoc = glGetUniformLocation(shaderProgram, "lightPos");
            glUniform3f(lightPosLoc, lightPos.x, lightPos.y, lightPos.z);

            Vector3f lightColor = lightNode.getLightColor();
            int lightColorLoc = glGetUniformLocation(shaderProgram, "lightColor");
            glUniform3f(lightColorLoc, lightColor.x, lightColor.y, lightColor.z);

            // Передача інтенсивності
            int lightIntensityLoc = glGetUniformLocation(shaderProgram, "lightIntensity");
            if (lightIntensityLoc != -1) {
                glUniform1f(lightIntensityLoc, lightNode.getLightIntensity());
            }
        } else {
            // Значення за замовчуванням
            int lightPosLoc = glGetUniformLocation(shaderProgram, "lightPos");
            glUniform3f(lightPosLoc, 5.0f, 5.0f, 5.0f);

            int lightColorLoc = glGetUniformLocation(shaderProgram, "lightColor");
            glUniform3f(lightColorLoc, 1.0f, 1.0f, 1.0f);
        }

        // Отримання локації для теневой карты
        int shadowMapLoc = glGetUniformLocation(shaderProgram, "shadowMap");
        if (shadowMapLoc != -1) {
            glUniform1i(shadowMapLoc, 1);  // Текстурний блок 1
        }

        // Локація для матриці простору світла
        int lightSpaceMatrixLoc = glGetUniformLocation(shaderProgram, "lightSpaceMatrix");
        if (lightSpaceMatrixLoc != -1) {
            // Матриця буде встановлена ззовні
        }

        // MVP матриця та інші розрахунки
        int mvpLoc = glGetUniformLocation(shaderProgram, "mvp");
        int modelLoc = glGetUniformLocation(shaderProgram, "model");
        int normalMatrixLoc = glGetUniformLocation(shaderProgram, "normalMatrix");

        // Застосовуємо матеріал
        mesh.getShaderMaterial().apply(shaderProgram);

        // Позиція камери для бликів
        int viewPosLoc = glGetUniformLocation(shaderProgram, "viewPos");
        glUniform3f(viewPosLoc, cameraPosition.x, cameraPosition.y, cameraPosition.z);

        // Обчислення матриць
        Matrix4f mvpMatrix = new Matrix4f();
        projectionMatrix.mul(viewMatrix, mvpMatrix);
        mvpMatrix.mul(mesh.getModelMatrix());

        // Матриця нормалей
        Matrix4f normalMatrix = new Matrix4f(mesh.getModelMatrix());
        normalMatrix.invert().transpose();

        // Передача матриць у шейдер
        float[] mvpBuffer = new float[16];
        mvpMatrix.get(mvpBuffer);
        glUniformMatrix4fv(mvpLoc, false, mvpBuffer);

        float[] modelBuffer = new float[16];
        mesh.getModelMatrix().get(modelBuffer);
        glUniformMatrix4fv(modelLoc, false, modelBuffer);

        float[] normalMatrixBuffer = new float[16];
        normalMatrix.get(normalMatrixBuffer);
        glUniformMatrix4fv(normalMatrixLoc, false, normalMatrixBuffer);

        glDrawElements(GL_TRIANGLES, mesh.getVertexCount(), GL_UNSIGNED_INT, 0);

        glBindVertexArray(0);
        glUseProgram(0);
    }
}