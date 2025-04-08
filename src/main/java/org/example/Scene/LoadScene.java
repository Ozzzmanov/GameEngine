package org.example.Scene;

import com.google.gson.*;
import org.example.Mesh;
import org.example.Node;
import org.example.ObjectLoader;
import org.example.ShaderMaterial;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.net.URL;

public class LoadScene {
    public static Node loadScene(String resourcePath) {
        GsonBuilder gsonBuilder = new GsonBuilder();

        // Регистрируем десериализаторы
        gsonBuilder.registerTypeAdapter(Node.class, new NodeDeserializer());
        gsonBuilder.registerTypeAdapter(Vector3f.class, new Vector3fDeserializer());
        gsonBuilder.registerTypeAdapter(Quaternionf.class, new QuaternionDeserializer());
        gsonBuilder.registerTypeAdapter(Mesh.class, new MeshDeserializer());

        Gson gson = gsonBuilder.create();

        try {
            URL resourceUrl = LoadScene.class.getClassLoader().getResource(resourcePath);
            if (resourceUrl == null) {
                System.err.println("Файл не найден: " + resourcePath);
                return null;
            }

            File file = new File(resourceUrl.toURI());
            try (FileReader reader = new FileReader(file)) {
                Node rootNode = gson.fromJson(reader, Node.class);
                System.out.println("Сцена загружена из файла: " + file.getAbsolutePath());
                return rootNode;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Десериализатор для Node
    private static class NodeDeserializer implements JsonDeserializer<Node> {
        @Override
        public Node deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            JsonObject jsonObject = json.getAsJsonObject();

            // Создаем узел с именем
            String name = jsonObject.get("name").getAsString();
            Node node = new Node(name);

            // Устанавливаем позицию
            if (jsonObject.has("position")) {
                Vector3f position = context.deserialize(jsonObject.get("position"), Vector3f.class);
                node.setPosition(position.x, position.y, position.z);
            }

            // Устанавливаем вращение
            if (jsonObject.has("rotation")) {
                Quaternionf rotation = context.deserialize(jsonObject.get("rotation"), Quaternionf.class);
                node.setRotationQuaternion(rotation);
            }

            // Устанавливаем масштаб
            if (jsonObject.has("scale")) {
                Vector3f scale = context.deserialize(jsonObject.get("scale"), Vector3f.class);
                node.setScale(scale);
            }

            // Устанавливаем тип узла
            if (jsonObject.has("nodeType")) {
                String nodeTypeStr = jsonObject.get("nodeType").getAsString();
                Node.NodeType nodeType = Node.NodeType.valueOf(nodeTypeStr);
                node.setNodeType(nodeType);

                // Если это узел света, устанавливаем его свойства
                if (nodeType == Node.NodeType.LIGHT) {
                    if (jsonObject.has("lightColor")) {
                        Vector3f lightColor = context.deserialize(jsonObject.get("lightColor"), Vector3f.class);
                        node.setLightColor(lightColor.x, lightColor.y, lightColor.z);
                    }

                    if (jsonObject.has("lightIntensity")) {
                        float lightIntensity = jsonObject.get("lightIntensity").getAsFloat();
                        node.setLightIntensity(lightIntensity);
                    }
                }
            }

            // Загружаем меши
            if (jsonObject.has("meshes")) {
                JsonArray meshesArray = jsonObject.getAsJsonArray("meshes");
                for (JsonElement meshElement : meshesArray) {
                    Mesh mesh = context.deserialize(meshElement, Mesh.class);
                    node.addMesh(mesh);
                }
            }

            // Рекурсивно загружаем дочерние узлы
            if (jsonObject.has("children")) {
                JsonArray childrenArray = jsonObject.getAsJsonArray("children");
                for (JsonElement childElement : childrenArray) {
                    Node childNode = context.deserialize(childElement, Node.class);
                    node.addChild(childNode);
                }
            }

            return node;
        }
    }

    // Десериализатор для Mesh
    private static class MeshDeserializer implements JsonDeserializer<Mesh> {
        @Override
        public Mesh deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            JsonObject jsonObject = json.getAsJsonObject();
            String resourcePath = jsonObject.get("resourcePath").getAsString();

            // Загружаем меш по пути к ресурсу
            Mesh mesh = ObjectLoader.loadObjModel(resourcePath);

            // Если есть материал, применяем его
            if (jsonObject.has("material")) {
                ShaderMaterial material = context.deserialize(jsonObject.get("material"), ShaderMaterial.class);
                mesh.setShaderMaterial(material);
            }

            return mesh;
        }
    }

    // Десериализаторы для Vector3f и Quaternionf
    private static class Vector3fDeserializer implements JsonDeserializer<Vector3f> {
        @Override
        public Vector3f deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            JsonObject jsonObject = json.getAsJsonObject();
            float x = jsonObject.get("x").getAsFloat();
            float y = jsonObject.get("y").getAsFloat();
            float z = jsonObject.get("z").getAsFloat();
            return new Vector3f(x, y, z);
        }
    }

    private static class QuaternionDeserializer implements JsonDeserializer<Quaternionf> {
        @Override
        public Quaternionf deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            JsonObject jsonObject = json.getAsJsonObject();
            float x = jsonObject.get("x").getAsFloat();
            float y = jsonObject.get("y").getAsFloat();
            float z = jsonObject.get("z").getAsFloat();
            float w = jsonObject.get("w").getAsFloat();
            return new Quaternionf(x, y, z, w);
        }
    }
}