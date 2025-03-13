package org.example;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class Node {
    private String name;
    private Node parent;
    private List<Node> children;
    private List<Mesh> meshes;

    // Локальні трансформації
    private Vector3f position;
    private Quaternionf rotation;
    private Vector3f scale;

    // Матриця локальної трансформації
    private Matrix4f localTransformation;
    // Матриця світової трансформації (включає трансформації всіх батьків)
    private Matrix4f worldTransformation;

    // Прапорець, що вказує, чи потрібно перерахувати локальну трансформацію
    private boolean localTransformationDirty;

    public Node(String name) {
        this.name = name;
        this.children = new ArrayList<>();
        this.meshes = new ArrayList<>();
        this.position = new Vector3f(0, 0, 0);
        this.rotation = new Quaternionf();
        this.scale = new Vector3f(1, 1, 1);
        this.localTransformation = new Matrix4f();
        this.worldTransformation = new Matrix4f();
        this.localTransformationDirty = true;
    }

    /**
     * Додає дочірній вузол до поточного вузла
     */
    public void addChild(Node child) {
        children.add(child);
        child.setParent(this);
    }

    /**
     * Встановлює батьківський вузол
     */
    private void setParent(Node parent) {
        this.parent = parent;
    }

    /**
     * Додає меш до поточного вузла
     */
    public void addMesh(Mesh mesh) {
        meshes.add(mesh);
    }

    /**
     * Встановлює позицію вузла
     */
    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        localTransformationDirty = true;
    }

    /**
     * Встановлює обертання вузла
     */
    public void setRotation(float x, float y, float z) {
        rotation.rotationXYZ(x, y, z);
        localTransformationDirty = true;
    }

    /**
     * Встановлює масштаб вузла
     */
    public void setScale(float x, float y, float z) {
        scale.set(x, y, z);
        localTransformationDirty = true;
    }

    /**
     * Оновлює локальну матрицю трансформації
     */
    private void updateLocalTransformation() {
        if (localTransformationDirty) {
            localTransformation.identity()
                    .translate(position)
                    .rotate(rotation)
                    .scale(scale);
            localTransformationDirty = false;
        }
    }

    /**
     * Оновлює світову матрицю трансформації
     */
    private void updateWorldTransformation() {
        updateLocalTransformation();

        if (parent != null) {
            parent.getWorldTransformation().mul(localTransformation, worldTransformation);
        } else {
            worldTransformation.set(localTransformation);
        }
    }

    /**
     * Повертає світову матрицю трансформації
     */
    public Matrix4f getWorldTransformation() {
        updateWorldTransformation();
        return worldTransformation;
    }

    /**
     * Застосовує світову трансформацію до поточного стану OpenGL
     */
    private void applyTransformation() {
        // Отримуємо та застосовуємо світову матрицю трансформації
        Matrix4f worldMatrix = getWorldTransformation();

        // Конвертуємо матрицю у формат для OpenGL
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            worldMatrix.get(fb);

            // Застосовуємо матрицю до поточного стану
            glPushMatrix();
            glMultMatrixf(fb);
        }
    }

    /**
     * Відновлює попередній стан трансформації
     */
    private void restoreTransformation() {
        glPopMatrix();
    }

    /**
     * Відображає всі меші у цьому вузлі та його дочірніх вузлах
     */
    public void render() {
        applyTransformation();

        // Відображаємо всі меші, що належать цьому вузлу
        for (Mesh mesh : meshes) {
            mesh.render();
        }

        // Відображаємо всі дочірні вузли
        for (Node child : children) {
            child.render();
        }

        restoreTransformation();
    }

    /**
     * Відображає всі меші у цьому вузлі та його дочірніх вузлах у режимі каркаса
     */
    public void renderWireframe() {
        applyTransformation();

        for (Mesh mesh : meshes) {
            mesh.renderWireframe();
        }

        for (Node child : children) {
            child.renderWireframe();
        }

        restoreTransformation();
    }

    /**
     * Відображає всі вершини у цьому вузлі та його дочірніх вузлах
     */
    public void renderVertices() {
        applyTransformation();

        for (Mesh mesh : meshes) {
            mesh.renderVertices();
        }

        for (Node child : children) {
            child.renderVertices();
        }

        restoreTransformation();
    }

    /**
     * Відображає всі ребра у цьому вузлі та його дочірніх вузлах
     */
    public void renderEdges() {
        applyTransformation();

        for (Mesh mesh : meshes) {
            mesh.renderEdges();
        }

        for (Node child : children) {
            child.renderEdges();
        }

        restoreTransformation();
    }

    /**
     * Очищає ресурси всіх мешів у цьому вузлі та його дочірніх вузлах
     */
    public void cleanup() {
        for (Mesh mesh : meshes) {
            mesh.cleanup();
        }

        for (Node child : children) {
            child.cleanup();
        }
    }

    /**
     * Отримує ім'я вузла
     */
    public String getName() {
        return name;
    }

    /**
     * Отримує дочірні вузли
     */
    public List<Node> getChildren() {
        return children;
    }

    /**
     * Отримує список мешів вузла
     */
    public List<Mesh> getMeshes() {
        return meshes;
    }
}