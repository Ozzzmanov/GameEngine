package org.example.Render.Shadow;

import org.example.Mesh;
import org.example.Render.RenderStrategy;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL30.*;

public class ShadowRenderStrategy implements RenderStrategy {

    @Override
    public void render(Mesh mesh, int shaderProgram, Matrix4f viewMatrix, Matrix4f projectionMatrix, Matrix4f lightSpaceMatrix) {
        glUseProgram(shaderProgram);
        glBindVertexArray(mesh.getVaoID());

        // Передаємо матриці в шейдер тіней
        int lightSpaceMatrixLoc = glGetUniformLocation(shaderProgram, "lightSpaceMatrix");
        int modelLoc = glGetUniformLocation(shaderProgram, "model");

        float[] lightSpaceMatrixData = new float[16];
        lightSpaceMatrix.get(lightSpaceMatrixData);
        glUniformMatrix4fv(lightSpaceMatrixLoc, false, lightSpaceMatrixData);

        float[] modelData = new float[16];
        mesh.getModelMatrix().get(modelData);
        glUniformMatrix4fv(modelLoc, false, modelData);

        // Малюємо
        glDrawElements(GL_TRIANGLES, mesh.getVertexCount(), GL_UNSIGNED_INT, 0);

        glBindVertexArray(0);
        glUseProgram(0);
    }
}