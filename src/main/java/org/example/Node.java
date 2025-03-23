package org.example;

import org.example.Editor.Component;
import org.example.Editor.NodeListener;
import org.example.Render.*;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

public class Node {
    // Уникальный идентификатор узла
    private final UUID id;
    private String name;
    private Node parent;
    private final List<Node> children;
    private final List<Mesh> meshes;

    // Компоненты узла
    private final Map<Class<?>, Component> components;

    private Vector3f position;
    private Quaternionf rotation;
    private Vector3f scale;

    private Matrix4f localTransformation;
    private Matrix4f worldTransformation;
    private boolean localTransformationDirty;

    // Флаг выбора для редактора
    private boolean selected;

    // События
    private final List<NodeListener> listeners;

    private NodeType nodeType = NodeType.DEFAULT;
    public enum NodeType {
        DEFAULT,
        LIGHT
    }

    private Vector3f lightColor = new Vector3f(1.0f, 1.0f, 1.0f); // Белый цвет
    private float lightIntensity = 1.0f;

    public Node(String name) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.children = new ArrayList<>();
        this.meshes = new ArrayList<>();
        this.components = new HashMap<>();
        this.listeners = new ArrayList<>();

        this.position = new Vector3f(0, 0, 0);
        this.rotation = new Quaternionf();
        this.scale = new Vector3f(1, 1, 1);
        this.localTransformation = new Matrix4f();
        this.worldTransformation = new Matrix4f();
        this.localTransformationDirty = true;
        this.selected = false;


    }

    // Добавление компонента к узлу
    public <T extends Component> void addComponent(T component) {
        components.put(component.getClass(), component);
        component.setNode(this);
        notifyNodeChanged();
    }

    // Получение компонента по типу
    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(Class<T> componentClass) {
        return (T) components.get(componentClass);
    }

    // Проверка наличия компонента
    public boolean hasComponent(Class<?> componentClass) {
        return components.containsKey(componentClass);
    }

    private void updateLocalTransformation() {
        if (!localTransformationDirty) return;

        localTransformation.identity()
                .translate(position)
                .rotate(rotation)
                .scale(scale);

        localTransformationDirty = false;
    }

    public void updateWorldTransformation() {
        updateLocalTransformation();

        if (parent != null) {
            worldTransformation.set(parent.getWorldTransformation()).mul(localTransformation);
        } else {
            worldTransformation.set(localTransformation);
        }

        for (Mesh mesh : meshes) {
            mesh.setModelMatrix(worldTransformation);
        }

        for (Node child : children) {
            child.updateWorldTransformation();
        }
    }

    public Matrix4f getWorldTransformation() {
        return new Matrix4f(worldTransformation);
    }

    public void render(int shaderProgram, Matrix4f viewMatrix, Matrix4f projectionMatrix, Vector3f cameraPosition) {
        updateWorldTransformation();

        // Отримуємо всі джерела світла в сцені
        List<Node> lightNodes = getRootNode().getLightNodes();

        // Рендеринг всіх мешів цього вузла
        for (Mesh mesh : meshes) {
            switch (nodeType) {
                case DEFAULT:
                    mesh.setRenderStrategy(new DefaultRenderStrategy());
                    break;
                case LIGHT:
                    mesh.setRenderStrategy(new LightRenderStrategy());
                    break;
            }
            mesh.render(shaderProgram, viewMatrix, projectionMatrix, cameraPosition, lightNodes);
        }

        // Рендеринг дочірніх вузлів
        for (Node child : children) {
            child.render(shaderProgram, viewMatrix, projectionMatrix, cameraPosition);
        }
    }

    // Вспомогательный метод для получения корневого узла
    private Node getRootNode() {
        Node current = this;
        while (current.getParent() != null) {
            current = current.getParent();
        }
        return current;
    }

    public void cleanup() {
        for (Mesh mesh : meshes) {
            mesh.cleanup();
        }
        for (Node child : children) {
            child.cleanup();
        }
        // Очистка компонентов
        for (Component component : components.values()) {
            component.cleanup();
        }
    }

    public void addChild(Node child) {
        children.add(child);
        child.setParent(this);
        notifyNodeChanged();
    }

    public void removeChild(Node child) {
        if (children.remove(child)) {
            child.setParent(null);
            notifyNodeChanged();
        }
    }

    private void setParent(Node parent) {
        this.parent = parent;
    }

    public void addMesh(Mesh mesh) {
        meshes.add(mesh);
        notifyNodeChanged();
    }

    public void removeMesh(Mesh mesh) {
        if (meshes.remove(mesh)) {
            notifyNodeChanged();
        }
    }

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        localTransformationDirty = true;
        notifyNodeChanged();
    }

    public void setRotation(float x, float y, float z) {
        rotation.rotationXYZ(x, y, z);
        localTransformationDirty = true;
        notifyNodeChanged();
    }

    // Добавьте этот метод в класс Node
    public void setRotationQuaternion(Quaternionf newRotation) {
        this.rotation.set(newRotation);
        localTransformationDirty = true;
        notifyNodeChanged();
    }

    public void setScale(float x, float y, float z) {
        scale.set(x, y, z);
        localTransformationDirty = true;
        notifyNodeChanged();
    }

    public void setScale(Vector3f vector3f) {
        scale.set(vector3f.x, vector3f.y, vector3f.z);
        localTransformationDirty = true;
        notifyNodeChanged();
    }

    // Методы для выделения узла
    public void setSelected(boolean selected) {
        this.selected = selected;
        notifySelectionChanged();
    }

    public boolean isSelected() {
        return selected;
    }

    // Методы для работы с событиями узла
    public void addNodeListener(NodeListener listener) {
        listeners.add(listener);
    }

    public void removeNodeListener(NodeListener listener) {
        listeners.remove(listener);
    }

    private void notifyNodeChanged() {
        for (NodeListener listener : listeners) {
            listener.onNodeChanged(this);
        }
    }

    private void notifySelectionChanged() {
        for (NodeListener listener : listeners) {
            listener.onSelectionChanged(this, selected);
        }
    }

    // Методы для поиска узлов в сцене
    public Node findNodeById(UUID id) {
        if (this.id.equals(id)) {
            return this;
        }

        for (Node child : children) {
            Node found = child.findNodeById(id);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    public Node findNodeByName(String name) {
        if (this.name.equals(name)) {
            return this;
        }

        for (Node child : children) {
            Node found = child.findNodeByName(name);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    // Получение всех узлов в сцене
    public List<Node> getAllNodes() {
        List<Node> allNodes = new ArrayList<>();
        collectNodes(allNodes);
        return allNodes;
    }

    private void collectNodes(List<Node> nodes) {
        nodes.add(this);
        for (Node child : children) {
            child.collectNodes(nodes);
        }
    }

    public List<Node> getLightNodes() {
        List<Node> lightNodes = new ArrayList<>();
        collectLightNodes(lightNodes);
        return lightNodes;
    }

    private void collectLightNodes(List<Node> nodes) {
        if (this.nodeType == NodeType.LIGHT) {
            nodes.add(this);
        }
        for (Node child : children) {
            child.collectLightNodes(nodes);
        }
    }

    public void setLightColor(float r, float g, float b) {
        this.lightColor.set(r, g, b);
        notifyNodeChanged();
    }

    public Vector3f getLightColor() {
        return new Vector3f(lightColor);
    }

    public void setLightIntensity(float intensity) {
        this.lightIntensity = intensity;
        notifyNodeChanged();
    }

    public float getLightIntensity() {
        return lightIntensity;
    }

    // Геттеры
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        notifyNodeChanged();
    }

    public Node getParent() {
        return parent;
    }

    public List<Node> getChildren() {
        return new ArrayList<>(children);
    }

    public List<Mesh> getMeshes() {
        return new ArrayList<>(meshes);
    }

    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    public Quaternionf getRotation() {
        return new Quaternionf(rotation);
    }

    public Vector3f getScale() {
        return new Vector3f(scale);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return id.equals(node.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Node{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }
}