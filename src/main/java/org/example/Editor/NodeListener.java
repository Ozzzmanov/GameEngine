package org.example.Editor;

import org.example.Node;

// Интерфейс для слушателей изменений узла
public interface NodeListener {
    void onNodeChanged(Node node);
    void onSelectionChanged(Node node, boolean selected);
}