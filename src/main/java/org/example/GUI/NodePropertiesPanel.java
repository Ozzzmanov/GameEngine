package org.example.GUI;

import imgui.ImGui;
import imgui.type.ImFloat;
import imgui.type.ImString;
import org.example.Editor.Editor;
import org.example.ImportObj;
import org.example.Mesh;
import org.example.Node;
import org.example.ShaderMaterial;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class NodePropertiesPanel {
    private Editor editor;
    private List<String> availableMeshes;

    private ImString nodeName = new ImString(64);
    private Vector3f position = new Vector3f(0, 0, 0);
    private Vector3f rotation = new Vector3f(0, 0, 0);
    private Vector3f scale = new Vector3f(1, 1, 1);

    // Матеріал
    private Vector3f ambient = new Vector3f(0.2f, 0.2f, 0.2f);
    private Vector3f diffuse = new Vector3f(0.8f, 0.8f, 0.8f);
    private Vector3f specular = new Vector3f(1.0f, 1.0f, 1.0f);
    private ImFloat shininess = new ImFloat(32.0f);


    public NodePropertiesPanel(Editor editor) {
        this.editor = editor;

        // Ініціалізація списку доступних моделей
        this.availableMeshes = new ArrayList<>();
        this.availableMeshes.add("/Object/cube.obj");
        this.availableMeshes.add("/Object/sphere.obj");
        this.availableMeshes.add("/Object/gold.obj");
    }

    public void render(Node selectedNode) {
        if (selectedNode == null) {
            ImGui.text("No node selected");
            return;
        }

        // Оновлюємо значення UI, коли вибраний вузол
        updateUIValues(selectedNode);

        // Назва вузла
        ImGui.text("Name:");
        if (ImGui.inputText("##NodeName", nodeName)) {
            selectedNode.setName(nodeName.get());
        }

        // ID вузла
        ImGui.text("ID: " + selectedNode.getId().toString());

        // Розділ Трансформація
        if (ImGui.collapsingHeader("Transform")) {
            // Позиція
            ImGui.text("Position:");
            float[] posArr = {position.x, position.y, position.z};
            if (ImGui.dragFloat3("##Position", posArr, 0.1f)) {
                position.set(posArr);
                selectedNode.setPosition(position.x, position.y, position.z);
            }

            // Поворот
            ImGui.text("Rotation:");
            float[] rotArr = {rotation.x, rotation.y, rotation.z};
            if (ImGui.dragFloat3("##Rotation", rotArr, 0.1f)) {
                rotation.set(rotArr[0], rotArr[1], rotArr[2]); // Update local rotation vector
                selectedNode.setRotation(
                        (float) Math.toRadians(rotation.x),
                        (float) Math.toRadians(rotation.y),
                        (float) Math.toRadians(rotation.z)
                );
            }

            // Масштаб
            ImGui.text("Scale:");
            float[] scaleArr = {scale.x, scale.y, scale.z};
            if (ImGui.dragFloat3("##Scale", scaleArr, 0.1f)) {
                scale.set(scaleArr);
                selectedNode.setScale(scale.x, scale.y, scale.z);
            }
        }

        // Інформація про ієрархію вузлів
        if (ImGui.collapsingHeader("Hierarchy")) {
            ImGui.text("Parent: " + (selectedNode.getParent() != null ? selectedNode.getParent().getName() : "None"));
            ImGui.text("Children: " + selectedNode.getChildren().size());
            ImGui.text("Meshes: " + selectedNode.getMeshes().size());
        }

        // Розділ Меші
        if (ImGui.collapsingHeader("Meshes")) {
            ImGui.separator();
            // Додавання моделі до вибраного вузла
            ImGui.sameLine();
            if (ImGui.button("Додати модель", 100, 25)) {
                if (selectedNode != null) {
                    ImGui.openPopup("AddMeshPopup");
                }
            }

            if (ImGui.beginPopup("AddMeshPopup")) {
                ImGui.text("Доступні моделі:");
                for (String meshPath : availableMeshes) {
                    if (ImGui.selectable(meshPath)) {
                        Mesh mesh = ImportObj.loadObjModel(meshPath);
                        selectedNode.addMesh(mesh);
                        editor.notifySceneChanged();
                        editor.registerNodeForPicking(selectedNode);
                        ImGui.closeCurrentPopup();
                    }
                }
                ImGui.endPopup();
            }

            for (int i = 0; i < selectedNode.getMeshes().size(); i++) {
                Mesh mesh = selectedNode.getMeshes().get(i);
                if (ImGui.treeNode("Mesh " + (i + 1))) {
                    renderMeshProperties(mesh);
                    ImGui.treePop();
                }
            }
        }

        // Розділ Компоненти (якщо є система компонентів)
        if (ImGui.collapsingHeader("Components")) {
            ImGui.text("Components will be shown here");
            // Кнопка додавання компонентів
            if (ImGui.button("Add Component", 120, 25)) {
                ImGui.openPopup("AddComponentPopup");
            }

            if (ImGui.beginPopup("AddComponentPopup")) {
                if (ImGui.selectable("Physics Component")) {
                    // Логіка додавання фізичного компонента
                }
                if (ImGui.selectable("Audio Component")) {
                    // Логіка додавання аудіо компонента
                }
                if (ImGui.selectable("Script Component")) {
                    // Логіка додавання скриптового компонента
                }
                ImGui.endPopup();
            }
        }
    }

    private void renderMeshProperties(Mesh mesh) {
        // Властивості матеріалу
        if (mesh.getShaderMaterial() != null) {
            ShaderMaterial material = mesh.getShaderMaterial();

            // Оновлюємо значення матеріалу в UI
            updateMaterialUIValues(material);

            ImGui.text("Material:");

            // Колір ambient
            ImGui.text("Ambient:");
            float[] ambArr = {ambient.x, ambient.y, ambient.z};
            if (ImGui.colorEdit3("##Ambient", ambArr)) {
                ambient.set(ambArr);
                material.setAmbient(ambient);
            }

            // Колір diffuse
            ImGui.text("Diffuse:");
            float[] diffArr = {diffuse.x, diffuse.y, diffuse.z};
            if (ImGui.colorEdit3("##Diffuse", diffArr)) {
                diffuse.set(diffArr);
                material.setDiffuse(diffuse);
            }

            // Колір specular
            ImGui.text("Specular:");
            float[] specArr = {specular.x, specular.y, specular.z};
            if (ImGui.colorEdit3("##Specular", specArr)) {
                specular.set(specArr);
                material.setSpecular(specular);
            }

            // Яскравість
            ImGui.text("Shininess:");
            if (ImGui.dragFloat("##Shininess", new float[]{shininess.get()}, 1.0f, 1.0f, 256.0f)) {
                material.setShininess(shininess.get());
            }

            // Швидкі налаштування матеріалів
            ImGui.text("Presets:");
            if (ImGui.button("Gold", 60, 25)) {
                mesh.setShaderMaterial(ShaderMaterial.createGold());
                updateMaterialUIValues(mesh.getShaderMaterial());
            }
            ImGui.sameLine();
            if (ImGui.button("Silver", 60, 25)) {
                mesh.setShaderMaterial(ShaderMaterial.createSilver());
                updateMaterialUIValues(mesh.getShaderMaterial());
            }
            ImGui.sameLine();
            if (ImGui.button("Holographic", 60, 25)) {
                mesh.setShaderMaterial(ShaderMaterial.createHolographicMaterial());
                updateMaterialUIValues(mesh.getShaderMaterial());
            }
        } else {
            ImGui.text("No material assigned");
        }
    }

    private void updateUIValues(Node node) {
        // Оновлюємо назву вузла
        nodeName.set(node.getName());

        // Оновлюємо позицію
        Vector3f pos = node.getPosition();
        position.set(pos);

        // Оновлюємо поворот (перетворюємо з радіан в градуси для відображення)
        Quaternionf rot = node.getRotation();
        float[] eulerAngles = getEulerAnglesFromQuaternion(rot);
        rotation.set((float) Math.toDegrees(eulerAngles[0]), (float) Math.toDegrees(eulerAngles[1]), (float) Math.toDegrees(eulerAngles[2]));

        // Оновлюємо масштаб
        Vector3f scl = node.getScale();
        scale.set(scl);
    }

    private void updateMaterialUIValues(ShaderMaterial material) {
        // Оновлюємо колір ambient
        Vector3f amb = material.getAmbient();
        ambient.set(amb);

        // Оновлюємо колір diffuse
        Vector3f diff = material.getDiffuse();
        diffuse.set(diff);

        // Оновлюємо колір specular
        Vector3f spec = material.getSpecular();
        specular.set(spec);

        // Оновлюємо яскравість
        shininess.set(material.getShininess());
    }

    private float[] getEulerAnglesFromQuaternion(Quaternionf q) {
        // Просте перетворення з кватерніону в кути Ейлера (порядок xyz)
        float[] angles = new float[3];

        // Кут на осі x (roll)
        double sinr_cosp = 2 * (q.w * q.x + q.y * q.z);
        double cosr_cosp = 1 - 2 * (q.x * q.x + q.y * q.y);
        angles[0] = (float) Math.atan2(sinr_cosp, cosr_cosp);

        // Кут на осі y (pitch)
        double sinp = 2 * (q.w * q.y - q.z * q.x);
        if (Math.abs(sinp) >= 1)
            angles[1] = (float) (Math.copySign(Math.PI / 2, sinp)); // використати 90 градусів, якщо поза межами
        else
            angles[1] = (float) Math.asin(sinp);

        // Кут на осі z (yaw)
        double siny_cosp = 2 * (q.w * q.z + q.x * q.y);
        double cosy_cosp = 1 - 2 * (q.y * q.y + q.z * q.z);
        angles[2] = (float) Math.atan2(siny_cosp, cosy_cosp);

        return angles;
    }
}
