package org.example.Editor;

import org.example.*;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.AbstractMap;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

import java.util.Map;
import java.util.AbstractMap.SimpleEntry;
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
    private final float ROTATE_SENSITIVITY = 0.02f;
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


    // Меші для кутів ейлера
    private Mesh circleYawMesh;
    private Mesh circlePitchMesh;
    private Mesh circleRollMesh;

    // Ноди для кутів ейлера
    private Node circleYawNode;
    private Node circlePitchNode;
    private Node circleRollNode;
    private Node circleRootNode;

    // Меші для векторів scale
    private Mesh vectorXScaleMesh;
    private Mesh vectorYScaleMesh;
    private Mesh vectorZScaleMesh;

    // Ноди для векторів scale
    private Node vectorXNode;
    private Node vectorYNode;
    private Node vectorZNode;
    private Node scaleRootNode;

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

        initArrows();
        initCircle();
        initVector();



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

    private void initArrows(){
        arrowsRootNode = new Node("arrowsRootNode");

        // Завантажуємо меш стрілки
        arrowXMesh = ImportObj.loadObjModel("/Object/Tools/Arrow/ArrowMeshX.obj");
        arrowYMesh = ImportObj.loadObjModel("/Object/Tools/Arrow/ArrowMeshY.obj");
        arrowZMesh = ImportObj.loadObjModel("/Object/Tools/Arrow/ArrowMeshZ.obj");

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
    }

    private void initCircle(){
        circleRootNode = new Node("circleRootNode");

        // Завантажуємо меш кутів ейлера
        circleYawMesh = ImportObj.loadObjModel("/Object/Tools/Circle/CircleMeshYaw.obj");
        circlePitchMesh = ImportObj.loadObjModel("/Object/Tools/Circle/CircleMeshPitch.obj");
        circleRollMesh = ImportObj.loadObjModel("/Object/Tools/Circle/CircleMeshRoll.obj");

        // Встановлюємо матеріали для кутів ейлера (червоний, зелений, синій)
        circleYawMesh.setShaderMaterial(ShaderMaterial.createRed());
        circlePitchMesh.setShaderMaterial(ShaderMaterial.createGreen());
        circleRollMesh.setShaderMaterial(ShaderMaterial.createBlue());

        // Створюємо ноди для кутів
        circleYawNode = new Node("circleYawNode");
        circlePitchNode = new Node("circlePitchNode");
        circleRollNode = new Node("circleRollNode");

        // Додаємо меші до нод
        circleYawNode.addMesh(circleYawMesh);
        circlePitchNode.addMesh(circlePitchMesh);
        circleRollNode.addMesh(circleRollMesh);


        // Додаємо кути до кореневого вузла
        circleRootNode.addChild(circleYawNode);
        circleRootNode.addChild(circlePitchNode);
        circleRootNode.addChild(circleRollNode);

        // Ховаємо кути, поки немає вибраного об'єкта
        circleRootNode.setScale(0, 0, 0);

        editor.setCircleRootNode(circleRootNode);
    }

    private void initVector(){
        scaleRootNode = new Node("scaleRootNode");

        // Завантажуємо меш векторів scale
        vectorXScaleMesh = ImportObj.loadObjModel("/Object/Tools/scaleVector/scaleVectorX.obj");
        vectorYScaleMesh = ImportObj.loadObjModel("/Object/Tools/scaleVector/scaleVectorY.obj");
        vectorZScaleMesh = ImportObj.loadObjModel("/Object/Tools/scaleVector/scaleVectorZ.obj");

        // Встановлюємо матеріали
        vectorXScaleMesh.setShaderMaterial(ShaderMaterial.createRed());
        vectorYScaleMesh.setShaderMaterial(ShaderMaterial.createGreen());
        vectorZScaleMesh.setShaderMaterial(ShaderMaterial.createBlue());

        // Створюємо ноди
        vectorXNode = new Node("vectorXNode");
        vectorYNode = new Node("vectorYNode");
        vectorZNode = new Node("vectorZNode");

        // Додаємо меші до нод
        vectorXNode.addMesh(vectorXScaleMesh);
        vectorYNode.addMesh(vectorYScaleMesh);
        vectorZNode.addMesh(vectorZScaleMesh);

        // Додаємодо кореневого вузла
        scaleRootNode.addChild(vectorXNode);
        scaleRootNode.addChild(vectorYNode);
        scaleRootNode.addChild(vectorZNode);

        scaleRootNode.setScale(0, 0, 0);

        editor.setVectorScaleRootNode(scaleRootNode);
    }


    @Override
    public void onNodeSelected(Node node) {
        Map<Node, TransformAxis> axisMap = Map.of(
                arrowXNode, TransformAxis.X,
                arrowYNode, TransformAxis.Y,
                arrowZNode, TransformAxis.Z,
                circleYawNode, TransformAxis.X,
                circlePitchNode, TransformAxis.Y,
                circleRollNode, TransformAxis.Z,
                vectorXNode, TransformAxis.X,
                vectorYNode, TransformAxis.Y,
                vectorZNode, TransformAxis.Z
        );

        if (axisMap.containsKey(node)) {
            currentAxis = axisMap.get(node);
            if (node == circleYawNode || node == circlePitchNode || node == circleRollNode) {
                currentMode = TransformMode.ROTATE;
            } else if (node == vectorXNode || node == vectorYNode || node == vectorZNode) {
                currentMode = TransformMode.SCALE;
            } else {
                currentMode = TransformMode.TRANSLATE;
            }
            startDragging();
            return;
        }

        // Якщо вибраний звичайний вузол
        selectedNode = node;

        if (node != null) {
            startPosition = new Vector3f(node.getPosition());
            startRotation = new Quaternionf(node.getRotation());
            startScale = new Vector3f(node.getScale());

            // Оновлюємо видимість інструментів відповідно до поточного режиму
            updateToolsVisibility();
        } else {
            arrowsRootNode.setScale(0, 0, 0);
            circleRootNode.setScale(0, 0, 0);
            scaleRootNode.setScale(0, 0, 0);
        }
    }


    @Override
    public void onSelectionCleared() {
        selectedNode = null;
        isDragging = false;
        currentAxis = TransformAxis.NONE;

        if (rootNode.getChildren().contains(arrowsRootNode)) {
            rootNode.removeChild(arrowsRootNode);
        }
        if (rootNode.getChildren().contains(circleRootNode)) {
            rootNode.removeChild(circleRootNode);
        }
        if (rootNode.getChildren().contains(scaleRootNode)) {
            rootNode.removeChild(scaleRootNode);
        }

        // Ховаємо
        arrowsRootNode.setScale(0, 0, 0);
        circleRootNode.setScale(0, 0, 0);
        scaleRootNode.setScale(0, 0, 0);
    }

    @Override
    public void onSceneChanged() {
        // Оновлюємо положення активного інструменту при зміні сцени
        if (selectedNode != null) {
            updateToolsVisibility();
        }
    }

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

        // Рендеримо тільки активний інструмент трансформації, якщо є вибраний вузол
        if (selectedNode != null && !isDragging) {
            switch (currentMode) {
                case TRANSLATE:
                    arrowsRootNode.render(mainShaderProgram, camera.getViewMatrix(), viewport.getProjectionMatrix());
                    break;
                case ROTATE:
                    circleRootNode.render(mainShaderProgram, camera.getViewMatrix(), viewport.getProjectionMatrix());
                    break;
                case SCALE:
                    scaleRootNode.render(mainShaderProgram, camera.getViewMatrix(), viewport.getProjectionMatrix());
                    break;
            }
        }
    }

    public void setTransformMode(TransformMode mode) {
        currentMode = mode;

        updateToolsVisibility();
    }

    /**
     * Оновлює видимість інструментів трансформації в залежності від поточного режиму
     */
    private void updateToolsVisibility() {
        if (selectedNode == null) return;

        float scale = 2.5f;
        Vector3f position = selectedNode.getPosition();

        // Ховаємо всі інструменти спочатку
        arrowsRootNode.setScale(0, 0, 0);
        circleRootNode.setScale(0, 0, 0);
        scaleRootNode.setScale(0, 0, 0);

        // Показуємо тільки активний інструмент
        switch (currentMode) {
            case TRANSLATE:
                arrowsRootNode.setPosition(position.x, position.y, position.z);
                arrowsRootNode.setScale(scale, scale, scale);
                break;
            case ROTATE:
                circleRootNode.setPosition(position.x, position.y, position.z);
                circleRootNode.setScale(scale, scale, scale);
                break;
            case SCALE:
                scaleRootNode.setPosition(position.x, position.y, position.z);
                scaleRootNode.setScale(scale, scale, scale);
                break;
        }
    }

    public TransformMode getCurrentMode() {
        return currentMode;
    }

    private void startDragging() {
        if (selectedNode == null) return;

        isDragging = true;
        startPosition = new Vector3f(selectedNode.getPosition());
        startMousePosition = new Vector2f(inputManager.getMouseX(), inputManager.getMouseY());

        // Вимикаємо блокування курсору для зручнішого перетягування
        if (inputManager.isCursorLocked()) {
            inputManager.toggleCursor();
        }

    }

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

        // Оновлюємо позицію кутів
        circleRootNode.setPosition(newPosition.x, newPosition.y, newPosition.z);

        scaleRootNode.setPosition(newPosition.x, newPosition.y, newPosition.z);
    }

    private void handleRotation() {
        Vector2f currentMouse = new Vector2f(inputManager.getMouseX(), inputManager.getMouseY());
        Vector2f delta = new Vector2f(currentMouse).sub(startMousePosition);
        delta.mul(ROTATE_SENSITIVITY);

        // Получаем текущий кватернион вращения
        Quaternionf currentRotation = selectedNode.getRotation();

        // Создаем временные кватернионы для инкрементального вращения
        Quaternionf deltaRotation = new Quaternionf();

        // Получаем векторы камеры
        Vector3f cameraRight = camera.getRightVector();
        Vector3f cameraUp = camera.getUpVector();
        Vector3f cameraForward = camera.getFront();

        // Определяем оси вращения в зависимости от текущего режима
        Vector3f rotationAxis = new Vector3f();
        float angle = 0;

        switch (currentAxis) {
            case X:
                // Используем глобальную ось X, скорректированную относительно камеры
                rotationAxis.set(1, 0, 0);
                angle = (cameraRight.dot(rotationAxis) > 0) ? delta.y : -delta.y;
                deltaRotation.fromAxisAngleRad(rotationAxis, (float)Math.toRadians(angle));
                break;
            case Y:
                rotationAxis.set(0, 1, 0);
                angle = (cameraUp.dot(rotationAxis) > 0) ? delta.x : -delta.x;
                deltaRotation.fromAxisAngleRad(rotationAxis, (float)Math.toRadians(angle));
                break;
            case Z:
                rotationAxis.set(0, 0, 1);
                angle = (cameraForward.dot(rotationAxis) > 0) ? delta.x : -delta.x;
                deltaRotation.fromAxisAngleRad(rotationAxis, (float)Math.toRadians(angle));
                break;
            case XY:
                // Для комбинированных вращений создаем два кватерниона и умножаем их
                Quaternionf xRot = new Quaternionf().fromAxisAngleRad(
                        new Vector3f(1, 0, 0),
                        (float)Math.toRadians(delta.y * Math.signum(cameraRight.dot(new Vector3f(1, 0, 0))))
                );
                Quaternionf yRot = new Quaternionf().fromAxisAngleRad(
                        new Vector3f(0, 1, 0),
                        (float)Math.toRadians(delta.x * Math.signum(cameraUp.dot(new Vector3f(0, 1, 0))))
                );
                deltaRotation.set(xRot.mul(yRot));
                break;
            // Аналогично для других комбинаций осей (XZ, YZ, XYZ)
            case XZ:
                // Комбинированное вращение вокруг осей X и Z
                Quaternionf xRotXZ = new Quaternionf().fromAxisAngleRad(
                        new Vector3f(1, 0, 0),
                        (float)Math.toRadians(delta.y * Math.signum(cameraRight.dot(new Vector3f(1, 0, 0))))
                );
                Quaternionf zRotXZ = new Quaternionf().fromAxisAngleRad(
                        new Vector3f(0, 0, 1),
                        (float)Math.toRadians(delta.x * Math.signum(cameraForward.dot(new Vector3f(0, 0, 1))))
                );
                deltaRotation.set(xRotXZ.mul(zRotXZ));
                break;
            case YZ:
                // Комбинированное вращение вокруг осей Y и Z
                Quaternionf yRotYZ = new Quaternionf().fromAxisAngleRad(
                        new Vector3f(0, 1, 0),
                        (float)Math.toRadians(delta.y * Math.signum(cameraUp.dot(new Vector3f(0, 1, 0))))
                );
                Quaternionf zRotYZ = new Quaternionf().fromAxisAngleRad(
                        new Vector3f(0, 0, 1),
                        (float)Math.toRadians(delta.x * Math.signum(cameraForward.dot(new Vector3f(0, 0, 1))))
                );
                deltaRotation.set(yRotYZ.mul(zRotYZ));
                break;
            case XYZ:
                // Вращение по трем осям с учетом направления камеры
                Quaternionf xRotXYZ = new Quaternionf().fromAxisAngleRad(
                        new Vector3f(1, 0, 0),
                        (float)Math.toRadians(delta.y * Math.signum(cameraRight.dot(new Vector3f(1, 0, 0))))
                );
                Quaternionf yRotXYZ = new Quaternionf().fromAxisAngleRad(
                        new Vector3f(0, 1, 0),
                        (float)Math.toRadians(delta.x * Math.signum(cameraUp.dot(new Vector3f(0, 1, 0))))
                );
                deltaRotation.set(xRotXYZ.mul(yRotXYZ));
                break;
        }

        // Применяем инкрементальное вращение к текущему кватерниону
        // При умножении справа, вращение происходит в локальной системе координат объекта
        Quaternionf newRotation = new Quaternionf(currentRotation).mul(deltaRotation);
        newRotation.normalize(); // Нормализуем для избежания ошибок накопления

        // Обновляем вращение узла
        applyNewRotation(newRotation);

        // Обновляем визуальные индикаторы вращения
        updateRotationIndicators();
    }

    // Вспомогательный метод для применения кватерниона напрямую
    private void applyNewRotation(Quaternionf rotation) {
        // Предполагается, что у вас есть доступ к внутреннему кватерниону узла
        // Это можно либо реализовать через новый метод в классе Node,
        // либо подстроить под вашу текущую архитектуру
        selectedNode.setRotationQuaternion(rotation);
    }

    private void updateRotationIndicators() {
        // Обновляем позицию индикаторов вращения
        circleRootNode.setPosition(selectedNode.getPosition().x, selectedNode.getPosition().y, selectedNode.getPosition().z);
        scaleRootNode.setPosition(selectedNode.getPosition().x, selectedNode.getPosition().y, selectedNode.getPosition().z);

        // Здесь можно также обновить ориентацию кругов вращения, если это необходимо
        // Например, выровнять их по осям координат с учетом текущего вращения
    }

    private void handleScaling() {
        Vector2f currentMouse = new Vector2f(inputManager.getMouseX(), inputManager.getMouseY());
        Vector2f delta = new Vector2f(currentMouse).sub(startMousePosition);

        Vector3f cameraRight = camera.getRightVector();
        Vector3f cameraUp = camera.getUpVector();
        Vector3f cameraForward = camera.getFront();

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

    private void finishTransform() {
        isDragging = false;
        startMousePosition = null;
        currentAxis = TransformAxis.NONE;

        // Оновлюємо позицію активного інструменту
        if (selectedNode != null) {
            updateToolsVisibility();
        }

    }

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
        circleRootNode.setPosition(startPosition.x, startPosition.y, startPosition.z);
        scaleRootNode.setPosition(startPosition.x, startPosition.y, startPosition.z);


        isDragging = false;
        startMousePosition = null;
        currentAxis = TransformAxis.NONE;

    }

    public boolean isDragging() {
        return isDragging;
    }

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