package org.example.Editor;

import org.example.*;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.IOException;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

/**
 * Інструмент трансформації для сцени редактора.
 * Дозволяє переміщати, обертати та масштабувати вибрані об'єкти.
 */
public class TransformTool implements EditorListener {
    // Режими трансформації
    public enum TransformMode {
        TRANSLATE,
        ROTATE,
        SCALE
    }

    // Осі трансформації
    public enum TransformAxis {
        X, Y, Z, XY, XZ, YZ, XYZ, NONE
    }

    private Node selectedNode;
    private final Editor editor;
    private final InputManager inputManager;
    private final Camera camera;
    private final Viewport viewport;

    // Поточний режим трансформації
    private TransformMode currentMode = TransformMode.TRANSLATE;
    private TransformAxis currentAxis = TransformAxis.NONE;

    // Стан трансформації
    private Vector3f startPosition = new Vector3f();
    private Quaternionf startRotation = new Quaternionf();
    private Vector3f startScale = new Vector3f();
    private Vector2f startMousePosition = new Vector2f();
    private boolean isDragging = false;

    // Константи для чутливості трансформації
    private final float TRANSLATE_SENSITIVITY = 0.01f;
    private final float ROTATE_SENSITIVITY = 0.5f;
    private final float SCALE_SENSITIVITY = 0.01f;

    // Меші для стрілок трансформації
    private Mesh arrowXMesh;
    private Mesh arrowYMesh;
    private Mesh arrowZMesh;

    // Ноди для стрілок трансформації
    private Node arrowXNode;
    private Node arrowYNode;
    private Node arrowZNode;
    private Node arrowsRootNode; // Корневий вузол для всіх стрілок

    private int mainShaderProgram;

    private Node rootNode;

    /**
     * Створює інструмент трансформації для редактора.
     *
     * @param editor редактор сцени
     * @param inputManager менеджер вводу
     * @param camera камера
     * @param viewport вьюпорт
     */
    public TransformTool(Editor editor, InputManager inputManager, Camera camera, Viewport viewport, Node rootNode) {
        this.editor = editor;
        this.inputManager = inputManager;
        this.camera = camera;
        this.viewport = viewport;
        this.editor.addEditorListener(this);
        this.rootNode = rootNode;

        initEditorTools();
    }

    private void initEditorTools() {
        // Створюємо кореневий вузол для стрілок
        arrowsRootNode = new Node("arrowsRootNode");

        // Завантажуємо меш стрілки
        arrowXMesh = ImportObj.loadObjModel("/Object/Tool/ArrowMeshX.obj");
        arrowYMesh = ImportObj.loadObjModel("/Object/Tool/ArrowMeshY.obj");
        arrowZMesh = ImportObj.loadObjModel("/Object/Tool/ArrowMeshZ.obj");

        // Встановлюємо матеріали для стрілок (червоний, зелений, синій)
        arrowXMesh.setShaderMaterial(ShaderMaterial.createRed());
        arrowYMesh.setShaderMaterial(ShaderMaterial.createGreen());
        arrowZMesh.setShaderMaterial(ShaderMaterial.createBlue());

        // Створюємо ноди для стрілок
        arrowXNode = new Node("arrowXNode");
        arrowYNode = new Node("arrowYNode");
        arrowZNode = new Node("arrowZNode");

        // Додаємо меші до нод
        arrowXNode.addMesh(arrowXMesh);
        arrowYNode.addMesh(arrowYMesh);
        arrowZNode.addMesh(arrowZMesh);

        // Додаємо стрілки до кореневого вузла
        arrowsRootNode.addChild(arrowXNode);
        arrowsRootNode.addChild(arrowYNode);
        arrowsRootNode.addChild(arrowZNode);

        // Ховаємо стрілки, поки немає вибраного об'єкта
        arrowsRootNode.setScale(0, 0, 0);


        editor.setArrowsRootNode(arrowsRootNode);
//        rootNode.addChild(arrowsRootNode);




        try {
            // Завантажуємо шейдери
            mainShaderProgram = ShaderLoader.loadShader(
                    "/Shader/mainShaderProgram/vertex_shader.glsl",
                    "/Shader/mainShaderProgram/fragment_shader.glsl"
            );
        } catch (IOException e) {
            throw new RuntimeException("Помилка завантаження шейдерів: " + e.getMessage());
        }
    }

