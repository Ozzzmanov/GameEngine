package org.example;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL30.*;

public class Mesh {
    private final int vaoID;
    private final int vboID;
    private final int eboID;
    private final int vertexCount;
    private ShaderMaterial shaderMaterial;
    private final float[] vertices;
    private final int[] indices;
    private Matrix4f modelMatrix;
    private final Vector3f position;

    private float rotationSpeed = 90.0f;

    public Mesh(float[] vertices, int[] indices) {
        this.vertices = vertices;
        this.indices = indices;
        this.vertexCount = indices.length;
        this.position = new Vector3f(0.0f, 0.0f, 0.0f);

        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        eboID = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.length);
        indexBuffer.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
        /**
         * --------------------------------------------------------------------------------------------------
         */
        // Атрибут 0: позиція вершини (x, y, z)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 8 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        /**
         * 0 – номер атрибута в шейдері (layout(location = 0))
         * 3 – кількість значень (x, y, z)
         * GL_FLOAT – тип даних
         * false – не нормалізуємо значення
         * 8 * Float.BYTES – крок (stride), тобто скільки байтів займає одна вершина (32 байти)
         * 0 – зміщення (offset), тобто де починаються координати (x, y, z) у буфері (з самого початку)
         */
        /**
         * --------------------------------------------------------------------------------------------------
         */
        // Атрибут 1: текстурні координати (u, v)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 8 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        /**
         * 1 – номер атрибута (layout(location = 1))
         * 2 – кількість значень (u, v)
         * GL_FLOAT – тип даних
         * false – не нормалізуємо
         * 8 * Float.BYTES – крок (stride) в 32 байти (повна довжина однієї вершини)
         * 3 * Float.BYTES – зміщення 12 байт (3 float значення, після x, y, z)
         */
        /**
         * --------------------------------------------------------------------------------------------------
         */
        // Атрибут 2: нормаль (nx, ny, nz)
        glVertexAttribPointer(2, 3, GL_FLOAT, false, 8 * Float.BYTES, 5 * Float.BYTES);
        glEnableVertexAttribArray(2);
        /**
         * 2 – номер атрибута (layout(location = 2))
         * 3 – кількість значень (nx, ny, nz)
         * GL_FLOAT – тип даних
         * false – не нормалізуємо
         * 8 * Float.BYTES – крок (stride) у 32 байти
         * 5 * Float.BYTES – зміщення 20 байт (5 float значень, після x, y, z, u, v)
         */
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        modelMatrix = new Matrix4f().identity();

