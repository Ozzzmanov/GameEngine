package org.example;

import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.*;

public class Viewport {
    private int width;
    private int height;
    private float fov = 45.0f;
    private float nearPlane = 0.1f;
    private float farPlane = 100.0f;

    public Viewport(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Налаштовує матрицю проекції
     */
    public void setupProjectionMatrix(Camera camera) {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();

        float aspectRatio = (float) width / height;
        gluPerspective(camera.getZoom(), aspectRatio, nearPlane, farPlane);
    }

    /**
     * Застосовує матрицю вигляду камери
     * @param camera Камера з якої буде взято матрицю вигляду
     */
    public void applyViewMatrix(Camera camera) {
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        // Отримуємо матрицю вигляду з камери
        Matrix4f viewMatrix = camera.getViewMatrix();

        // Конвертуємо матрицю JOML у float масив для OpenGL
        float[] matrixData = new float[16];
        viewMatrix.get(matrixData);

        // Встановлюємо матрицю вигляду в конвеєр OpenGL
        glLoadMatrixf(matrixData);
    }

    /**
     * Реалізація gluPerspective для створення перспективної проекції
     */
    private void gluPerspective(float fovy, float aspect, float zNear, float zFar) {
        float ymax = zNear * (float) Math.tan(Math.toRadians(fovy / 2));
        float ymin = -ymax;
        float xmin = ymin * aspect;
        float xmax = ymax * aspect;

        glFrustum(xmin, xmax, ymin, ymax, zNear, zFar);
    }

    /**
     * Викликати при зміні розміру вікна
     */
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        glViewport(0, 0, width, height);
    }

    // Геттери і сеттери
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
    }

    public float getNearPlane() {
        return nearPlane;
    }

    public void setNearPlane(float nearPlane) {
        this.nearPlane = nearPlane;
    }

    public float getFarPlane() {
        return farPlane;
    }

    public void setFarPlane(float farPlane) {
        this.farPlane = farPlane;
    }
}