    /**
     * Обробник вибору вузла в редакторі.
     *
     * @param node вибраний вузол
     */
    @Override
    public void onNodeSelected(Node node) {
        // Спочатку перевіряємо, чи це стрілка
        if (node == arrowXNode) {
            // Якщо вибрана стрілка X, активуємо режим перетягування для осі X
            currentAxis = TransformAxis.X;
            startDragging();
            return;
        } else if (node == arrowYNode) {
            // Якщо вибрана стрілка Y, активуємо режим перетягування для осі Y
            currentAxis = TransformAxis.Y;
            startDragging();
            return;
        } else if (node == arrowZNode) {
            // Якщо вибрана стрілка Z, активуємо режим перетягування для осі Z
            currentAxis = TransformAxis.Z;
            startDragging();
            return;
        }

        // Якщо вибраний звичайний вузол
        selectedNode = node;

        // Зберігаємо початкову позицію при виборі вузла
        if (node != null) {
            startPosition = new Vector3f(node.getPosition());
            startRotation = new Quaternionf(node.getRotation());
            startScale = new Vector3f(node.getScale());

            // Показуємо стрілки трансформації і переміщуємо їх до вибраного вузла
            arrowsRootNode.setPosition(node.getPosition().x, node.getPosition().y, node.getPosition().z);
            arrowsRootNode.setScale(1, 1, 1);

            // Додаємо стрілки до кореневого вузла сцени тільки якщо їх ще немає
            if (!rootNode.getChildren().contains(arrowsRootNode)) {
                rootNode.addChild(arrowsRootNode);
            }
        } else {
            // Ховаємо стрілки, якщо немає вибраного вузла
            arrowsRootNode.setScale(0, 0, 0);
        }
    }

    /**
     * Обробник очищення вибору в редакторі.
     */
    @Override
    public void onSelectionCleared() {
        selectedNode = null;
        isDragging = false;
        currentAxis = TransformAxis.NONE;

        if (rootNode.getChildren().contains(arrowsRootNode)) {
            rootNode.removeChild(arrowsRootNode);
        }

        // Ховаємо стрілки
        arrowsRootNode.setScale(0, 0, 0);
    }

    /**
     * Обробник зміни сцени.
     */
    @Override
    public void onSceneChanged() {
        // Оновлюємо положення стрілок при зміні сцени
        if (selectedNode != null) {
            arrowsRootNode.setPosition(selectedNode.getPosition().x, selectedNode.getPosition().y, selectedNode.getPosition().z);
        }
    }

    /**
     * Оновлює стан інструменту трансформації.
     * Має викликатися у кожному кадрі.
     */
    public void update() {
        // Якщо є вибраний вузол і режим перетягування активний
        if (isDragging && selectedNode != null) {

            // Обробляємо трансформацію відповідно до обраного режиму
            switch (currentMode) {
                case TRANSLATE:
                    handleTranslation();
                    break;
                case ROTATE:
                    handleRotation();
                    break;
                case SCALE:
                    handleScaling();
                    break;
            }

            // Закінчуємо перетягування, коли кнопка миші відпущена
            if (!inputManager.isMouseButtonPressed(GLFW_MOUSE_BUTTON_LEFT)) {
                finishTransform();
            }
        }

        // Скасовуємо трансформацію клавішею Escape
        if (inputManager.isKeyDown(GLFW_KEY_ESCAPE)) {
            cancelTransform();
        }

        // Рендеримо стрілки трансформації, якщо є вибраний вузол
        if (selectedNode != null && !isDragging) {
            arrowsRootNode.render(mainShaderProgram, camera.getViewMatrix(), viewport.getProjectionMatrix());
        }
    }

