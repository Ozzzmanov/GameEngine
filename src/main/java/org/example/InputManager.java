package org.example;

import static org.lwjgl.glfw.GLFW.*;

public class InputManager {
    private Camera camera;
    private long window;

    // Масив стану клавіш
    private boolean[] keys = new boolean[1024];
    // Додаткові масиви для нових подій клавіш
    private boolean[] keysPressed = new boolean[1024]; // Натиснуто у поточному кадрі
    private boolean[] keysReleased = new boolean[1024]; // Відпущено у поточному кадрі

    // Стан миші
    private float lastX, lastY;
    // Координати миші
    private float mouseX, mouseY;
    private boolean firstMouse = true;
    private boolean[] mouseButtons = new boolean[8];
    private float scrollOffset = 0.0f;

    // Таймінг
    private float deltaTime = 0.0f;
    private float lastFrame = 0.0f;

    // Налаштування швидкості руху
    private float movementSpeed = 2.5f;
    private float mouseSensitivity = 1.2f;

    // Режим роботи
    private boolean cursorLocked = true;


    public InputManager(long window, Camera camera) {
        this.window = window;
        this.camera = camera;

        setupCallbacks();
    }

    /**
     * Налаштовує зворотні виклики для обробки подій вводу
     */
    private void setupCallbacks() {
        // Налаштування обробників подій клавіатури
        glfwSetKeyCallback(window, (windowHandle, key, scancode, action, mods) -> {
            if (key >= 0 && key < 1024) {
                if (action == GLFW_PRESS) {
                    keysPressed[key] = !keys[key]; // Запам'ятовуємо нові натискання
                    keys[key] = true;
                } else if (action == GLFW_RELEASE) {
                    keysReleased[key] = keys[key]; // Запам'ятовуємо нові відпускання
                    keys[key] = false;
                }
            }
        });

        // Налаштування обробників подій позиції миші
        glfwSetCursorPosCallback(window, (windowHandle, xpos, ypos) -> {


            if (cursorLocked) {
                if (firstMouse) {
                    lastX = (float) xpos;
                    lastY = (float) ypos;
                    firstMouse = false;
                }

                float xOffset = (float) xpos - lastX;
                float yOffset = lastY - (float) ypos;

                lastX = (float) xpos;
                lastY = (float) ypos;

                processMouseMovement(xOffset, yOffset);
            } else {
                mouseX = (float) xpos;
                mouseY = (float) ypos;
            }
        });

        // Налаштування обробників кнопок миші
        glfwSetMouseButtonCallback(window, (windowHandle, button, action, mods) -> {
            if (button >= 0 && button < 8) {
                mouseButtons[button] = action == GLFW_PRESS;
            }
        });

        // Налаштування обробника колеса миші
        glfwSetScrollCallback(window, (windowHandle, xoffset, yoffset) -> {
            scrollOffset = (float) yoffset;
            processMouseScroll(scrollOffset);
        });

        // Встановлення режиму захоплення курсору
        updateCursorMode();
    }

    /**
     * Викликається кожен кадр для оновлення стану контролера
     */
    public void update() {
        // Обчислення deltaTime
        float currentFrame = (float) glfwGetTime();
        deltaTime = currentFrame - lastFrame;
        lastFrame = currentFrame;

        // Обробка клавіатурного вводу
        processKeyboardInput();

        // Скидання натискань/відпускань для наступного кадру
        resetFrameInputState();
    }

