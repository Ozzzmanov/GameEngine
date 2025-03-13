package org.example;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Core {
    private long window;
    private int WIDTH = 1280;
    private int HEIGHT = 720;

    private List<Mesh> meshes = new ArrayList<>();
    private Grid grid;
    private Camera camera;
    private InputManager InputManager;
    private Viewport viewport;
    private Editor editor;
    private Node rootNode;

    private void run() {
        init();
        loop();

        // Очистка памяти
        rootNode.cleanup();

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit())
            throw new IllegalStateException("Помилка ініціалізації GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(WIDTH, HEIGHT, "Рендеринг Mesh", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Помилка створення вікна GLFW");
        // Init Viewport
        viewport = new Viewport(WIDTH, HEIGHT);

        // Встановлюємо callback для зміни розміру вікна
        glfwSetFramebufferSizeCallback(window, (window, width, height) -> {
            WIDTH = width;
            HEIGHT = height;
            viewport.resize(width, height);
        });

        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetWindowSize(window, pWidth, pHeight);
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        GL.createCapabilities();

        glEnable(GL_DEPTH_TEST); // Глубина для 3D

        // Ініціалізація сітки
        grid = new Grid();
        grid.setGridSize(0.2f);
        grid.setGridExtent(5.0f);

        // Ініціалізація мешу
        Mesh goldMesh = ImportObj.loadObjModel("/Object/gold.obj");
        Mesh cubeMesh = ImportObj.loadObjModel("/Object/cube.obj");
        meshes.add(goldMesh);

        // Створення та налаштування камери
        camera = new Camera(new Vector3f(3.0f, 3.0f, 3.0f), new Vector3f(0.0f, 1.0f, 0.0f), -135.0f, -30.0f);
        InputManager = new InputManager(window, camera);

        editor = new Editor(InputManager, viewport, camera, meshes);

        // Сцена
        rootNode = new Node("rootNode");
        Node goldNode = new Node("goldNode");
        goldNode.addMesh(goldMesh);



        Node cubeNode = new Node("cubeNode");
        cubeNode.setPosition(2.0f, 0.0f, 0.0f);
        cubeNode.setScale(0.5f, 0.5f, 0.5f);
        cubeNode.addMesh(cubeMesh);

        rootNode.addChild(goldNode);
        goldNode.addChild(cubeNode);


    }


    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Оновлюємо контролер вводу
            InputManager.update();

            editor.update();

            // Налаштування проекції та вигляду за допомогою Viewport
            viewport.setupProjectionMatrix(camera);
            viewport.applyViewMatrix(camera);


            rootNode.renderWireframe();

            editor.renderSelection();

            grid.draw();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    public static void main(String[] args) {
        new Core().run();
    }
}