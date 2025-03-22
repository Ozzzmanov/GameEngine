package org.example.GUI;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import org.example.Editor.TransformTool;

public class ToolBarPanel extends AbstractPanel {
    private final TransformTool transformTool;

    public ToolBarPanel(float posX, float posY, float width, float height, TransformTool transformTool) {
        super("Tool", posX, posY, width, height);
        this.transformTool = transformTool;
        this.windowFlags = ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize;
    }

    @Override
    protected void renderContent() {
        // Кнопка TRANSLATE
        if (ImGui.button("TRANSLATE")) {
            transformTool.setTransformMode(TransformTool.TransformMode.TRANSLATE);
        }
        ImGui.sameLine();

        // Кнопка ROTATE
        if (ImGui.button("ROTATE")) {
            transformTool.setTransformMode(TransformTool.TransformMode.ROTATE);
        }
        ImGui.sameLine();

        // Кнопка SCALE
        if (ImGui.button("SCALE")) {
            transformTool.setTransformMode(TransformTool.TransformMode.SCALE);
        }
    }
}