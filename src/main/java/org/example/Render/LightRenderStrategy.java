package org.example.Render;

import org.example.Mesh;
import org.example.Node;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

import static org.lwjgl.opengl.GL30.*;

public class LightRenderStrategy implements RenderStrategy{

    @Override
    public void render(Mesh mesh, int shaderProgram, Matrix4f viewMatrix, Matrix4f projectionMatrix, Vector3f cameraPosition, List<Node> lightNodes) {
        glUseProgram(shaderProgram);
        glBindVertexArray(mesh.getVaoID());

        int mvpLoc = glGetUniformLocation(shaderProgram, "mvp");

        Matrix4f mvpMatrix = new Matrix4f();
        projectionMatrix.mul(viewMatrix, mvpMatrix);
        mvpMatrix.mul(mesh.getModelMatrix());

        float[] mvpBuffer = new float[16];
        mvpMatrix.get(mvpBuffer);
        glUniformMatrix4fv(mvpLoc, false, mvpBuffer);

        // Встановлюємо колір джерела світла
        int lightColorLoc = glGetUniformLocation(shaderProgram, "lightColor");
        glUniform3f(lightColorLoc, 1.0f, 1.0f, 0.0f); // Жовтий колір для джерела світла

        glDrawElements(GL_TRIANGLES, mesh.getVertexCount(), GL_UNSIGNED_INT, 0);

        glBindVertexArray(0);
        glUseProgram(0);
    }
}
