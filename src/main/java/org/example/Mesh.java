package org.example;

import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL30.*;

public class Mesh {
    private int vaoID;
    private int vboID;
    private int eboID;
    private int vertexCount;

    private float[] vertices;
    private int[] indices;

    public Mesh(float[] vertices, int[] indices) {
        this.vertices = vertices;
        this.indices = indices;
        this.vertexCount = indices.length;

        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        // VBO (Вершины)
        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        // EBO (Индексы)
        eboID = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.length);
        indexBuffer.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

        // Атрибуты вершин
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        // Отвязка
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    // Рендер трикутників (заповнених)
    public void render() {
        glBindVertexArray(vaoID);
        glEnableVertexAttribArray(0);
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
        glDisableVertexAttribArray(0);
        glBindVertexArray(0);
    }

    // Рендер только вершин
    public void renderVertices() {
        glBindVertexArray(vaoID);
        glEnableVertexAttribArray(0);
        glPointSize(5.0f); // Розмір точок
        glDrawElements(GL_POINTS, vertexCount, GL_UNSIGNED_INT, 0);
        glDisableVertexAttribArray(0);
        glBindVertexArray(0);
    }

    // Рендер только ребер
    public void renderEdges() {
        glBindVertexArray(vaoID);
        glEnableVertexAttribArray(0);
        glDrawElements(GL_LINES, vertexCount, GL_UNSIGNED_INT, 0);
        glDisableVertexAttribArray(0);
        glBindVertexArray(0);
    }

    // Рендер каркаса (сетки)
    public void renderWireframe() {
        glBindVertexArray(vaoID);
        glEnableVertexAttribArray(0);

        // Зберігаємо режим полігонів
        glPushAttrib(GL_POLYGON_BIT);
        // Встановлюємо режим каркаса
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);

        // Відновлюємо попередній режим полигонов
        glPopAttrib();

        glDisableVertexAttribArray(0);
        glBindVertexArray(0);
    }


    // Заповнені трикутники
    public static void renderAll(List<Mesh> meshes) {
        for (Mesh mesh : meshes) {
            mesh.render();
        }
    }
    // Вершини
    public static void renderAllVertices(List<Mesh> meshes) {
        for (Mesh mesh : meshes) {
            mesh.renderVertices();
        }
    }
    // Ребра
    public static void renderAllEdges(List<Mesh> meshes) {
        for (Mesh mesh : meshes) {
            mesh.renderEdges();
        }
    }
    // Сітка
    public static void renderAllWireframe(List<Mesh> meshes) {
        for (Mesh mesh : meshes) {
            mesh.renderWireframe();
        }
    }

    public void cleanup() {
        glDeleteBuffers(vboID);
        glDeleteBuffers(eboID);
        glDeleteVertexArrays(vaoID);
    }

    public float[] getVertices() {
        return vertices;
    }

    public int[] getIndices() {
        return indices;
    }


}
