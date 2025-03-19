package org.example.Editor;

import org.example.Node;

// Базовый интерфейс для всех компонентов
public interface Component {
    void setNode(Node node);
    void cleanup();
}