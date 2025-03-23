package org.example.GUI;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import org.example.Editor.TransformTool;

/**
 * Панель інструментів `ToolBarPanel`, що реалізує управління інструментами трансформації.
 * Панель дозволяє вибрати режим трансформації: переміщення (TRANSLATE), обертання (ROTATE) або масштабування (SCALE).
 *
 * Основні функції:
 * - Створює кнопку для кожного інструменту трансформації.
 * - Встановлює режим трансформації через об'єкт `TransformTool` при натисканні на відповідну кнопку.
 * - Всі кнопки відображаються в одному рядку для зручності доступу користувача.
 *
 * Основні методи:
 * - `renderContent()` – реалізує виведення кнопок для вибору інструменту.
 *
 * Залежності:
 * - `ImGui` для створення графічного інтерфейсу.
 * - `TransformTool` для управління поточним режимом трансформації.
 *
 * @author Вадим Овсюк
 * @version 0.8
 * @since 2025-03-23
 */

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