    private void processKeyboardInput() {
        // Рух камери
        if (keys[GLFW_KEY_W]) {
            camera.processKeyboard(Camera.CameraMovement.FORWARD, deltaTime * movementSpeed);
        }
        if (keys[GLFW_KEY_S]) {
            camera.processKeyboard(Camera.CameraMovement.BACKWARD, deltaTime * movementSpeed);
        }
        if (keys[GLFW_KEY_A]) {
            camera.processKeyboard(Camera.CameraMovement.LEFT, deltaTime * movementSpeed);
        }
        if (keys[GLFW_KEY_D]) {
            camera.processKeyboard(Camera.CameraMovement.RIGHT, deltaTime * movementSpeed);
        }
        if (keys[GLFW_KEY_SPACE]) {
            camera.processKeyboard(Camera.CameraMovement.UP, deltaTime * movementSpeed);
        }
        if (keys[GLFW_KEY_LEFT_SHIFT]) {
            camera.processKeyboard(Camera.CameraMovement.DOWN, deltaTime * movementSpeed);
        }
        // Позиція камери
        if (keys[GLFW_KEY_1]) {
            camera.positionKeyboard(Camera.CameraPosition.FRONT);
        }
        if (keys[GLFW_KEY_1] && keys[GLFW_KEY_LEFT_CONTROL]) {
            camera.positionKeyboard(Camera.CameraPosition.BACK);
        }
        if (keys[GLFW_KEY_2]) {
            camera.positionKeyboard(Camera.CameraPosition.RIGHT);
        }
        if (keys[GLFW_KEY_2] && keys[GLFW_KEY_LEFT_CONTROL]) {
            camera.positionKeyboard(Camera.CameraPosition.LEFT);
        }
        if (keys[GLFW_KEY_3]) {
            camera.positionKeyboard(Camera.CameraPosition.TOP);
        }
        if (keys[GLFW_KEY_3] && keys[GLFW_KEY_LEFT_CONTROL]) {
            camera.positionKeyboard(Camera.CameraPosition.BOTTOM);
        }


//        // Вихід з програми при натисканні ESC
//        if (keys[GLFW_KEY_ESCAPE]) {
//            glfwSetWindowShouldClose(window, true);
//        }

        // Перемикання режиму миші при натисканні TAB
        if (keysPressed[GLFW_KEY_TAB]) {
            toggleCursor();
        }
    }

    private void processMouseMovement(float xOffset, float yOffset) {
        xOffset *= mouseSensitivity;
        yOffset *= mouseSensitivity;

        camera.processMouseMovement(xOffset, yOffset, true);
    }

    private void processMouseScroll(float yOffset) {
        camera.processMouseScroll(yOffset);
    }

    /**
     * Перемикає режим видимості курсору
     */
    public void toggleCursor() {
        cursorLocked = !cursorLocked;
        updateCursorMode();
    }

    /**
     * Оновлює режим курсора відповідно до налаштувань
     */
    private void updateCursorMode() {
        if (cursorLocked) {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            firstMouse = true;
        } else {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        }
    }

    /**
     * Скидає стан вводу одноразових подій перед наступним кадром
     */
    private void resetFrameInputState() {
        // Скидання подій натискання/відпускання клавіш
        for (int i = 0; i < 1024; i++) {
            keysPressed[i] = false;
            keysReleased[i] = false;
        }

        // Скидання прокрутки миші
        scrollOffset = 0.0f;
    }

    /**
     * Перевіряє чи була натиснута клавіша у поточному кадрі
     */
    public boolean isKeyPressed(int key) {
        return keysPressed[key];
    }


    /**
     * Перевіряє чи була відпущена клавіша у поточному кадрі
     */
    public boolean isKeyReleased(int key) {
        return keysReleased[key];
    }

    /**
     * Перевіряє чи утримується клавіша
     */
    public boolean isKeyDown(int key) {
        return keys[key];
    }

    /**
     * Перевіряє чи натиснута кнопка миші
     */
    public boolean isMouseButtonPressed(int button) {
        return mouseButtons[button];
    }

    // Геттери і сеттери
    public float getMovementSpeed() {
        return movementSpeed;
    }

    public void setMovementSpeed(float movementSpeed) {
        this.movementSpeed = movementSpeed;
    }

    public float getMouseSensitivity() {
        return mouseSensitivity;
    }

    public void setMouseSensitivity(float mouseSensitivity) {
        this.mouseSensitivity = mouseSensitivity;
    }

    public float getDeltaTime() {
        return deltaTime;
    }

    public float getMouseX() {
        return mouseX;
    }

    public float getMouseY() {
        return mouseY;
    }

    public boolean isCursorLocked() {
        return cursorLocked;
    }
}