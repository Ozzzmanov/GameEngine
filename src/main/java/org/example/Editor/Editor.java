package org.example.Editor;

import imgui.ImGui;
import org.example.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Editor implements NodeListener {
    private InputManager inputManager;
    private Viewport viewport;
    private Camera camera;
    private Node rootNode;
    private Node selectedNode = null;

    // Тип вузла для визначення ролі у сцені
    public enum NodeType {
        SCENE_OBJECT,     // Звичайний об'єкт сцени
        TRANSFORM_TOOL,   // Інструмент трансформації (стрілки)
        ROTATION_TOOL,    // Інструмент обертання (кола)
        SCALE_TOOL    // Інструмент масштабування
    }

    // Список кореневих вузлів для різних типів інструментів
    private Map<NodeType, Node> toolRootNodes = new HashMap<>();

    // Shader program для цветового выбора объектов
    private int pickingShaderProgram;

    // Framebuffer objects для off-screen рендеринга
    private int pickingFBO;
    private int pickingTexture;
    private int pickingDepthRBO;

    // Карта ID объектов и их цветов
    private Map<Integer, UUID> colorToNodeId = new HashMap<>();
    private Map<UUID, Integer> nodeIdToColor = new HashMap<>();
    private int nextColorId = 1; // Начинаем с 1, 0 зарезервировано для фона

    // Карта вузлів до їх типів
    private Map<UUID, NodeType> nodeIdToType = new HashMap<>();

    // События редактора
    private List<EditorListener> listeners = new ArrayList<>();

    public Editor(InputManager inputManager, Viewport viewport, Camera camera, Node rootNode) {
        this.inputManager = inputManager;
        this.viewport = viewport;
        this.camera = camera;
        this.rootNode = rootNode;

        // Реєструємо кореневий вузол сцени
        toolRootNodes.put(NodeType.SCENE_OBJECT, rootNode);

        // Инициализируем шейдер для цветового выбора
        initPickingShader();

        // Инициализируем FBO для off-screen рендеринга
        initPickingFBO();

        // Инициализируем все узлы сцены
        initSceneNodes();

        // Подписываемся на события корневого узла и всех дочерних узлов
        subscribeToNodeEvents(rootNode);
    }

    private void initPickingShader() {
        try {
            pickingShaderProgram = ShaderLoader.loadShader(
                    "/Shader/pickingShaderProgram/picking_vertex.glsl",
                    "/Shader/pickingShaderProgram/picking_fragment.glsl"
            );
        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки шейдеров для выбора: " + e.getMessage());
        }
    }

    private void initPickingFBO() {
        // Создаем FBO для выбора объектов
        pickingFBO = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, pickingFBO);

        // Создаем текстуру для цветового вывода
        pickingTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, pickingTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, viewport.getWidth(), viewport.getHeight(),
                0, GL_RGB, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, pickingTexture, 0);

        // Создаем буфер глубины
        pickingDepthRBO = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, pickingDepthRBO);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, viewport.getWidth(), viewport.getHeight());
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, pickingDepthRBO);

        // Проверяем статус FBO
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Ошибка создания FBO для выбора объектов");
        }

        // Возвращаемся к основному буферу кадра
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    // Инициализация всех узлов сцены
    private void initSceneNodes() {
        List<Node> allNodes = rootNode.getAllNodes();
        for (Node node : allNodes) {
            if (!node.getMeshes().isEmpty()) {
                // Присваиваем цвет только узлам с мешами
                assignColorToNode(node, NodeType.SCENE_OBJECT);
            }
        }
    }

    // Реєструємо ноду для пікингу
    public void registerNodeForPicking(Node node, NodeType nodeType) {
        if (!node.getMeshes().isEmpty() && !nodeIdToColor.containsKey(node.getId())) {
            assignColorToNode(node, nodeType);
        }
    }

    // Перевантажений метод для сумісності із старим кодом
    public void registerNodeForPicking(Node node) {
        registerNodeForPicking(node, NodeType.SCENE_OBJECT);
    }

    private void subscribeToNodeEvents(Node node) {
        node.addNodeListener(this);
        for (Node child : node.getChildren()) {
            subscribeToNodeEvents(child);
        }
    }

    // Присваивание уникального цвета узлу
    private void assignColorToNode(Node node, NodeType nodeType) {
        int colorId = nextColorId++;
        nodeIdToColor.put(node.getId(), colorId);
        colorToNodeId.put(colorId, node.getId());
        nodeIdToType.put(node.getId(), nodeType);
    }

    public void resizePickingFBO(int width, int height) {
        // Обновляем размеры FBO при изменении размеров окна
        glBindTexture(GL_TEXTURE_2D, pickingTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, (ByteBuffer) null);

        glBindRenderbuffer(GL_RENDERBUFFER, pickingDepthRBO);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);

        glBindTexture(GL_TEXTURE_2D, 0);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);
    }

    public void update() {
        // Перевіряємо натискання ЛІВОЇ кнопки миші для вибору об'єкта
        if (inputManager.isMouseButtonPressed(GLFW_MOUSE_BUTTON_LEFT) &&
                !inputManager.isCursorLocked() &&
                !ImGui.getIO().getWantCaptureMouse()) {

            // Перевіряємо, чи не відбувається перетягування в TransformTool
            // Цю перевірку треба додати, щоб не переривати поточну операцію
            boolean isTransformToolDragging = false;
            for (EditorListener listener : listeners) {
                if (listener instanceof TransformTool) {
                    TransformTool tool = (TransformTool) listener;
                    isTransformToolDragging = tool.isDragging();
                    break;
                }
            }

            // Виконуємо вибір об'єкта тільки якщо немає активного перетягування
            if (!isTransformToolDragging) {
                performPicking(inputManager.getMouseX(), inputManager.getMouseY());
            }
        }

        // Обробка вводу для маніпуляції з вибраним вузлом залишається
        if (selectedNode != null) {
            handleNodeManipulation();
        }
    }

    private void handleNodeManipulation() {
        // Реализация манипуляции выбранным узлом через ввод
        // Например, перемещение, вращение, масштабирование
        // Это может зависеть от текущего режима редактирования (перемещение/вращение/масштабирование)
    }

    // Метод для перевода ID объекта в цвет для шейдера
    private Vector3f idToColor(int id) {
        float r = ((id >>  0) & 0xFF) / 255.0f;
        float g = ((id >>  8) & 0xFF) / 255.0f;
        float b = ((id >> 16) & 0xFF) / 255.0f;
        return new Vector3f(r, g, b);
    }

    // Метод для перевода цвета в ID объекта
    private int colorToId(byte r, byte g, byte b) {
        return (r & 0xFF) | ((g & 0xFF) << 8) | ((b & 0xFF) << 16);
    }

    // Функция для рендеринга сцены с уникальными цветами для выбора
    private void renderPickingScene() {
        glBindFramebuffer(GL_FRAMEBUFFER, pickingFBO);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glUseProgram(pickingShaderProgram);

        // Получаем матрицы вида и проекции
        Matrix4f viewMatrix = camera.getViewMatrix();
        Matrix4f projectionMatrix = viewport.getProjectionMatrix();

        // Расположение униформ в шейдере
        int mvpLoc = glGetUniformLocation(pickingShaderProgram, "mvp");
        int colorLoc = glGetUniformLocation(pickingShaderProgram, "objectColor");

        // Рендерим усі типи вузлів
        for (Node rootNode : toolRootNodes.values()) {
            if (rootNode != null) {
                renderNodeForPicking(rootNode, viewMatrix, projectionMatrix, mvpLoc, colorLoc);
            }
        }

        glUseProgram(0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    // Рекурсивный рендеринг узлов для выбора
    private void renderNodeForPicking(Node node, Matrix4f viewMatrix, Matrix4f projectionMatrix,
                                      int mvpLoc, int colorLoc) {
        // Обновляем мировую трансформацию узла
        node.updateWorldTransformation();

        // Рендерим только если у узла есть меши и назначен ID
        if (!node.getMeshes().isEmpty() && nodeIdToColor.containsKey(node.getId())) {

            int colorId = nodeIdToColor.get(node.getId());
            Vector3f nodeColor = idToColor(colorId);

            // Устанавливаем цвет для выбора
            glUniform3f(colorLoc, nodeColor.x, nodeColor.y, nodeColor.z);

            // Рендерим все меши узла
            for (Mesh mesh : node.getMeshes()) {
                Matrix4f modelMatrix = node.getWorldTransformation();

                // Вычисляем и передаем MVP матрицу
                Matrix4f mvpMatrix = new Matrix4f();
                projectionMatrix.mul(viewMatrix, mvpMatrix);
                mvpMatrix.mul(modelMatrix);

                float[] mvpBuffer = new float[16];
                mvpMatrix.get(mvpBuffer);
                glUniformMatrix4fv(mvpLoc, false, mvpBuffer);

                // Рендерим меш (только геометрию, без материалов)
                glBindVertexArray(mesh.getVaoID());
                glDrawElements(GL_TRIANGLES, mesh.getIndices().length, GL_UNSIGNED_INT, 0);
                glBindVertexArray(0);
            }
        }

        // Рекурсивно рендерим дочерние узлы
        for (Node child : node.getChildren()) {
            renderNodeForPicking(child, viewMatrix, projectionMatrix, mvpLoc, colorLoc);
        }
    }

    public void performPicking(float mouseX, float mouseY) {
        // Рендерим сцену с уникальными цветами для выбора
        renderPickingScene();

        // Читаем пиксель по координатам мыши
        ByteBuffer pixelBuffer = BufferUtils.createByteBuffer(3);
        glBindFramebuffer(GL_FRAMEBUFFER, pickingFBO);

        // Переворачиваем координату Y, так как в OpenGL 0 находится внизу
        int y = viewport.getHeight() - (int)mouseY;
        glReadPixels((int)mouseX, y, 1, 1, GL_RGB, GL_UNSIGNED_BYTE, pixelBuffer);

        byte r = pixelBuffer.get(0);
        byte g = pixelBuffer.get(1);
        byte b = pixelBuffer.get(2);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        // Преобразуем цвет в ID объекта
        int colorId = colorToId(r, g, b);

        // Если ID = 0, значит выбран фон
        if (colorId == 0) {
            clearSelection();
            return;
        }

        // Находим выбранный узел по ID
        UUID nodeId = colorToNodeId.get(colorId);
        if (nodeId != null) {
            // Отримуємо тип вузла
            NodeType nodeType = nodeIdToType.get(nodeId);

            // Знаходимо вузол в залежності від його типу
            Node node = findNodeById(nodeId);

            if (node != null) {
                // Викликаємо відповідний обробник в залежності від типу вузла
                handleNodeSelection(node, nodeType);
            }
        } else {
            clearSelection();
        }
    }

    // Метод для пошуку вузла за його ID у всіх доступних кореневих вузлах
    private Node findNodeById(UUID nodeId) {
        for (Node rootNode : toolRootNodes.values()) {
            if (rootNode != null) {
                Node node = rootNode.findNodeById(nodeId);
                if (node != null) {
                    return node;
                }
            }
        }
        return null;
    }

    // Метод для обробки вибору вузла в залежності від його типу
    private void handleNodeSelection(Node node, NodeType nodeType) {
        switch (nodeType) {
            case SCENE_OBJECT:
                // Обробляємо вибір звичайного об'єкта сцени
                selectNode(node);

                break;

            case TRANSFORM_TOOL:
                for (EditorListener listener : listeners) {
                    listener.onNodeSelected(node);
                }

                break;

            case ROTATION_TOOL:
                for (EditorListener listener : listeners) {
                    listener.onNodeSelected(node);
                }

                break;

            case SCALE_TOOL:
                for (EditorListener listener : listeners) {
                    listener.onNodeSelected(node);
                }

                break;

            default:
                // Для невідомих типів вузлів просто вибираємо їх
                selectNode(node);
                break;
        }
    }

    public void selectNode(Node node) {
        // Снимаем выделение с предыдущего узла
        if (selectedNode != null) {
            selectedNode.setSelected(false);
        }

        // Устанавливаем выделение нового узла
        selectedNode = node;
        selectedNode.setSelected(true);

        // Уведомляем слушателей о выборе узла
        for (EditorListener listener : listeners) {
            listener.onNodeSelected(node);
        }
    }

    public void clearSelection() {
        if (selectedNode != null) {
            selectedNode.setSelected(false);
            selectedNode = null;

            // Уведомляем слушателей о снятии выделения
            for (EditorListener listener : listeners) {
                listener.onSelectionCleared();
            }
        }
    }

    public void renderSelection() {
        // Если есть выбранный объект, рендерим его с подсветкой
        if (selectedNode != null) {
            // Сохраняем текущие состояния OpenGL
            boolean blendEnabled = glIsEnabled(GL_BLEND);

            IntBuffer blendFuncBuffer = BufferUtils.createIntBuffer(2);
            glGetIntegerv(GL_BLEND_SRC, blendFuncBuffer);
            glGetIntegerv(GL_BLEND_DST, (IntBuffer) blendFuncBuffer.position(1));

            IntBuffer polygonModeBuffer = BufferUtils.createIntBuffer(1);
            glGetIntegerv(GL_POLYGON_MODE, polygonModeBuffer);
            int originalPolygonMode = polygonModeBuffer.get(0);

            FloatBuffer lineWidthBuffer = BufferUtils.createFloatBuffer(1);
            glGetFloatv(GL_LINE_WIDTH, lineWidthBuffer);
            float originalLineWidth = lineWidthBuffer.get(0);

            // Настраиваем параметры рендеринга для подсветки
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            glLineWidth(2.0f);

            // Получаем матрицы вида и проекции
            Matrix4f viewMatrix = camera.getViewMatrix();
            Matrix4f projectionMatrix = viewport.getProjectionMatrix();

            // Используем шейдер для подсветки
            glUseProgram(pickingShaderProgram);
            int mvpLoc = glGetUniformLocation(pickingShaderProgram, "mvp");
            int colorLoc = glGetUniformLocation(pickingShaderProgram, "objectColor");

            // Устанавливаем оранжевый цвет для выделения
            glUniform3f(colorLoc, 1.0f, 0.5f, 0.0f);

            // Рендерим все меши выбранного узла
            selectedNode.updateWorldTransformation();
            for (Mesh mesh : selectedNode.getMeshes()) {
                Matrix4f modelMatrix = selectedNode.getWorldTransformation();

                // Вычисляем и передаем MVP матрицу
                Matrix4f mvpMatrix = new Matrix4f();
                projectionMatrix.mul(viewMatrix, mvpMatrix);
                mvpMatrix.mul(modelMatrix);

                float[] mvpBuffer = new float[16];
                mvpMatrix.get(mvpBuffer);
                glUniformMatrix4fv(mvpLoc, false, mvpBuffer);

                // Рендерим меш
                glBindVertexArray(mesh.getVaoID());
                glDrawElements(GL_TRIANGLES, mesh.getIndices().length, GL_UNSIGNED_INT, 0);
                glBindVertexArray(0);
            }

            // Восстанавливаем предыдущее состояние OpenGL
            if (!blendEnabled) {
                glDisable(GL_BLEND);
            } else {
                glBlendFunc(blendFuncBuffer.get(0), blendFuncBuffer.get(1));
            }

            glPolygonMode(GL_FRONT_AND_BACK, originalPolygonMode);
            glLineWidth(originalLineWidth);

            // Отключаем шейдер
            glUseProgram(0);
        }
    }

    // Методы для работы с событиями редактора
    public void addEditorListener(EditorListener listener) {
        listeners.add(listener);
    }

    public void removeEditorListener(EditorListener listener) {
        listeners.remove(listener);
    }

    // Реализация интерфейса NodeListener
    @Override
    public void onNodeChanged(Node node) {
        // Если узел изменился, обновляем сцену
        for (EditorListener listener : listeners) {
            listener.onSceneChanged();
        }
    }

    @Override
    public void onSelectionChanged(Node node, boolean selected) {
        // Обработка изменения выделения узла
        if (selected) {
            selectedNode = node;
            for (EditorListener listener : listeners) {
                listener.onNodeSelected(node);
            }
        } else if (selectedNode == node) {
            selectedNode = null;
            for (EditorListener listener : listeners) {
                listener.onSelectionCleared();
            }
        }
    }

    public Node getSelectedNode() {
        return selectedNode;
    }

    public void notifySceneChanged() {
        // Уведомляем всех слушателей о изменении сцены
        for (EditorListener listener : listeners) {
            listener.onSceneChanged();
        }
    }

    // Оновлені методи для роботи з інструментами
    public void setToolRootNode(NodeType nodeType, Node rootNode) {
        toolRootNodes.put(nodeType, rootNode);

        // Реєструємо всі дочірні вузли для пікінгу з відповідним типом
        if (rootNode != null) {
            for (Node child : rootNode.getChildren()) {
                registerNodeForPicking(child, nodeType);
            }
        }
    }

    // Метод для сумісності із старим кодом
    public void setArrowsRootNode(Node arrowsRootNode) {
        setToolRootNode(NodeType.TRANSFORM_TOOL, arrowsRootNode);
    }

    // Метод для сумісності із старим кодом
    public void setCircleRootNode(Node circleRootNode) {
        setToolRootNode(NodeType.ROTATION_TOOL, circleRootNode);
    }

    public void setVectorScaleRootNode(Node scaleRootNode) {
        setToolRootNode(NodeType.SCALE_TOOL, scaleRootNode);
    }

    public void cleanup() {
        // Удаляем шейдерную программу
        glDeleteProgram(pickingShaderProgram);

        // Удаляем FBO и связанные ресурсы
        glDeleteFramebuffers(pickingFBO);
        glDeleteTextures(pickingTexture);
        glDeleteRenderbuffers(pickingDepthRBO);
    }
}