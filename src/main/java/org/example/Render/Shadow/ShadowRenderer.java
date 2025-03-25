package org.example.Render.Shadow;

import org.example.Mesh;
import org.example.Node;
import org.joml.Matrix4f;

public class ShadowRenderer {
    private IShadowMap shadowMap;
    private int shadowShaderProgram;

    public ShadowRenderer(IShadowMap shadowMap, int shadowShaderProgram) {
        this.shadowMap = shadowMap;
        this.shadowShaderProgram = shadowShaderProgram;
    }

    public void renderNodeShadows(Node node, Matrix4f lightSpaceMatrix) {
        ShadowRenderStrategy shadowStrategy = new ShadowRenderStrategy();

        // Рендеримо меші поточного вузла
        for (Mesh mesh : node.getMeshes()) {
            shadowStrategy.render(mesh, shadowShaderProgram, null, null, lightSpaceMatrix);
        }

        // Рекурсивно рендеримо дочірні вузли
        for (Node child : node.getChildren()) {
            if (child.getNodeType() == Node.NodeType.DEFAULT) {
                renderNodeShadows(child, lightSpaceMatrix);
            }
        }
    }
}