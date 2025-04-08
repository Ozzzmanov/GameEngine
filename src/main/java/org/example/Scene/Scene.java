package org.example.Scene;

import org.example.Node;

public class Scene {
    private String sceneName;
    private Node rootNode;

    public Scene(String sceneName, Node rootNode) {
        this.sceneName = sceneName;
        this.rootNode = rootNode;
    }

    public Node getRootNode() {
        return rootNode;
    }

    public void setRootNode(Node rootNode) {
        this.rootNode = rootNode;
    }

    public String getSceneName() {
        return sceneName;
    }

    public void setSceneName(String sceneName) {
        this.sceneName = sceneName;
    }


}
