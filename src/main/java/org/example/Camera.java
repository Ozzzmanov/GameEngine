package org.example;

import org.example.Editor.Editor;
import org.example.Editor.EditorListener;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera implements EditorListener {
    private Editor editor;

    // Позиція камери
    private Vector3f position;
    // Вектор напрямку (куди дивиться камера)
    private Vector3f front;
    // Вектор "вгору" для камери
    private Vector3f up;
    // Вектор "вправо" для камери
    private Vector3f right;
    // Світовий вектор "вгору"
    private final Vector3f worldUp;


    // Кути Ейлера
    private float yaw;   // Поворот вліво/вправо (навколо осі Y)
    private float pitch; // Поворот вгору/вниз (навколо осі X)

    // Налаштування камери
    private float moveSpeed = 2.5f;
    private float mouseSensitivity = 0.1f;
    private float zoom = 45.0f;

    private float offsetCamera = 12.0f;

    private Node selectedNode = null;

    /**
     * Конструктор з користувацькими параметрами
     *
     * @param position початкова позиція камери
     * @param up вектор "вгору"
     * @param yaw початковий кут повороту вліво/вправо
     * @param pitch початковий кут повороту вгору/вниз
     */
    public Camera(Vector3f position, Vector3f up, float yaw, float pitch) {
        this.position = position;
        this.worldUp = up;
        this.yaw = yaw;
        this.pitch = pitch;

        this.front = new Vector3f(0.0f, 0.0f, -1.0f);
        this.right = new Vector3f();
        this.up = new Vector3f();

        updateCameraVectors();
    }

    /**
     * Отримати матрицю вигляду для рендерингу
     *
     * @return матриця вигляду
     */
    public Matrix4f getViewMatrix() {
        Matrix4f view = new Matrix4f();
        Vector3f target = new Vector3f();
        position.add(front, target);
        return view.lookAt(position, target, up);
    }

    /**
     * Оновлення векторів камери на основі кутів Ейлера
     */
    private void updateCameraVectors() {
        // Обчислення нового вектора напрямку
        front.x = (float) Math.cos(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));
        front.y = (float) Math.sin(Math.toRadians(pitch));
        front.z = (float) Math.sin(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));
        front.normalize();

        // Обчислення вектора "вправо" та "вгору"
        front.cross(worldUp, right).normalize();
        right.cross(front, up).normalize();
    }

    /**
     * Переміщення камери на основі клавіші напрямку
     *
     * @param direction напрямок руху
     * @param deltaTime час між кадрами для плавного руху
     */
    public void processKeyboard(CameraMovement direction, float deltaTime) {
        float velocity = moveSpeed * deltaTime;
        Vector3f tempVec = new Vector3f();

        switch (direction) {
            case FORWARD:
                front.mul(velocity, tempVec);
                position.add(tempVec);
                break;
            case BACKWARD:
                front.mul(velocity, tempVec);
                position.sub(tempVec);
                break;
            case LEFT:
                right.mul(velocity, tempVec);
                position.sub(tempVec);
                break;
            case RIGHT:
                right.mul(velocity, tempVec);
                position.add(tempVec);
                break;
            case UP:
                up.mul(velocity, tempVec);
                position.add(tempVec);
                break;
            case DOWN:
                up.mul(velocity, tempVec);
                position.sub(tempVec);
                break;
        }
    }

    public void positionKeyboard(CameraPosition cameraPosition){
        Vector3f positionNode = selectedNode.getPosition();
        switch (cameraPosition){
            case FRONT:
                position.set(positionNode.x + offsetCamera, positionNode.y, positionNode.z);
                yaw = 180;
                pitch = 0;
                break;

            case BACK:
                position.set(positionNode.x - offsetCamera, positionNode.y, positionNode.z);
                yaw = 0;
                pitch = 0;
                break;

            case RIGHT:
                position.set(positionNode.x, positionNode.y, positionNode.z + offsetCamera);
                yaw = -90;
                pitch = 0;
                break;

            case LEFT:
                position.set(positionNode.x, positionNode.y, positionNode.z - offsetCamera);
                yaw = 90;
                pitch = 0;
                break;

            case TOP:
                position.set(positionNode.x, positionNode.y + offsetCamera, positionNode.z);
                yaw = 0;
                pitch = -90;
                break;

            case BOTTOM:
                position.set(positionNode.x, positionNode.y - offsetCamera, positionNode.z);
                yaw = 0;
                pitch = 90;
                break;
        }
        updateCameraVectors();
    }

    /**
     * Обробка руху миші для обертання камери
     *
     * @param xOffset зміщення по осі X
     * @param yOffset зміщення по осі Y
     * @param constrainPitch обмеження кута нахилу (щоб не перевертати камеру)
     */
    public void processMouseMovement(float xOffset, float yOffset, boolean constrainPitch) {
        xOffset *= mouseSensitivity;
        yOffset *= mouseSensitivity;

        yaw += xOffset;
        pitch += yOffset;

        // Обмеження кута нахилу для запобігання перевертання камери
        if (constrainPitch) {
            if (pitch > 89.0f) {
                pitch = 89.0f;
            }
            if (pitch < -89.0f) {
                pitch = -89.0f;
            }
        }

        updateCameraVectors();
    }

    /**
     * Обробка прокрутки миші для зміни масштабу
     *
     * @param yOffset зміщення колеса миші
     */
    public void processMouseScroll(float yOffset) {
        zoom -= yOffset;
        if (zoom < 1.0f) {
            zoom = 1.0f;
        }
        if (zoom > 45.0f) {
            zoom = 45.0f;
        }
    }

    // Гетери та сетери
    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getFront() {
        return front;
    }

    public float getZoom() {
        return zoom;
    }

    public void setPosition(Vector3f position) {
        this.position = position;
    }

    public void setMoveSpeed(float moveSpeed) {
        this.moveSpeed = moveSpeed;
    }

    public void setMouseSensitivity(float mouseSensitivity) {
        this.mouseSensitivity = mouseSensitivity;
    }

    public Vector3f getRightVector() {
        return right;
    }

    public Vector3f getUpVector() {
        return up;
    }


    // Перелік можливих напрямків руху камери
    public enum CameraMovement {
        FORWARD,
        BACKWARD,
        LEFT,
        RIGHT,
        UP,
        DOWN
    }

    public enum  CameraPosition {
        FRONT, // +X
        BACK, // -X
        RIGHT, // +Z
        LEFT, // -Z
        TOP, // +Y
        BOTTOM // -Y
    }

    @Override
    public void onNodeSelected(Node node) {
        selectedNode = node;
    }

    @Override
    public void onSelectionCleared() {

    }

    @Override
    public void onSceneChanged() {

    }

    public void setEditor(Editor editor) {
        this.editor = editor;
        this.editor.addEditorListener(this);
    }

}