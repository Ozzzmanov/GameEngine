package org.example;

import org.example.Editor.Editor;
import org.example.Editor.TransformTool;
import org.example.GUI.GUI;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;


public class Core {
    private long window;
    private int WIDTH = 1280;
    private int HEIGHT = 720;

    private Viewport viewport;
    private List<Mesh> meshes = new ArrayList<>();

    // Shader program для основного рендеринга
    private int mainShaderProgram;
    private int lightShaderProgram;
    private Camera camera;
    private InputManager inputManager;
    private Node node;

    private Grid grid;

    private Editor editor;

    private TransformTool transformTool;
    private GUI gui;


    private void run() {
        init();
        loop();

        for (Mesh mesh : meshes) {
            mesh.cleanup();
        }

        // Удаляем шейдерные программы
        glDeleteProgram(mainShaderProgram);
        glDeleteProgram(lightShaderProgram);
        meshes.clear();

        if (node != null) {
            node.cleanup();
        }
        if(grid != null){
            grid.cleanup();
        }
        if(editor != null){
            editor.cleanup();
        }
        if(transformTool != null){
            transformTool.cleanup();
        }


        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit())
            throw new IllegalStateException("Ошибка инициализации GLFW");

        // Настройка GLFW для OpenGL 3.3
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        // Создание окна
        window = glfwCreateWindow(WIDTH, HEIGHT, "3D Рендеринг с шейдерами", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Ошибка создания окна GLFW");

        // Инициализация Viewport
        viewport = new Viewport(WIDTH, HEIGHT);

        // Устанавливаем callback для изменения размера окна
        glfwSetFramebufferSizeCallback(window, (window, width, height) -> {
            WIDTH = width;
            HEIGHT = height;
            viewport.resize(width, height);
            editor.resizePickingFBO(width, height); // Редактор
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
        glfwSwapInterval(1); // Включаем вертикальную синхронизацию
        glfwShowWindow(window);

        // Загрузка всех функций OpenGL для текущего контекста
        GL.createCapabilities();

        // Включаем тест глубины для 3D
        glEnable(GL_DEPTH_TEST);
        // Для прозрачных материалов
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        try {
            // Загружаем шейдеры
            mainShaderProgram = ShaderLoader.loadShader(
                    "/Shader/mainShaderProgram/vertex_shader.glsl",
                    "/Shader/mainShaderProgram/fragment_shader.glsl"
            );
            lightShaderProgram = ShaderLoader.loadShader(
                    "/Shader/lightShaderProgram/light_vertex.glsl",
                    "/Shader/lightShaderProgram/light_fragment.glsl"
            );

        } catch (IOException e) {
            throw new RuntimeException("Ошибка загрузки шейдеров: " + e.getMessage());
        }




        // Создание и настройка камеры
        camera = new Camera(new Vector3f(3.0f, 3.0f, 3.0f), new Vector3f(0.0f, 1.0f, 0.0f), -135.0f, -30.0f);
        inputManager = new InputManager(window, camera);

        // Загрузка моделей и настройка материалов
        Mesh gold = ImportObj.loadObjModel("/Object/gold.obj");
        gold.setShaderMaterial(ShaderMaterial.createGold()); // Применяем золотой материал к кубу

        Mesh sphereMesh = ImportObj.loadObjModel("/Object/sphere.obj");
        sphereMesh.setShaderMaterial(ShaderMaterial.createSilver());

        Mesh cubes = ImportObj.loadObjModel("/Object/cube.obj");
        cubes.setShaderMaterial(ShaderMaterial.createSilver());

        Mesh sun = ImportObj.loadObjModel("/Object/sphere.obj");

        node = new Node("rootNode");
            Node meshNode = new Node("meshNode");
                Node cubeNode = new Node("cubeNode");
                    cubeNode.addMesh(cubes);
                    cubeNode.setPosition(2,0,-5);
                Node goldNode = new Node("goldNode");
                    goldNode.addMesh(gold);
                    goldNode.setPosition(4,0,4);
                Node sphere = new Node("sphereNode");
                    sphere.addMesh(sphereMesh);
            Node lightNode = new Node("lightNode");
                lightNode.addMesh(sun);
                lightNode.setPosition(5,5,5);

        node.addChild(meshNode);
            meshNode.addChild(cubeNode);
            meshNode.addChild(goldNode);
            meshNode.addChild(sphere);
        node.addChild(lightNode);



        grid = new Grid();
        editor = new Editor(inputManager, viewport, camera, node);

        transformTool = new TransformTool(editor, inputManager,camera,viewport,node);

        gui = new GUI(window,editor,node);
    }

    private void loop() {

        // Цвет фона
        glClearColor(0.3f, 0.3f, 0.3f, 1.0f);

        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Обновляем контроллер ввода
            inputManager.update();

            // Получаем матрицы вида и проекции от камеры и вьюпорта
            Matrix4f viewMatrix = camera.getViewMatrix();
            Matrix4f projectionMatrix = viewport.getProjectionMatrix();

            for (Node child : node.getChildren()) {
                switch (child.getName()) {
                    case "meshNode":
                        child.render(mainShaderProgram, viewMatrix, projectionMatrix);
                        break;
                    case "lightNode":
                        child.renderLight(lightShaderProgram, viewMatrix, projectionMatrix);
                        break;
                }
            }



            grid.render(mainShaderProgram, viewMatrix, projectionMatrix);

            editor.update();
            editor.renderSelection();

            transformTool.update();

            gui.render();

            // Отключаем шейдер
            glUseProgram(0);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    public static void main(String[] args) {
        new Core().run();
    }
}