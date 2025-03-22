package org.example.GUI;

import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImString;
import org.example.Editor.Editor;
import org.example.Editor.EditorListener;
import org.example.Mesh;
import org.example.Node;

import java.util.ArrayList;
import java.util.List;

public class NodeTreePanel extends AbstractPanel implements EditorListener {
    private Editor editor;
    private Node rootNode;
    private ImString newNodeName = new ImString(64);
    private Node selectedNode = null;
    private List<String> availableMeshes;

    public NodeTreePanel(float posX, float posY, float width, float height, Editor editor, Node rootNode) {
        super("Hierarchy", posX, posY, width, height);
        this.editor = editor;
        this.rootNode = rootNode;
        this.editor.addEditorListener(this);

        // Ініціалізація списку доступних моделей
        this.availableMeshes = new ArrayList<>();
        this.availableMeshes.add("/Object/Primitives/cube.obj");
        this.availableMeshes.add("/Object/Primitives/sphere.obj");
        this.availableMeshes.add("/Object/Models/gold.obj");
    }

    @Override
    protected void renderContent() {
        ImGui.text("Ієрархія сцени");
        ImGui.separator();

        // Кнопка додавання нового вузла
        if (ImGui.button("Додати вузол", 125, 25)) {
            ImGui.openPopup("AddNodePopup");
        }

        if (ImGui.beginPopup("AddNodePopup")) {
            ImGui.text("Назва вузла:");
            ImGui.inputText("##NodeName", newNodeName);

            if (ImGui.button("Створити", 100, 25) && !newNodeName.get().trim().isEmpty()) {
                Node newNode = new Node(newNodeName.get());

                // Додаємо до вибраного вузла, якщо він є, інакше до кореневого
                if (selectedNode != null) {
                    selectedNode.addChild(newNode);
                } else {
                    rootNode.addChild(newNode);
                }

                ImGui.closeCurrentPopup();
                newNodeName.set("");
                editor.notifySceneChanged();
            }

            ImGui.sameLine();
            if (ImGui.button("Скасувати", 100, 25)) {
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
        }

        // Видалення вибраного вузла
        ImGui.sameLine();
        if (ImGui.button("Видалити", 125, 25) && selectedNode != null && selectedNode.getParent() != null) {
            selectedNode.getParent().removeChild(selectedNode);
            editor.clearSelection();
            editor.notifySceneChanged();
        }

        ImGui.separator();

        // Відображення ієрархії вузлів
        if (rootNode != null) {
            renderNodeTree(rootNode);
        }
    }

    private void renderNodeTree(Node node) {
        int flags = ImGuiTreeNodeFlags.OpenOnArrow | ImGuiTreeNodeFlags.OpenOnDoubleClick;

        // Виділяємо вузол, якщо він вибраний
        if (node.isSelected()) {
            flags |= ImGuiTreeNodeFlags.Selected;
        }

        // Робимо вузли без дочірніх елементів більш компактними
        if (node.getChildren().isEmpty()) {
            flags |= ImGuiTreeNodeFlags.Leaf;
        }

        // Відображення назви вузла з кількістю моделей
        String nodeName = node.getName();
        if (!node.getMeshes().isEmpty()) {
            nodeName += " (" + node.getMeshes().size() + " моделей)";
        }

        boolean isOpen = ImGui.treeNodeEx(nodeName + "##" + node.getId(), flags);

        // Обробка вибору вузла при кліці
        if (ImGui.isItemClicked()) {
            editor.selectNode(node);
            selectedNode = node;
        }

        // Контекстне меню при кліку правою кнопкою
        if (ImGui.beginPopupContextItem()) {
            if (ImGui.menuItem("Перейменувати")) {
                ImGui.openPopup("RenameNode");
            }

            if (ImGui.menuItem("Видалити") && node.getParent() != null) {
                node.getParent().removeChild(node);
                editor.clearSelection();
                editor.notifySceneChanged();
                ImGui.endPopup();
                if (isOpen) {
                    ImGui.treePop();
                }
                return;
            }

            ImGui.endPopup();
        }

        // Вікно перейменування вузла
        if (ImGui.beginPopup("RenameNode")) {
            ImString newName = new ImString(node.getName(), 64);
            ImGui.inputText("##RenameInput", newName);

            if (ImGui.button("OK", 100, 25) && !newName.get().trim().isEmpty()) {
                node.setName(newName.get());
                editor.notifySceneChanged();
                ImGui.closeCurrentPopup();
            }

            ImGui.sameLine();
            if (ImGui.button("Скасувати", 100, 25)) {
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
        }

        // Відображення дочірніх вузлів
        if (isOpen) {
            // Відображення моделей вузла
            for (Mesh mesh : node.getMeshes()) {
                ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 0.5f, 0.8f, 1.0f, 1.0f);
                ImGui.treeNodeEx("Модель##" + mesh.hashCode(), ImGuiTreeNodeFlags.Leaf | ImGuiTreeNodeFlags.NoTreePushOnOpen);
                ImGui.popStyleColor();

                // Контекстне меню для моделі
                if (ImGui.beginPopupContextItem()) {
                    if (ImGui.menuItem("Видалити модель")) {
                        node.removeMesh(mesh);
                        editor.notifySceneChanged();
                    }
                    ImGui.endPopup();
                }
            }

            // Відображення дочірніх вузлів
            for (Node child : node.getChildren()) {
                renderNodeTree(child);
            }

            ImGui.treePop();
        }
    }

    @Override
    public void onNodeSelected(Node node) {
        selectedNode = node;
    }

    @Override
    public void onSelectionCleared() {
        selectedNode = null;
    }

    @Override
    public void onSceneChanged() {
        // Оновлення дерева при зміні сцени
    }


}
