package org.example;

import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ObjectLoader {

    public static Mesh loadObjModel(String resourcePath) {
        List<Vector3f> positions = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        List<Vector2f> texCoords = new ArrayList<>();

        try {
            // Загрузка из ресурсов
            InputStream inputStream = ObjectLoader.class.getResourceAsStream(resourcePath);
            if (inputStream == null) {
                throw new IOException("Не удалось найти ресурс: " + resourcePath);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            // Первый проход - чтение всех вершин и нормалей
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");

                if (parts.length > 0) {
                    // Координаты вершин
                    if (parts[0].equals("v") && parts.length >= 4) {
                        float x = Float.parseFloat(parts[1]);
                        float y = Float.parseFloat(parts[2]);
                        float z = Float.parseFloat(parts[3]);
                        positions.add(new Vector3f(x, y, z));
                    }
                    // Нормали
                    else if (parts[0].equals("vn") && parts.length >= 4) {
                        float nx = Float.parseFloat(parts[1]);
                        float ny = Float.parseFloat(parts[2]);
                        float nz = Float.parseFloat(parts[3]);
                        Vector3f normal = new Vector3f(nx, ny, nz).normalize(); // Нормализуем сразу
                        normals.add(normal);
                    }
                    // Текстурні координати
                    else if (parts[0].equals("vt") && parts.length >= 3) {
                        float u = Float.parseFloat(parts[1]);
                        float v = Float.parseFloat(parts[2]);
                        texCoords.add(new Vector2f(u, v));
                    }
                }
            }

            // Если в модели нет нормалей, создаем временную структуру для их расчета
            boolean hasNormals = !normals.isEmpty();
            Vector3f[] tempNormals = null;

            if (!hasNormals) {
                tempNormals = new Vector3f[positions.size()];
                for (int i = 0; i < positions.size(); i++) {
                    tempNormals[i] = new Vector3f(0.0f, 0.0f, 0.0f);
                }
            }

            // Сбрасываем поток и начинаем второй проход
            reader.close();
            inputStream = ObjectLoader.class.getResourceAsStream(resourcePath);
            reader = new BufferedReader(new InputStreamReader(inputStream));

            // Хранит уникальные комбинации вершин и их нормалей
            List<Vector3f> finalPositions = new ArrayList<>();
            List<Vector3f> finalNormals = new ArrayList<>();
            List<Vector2f> finalTexCoords = new ArrayList<>(); // Добавляем хранилище текстурных координат

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");

                if (parts.length > 0 && parts[0].equals("f")) {
                    if (parts.length < 4) continue; // Пропускаем, если меньше трех вершин

                    int[] vertIndices = new int[parts.length - 1];
                    int[] normIndices = new int[parts.length - 1];
                    int[] texIndices = new int[parts.length - 1]; // Текстурні

                    // Получаем индексы вершин и нормалей
                    for (int i = 1; i < parts.length; i++) {
                        String[] elements = parts[i].split("/");
                        vertIndices[i-1] = Integer.parseInt(elements[0]) - 1; // OBJ индексы с 1

                        // Проверяем текстурные координаты
                        if (elements.length >= 2 && !elements[1].isEmpty()) {
                            texIndices[i-1] = Integer.parseInt(elements[1]) - 1;
                        } else {
                            texIndices[i-1] = -1; // Нет текстурных координат
                        }

                        if (hasNormals && elements.length >= 3 && !elements[2].isEmpty()) {
                            normIndices[i-1] = Integer.parseInt(elements[2]) - 1;
                        } else {
                            normIndices[i-1] = -1; // Нет нормали
                        }
                    }

                    // Если нет нормалей, вычисляем их для грани
                    if (!hasNormals) {
                        Vector3f v0 = positions.get(vertIndices[0]);
                        Vector3f v1 = positions.get(vertIndices[1]);
                        Vector3f v2 = positions.get(vertIndices[2]);

                        Vector3f edge1 = new Vector3f(v1).sub(v0);
                        Vector3f edge2 = new Vector3f(v2).sub(v0);
                        Vector3f normal = new Vector3f(edge1).cross(edge2).normalize();

                        // Добавляем эту нормаль ко всем вершинам грани
                        for (int idx : vertIndices) {
                            tempNormals[idx].add(normal);
                        }
                    }

                    // Триангуляция и создание уникальных вершин
                    for (int i = 1; i < vertIndices.length - 1; i++) {
                        addVertex(vertIndices[0], normIndices[0], texIndices[0],
                                positions, hasNormals ? normals : null, texCoords,
                                tempNormals, hasNormals, finalPositions, finalNormals,
                                finalTexCoords, indices);

                        addVertex(vertIndices[i], normIndices[i], texIndices[i],
                                positions, hasNormals ? normals : null, texCoords,
                                tempNormals, hasNormals, finalPositions, finalNormals,
                                finalTexCoords, indices);

                        addVertex(vertIndices[i+1], normIndices[i+1], texIndices[i+1],
                                positions, hasNormals ? normals : null, texCoords,
                                tempNormals, hasNormals, finalPositions, finalNormals,
                                finalTexCoords, indices);
                    }
                }
            }

            // Создаем финальный массив вершинных данных
            float[] verticesArray = new float[finalPositions.size() * 8]; // xyz + uv + normal xyz

            for (int i = 0; i < finalPositions.size(); i++) {
                Vector3f pos = finalPositions.get(i);
                Vector2f tex = finalTexCoords.get(i);
                Vector3f norm = finalNormals.get(i);

                // Позиція
                verticesArray[i * 8] = pos.x;
                verticesArray[i * 8 + 1] = pos.y;
                verticesArray[i * 8 + 2] = pos.z;

                // Текстурні координати
                verticesArray[i * 8 + 3] = tex.x;
                verticesArray[i * 8 + 4] = tex.y;

                // Нормаль
                verticesArray[i * 8 + 5] = norm.x;
                verticesArray[i * 8 + 6] = norm.y;
                verticesArray[i * 8 + 7] = norm.z;
            }

            // Преобразуем индексы
            int[] indicesArray = new int[indices.size()];
            for (int i = 0; i < indices.size(); i++) {
                indicesArray[i] = indices.get(i);
            }

            reader.close();
            return new Mesh(verticesArray, indicesArray);

        } catch (IOException e) {
            System.err.println("Ошибка при загрузке модели: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (NumberFormatException e) {
            System.err.println("Ошибка при парсинге числа: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Вспомогательный метод для добавления вершины (обновленный для поддержки текстур)
    private static void addVertex(int posIndex, int normIndex, int texIndex,
                                  List<Vector3f> positions, List<Vector3f> normals,
                                  List<Vector2f> texCoords, Vector3f[] tempNormals,
                                  boolean hasNormals, List<Vector3f> finalPositions,
                                  List<Vector3f> finalNormals, List<Vector2f> finalTexCoords,
                                  List<Integer> indices) {
        Vector3f position = positions.get(posIndex);
        Vector3f normal;
        Vector2f texCoord;

        if (hasNormals && normIndex >= 0) {
            normal = new Vector3f(normals.get(normIndex));
        } else if (tempNormals != null) {
            normal = new Vector3f(tempNormals[posIndex]).normalize();
        } else {
            normal = new Vector3f(0, 1, 0); // Запасной вариант
        }

        // Получаем текстурные координаты или используем дефолтные, если их нет
        if (texIndex >= 0 && texIndex < texCoords.size()) {
            texCoord = new Vector2f(texCoords.get(texIndex));
        } else {
            texCoord = new Vector2f(0, 0); // Дефолтные текстурные координаты
        }

        // Добавляем вершину и получаем её индекс
        finalPositions.add(new Vector3f(position));
        finalNormals.add(new Vector3f(normal));
        finalTexCoords.add(new Vector2f(texCoord));
        indices.add(finalPositions.size() - 1);
    }
}