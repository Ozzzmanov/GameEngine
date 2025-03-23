package org.example.GUI;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.example.Editor.Editor;
import org.example.Editor.TransformTool;
import org.example.Node;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Клас `GUI` відповідає за створення та управління графічним інтерфейсом редактора.
 * Використовує бібліотеку ImGui для відображення панелей та інструментів.
 *
 * Основні функції:
 * - Ініціалізація ImGui
 * - Завантаження шрифтів
 * - Управління панелями редактора
 * - Рендеринг інтерфейсу
 * - Оновлення макету при зміні розміру вікна
 *
 * Залежності:
 * - imgui (бібліотека для графічного інтерфейсу)
 * - org.example.Editor (редактор та інструменти)
 * - org.example.Node (структура даних сцени)
 *
 * @author Вадим Овсюк
 * @version 0.8
 * @since 2025-03-23
 */

public class GUI {
    // Основні компоненти ImGui
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private long window;

    // Розміри панелей
    private final float topBarHeight = 40;
    private final float topToolBarHeight = 60;
    private float leftBarWidth = 350;
    private float rightPanelWidth = 250;

    // Редактор та інструменти
    private Editor editor;
    private TransformTool transformTool;

    // Список панелей для управління та рендерингу
    private List<Panel> panels = new ArrayList<>();

    // Основні панелі як окремі посилання для швидкого доступу
    private TopBarPanel topBarPanel;
    private ToolBarPanel toolBarPanel;
    private NodeTreePanel nodeTreePanel;
    private NodePropertiesPanel nodePropertiesPanel;

    public GUI(long window, Editor editor, Node rootNode, TransformTool transformTool) {
        this.window = window;
        this.editor = editor;
        this.transformTool = transformTool;

        initImGui();
        initPanels(rootNode);
    }

    private void initImGui() {
        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null);
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);

        // Завантаження шрифту
        loadFont(io);

        imGuiGlfw.init(window, true);
        imGuiGl3.init("#version 330");
    }

    private void loadFont(ImGuiIO io) {
        io.getFonts().clear();

        try {
            InputStream fontStream = getClass().getResourceAsStream("/fonts/Roboto-Regular.ttf");
            if (fontStream != null) {
                // Тимчасове збереження шрифту
                Path tempFont = Files.createTempFile("roboto", ".ttf");
                Files.copy(fontStream, tempFont, StandardCopyOption.REPLACE_EXISTING);

                io.getFonts().addFontFromFileTTF(tempFont.toString(), 18, io.getFonts().getGlyphRangesCyrillic());

                // Видалення тимчасового файлу
                Files.delete(tempFont);
            } else {
                System.err.println("Шрифт не знайдено в ресурсах");
            }
        } catch (IOException e) {
            System.err.println("Помилка при завантаженні шрифту: " + e.getMessage());
        }
    }

    private void initPanels(Node rootNode) {
        ImGuiIO io = ImGui.getIO();
        float displayWidth = io.getDisplaySizeX();
        float displayHeight = io.getDisplaySizeY();

        // Створення панелей
        topBarPanel = new TopBarPanel(0, 0, displayWidth, topBarHeight, editor);
        panels.add(topBarPanel);

        toolBarPanel = new ToolBarPanel(
                leftBarWidth, topBarHeight,
                displayWidth - leftBarWidth - rightPanelWidth, topToolBarHeight,
                transformTool);
        panels.add(toolBarPanel);

        nodeTreePanel = new NodeTreePanel(
                0, topBarHeight,
                leftBarWidth, displayHeight - topBarHeight,
                editor, rootNode);
        panels.add(nodeTreePanel);

        nodePropertiesPanel = new NodePropertiesPanel(
                displayWidth - rightPanelWidth, topBarHeight,
                rightPanelWidth, displayHeight - topBarHeight,
                editor);
        panels.add(nodePropertiesPanel);

        // Ініціалізація всіх панелей
        for (Panel panel : panels) {
            panel.init();
        }
    }

    public void render() {
        imGuiGlfw.newFrame();
        ImGui.newFrame();

        // Рендеринг всіх панелей
        for (Panel panel : panels) {
            panel.render();
        }

        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());

        updateLayout(); // Оновлюємо
    }

    public void cleanup() {
        // Очищення всіх панелей
        for (Panel panel : panels) {
            panel.cleanup();
        }

        imGuiGl3.dispose();
        imGuiGlfw.dispose();
        ImGui.destroyContext();
    }

    // Методи для доступу до панелей та їх властивостей
    public float getLeftBarWidth() {
        return nodeTreePanel.getWidth();
    }

    public float getRightPanelWidth() {
        return nodePropertiesPanel.getWidth();
    }

    public void updateLayout() {
        ImGuiIO io = ImGui.getIO();
        float displayWidth = io.getDisplaySizeX();
        float displayHeight = io.getDisplaySizeY();

        // Оновлення розмірів та позицій панелей, якщо розмір вікна змінюється
        leftBarWidth = nodeTreePanel.getWidth();
        rightPanelWidth = nodePropertiesPanel.getWidth();

        topBarPanel.setSize(displayWidth, topBarHeight);

        toolBarPanel.setPosition(leftBarWidth, topBarHeight);
        toolBarPanel.setSize(displayWidth - leftBarWidth - rightPanelWidth, topToolBarHeight);

        nodeTreePanel.setSize(leftBarWidth, displayHeight - topBarHeight);

        nodePropertiesPanel.setPosition(displayWidth - rightPanelWidth, topBarHeight);
        nodePropertiesPanel.setSize(rightPanelWidth, displayHeight - topBarHeight);
    }
}