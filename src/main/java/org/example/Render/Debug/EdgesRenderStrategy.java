package org.example.Render.Debug;

import org.example.Mesh;
import org.example.Node;
import org.example.Render.RenderStrategy;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

public class EdgesRenderStrategy implements RenderStrategy {
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

        glEnableVertexAttribArray(0);
        glDrawElements(GL_LINES, mesh.getVertexCount(), GL_UNSIGNED_INT, 0);
        glDisableVertexAttribArray(0);
        glBindVertexArray(0);

        glUseProgram(0);

    }
}
