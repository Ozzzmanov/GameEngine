package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ImportObj {

    public static Mesh loadObjModel(String resourcePath) {
        List<Float> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        try {
            // Загрузка из ресурсов вместо прямого пути к файлу
            InputStream inputStream = ImportObj.class.getResourceAsStream(resourcePath);
            if (inputStream == null) {
                throw new IOException("Не вдалось знайти ресурс: " + resourcePath);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            List<Float> positionsList = new ArrayList<>();

            int vertexCount = 0;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");

                if (parts.length > 0) {
                    // Координаты вершин
                    if (parts[0].equals("v")) {
                        positionsList.add(Float.parseFloat(parts[1]));
                        positionsList.add(Float.parseFloat(parts[2]));
                        positionsList.add(Float.parseFloat(parts[3]));
                    }
                    // Грани (индексы)
                    else if (parts[0].equals("f")) {
                        // В OBJ индексы начинаются с 1, а в OpenGL с 0
                        for (int i = 1; i <= 3; i++) {
                            String[] vertexData = parts[i].split("/");
                            int vertexIndex = Integer.parseInt(vertexData[0]) - 1;

                            indices.add(vertexIndex);
                        }

                        // Обработка четырехугольников (разбиение на два треугольника)
                        if (parts.length > 4) {
                            String[] vertexData1 = parts[1].split("/");
                            String[] vertexData3 = parts[3].split("/");
                            String[] vertexData4 = parts[4].split("/");

                            int vertexIndex1 = Integer.parseInt(vertexData1[0]) - 1;
                            int vertexIndex3 = Integer.parseInt(vertexData3[0]) - 1;
                            int vertexIndex4 = Integer.parseInt(vertexData4[0]) - 1;

                            indices.add(vertexIndex1);
                            indices.add(vertexIndex3);
                            indices.add(vertexIndex4);
                        }
                    }
                }
            }

            reader.close();

            // Преобразование из списков в массивы
            float[] verticesArray = new float[positionsList.size()];
            for (int i = 0; i < positionsList.size(); i++) {
                verticesArray[i] = positionsList.get(i);
            }

            int[] indicesArray = new int[indices.size()];
            for (int i = 0; i < indices.size(); i++) {
                indicesArray[i] = indices.get(i);
            }

            return new Mesh(verticesArray, indicesArray);

        } catch (IOException e) {
            System.err.println("Ошибка при загрузке модели: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Расширенная версия метода, которая также загружает из ресурсов и обрабатывает нормали и текстурные координаты
    public static Mesh loadObjModelWithNormalsAndTextures(String resourcePath) {
        List<Float> verticesList = new ArrayList<>();
        List<Float> normalsList = new ArrayList<>();
        List<Float> texturesList = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        try {
            InputStream inputStream = ImportObj.class.getResourceAsStream(resourcePath);
            if (inputStream == null) {
                throw new IOException("Не удалось найти ресурс: " + resourcePath);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            List<Float> tempVertices = new ArrayList<>();
            List<Float> tempNormals = new ArrayList<>();
            List<Float> tempTextures = new ArrayList<>();

            List<Float> finalVertices = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");

                if (parts.length > 0) {
                    // Вершины
                    if (parts[0].equals("v")) {
                        tempVertices.add(Float.parseFloat(parts[1]));
                        tempVertices.add(Float.parseFloat(parts[2]));
                        tempVertices.add(Float.parseFloat(parts[3]));
                    }
                    // Текстурные координаты
                    else if (parts[0].equals("vt") && parts.length >= 3) {
                        tempTextures.add(Float.parseFloat(parts[1]));
                        tempTextures.add(Float.parseFloat(parts[2]));
                    }
                    // Нормали
                    else if (parts[0].equals("vn")) {
                        tempNormals.add(Float.parseFloat(parts[1]));
                        tempNormals.add(Float.parseFloat(parts[2]));
                        tempNormals.add(Float.parseFloat(parts[3]));
                    }
                    // Грани
                    else if (parts[0].equals("f")) {
                        processVertex(parts[1], indices, tempVertices, tempTextures, tempNormals, finalVertices);
                        processVertex(parts[2], indices, tempVertices, tempTextures, tempNormals, finalVertices);
                        processVertex(parts[3], indices, tempVertices, tempTextures, tempNormals, finalVertices);

                        if (parts.length > 4) {
                            processVertex(parts[1], indices, tempVertices, tempTextures, tempNormals, finalVertices);
                            processVertex(parts[3], indices, tempVertices, tempTextures, tempNormals, finalVertices);
                            processVertex(parts[4], indices, tempVertices, tempTextures, tempNormals, finalVertices);
                        }
                    }
                }
            }

            reader.close();

            // Преобразование из списков в массивы
            float[] verticesArray = new float[finalVertices.size()];
            for (int i = 0; i < finalVertices.size(); i++) {
                verticesArray[i] = finalVertices.get(i);
            }

            int[] indicesArray = new int[indices.size()];
            for (int i = 0; i < indices.size(); i++) {
                indicesArray[i] = indices.get(i);
            }

            return new Mesh(verticesArray, indicesArray);

        } catch (IOException e) {
            System.err.println("Ошибка при загрузке модели: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static void processVertex(String vertexData, List<Integer> indices,
                                      List<Float> vertices, List<Float> textures,
                                      List<Float> normals, List<Float> finalVertices) {
        String[] vertex = vertexData.split("/");
        int vertexIndex = Integer.parseInt(vertex[0]) - 1;
        indices.add(vertexIndex);

        if (vertex.length >= 2 && !vertex[1].isEmpty()) {
            int textureIndex = Integer.parseInt(vertex[1]) - 1;
            // Добавление текстурных координат
        }

        if (vertex.length >= 3) {
            int normalIndex = Integer.parseInt(vertex[2]) - 1;
            // Добавление нормалей
        }
    }
}