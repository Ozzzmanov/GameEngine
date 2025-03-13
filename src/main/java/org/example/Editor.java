package org.example;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Editor {
    private InputManager inputManager;
    private Viewport viewport;
    private Camera camera;
    private List<Mesh> meshes;
    private int selectedMeshIndex = -1;

    // Для вибору за допомогою променя
    private Vector3f rayOrigin = new Vector3f();
    private Vector3f rayDirection = new Vector3f();

    public Editor(InputManager inputManager, Viewport viewport, Camera camera, List<Mesh> meshes) {
        this.inputManager = inputManager;
        this.viewport = viewport;
        this.camera = camera;
        this.meshes = meshes;
    }

    public void update() {
        // Обробка вводу миші для вибору за допомогою променя
        if (inputManager.isMouseButtonDown(GLFW_MOUSE_BUTTON_LEFT) && !inputManager.isCursorLocked()) {
            performRayPicking(inputManager.getMouseX(), inputManager.getMouseY());
        }
    }

    /**
     * Перетворює координати екрану в промінь у світовому просторі
     */
    private void calculateRay(float mouseX, float mouseY) {
        // Перетворюємо координати екрану в нормалізовані координати пристрою (NDC)
        float x = (2.0f * mouseX) / viewport.getWidth() - 1.0f;
        float y = 1.0f - (2.0f * mouseY) / viewport.getHeight(); // Y інвертоване

        // Створюємо NDC координати (від -1 до 1)
        Vector4f rayClip = new Vector4f(x, y, -1.0f, 1.0f);

        // Перетворюємо з простору кліпу в простір ока
        Matrix4f projMatrix = createProjectionMatrix();
        Matrix4f invProjMatrix = new Matrix4f();
        projMatrix.invert(invProjMatrix);

        Vector4f rayEye = new Vector4f();
        invProjMatrix.transform(rayClip, rayEye);
        rayEye.z = -1.0f;
        rayEye.w = 0.0f;

        // Перетворюємо з простору ока в світовий простір
        Matrix4f viewMatrix = camera.getViewMatrix();
        Matrix4f invViewMatrix = new Matrix4f();
        viewMatrix.invert(invViewMatrix);

        Vector4f rayWorld = new Vector4f();
        invViewMatrix.transform(rayEye, rayWorld);

        rayDirection.set(rayWorld.x, rayWorld.y, rayWorld.z).normalize();
        rayOrigin.set(camera.getPosition());
    }

    /**
     * Створює матрицю проекції, що відповідає налаштуванням viewport
     */
    private Matrix4f createProjectionMatrix() {
        float aspectRatio = (float) viewport.getWidth() / viewport.getHeight();
        Matrix4f projMatrix = new Matrix4f().perspective(
                (float) Math.toRadians(viewport.getFov()),
                aspectRatio,
                viewport.getNearPlane(),
                viewport.getFarPlane()
        );
        return projMatrix;
    }

    /**
     * Виконує вибір за допомогою променя за заданими координатами екрану
     */
    public void performRayPicking(float mouseX, float mouseY) {
        calculateRay(mouseX, mouseY);

        float closestDistance = Float.MAX_VALUE;
        int closestMeshIndex = -1;

        // Перевіряємо промінь з кожним об'єктом
        for (int i = 0; i < meshes.size(); i++) {
            Mesh mesh = meshes.get(i);
            float[] intersection = intersectRayWithMesh(rayOrigin, rayDirection, mesh);

            if (intersection != null) {
                float distance = intersection[0];
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestMeshIndex = i;
                }
            }
        }

        // Оновлюємо вибір
        if (closestMeshIndex != -1) {
            selectedMeshIndex = closestMeshIndex;
            System.out.println("Вибраний об'єкт: " + selectedMeshIndex);
        }
    }

    /**
     * Перевіряє, чи перетинається промінь з об'єктом
     * @return масив з [відстань, u, v], якщо перетин знайдений, null в іншому випадку
     */
    private float[] intersectRayWithMesh(Vector3f rayOrigin, Vector3f rayDirection, Mesh mesh) {
        float closestDistance = Float.MAX_VALUE;
        float[] result = null;

        // Отримуємо вершини та індекси з об'єкта
        float[] vertices = mesh.getVertices();
        int[] indices = mesh.getIndices();

        // Для кожного трикутника в об'єкті
        for (int i = 0; i < indices.length; i += 3) {
            int i1 = indices[i] * 3;
            int i2 = indices[i + 1] * 3;
            int i3 = indices[i + 2] * 3;

            Vector3f v1 = new Vector3f(vertices[i1], vertices[i1 + 1], vertices[i1 + 2]);
            Vector3f v2 = new Vector3f(vertices[i2], vertices[i2 + 1], vertices[i2 + 2]);
            Vector3f v3 = new Vector3f(vertices[i3], vertices[i3 + 1], vertices[i3 + 2]);

            float[] intersection = intersectRayWithTriangle(rayOrigin, rayDirection, v1, v2, v3);

            if (intersection != null && intersection[0] < closestDistance) {
                closestDistance = intersection[0];
                result = intersection;
            }
        }

        return result;
    }

    /**
     * Перевіряє, чи перетинається промінь з трикутником за допомогою алгоритму Мёллера–Трамбора
     * @return масив з [відстань, u, v], якщо перетин знайдений, null в іншому випадку
     */
    private float[] intersectRayWithTriangle(Vector3f rayOrigin, Vector3f rayDirection,
                                             Vector3f v1, Vector3f v2, Vector3f v3) {
        // Обчислюємо вектори по двох краях трикутника
        Vector3f edge1 = new Vector3f();
        Vector3f edge2 = new Vector3f();
        Vector3f tvec = new Vector3f();
        Vector3f pvec = new Vector3f();
        Vector3f qvec = new Vector3f();

        v2.sub(v1, edge1);
        v3.sub(v1, edge2);

        // Початкова калькуляція
        rayDirection.cross(edge2, pvec);

        // Якщо детермінант близький до нуля, промінь лежить в площині трикутника
        float det = edge1.dot(pvec);

        // Перевірка на відсікання (прибрати коментар, якщо хочете подвійні трикутники)
        if (det < 0.000001f) {
            return null;
        }

        float invDet = 1.0f / det;

        // Обчислюємо відстань від v1 до початку променя
        rayOrigin.sub(v1, tvec);

        // Обчислюємо параметр u
        float u = tvec.dot(pvec) * invDet;
        if (u < 0.0f || u > 1.0f) {
            return null;
        }

        // Обчислюємо параметр v
        tvec.cross(edge1, qvec);
        float v = rayDirection.dot(qvec) * invDet;
        if (v < 0.0f || u + v > 1.0f) {
            return null;
        }

        // Обчислюємо t (відстань уздовж променя)
        float t = edge2.dot(qvec) * invDet;

        if (t > 0.0f) {
            return new float[] { t, u, v };
        }

        return null;
    }

    /**
     * Відображає підсвітку для поточного вибраного об'єкта
     */
    public void renderSelection() {
        if (selectedMeshIndex >= 0 && selectedMeshIndex < meshes.size()) {
            // Зберігаємо поточний стан OpenGL
            glPushAttrib(GL_ALL_ATTRIB_BITS);

            // Налаштовуємо параметри рендеринга для вибору
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            glEnable(GL_POLYGON_OFFSET_FILL);
            glPolygonOffset(1.0f, 1.0f);

            // Малюємо вибраний об'єкт з підсвіткою
            glColor4f(1.0f, 0.5f, 0.0f, 0.3f); // Оранжевий колір для вибору
            meshes.get(selectedMeshIndex).render();

            // Відновлюємо попередній стан OpenGL
            glPopAttrib();
        }
    }

    /**
     * Повертає індекс поточного вибраного об'єкта
     */
    public int getSelectedMeshIndex() {
        return selectedMeshIndex;
    }

    /**
     * Встановлює індекс вибраного об'єкта
     */
    public void setSelectedMeshIndex(int index) {
        if (index >= -1 && index < meshes.size()) {
            this.selectedMeshIndex = index;
        }
    }

    /**
     * Очищає поточний вибір
     */
    public void clearSelection() {
        this.selectedMeshIndex = -1;
    }
}
