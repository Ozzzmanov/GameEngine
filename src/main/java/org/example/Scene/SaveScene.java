package org.example.Scene;

import com.google.gson.*;
import org.example.Mesh;
import org.example.Node;
import org.example.ObjectLoader;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Paths;
import java.util.UUID;

public class SaveScene {
    public static void saveScene(Node rootNode, String resourcePath) {
        GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting();

        // Регистрируем адаптеры для классов
        gsonBuilder.registerTypeAdapter(Node.class, new NodeSerializer());
        gsonBuilder.registerTypeAdapter(Vector3f.class, new Vector3fSerializer());
        gsonBuilder.registerTypeAdapter(Quaternionf.class, new QuaternionSerializer());
        gsonBuilder.registerTypeAdapter(Mesh.class, new MeshSerializer());

        Gson gson = gsonBuilder.create();

        try {
            URL resourceUrl = SaveScene.class.getClassLoader().getResource(resourcePath);
            File file;

            if (resourceUrl == null) {
                file = Paths.get("src/main/resources/" + resourcePath).toFile();
                file.getParentFile().mkdirs();
                file.createNewFile();
            } else {
                file = new File(resourceUrl.toURI());
            }

            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(rootNode, writer);
                System.out.println("Файл сохранён: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Сериализатор для Node
    private static class NodeSerializer implements JsonSerializer<Node> {
        @Override
        public JsonElement serialize(Node src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject result = new JsonObject();

            // Сериализуем основные свойства
            result.addProperty("id", src.getId().toString());
            result.addProperty("name", src.getName());
            result.add("position", context.serialize(src.getPosition()));
            result.add("rotation", context.serialize(src.getRotation()));
            result.add("scale", context.serialize(src.getScale()));
            result.addProperty("nodeType", src.getNodeType().toString());

            // Сериализуем светозависимые свойства
            if (src.getNodeType() == Node.NodeType.LIGHT) {
                result.add("lightColor", context.serialize(src.getLightColor()));
                result.addProperty("lightIntensity", src.getLightIntensity());
            }

            // Сериализуем только названия мешей
            if (src.getMeshes() != null && !src.getMeshes().isEmpty()) {
                JsonArray meshes = new JsonArray();
                for (Mesh mesh : src.getMeshes()) {
                    meshes.add(context.serialize(mesh));
                }
                result.add("meshes", meshes);
            }

            // Рекурсивно сериализуем дочерние узлы
            if (src.getChildren() != null && !src.getChildren().isEmpty()) {
                JsonArray children = new JsonArray();
                for (Node child : src.getChildren()) {
                    children.add(context.serialize(child));
                }
                result.add("children", children);
            }

            return result;
        }
    }

    // Сериализатор для Mesh
    private static class MeshSerializer implements JsonSerializer<Mesh> {
        @Override
        public JsonElement serialize(Mesh src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject result = new JsonObject();
            // Сохраняем только путь к ресурсу меша
            result.addProperty("resourcePath", src.getResourcePath());
            // Если у меша есть материал, тоже сохраняем информацию о нём
            if (src.getShaderMaterial() != null) {
                result.add("material", context.serialize(src.getShaderMaterial()));
            }
            return result;
        }
    }

    // Сериализаторы для Vector3f и Quaternionf
    private static class Vector3fSerializer implements JsonSerializer<Vector3f> {
        @Override
        public JsonElement serialize(Vector3f src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject result = new JsonObject();
            result.addProperty("x", src.x);
            result.addProperty("y", src.y);
            result.addProperty("z", src.z);
            return result;
        }
    }

    private static class QuaternionSerializer implements JsonSerializer<Quaternionf> {
        @Override
        public JsonElement serialize(Quaternionf src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject result = new JsonObject();
            result.addProperty("x", src.x);
            result.addProperty("y", src.y);
            result.addProperty("z", src.z);
            result.addProperty("w", src.w);
            return result;
        }
    }
}