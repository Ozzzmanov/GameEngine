package org.example.GUI;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;

/**
 * Абстрактний клас `AbstractPanel`, що реалізує базову логіку для всіх панелей GUI.
 * Забезпечує стандартне відображення вікна та управління його розмірами й позицією.
 *
 * Основні функції:
 * - Встановлює позицію та розмір вікна перед рендерингом.
 * - Використовує ImGuiWindowFlags для заборони переміщення та згортання.
 * - Дозволяє нащадкам реалізовувати метод `renderContent()` для вмісту панелі.
 *
 * Основні методи:
 * - `render()` – відображає вікно та викликає `renderContent()`.
 * - `init()` – базова ініціалізація, яку можуть перевизначати підкласи.
 * - `cleanup()` – базове очищення ресурсів.
 * - `setPosition(x, y)`, `setSize(width, height)` – методи для зміни положення та розміру панелі.
 *
 * @author Вадим Овсюк
 * @version 0.8
 * @since 2025-03-23
 */

public abstract class AbstractPanel implements Panel {
    protected String title;
    protected float posX, posY;
    protected float width, height;
    protected int windowFlags;

    public AbstractPanel(String title, float posX, float posY, float width, float height) {
        this.title = title;
        this.posX = posX;
        this.posY = posY;
        this.width = width;
        this.height = height;
        this.windowFlags = ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoCollapse;
    }

    @Override
    public void render() {
        ImGui.setNextWindowPos(posX, posY);
        ImGui.setNextWindowSize(width, height);

        if (ImGui.begin(title, windowFlags)) {
            renderContent();
        }

        // Оновлення розмірів вікна, якщо воно змінилося
        width = ImGui.getWindowSizeX();
        height = ImGui.getWindowSizeY();

        ImGui.end();
    }

    // Метод, який повинні реалізувати нащадки для відображення вмісту панелі
    protected abstract void renderContent();

    @Override
    public void init() {
        // Базова ініціалізація, яку можуть перевизначити підкласи
    }

    @Override
    public void cleanup() {
        // Базове очищення, яке можуть перевизначити підкласи
    }

    // Гетери та сетери
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public float getPosX() { return posX; }
    public float getPosY() { return posY; }

    public void setPosition(float x, float y) {
        this.posX = x;
        this.posY = y;
    }

    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
    }
}