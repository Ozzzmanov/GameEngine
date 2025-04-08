package org.example;

import org.example.Editor.Editor;
import org.example.Editor.TransformTool;
import org.example.GUI.GUI;
import org.example.Render.Shadow.IShadowMap;
import org.example.Render.Shadow.ShadowMap;
import org.example.Render.Shadow.ShadowRenderer;
import org.example.Scene.LoadScene;
import org.example.Scene.SaveScene;
import org.example.Scene.Scene;
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

    private int mainShaderProgram;
    private int lightShaderProgram;
    private int gridShaderProgram;
    private int shadowShaderProgram;

    private Camera camera;
    private InputManager inputManager;
    private Node node;
    private Grid grid;
    private Editor editor;
    private TransformTool transformTool;
    private GUI gui;
    private IShadowMap shadowMap;

    private Scene scene;

    private void run() {
        init();
        loop();

        //FIXME: Очистка должна быть отдельно
        for (Mesh mesh : meshes) {
            mesh.cleanup();
        }

        // Удаляем шейдерные программы
        glDeleteProgram(mainShaderProgram);
        glDeleteProgram(lightShaderProgram);
        glDeleteProgram(gridShaderProgram);
        glDeleteProgram(shadowShaderProgram);
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
        if(shadowMap != null) {
            shadowMap.cleanup(); // Очистка ресурсов теневой карты
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
        window = glfwCreateWindow(WIDTH, HEIGHT, "HexEngine", NULL, NULL);
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

        //FIXME: Загружка шейдеров должна быть отдельно
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
            gridShaderProgram = ShaderLoader.loadShader(
                    "/Shader/gridShaderProgram/vertex_shader.glsl",
                    "/Shader/gridShaderProgram/fragment_shader.glsl"
            );
            shadowShaderProgram = ShaderLoader.loadShader(
                    "/Shader/shadowShaderProgram/shadow_vertex.glsl",
                    "/Shader/shadowShaderProgram/shadow_fragment.glsl"
            );

        } catch (IOException e) {
            throw new RuntimeException("Ошибка загрузки шейдеров: " + e.getMessage());
        }

        // Создание карты теней
        shadowMap = new ShadowMap(shadowShaderProgram);

        // Создание и настройка камеры
        camera = new Camera(new Vector3f(3.0f, 3.0f, 3.0f), new Vector3f(0.0f, 1.0f, 0.0f), -135.0f, -30.0f);
        inputManager = new InputManager(window, camera);




        //FIXME: Загрузка моделей должна быть отдельно

        // Загрузка моделей и настройка материалов
        Mesh gold = ObjectLoader.loadObjModel("/Object/Models/gold.obj");
        gold.setShaderMaterial(ShaderMaterial.createGold());

        Mesh sphereMesh = ObjectLoader.loadObjModel("/Object/Primitives/sphere.obj");
        sphereMesh.setShaderMaterial(ShaderMaterial.createSilver());

        Mesh cubes = ObjectLoader.loadObjModel("/Object/Primitives/cube.obj");
        cubes.setShaderMaterial(ShaderMaterial.createTexturedMaterial("/Textures/primitivesPack/wood_01.png"));

        Mesh barrel = ObjectLoader.loadObjModel("/Object/Models/barrel.obj");
        barrel.setShaderMaterial(ShaderMaterial.createTexturedMaterial("/Textures/barrel/Barrel_BaseColor.jpg"));

        Mesh sun = ObjectLoader.loadObjModel("/Object/Primitives/sphere.obj");

        scene = new Scene("default", LoadScene.loadScene("Scene/test.json"));
//        scene = new Scene("default", node = new Node("empty"));
        node = scene.getRootNode();

        grid = new Grid();
        editor = new Editor(inputManager, viewport, camera, node);
        camera.setEditor(editor); // Передаем editor


        transformTool = new TransformTool(editor, inputManager,camera,viewport,node);

        gui = new GUI(window,editor,node,transformTool);
    }

    private void loop() {
        // Колір фону
        glClearColor(0.3f, 0.3f, 0.3f, 1.0f);

        ShadowRenderer shadowRenderer = new ShadowRenderer(shadowMap, shadowShaderProgram);
        //FIXME: Оптимизировать
        while (!glfwWindowShouldClose(window)) {
            // Оновлюємо контролер вводу
            inputManager.update();

            // Отримуємо позицію камери та матриці виду і проекції
            Vector3f cameraPosition = camera.getPosition();
            Matrix4f viewMatrix = camera.getViewMatrix();
            Matrix4f projectionMatrix = viewport.getProjectionMatrix();

            // Отримуємо вузли джерел світла
            List<Node> lightNodes = node.getLightNodes();
            Vector3f lightPos = lightNodes.isEmpty() ? new Vector3f(5, 5, 5) : lightNodes.get(0).getPosition();

            // Оновлюємо матрицю простору світла для тіней
            shadowMap.updateLightSpaceMatrix(lightPos, new Vector3f(0, 0, 0), 0.1f, 25.0f);

            // Перший прохід - рендеринг в карту тіней
            shadowMap.bindForShadowPass();
            // Рендеримо тільки основні об'єкти (не джерела світла та сітку)
            for (Node child : node.getChildren()) {
                if (child.getNodeType() == Node.NodeType.DEFAULT) {
                    // Для тіньової карти використовуємо тільки основні вузли
                    shadowRenderer.renderNodeShadows(child, shadowMap.getLightSpaceMatrix());
                }
            }
            shadowMap.unbind(WIDTH, HEIGHT);

            // Другий прохід - основний рендеринг
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Прив'язуємо текстуру тіньової карти
            shadowMap.bindDepthMapForReading(1); // Текстурний блок 1

            // Для кожної шейдерної програми передаємо тіньову карту
            glUseProgram(mainShaderProgram);
            int shadowMapLoc = glGetUniformLocation(mainShaderProgram, "shadowMap");
            glUniform1i(shadowMapLoc, 1);  // Текстурний блок 1

            int lightSpaceMatrixLoc = glGetUniformLocation(mainShaderProgram, "lightSpaceMatrix");
            float[] lightSpaceMatrixData = new float[16];
            shadowMap.getLightSpaceMatrix().get(lightSpaceMatrixData);
            glUniformMatrix4fv(lightSpaceMatrixLoc, false, lightSpaceMatrixData);

            // Рендеримо сцену звичайним чином
            for (Node child : node.getChildren()) {
                switch (child.getNodeType()) {
                    case DEFAULT:
                        child.render(mainShaderProgram, viewMatrix, projectionMatrix, cameraPosition);
                        break;
                    case LIGHT:
                        child.render(lightShaderProgram, viewMatrix, projectionMatrix, cameraPosition);
                        break;
                }
            }


            grid.render(gridShaderProgram, viewMatrix, projectionMatrix);
            editor.update();
            editor.renderSelection();

            transformTool.update();

            gui.render();

            // Вимикаємо шейдер
            glUseProgram(0);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    public static void main(String[] args) {
        new Core().run();
    }
}