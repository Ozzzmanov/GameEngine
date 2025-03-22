package org.example.GUI;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import org.example.Editor.Editor;

public class TopBarPanel extends AbstractPanel {
    private final float buttonHeight;
    private final float buttonWidth;
    private final Editor editor;

    public TopBarPanel(float posX, float posY, float width, float height, Editor editor) {
        super("TopBar", posX, posY, width, height);
        this.buttonHeight = 25;
        this.buttonWidth = 80;
        this.editor = editor;
        this.windowFlags = ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize |
                ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoTitleBar;
    }

    @Override
    protected void renderContent() {
        // Кнопка "File"
        ImGui.setCursorPosX(10);
        if (ImGui.button("File", buttonWidth, buttonHeight)) {
            ImGui.openPopup("MenuPopup");
        }

        if (ImGui.beginPopup("MenuPopup")) {
            if (ImGui.menuItem("New Scene")) {
                // Логіка створення нової сцени
            }

            if (ImGui.menuItem("Save Scene")) {
                // Логіка збереження сцени
            }

            if (ImGui.menuItem("Load Scene")) {
                // Логіка завантаження сцени
            }

            ImGui.separator();

            if (ImGui.menuItem("Exit")) {
                System.exit(0);
            }
            ImGui.endPopup();
        }

        // Кнопка "Edit"
        ImGui.sameLine();
        if (ImGui.button("Edit", buttonWidth, buttonHeight)) {
            ImGui.openPopup("EditMenu");
        }

        if (ImGui.beginPopup("EditMenu")) {
            if (ImGui.menuItem("Undo", "Ctrl+Z")) {
                // Логіка скасування дії
            }

            if (ImGui.menuItem("Redo", "Ctrl+Y")) {
                // Логіка повторення дії
            }

            ImGui.separator();

            if (ImGui.menuItem("Delete Selected", "Del")) {
                if (editor.getSelectedNode() != null && editor.getSelectedNode().getParent() != null) {
                    editor.getSelectedNode().getParent().removeChild(editor.getSelectedNode());
                    editor.clearSelection();
                }
            }

            ImGui.endPopup();
        }
    }
}