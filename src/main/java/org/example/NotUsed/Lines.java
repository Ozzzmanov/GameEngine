package org.example.NotUsed;

import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL30.*;

public class Lines {


    public void render(int shaderProgram, Matrix4f viewMatrix, Matrix4f projectionMatrix) {

        glUseProgram(shaderProgram);

        int vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);


        float[] vertices = {
                5, 5, 5,  // Точка 1
                -5,  5, -5   // Точка 2
        };
        int vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);


        int[] indices = { 0, 1 }; // Индексы для линий
        int eboID = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);


        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glLineWidth(5.0f);
        glDrawElements(GL_LINES, indices.length, GL_UNSIGNED_INT, 0);


        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

    }
}
