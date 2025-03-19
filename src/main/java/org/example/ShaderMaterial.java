package org.example;

import org.joml.Vector3f;

import static org.lwjgl.opengl.GL20.*;

public class ShaderMaterial {
    private Vector3f ambient;
    private Vector3f diffuse;
    private Vector3f specular;
    private float shininess;

    public ShaderMaterial(Vector3f ambient, Vector3f diffuse, Vector3f specular, float shininess) {
        this.ambient = ambient;
        this.diffuse = diffuse;
        this.specular = specular;
        this.shininess = shininess;
    }

    public void apply(int shaderProgram) {
        int ambientLoc = glGetUniformLocation(shaderProgram, "material.ambient");
        int diffuseLoc = glGetUniformLocation(shaderProgram, "material.diffuse");
        int specularLoc = glGetUniformLocation(shaderProgram, "material.specular");
        int shininessLoc = glGetUniformLocation(shaderProgram, "material.shininess");

        glUniform3f(ambientLoc, ambient.x, ambient.y, ambient.z);
        glUniform3f(diffuseLoc, diffuse.x, diffuse.y, diffuse.z);
        glUniform3f(specularLoc, specular.x, specular.y, specular.z);
        glUniform1f(shininessLoc, shininess);
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
}