        // По умолчанию создаем базовый материал
        shaderMaterial = new ShaderMaterial(
                new Vector3f(0.8f, 0.2f, 0.2f), // ambient
                new Vector3f(0.8f, 0.2f, 0.2f), // diffuse
                new Vector3f(0.5f, 0.5f, 0.5f), // specular
                32.0f // shininess
        );
    }

    public void update(float deltaTime) {
        float angle = rotationSpeed * deltaTime;
        modelMatrix.rotate(angle, new Vector3f(0.0f, 1.0f, 0.0f));
    }

    public void render(int shaderProgram, Matrix4f viewMatrix, Matrix4f projectionMatrix, Vector3f cameraPosition) {
        glUseProgram(shaderProgram);
        glBindVertexArray(vaoID);

        // Позиция источника света
        int lightPosLoc = glGetUniformLocation(shaderProgram, "lightPos");
        glUniform3f(lightPosLoc, 5.0f, 5.0f, 5.0f); // Позиция света (соответствует сфере)

        // Цвет источника света
        int lightColorLoc = glGetUniformLocation(shaderProgram, "lightColor");
        glUniform3f(lightColorLoc, 1.0f, 1.0f, 1.0f); // Белый свет

        // MVP матрица и Model матрица для расчета освещения
        int mvpLoc = glGetUniformLocation(shaderProgram, "mvp");
        int modelLoc = glGetUniformLocation(shaderProgram, "model");
        int normalMatrixLoc = glGetUniformLocation(shaderProgram, "normalMatrix");

        // Применяем материал
        if (shaderMaterial != null) {
            shaderMaterial.apply(shaderProgram);
        }

        // Позиция камеры для бликов - використовуємо реальну позицію камери
        int viewPosLoc = glGetUniformLocation(shaderProgram, "viewPos");
        glUniform3f(viewPosLoc, cameraPosition.x, cameraPosition.y, cameraPosition.z);

        // Вычисление матриц
        Matrix4f mvpMatrix = new Matrix4f();
        projectionMatrix.mul(viewMatrix, mvpMatrix);
        mvpMatrix.mul(modelMatrix);

        // Матрица нормалей (инвертированная транспонированная модельная матрица)
        Matrix4f normalMatrix = new Matrix4f(modelMatrix);
        normalMatrix.invert().transpose();

        // Передача матриц в шейдер
        float[] mvpBuffer = new float[16];
        mvpMatrix.get(mvpBuffer);
        glUniformMatrix4fv(mvpLoc, false, mvpBuffer);

        float[] modelBuffer = new float[16];
        modelMatrix.get(modelBuffer);
        glUniformMatrix4fv(modelLoc, false, modelBuffer);

        float[] normalMatrixBuffer = new float[16];
        normalMatrix.get(normalMatrixBuffer);
        glUniformMatrix4fv(normalMatrixLoc, false, normalMatrixBuffer);

        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);

        glBindVertexArray(0);
        glUseProgram(0);
    }

    public void renderLight(int lightShaderProgram, Matrix4f viewMatrix, Matrix4f projectionMatrix) {

        glUseProgram(lightShaderProgram);
        glBindVertexArray(vaoID);

        int mvpLoc = glGetUniformLocation(lightShaderProgram, "mvp");

        Matrix4f mvpMatrix = new Matrix4f();
        projectionMatrix.mul(viewMatrix, mvpMatrix);
        mvpMatrix.mul(modelMatrix);

        float[] mvpBuffer = new float[16];
        mvpMatrix.get(mvpBuffer);
        glUniformMatrix4fv(mvpLoc, false, mvpBuffer);

        // Устанавливаем цвет источника света
        int lightColorLoc = glGetUniformLocation(lightShaderProgram, "lightColor");
        glUniform3f(lightColorLoc, 1.0f, 1.0f, 0.0f); // Желтый цвет для источника света


        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);

        glBindVertexArray(0);
        glUseProgram(0);
    }

    public void renderVertices() {
        glBindVertexArray(vaoID);
        glEnableVertexAttribArray(0);
        glPointSize(5.0f);
        glDrawElements(GL_POINTS, vertexCount, GL_UNSIGNED_INT, 0);
        glDisableVertexAttribArray(0);
        glBindVertexArray(0);
    }

    public void renderEdges() {
        glBindVertexArray(vaoID);
        glEnableVertexAttribArray(0);
        glDrawElements(GL_LINES, vertexCount, GL_UNSIGNED_INT, 0);
        glDisableVertexAttribArray(0);
        glBindVertexArray(0);
    }

    public void renderWireframe() {
        glBindVertexArray(vaoID);
        glEnableVertexAttribArray(0);
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glDisableVertexAttribArray(0);
        glBindVertexArray(0);
    }

    public static void renderAllVertices(List<Mesh> meshes) {
        for (Mesh mesh : meshes) {
            mesh.renderVertices();
        }
    }

    public static void renderAllEdges(List<Mesh> meshes) {
        for (Mesh mesh : meshes) {
            mesh.renderEdges();
        }
    }

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

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        modelMatrix.identity().translate(position);
    }

    public float[] getVertices() {
        return vertices;
    }

    public int[] getIndices() {
        return indices;
    }

    public void setShaderMaterial(ShaderMaterial shaderMaterial) {
        this.shaderMaterial = shaderMaterial;
    }

    public void setModelMatrix(Matrix4f modelMatrix){
        this.modelMatrix = modelMatrix;
    }

    public ShaderMaterial getShaderMaterial() {
        return shaderMaterial;
    }

    public int getVaoID() {
        return vaoID;
    }

    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    public Matrix4f getModelMatrix() {
        return new Matrix4f(modelMatrix);
    }


}