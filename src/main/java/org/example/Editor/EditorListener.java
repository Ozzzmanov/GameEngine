package org.example.Editor;

import org.example.Node;

/**
 * Інтерфейс для прослуховування подій редактора, таких як вибір вузла, зміни сцени та очищення вибору.
 */
public interface EditorListener {
    /**
     * Викликається, коли вузол вибрано в редакторі.
     *
     * @param node Вузол, який було вибрано
     */
    void onNodeSelected(Node node);

    /**
     * Викликається, коли вибір очищено в редакторі.
     */
    void onSelectionCleared();

    /**
     * Викликається, коли відбуваються будь-які зміни в сцені.
     */
    void onSceneChanged();
}
