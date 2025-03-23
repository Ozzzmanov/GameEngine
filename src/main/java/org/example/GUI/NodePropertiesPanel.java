package org.example.GUI;

import imgui.ImGui;
import imgui.type.ImFloat;
import imgui.type.ImString;
import org.example.*;
import org.example.Editor.Editor;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Клас `NodePropertiesPanel`, що відповідає за відображення та редагування властивостей вибраного вузла сцени.
 * Панель дозволяє змінювати трансформації, матеріали, текстури, а також управляти моделями та компонентами.
 *
 * Основні функції:
 * - Відображає інформацію про вибраний вузол (назва, ID, ієрархію, трансформацію).
 * - Дозволяє редагувати положення, обертання та масштаб вузла.
 * - Надає можливість змінювати матеріали, текстури та використовувати готові пресети.
 * - Дозволяє додавати або видаляти 3D-моделі (меші) у вибраного вузла.
 * - Підтримує додавання компонентів, таких як фізика, аудіо або скриптові модулі.
 *
 * Основні методи:
 * - `renderContent()` – відображає вміст панелі, оновлюючи значення параметрів вибраного вузла.
 * - `updateUIValues(Node node)` – синхронізує дані вузла з UI.
 * - `updateMaterialUIValues(ShaderMaterial material)` – оновлює UI відповідно до параметрів матеріалу.
 * - `renderMeshProperties(Mesh mesh)` – рендерить налаштування матеріалів та текстур для кожної моделі вузла.
 *
 * @author Вадим Овсюк
 * @version 0.8
 * @since 2025-03-23
 */


public class NodePropertiesPanel extends AbstractPanel {
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

    // Текстури
    private TextureLoader diffuseMap;
    private List<String> availableTextures;

    public NodePropertiesPanel(float posX, float posY, float width, float height, Editor editor) {
        super("Properties", posX, posY, width, height);
        this.editor = editor;

        // Ініціалізація списку доступних моделей
        this.availableMeshes = new ArrayList<>();
        this.availableMeshes.add("/Object/Primitives/cube.obj");
        this.availableMeshes.add("/Object/Primitives/sphere.obj");
        this.availableMeshes.add("/Object/Models/gold.obj");
        this.availableMeshes.add("/Object/Primitives/Con.obj");
        this.availableMeshes.add("/Object/Primitives/SphereHighPoly.obj");

        // Ініціалізація списку доступних текстур
        this.availableTextures = new ArrayList<>();
        this.availableTextures.add("/Textures/primitivesPack/wood_01.png");
        this.availableTextures.add("/Textures/primitivesPack/metal_01.png");
        this.availableTextures.add("/Textures/primitivesPack/brick_01.png");
    }

    @Override
    protected void renderContent() {
        Node selectedNode = editor.getSelectedNode();

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
            ImGui.spacing();
            if (ImGui.button("Додати модель", 125, 25)) {
                if (selectedNode != null) {
                    ImGui.openPopup("AddMeshPopup");
                }
            }

            if (ImGui.beginPopup("AddMeshPopup")) {
                ImGui.text("Доступні моделі:");
                for (String meshPath : availableMeshes) {
                    if (ImGui.selectable(meshPath)) {
                        Mesh mesh = ObjectLoader.loadObjModel(meshPath);
                        mesh.setShaderMaterial(ShaderMaterial.createSilver());
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
        if (ImGui.treeNode("Material")) {
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
                    material.setAmbient(new Vector3f(ambient));
                }

                // Колір diffuse
                ImGui.text("Diffuse:");
                float[] diffArr = {diffuse.x, diffuse.y, diffuse.z};
                if (ImGui.colorEdit3("##Diffuse", diffArr)) {
                    diffuse.set(diffArr);
                    material.setDiffuse(new Vector3f(diffuse));
                }

                // Колір specular
                ImGui.text("Specular:");
                float[] specArr = {specular.x, specular.y, specular.z};
                if (ImGui.colorEdit3("##Specular", specArr)) {
                    specular.set(specArr);
                    material.setSpecular(new Vector3f(specular));
                }

                // Яскравість
                ImGui.text("Shininess:");
                float[] shininessArr = {shininess.get()};
                if (ImGui.dragFloat("##Shininess", shininessArr, 1.0f, 1.0f, 256.0f)) {
                    shininess.set(shininessArr[0]);
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
                ImGui.text("Відсутній матеріал");
            }

            ImGui.treePop();

        }
        if (ImGui.treeNode("Texture")) {
            if (mesh.getShaderMaterial() != null) {
                ShaderMaterial material = mesh.getShaderMaterial();

                updateMaterialUIValues(material);

                ImGui.text("Textures");

                ImGui.sameLine();
                ImGui.spacing();
                if (ImGui.button("Додати текстуру", 125, 25)) {
                    ImGui.openPopup("AddTexturePopup");
                }

                if (ImGui.beginPopup("AddTexturePopup")) {
                    ImGui.text("Доступні текстури:");
                    for (String texturePath : availableTextures) {
                        if (ImGui.selectable(texturePath)) {
                            material.setDiffuseMapPath(texturePath);
                            updateMaterialUIValues(material);
                            editor.notifySceneChanged();
                            ImGui.closeCurrentPopup();
                        }
                    }
                    ImGui.endPopup();
                }

                if (diffuseMap != null) {
                    ImGui.text("Texture id: " + diffuseMap.getId());
                    ImGui.text("Розмір: " + diffuseMap.getWidth() + "x" + diffuseMap.getHeight());

                    // Додавання кнопки видалення текстури
                    if (ImGui.button("Видалити текстуру", 125, 25)) {
                        material.setDiffuseMap(null);
                        diffuseMap = null;
                        editor.notifySceneChanged();
                    }

                    // Додаткові параметри текстури
                    if (ImGui.collapsingHeader("Налаштування текстури")) {
                        ImGui.text("Додаткові параметри текстури будуть тут");
                        // - Масштабування текстури
                        // - Зміщення текстури
                        // - Фільтрація
                    }
                } else {
                    ImGui.text("Відсутня текстура");
                }
            } else {
                ImGui.text("Відсутній матеріал");
            }

            ImGui.treePop();
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

        // Обновляем цвет ambient
        Vector3f amb = material.getAmbient();
        ambient = new Vector3f(amb.x, amb.y, amb.z);

        // Обновляем цвет diffuse
        Vector3f diff = material.getDiffuse();
        diffuse = new Vector3f(diff.x, diff.y, diff.z);

        // Обновляем цвет specular
        Vector3f spec = material.getSpecular();
        specular = new Vector3f(spec.x, spec.y, spec.z);

        // Обновляем яркость
        shininess.set(material.getShininess());

        // Обновляем текстуры
        if(material.hasTexture()) {
            diffuseMap = material.getDiffuseMap();
        } else {
            diffuseMap = null;
        }
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
