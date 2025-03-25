package org.example.Render.Shadow;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public interface IShadowMap {
    void updateLightSpaceMatrix(Vector3f lightPos, Vector3f lightTarget, float near, float far);
    void bindForShadowPass();
    void unbind(int width, int height);
    void bindDepthMapForReading(int textureUnit);
    Matrix4f getLightSpaceMatrix();
    void cleanup();
}