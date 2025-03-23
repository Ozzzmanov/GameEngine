package org.example.GUI;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import org.example.Editor.Editor;

/**
 * Панель верхнього меню `TopBarPanel`, що реалізує функціонал для доступу до основних команд редактора.
 * Панель містить кнопки для вибору операцій з файлами та редагування.
 * - Кнопка "File" нічого не робить.
 * - Кнопка "Edit" нічого не робить.
 *
 * Основні функції:
 * - Кнопка "File" повинна надавати доступ до операцій з файлами через спливаюче меню.
 * - Кнопка "Edit" повинна дозволяти редагувати об'єкти, наприклад, скасувати або повторити дії.
 * - Кожен пункт меню обробляє відповідні дії в редакторі.
 *
 * Основні методи:
 * - `renderContent()` – реалізує відображення кнопок та меню для операцій "File" та "Edit".
 *
 * Залежності:
 * - `ImGui` для створення графічного інтерфейсу.
 * - `Editor` для доступу до функцій редактора, зокрема для видалення вибраного елемента.
 *
 * @author Вадим Овсюк
 * @version 0.8
 * @since 2025-03-23
 */

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