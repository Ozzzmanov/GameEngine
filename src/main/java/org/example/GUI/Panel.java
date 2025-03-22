package org.example.GUI;

// Базовий інтерфейс для всіх панелей GUI
public interface Panel {
    void render();
    void init();
    void cleanup();
}