    /**
     * Встановлює режим трансформації.
     *
     * @param mode новий режим трансформації
     */
    public void setTransformMode(TransformMode mode) {
        currentMode = mode;
        System.out.println("Режим трансформації: " + mode);
    }

    /**
     * Починає процес трансформації.
     */
    private void startDragging() {
        if (selectedNode == null) return;

        isDragging = true;
        startPosition = new Vector3f(selectedNode.getPosition());
        startMousePosition = new Vector2f(inputManager.getMouseX(), inputManager.getMouseY());

        // Вимикаємо блокування курсору для зручнішого перетягування
        if (inputManager.isCursorLocked()) {
            inputManager.toggleCursor();
        }

        System.out.println("Розпочато перетягування: " + selectedNode.getName() + " вздовж осі " + currentAxis);
    }

    /**
     * Обробляє переміщення об'єкта.
     */
    private void handleTranslation() {
        Vector2f currentMouse = new Vector2f(inputManager.getMouseX(), inputManager.getMouseY());
        Vector2f delta = new Vector2f(currentMouse).sub(startMousePosition);
        delta.mul(TRANSLATE_SENSITIVITY);

        // Отримуємо вектори з камери
        Vector3f cameraRight = camera.getRightVector();
        Vector3f cameraUp = camera.getUpVector();
        Vector3f cameraForward = camera.getFront();

        // Створюємо нову позицію на основі початкової
        Vector3f newPosition = new Vector3f(startPosition);

        // Застосовуємо зміщення в залежності від обраної осі, з урахуванням напрямку камери
        switch (currentAxis) {
            case X:
                // Проекція руху миші на площину екрану
                float xMovement = delta.x * cameraRight.dot(new Vector3f(1, 0, 0))
                        - delta.y * cameraUp.dot(new Vector3f(1, 0, 0));
                newPosition.add(new Vector3f(1, 0, 0).mul(xMovement));
                break;
            case Y:
                float yMovement = delta.x * cameraRight.dot(new Vector3f(0, 1, 0))
                        - delta.y * cameraUp.dot(new Vector3f(0, 1, 0));
                newPosition.add(new Vector3f(0, 1, 0).mul(yMovement));
                break;
            case Z:
                float zMovement = delta.x * cameraRight.dot(new Vector3f(0, 0, 1))
                        - delta.y * cameraUp.dot(new Vector3f(0, 0, 1));
                newPosition.add(new Vector3f(0, 0, 1).mul(zMovement));
                break;
            case XY:
                newPosition.add(cameraRight.mul(delta.x)).add(cameraUp.mul(-delta.y));
                break;
            case XZ:
                // Проекція на площину XZ
                Vector3f xzRight = new Vector3f(cameraRight.x, 0, cameraRight.z).normalize();
                Vector3f xzForward = new Vector3f(cameraForward.x, 0, cameraForward.z).normalize();
                newPosition.add(xzRight.mul(delta.x)).add(xzForward.mul(-delta.y));
                break;
            case YZ:
                // Аналогічно для YZ
                break;
            case XYZ:
                // Рух у площині, перпендикулярній до напрямку погляду
                newPosition.add(cameraRight.mul(delta.x)).add(cameraUp.mul(-delta.y));
                break;
        }

        // Застосовуємо нову позицію до вузла
        selectedNode.setPosition(newPosition.x, newPosition.y, newPosition.z);

        // Оновлюємо позицію стрілок
        arrowsRootNode.setPosition(newPosition.x, newPosition.y, newPosition.z);
    }

