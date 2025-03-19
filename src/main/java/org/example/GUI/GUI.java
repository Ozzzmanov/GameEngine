package org.example.GUI;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.example.Editor.Editor;
import org.example.Node;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class GUI {
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private long window;

    // topBar
    private final float topBarHeight = 40; // Храним ширину панели
    private final float topBarButtonHeight = 25;
    private final float topBarButtonWidth = 80;

    // leftBar
    private float leftBarWidth = 350; // Храним ширину панели

    // Panels
    private NodeTreePanel nodeTreePanel;
    private NodePropertiesPanel nodePropertiesPanel;

    private Editor editor;
    private Node rootNode;

    public GUI(long window, Editor editor, Node rootNode) {
        this.window = window;
        this.editor = editor;
        this.rootNode = rootNode;

        init();

        // Initialize panels
        nodeTreePanel = new NodeTreePanel(editor, rootNode);
        nodePropertiesPanel = new NodePropertiesPanel(editor);
    }

    public void init() {
        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null);
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);

        // Загрузка шрифта
        io.getFonts().clear();

        try {
            InputStream fontStream = getClass().getResourceAsStream("/fonts/Roboto-Regular.ttf");
            if (fontStream != null) {
                // Временное сохранение шрифта
                Path tempFont = Files.createTempFile("roboto", ".ttf");
                Files.copy(fontStream, tempFont, StandardCopyOption.REPLACE_EXISTING);

                io.getFonts().addFontFromFileTTF(tempFont.toString(), 18, io.getFonts().getGlyphRangesCyrillic());
                System.out.println("Шрифт успешно загружен");

                // Удаление временного файла
                Files.delete(tempFont);
            } else {
                System.err.println("Шрифт не найден в ресурсах");
            }
        } catch (IOException e) {
            System.err.println("Ошибка при загрузке шрифта: " + e.getMessage());
        }

        imGuiGlfw.init(window, true);
        imGuiGl3.init("#version 330");
    }

    public void render() {
        imGuiGlfw.newFrame();
        ImGui.newFrame();

        renderTopBar();
        renderLeftBar();
        renderPropertiesPanel();

        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
    }

    private void renderTopBar() {
        ImGui.setNextWindowPos(0, 0); // Фиксируем позицию окна в верхней части экрана
        ImGui.setNextWindowSize(ImGui.getIO().getDisplaySizeX(), topBarHeight); // Устанавливаем ширину окна = ширине экрана, а высоту = 30

        // Создаём окно с флагами
        ImGui.begin("TopBar", ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoTitleBar);

        // Устанавливаем позицию кнопки слева
        ImGui.setCursorPosX(10); // Отступ 10px слева

        // Устанавливаем размер кнопки
        if (ImGui.button("File", topBarButtonWidth, topBarButtonHeight)) { // Ширина 80px, высота 25px
            ImGui.openPopup("MenuPopup");
        }

        if (ImGui.beginPopup("MenuPopup")) {
            if (ImGui.menuItem("New Scene")) {
                // Create a new scene logic
            }

            if (ImGui.menuItem("Save Scene")) {
                // Save scene logic
            }

            if (ImGui.menuItem("Load Scene")) {
                // Load scene logic
            }

            ImGui.separator();

            if (ImGui.menuItem("Exit")) {
                System.exit(0);
            }
            ImGui.endPopup();
        }

        // Add Edit Menu
        ImGui.sameLine();
        if (ImGui.button("Edit", topBarButtonWidth, topBarButtonHeight)) {
            ImGui.openPopup("EditMenu");
        }

        if (ImGui.beginPopup("EditMenu")) {
            if (ImGui.menuItem("Undo", "Ctrl+Z")) {
                // Undo logic
            }

            if (ImGui.menuItem("Redo", "Ctrl+Y")) {
                // Redo logic
            }

            ImGui.separator();

            if (ImGui.menuItem("Delete Selected", "Del")) {
                // Delete selected node logic
                if (editor.getSelectedNode() != null && editor.getSelectedNode().getParent() != null) {
                    editor.getSelectedNode().getParent().removeChild(editor.getSelectedNode());
                    editor.clearSelection();
//                    editor.notifySceneChanged();
                }
            }

            ImGui.endPopup();
        }

        ImGui.end();
    }

    private void renderLeftBar() {
        ImGui.setNextWindowPos(0, topBarHeight);
        ImGui.setNextWindowSize(leftBarWidth, ImGui.getIO().getDisplaySizeY() - topBarHeight);

        ImGui.begin("Hierarchy", ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoCollapse);

        // Render node tree panel
        nodeTreePanel.render();

        // Получаем новую ширину окна (если пользователь изменил размер)
        leftBarWidth = ImGui.getWindowSizeX();

        ImGui.end();
    }

    private void renderPropertiesPanel() {
        float rightPanelWidth = 250;
        ImGui.setNextWindowPos(ImGui.getIO().getDisplaySizeX() - rightPanelWidth, topBarHeight);
        ImGui.setNextWindowSize(rightPanelWidth, ImGui.getIO().getDisplaySizeY() - topBarHeight);

        ImGui.begin("Properties", ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoCollapse);

        // Render node properties panel
        nodePropertiesPanel.render(editor.getSelectedNode());

        ImGui.end();
    }

    public void cleanup() {
        imGuiGl3.dispose();
        imGuiGlfw.dispose();
        ImGui.destroyContext();
    }
}