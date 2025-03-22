package org.example;

import org.joml.Vector3f;

import java.io.IOException;

import static org.lwjgl.opengl.GL20.*;

public class ShaderMaterial {
    private Vector3f ambient;
    private Vector3f diffuse;
    private Vector3f specular;
    private float shininess;

    private TextureLoader diffuseMap;  // Текстура
    private boolean hasTexture;  // Прапор наявності текстури


    public ShaderMaterial(Vector3f ambient, Vector3f diffuse, Vector3f specular, float shininess) {
        this.ambient = ambient;
        this.diffuse = diffuse;
        this.specular = specular;
        this.shininess = shininess;
        this.hasTexture = false;
    }

    // Конструктор з текстурою
    public ShaderMaterial(Vector3f ambient, Vector3f diffuse, Vector3f specular, float shininess, TextureLoader diffuseMap) {
        this(ambient, diffuse, specular, shininess);
        this.diffuseMap = diffuseMap;
        this.hasTexture = diffuseMap != null;
    }

    public void apply(int shaderProgram) {
        int ambientLoc = glGetUniformLocation(shaderProgram, "material.ambient");
        int diffuseLoc = glGetUniformLocation(shaderProgram, "material.diffuse");
        int specularLoc = glGetUniformLocation(shaderProgram, "material.specular");
        int shininessLoc = glGetUniformLocation(shaderProgram, "material.shininess");
        int useTextureLoc = glGetUniformLocation(shaderProgram, "material.useTexture");

        glUniform3f(ambientLoc, ambient.x, ambient.y, ambient.z);
        glUniform3f(diffuseLoc, diffuse.x, diffuse.y, diffuse.z);
        glUniform3f(specularLoc, specular.x, specular.y, specular.z);
        glUniform1f(shininessLoc, shininess);
        glUniform1i(useTextureLoc, hasTexture ? 1 : 0);

        if (hasTexture) {
            // Активуємо та прив'язуємо текстуру
            int diffuseMapLoc = glGetUniformLocation(shaderProgram, "material.diffuseMap");
            glActiveTexture(GL_TEXTURE0);
            diffuseMap.bind();
            glUniform1i(diffuseMapLoc, 0);
        }
    }


    public static ShaderMaterial createTexturedMaterial(String texturePath) {
        try {
            TextureLoader textureLoader = new TextureLoader(texturePath);
            return new ShaderMaterial(
                    new Vector3f(0.5f, 0.5f, 0.5f),  // ambient - середній сірий
                    new Vector3f(1.0f, 1.0f, 1.0f),  // diffuse - білий для повного відображення текстури
                    new Vector3f(0.3f, 0.3f, 0.3f),  // specular - легкий блиск
                    16.0f,                           // shininess - середній блиск
                    textureLoader
            );
        } catch (IOException e) { // Исправлено: было "IOExceptioe"
            System.err.println("Помилка при завантаженні текстури: " + e.getMessage());
            e.printStackTrace();
            // Повертаємо стандартний матеріал при помилці
            return createRed();
        }
    }


    // Создание предопределенных материалов
    public static ShaderMaterial createRed() {
        return new ShaderMaterial(
                new Vector3f(0.8f, 0.1f, 0.1f),
                new Vector3f(0.8f, 0.1f, 0.1f),
                new Vector3f(0.5f, 0.5f, 0.5f),
                32.0f
        );
    }

    public static ShaderMaterial createHolographicMaterial() {
        return new ShaderMaterial(
                new Vector3f(0.1f, 0.1f, 0.1f), // Темный амбиентный цвет
                new Vector3f(0.2f, 0.8f, 0.9f), // Яркий синий-голубой основной цвет
                new Vector3f(0.9f, 0.9f, 0.9f), // Очень светлый, почти белый для отражений
                128f // Очень высокий блеск, чтобы отражать окружающие объекты
        );
    }

    public static ShaderMaterial createGreen() {
        return new ShaderMaterial(
                new Vector3f(0.1f, 0.8f, 0.1f),
                new Vector3f(0.1f, 0.8f, 0.1f),
                new Vector3f(0.5f, 0.5f, 0.5f),
                32.0f
        );
    }

    public static ShaderMaterial createBlue() {
        return new ShaderMaterial(
                new Vector3f(0.1f, 0.1f, 0.8f),
                new Vector3f(0.1f, 0.1f, 0.8f),
                new Vector3f(0.5f, 0.5f, 0.5f),
                32.0f
        );
    }

    // Металлические материалы
    public static ShaderMaterial createGold() {
        return new ShaderMaterial(
                new Vector3f(0.24725f, 0.1995f, 0.0745f),
                new Vector3f(0.75164f, 0.60648f, 0.22648f),
                new Vector3f(0.628281f, 0.555802f, 0.366065f),
                51.2f
        );
    }

    public static ShaderMaterial createSilver() {
        return new ShaderMaterial(
                new Vector3f(0.19225f, 0.19225f, 0.19225f),
                new Vector3f(0.50754f, 0.50754f, 0.50754f),
                new Vector3f(0.508273f, 0.508273f, 0.508273f),
                51.2f
        );
    }

    // Геттеры и сеттеры
    public Vector3f getAmbient() {
        return ambient;
    }

    public void setAmbient(Vector3f ambient) {
        this.ambient = ambient;
    }

    public Vector3f getDiffuse() {
        return diffuse;
    }

    public void setDiffuse(Vector3f diffuse) {
        this.diffuse = diffuse;
    }

    public Vector3f getSpecular() {
        return specular;
    }

    public void setSpecular(Vector3f specular) {
        this.specular = specular;
    }

    public float getShininess() {
        return shininess;
    }

    public void setShininess(float shininess) {
        this.shininess = shininess;
    }

    public TextureLoader getDiffuseMap() {
        return diffuseMap;
    }

    public void setDiffuseMap(TextureLoader diffuseMap) {
        if (this.diffuseMap != null && diffuseMap == null) {
            // Очищаем предыдущую текстуру, если она была
            this.diffuseMap.cleanup();
        }
        this.diffuseMap = diffuseMap;
        this.hasTexture = diffuseMap != null;
    }

    public void setDiffuseMapPath(String texturePath) {
        try {
            this.diffuseMap = new TextureLoader(texturePath);
            this.hasTexture = true;
        } catch (IOException e) {
            System.err.println("Помилка при завантаженні текстури: " + e.getMessage());
            e.printStackTrace();
            this.hasTexture = false;
        }
    }

    public boolean hasTexture() {
        return hasTexture;
    }
}