    /**
     * Обробляє обертання об'єкта.
     */
    private void handleRotation() {
        Vector2f currentMouse = new Vector2f(inputManager.getMouseX(), inputManager.getMouseY());
        Vector2f delta = new Vector2f(currentMouse).sub(startMousePosition);
        delta.mul(ROTATE_SENSITIVITY * 0.01f);

        // Отримуємо поточні кути повороту
        Vector3f rotationAngles = new Vector3f();
        selectedNode.getRotation().getEulerAnglesXYZ(rotationAngles);

        // Застосовуємо обертання в залежності від обраної осі
        switch (currentAxis) {
            case X:
                rotationAngles.x += delta.y;
                break;
            case Y:
                rotationAngles.y += delta.x;
                break;
            case Z:
                rotationAngles.z += delta.x;
                break;
            case XYZ:
                rotationAngles.x += delta.y;
                rotationAngles.y += delta.x;
                break;
        }

        // Застосовуємо обертання до вузла
        selectedNode.setRotation(rotationAngles.x, rotationAngles.y, rotationAngles.z);
    }

    /**
     * Обробляє масштабування об'єкта.
     */
    private void handleScaling() {
        Vector2f currentMouse = new Vector2f(inputManager.getMouseX(), inputManager.getMouseY());
        Vector2f delta = new Vector2f(currentMouse).sub(startMousePosition);

        // Обчислюємо коефіцієнт масштабування
        float scaleFactor = 1.0f + (delta.x + delta.y) * SCALE_SENSITIVITY;

        // Створюємо новий масштаб на основі початкового
        Vector3f newScale = new Vector3f(startScale);

        // Застосовуємо масштабування в залежності від обраної осі
        switch (currentAxis) {
            case X:
                newScale.x *= scaleFactor;
                break;
            case Y:
                newScale.y *= scaleFactor;
                break;
            case Z:
                newScale.z *= scaleFactor;
                break;
            case XY:
                newScale.x *= scaleFactor;
                newScale.y *= scaleFactor;
                break;
            case XZ:
                newScale.x *= scaleFactor;
                newScale.z *= scaleFactor;
                break;
            case YZ:
                newScale.y *= scaleFactor;
                newScale.z *= scaleFactor;
                break;
            case XYZ:
                newScale.mul(scaleFactor);
                break;
        }

        // Застосовуємо новий масштаб до вузла
        selectedNode.setScale(newScale.x, newScale.y, newScale.z);
    }

    /**
     * Завершує трансформацію та зберігає результат.
     */
    private void finishTransform() {
        isDragging = false;
        startMousePosition = null;
        currentAxis = TransformAxis.NONE;

        // Оновлюємо позицію стрілок
        if (selectedNode != null) {
            arrowsRootNode.setPosition(selectedNode.getPosition().x, selectedNode.getPosition().y, selectedNode.getPosition().z);
        }

        System.out.println("Трансформацію завершено: " +
                (selectedNode != null ? selectedNode.getName() : "немає вибраного вузла"));
    }

    /**
     * Скасовує трансформацію та відновлює початковий стан.
     */
    private void cancelTransform() {
        if (selectedNode == null) return;

        // Відновлюємо початкову позицію
        selectedNode.setPosition(startPosition.x, startPosition.y, startPosition.z);

        // Також відновлюємо початкове обертання та масштаб, якщо вони були змінені
        if (currentMode == TransformMode.ROTATE) {
            selectedNode.setRotation(startRotation.x, startRotation.y, startRotation.z);
        } else if (currentMode == TransformMode.SCALE) {
            selectedNode.setScale(startScale.x, startScale.y, startScale.z);
        }

        // Оновлюємо позицію стрілок
        arrowsRootNode.setPosition(startPosition.x, startPosition.y, startPosition.z);

        isDragging = false;
        startMousePosition = null;
        currentAxis = TransformAxis.NONE;

        System.out.println("Трансформацію скасовано: " + selectedNode.getName());
    }

    public boolean isDragging() {
        return isDragging;
    }

    /**
     * Очищає ресурси при завершенні роботи.
     */
    public void cleanup() {
        // Очищаємо ресурси, пов'язані з мешами
        if (arrowXMesh != null) {
            arrowXMesh.cleanup();
        }
        if (arrowYMesh != null) {
            arrowYMesh.cleanup();
        }
        if (arrowZMesh != null) {
            arrowZMesh.cleanup();
        }
    }
}