package org.example;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL20.*;

// Класс для управления областью просмотра и матрицами проекции
public class Viewport {
    private int width;
    private int height;
    private float fov = 45.0f;
    private float nearPlane = 0.1f;
    private float farPlane = 100.0f;

    // Матрицы для шейдеров
    private Matrix4f projectionMatrix;

    public Viewport(int width, int height) {
        this.width = width;
        this.height = height;
        this.projectionMatrix = new Matrix4f();
        updateProjectionMatrix();
    }

    // Обновление матрицы проекции
    private void updateProjectionMatrix() {
        float aspectRatio = (float) width / height;
        projectionMatrix.identity();
        projectionMatrix.perspective((float) Math.toRadians(fov), aspectRatio, nearPlane, farPlane);
    }

    // Установка матрицы проекции в шейдер
    public void setupProjectionMatrix(int shaderProgram) {
        int projMatrixLoc = glGetUniformLocation(shaderProgram, "projection");

        float[] matrixBuffer = new float[16];
        projectionMatrix.get(matrixBuffer);
        glUniformMatrix4fv(projMatrixLoc, false, matrixBuffer);
    }

    // Установка матрицы вида в шейдер
    public void applyViewMatrix(int shaderProgram, Camera camera) {
        int viewMatrixLoc = glGetUniformLocation(shaderProgram, "view");

        Matrix4f viewMatrix = camera.getViewMatrix();
        float[] matrixBuffer = new float[16];
        viewMatrix.get(matrixBuffer);
        glUniformMatrix4fv(viewMatrixLoc, false, matrixBuffer);

        // Также передаем позицию камеры для расчетов освещения
        int viewPosLoc = glGetUniformLocation(shaderProgram, "viewPos");
        Vector3f cameraPos = camera.getPosition();
        glUniform3f(viewPosLoc, cameraPos.x, cameraPos.y, cameraPos.z);
    }

    // Вызывать при изменении размера окна
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        glViewport(0, 0, width, height);
        updateProjectionMatrix();
    }

    // Геттеры и сеттеры
    public Matrix4f getProjectionMatrix() {
        return new Matrix4f(projectionMatrix);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float getFov() {
        return fov;
    }

    public void setFov(float fov) {
        this.fov = fov;
        updateProjectionMatrix();
    }

    public float getNearPlane() {
        return nearPlane;
    }

    public void setNearPlane(float nearPlane) {
        this.nearPlane = nearPlane;
        updateProjectionMatrix();
    }

    public float getFarPlane() {
        return farPlane;
    }

    public void setFarPlane(float farPlane) {
        this.farPlane = farPlane;
        updateProjectionMatrix();
    }
}