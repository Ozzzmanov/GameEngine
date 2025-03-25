package org.example.Render.Shadow;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL30.*;

public class ShadowMap implements IShadowMap {
    private int depthMapFBO;
    private int depthMap;
    private final int SHADOW_WIDTH = 4096;
    private final int SHADOW_HEIGHT = 4096;
    private Matrix4f lightSpaceMatrix;
    private int shadowShaderProgram;

    public ShadowMap(int shadowShaderProgram) {
        this.shadowShaderProgram = shadowShaderProgram;
        this.lightSpaceMatrix = new Matrix4f();
        init();
    }

    private void init() {
        // Створюємо фреймбуфер для тіньової карти
        depthMapFBO = glGenFramebuffers();

        /**
         * Створює текстуру для зберігання глибини та налаштовує її параметри.
         * Використовується для генерації карти глибини, необхідної для тіней у рендерингу.
         */
        depthMap = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, depthMap);

        /**
         * Виділяємо пам'ять під текстуру глибини.
         * Формат текстури – GL_DEPTH_COMPONENT, оскільки вона містить лише значення глибини.
         * Використовуємо FLOAT-значення для високої точності.
         */
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT,
                SHADOW_WIDTH, SHADOW_HEIGHT, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (FloatBuffer) null);

        /**
         * Налаштовуємо параметри текстури:
         * - Лінійна інтерполяція замінена на найближчий сусід (NEAREST) для уникнення розмиття.
         * - Краї текстури фіксуються білим кольором для коректного рендерингу тіней.
         */

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);

        // Встановлюємо білий колір для границь текстури
        float[] borderColor = { 1.0f, 1.0f, 1.0f, 1.0f };
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);

        /**
         * Прив'язуємо текстуру до фреймбуфера:
         * - Використовуємо її як буфер глибини.
         * - Вимикаємо кольоровий буфер, оскільки ця текстура використовується тільки для зберігання глибини.
         */
        glBindFramebuffer(GL_FRAMEBUFFER, depthMapFBO);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthMap, 0);
        glDrawBuffer(GL_NONE); // Відключаємо буфер кольору
        glReadBuffer(GL_NONE); // Відключаємо зчитування буфера кольору

        // Перевіряємо коректність налаштування фреймбуфера
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("Помилка при створенні фреймбуфера тіньової карти");
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    @Override
    public void updateLightSpaceMatrix(Vector3f lightPos, Vector3f lightTarget, float near, float far) {
        // Створюємо матрицю проекції та виду для джерела світла
        float aspectRatio = (float) SHADOW_WIDTH / SHADOW_HEIGHT;

        // Матриця проекції - ортографічна для тіньових карт
        Matrix4f lightProjection = new Matrix4f().ortho(
                -15.0f, 15.0f,    // ліворуч, праворуч
                -15.0f, 15.0f,    // знизу, зверху
                near, far         // близько, далеко
        );

        // Матриця виду - дивимося з позиції світла
        Matrix4f lightView = new Matrix4f().lookAt(
                lightPos,                                    // позиція світла
                lightTarget,                                 // цільова точка
                new Vector3f(0.0f, 1.0f, 0.0f)              // верх
        );

        // Матриця перетворення простору світла
        lightProjection.mul(lightView, lightSpaceMatrix);
    }

    @Override
    public void bindForShadowPass() {
        // Налаштування видового екрану та буферу глибини
        glViewport(0, 0, SHADOW_WIDTH, SHADOW_HEIGHT);
        glBindFramebuffer(GL_FRAMEBUFFER, depthMapFBO);
        glClear(GL_DEPTH_BUFFER_BIT);

        // Використовуємо шейдер для рендерингу тіней
        glUseProgram(shadowShaderProgram);

        // Завантажуємо матрицю перетворення простору світла в шейдер
        int lightSpaceMatrixLoc = glGetUniformLocation(shadowShaderProgram, "lightSpaceMatrix");
        FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
        lightSpaceMatrix.get(matrixBuffer);
        glUniformMatrix4fv(lightSpaceMatrixLoc, false, matrixBuffer);
    }

    @Override
    public void unbind(int width, int height) {
        // Відновлюємо основний фреймбуфер та розмір видового екрану
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, width, height);
    }

    @Override
    public void bindDepthMapForReading(int textureUnit) {
        // Прив'язуємо тіньову карту для читання в шейдері
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glBindTexture(GL_TEXTURE_2D, depthMap);
    }

    @Override
    public Matrix4f getLightSpaceMatrix() {
        return new Matrix4f(lightSpaceMatrix);
    }

    @Override
    public void cleanup() {
        glDeleteFramebuffers(depthMapFBO);
        glDeleteTextures(depthMap);
    }
}