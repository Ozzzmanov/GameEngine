package org.example.Render;

import org.example.Grid;
import org.example.Mesh;
import org.example.Node;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.List;

/**
 * Інтерфейс `RenderStrategy`, який визначає стратегію рендерингу для різних типів об'єктів.
 * Цей інтерфейс дозволяє визначити різні підходи до рендерингу, залежно від переданого об'єкта `Mesh`,
 * шейдерної програми, матриць виду та проекції, позиції камери та списку вузлів освітлення.
 *
 * Основні функції:
 * - Метод `render()` виконує рендеринг заданого об'єкта `Mesh` з використанням відповідних матриць та шейдерів.
 * - Параметри включають інформацію про об'єкт, камеру та освітлення, що дозволяє здійснити коректне відображення сцени.
 *
 * Параметри:
 * - `mesh` – об'єкт, який потрібно відобразити.
 * - `shaderProgram` – ідентифікатор шейдерної програми для рендерингу.
 * - `viewMatrix` – матриця виду для камери.
 * - `projectionMatrix` – матриця проекції для камери.
 * - `cameraPosition` – позиція камери в 3D просторі.
 * - `lightNodes` – список вузлів, що містять інформацію про джерела світла в сцені.
 *
 * Використання:
 * - Рендеринг може бути реалізований через різні стратегії.
 * - Кожна стратегія може обробляти специфічні методи рендерингу в залежності від типу сцени чи вимог.
 *
 * @author Вадим Овсюк
 * @version 0.8
 * @since 2025-03-23
 */

public interface RenderStrategy {
    default void render(Mesh mesh, int shaderProgram, Matrix4f viewMatrix, Matrix4f projectionMatrix,
                        Vector3f cameraPosition, List<Node> lightNodes) {
        // Реалізація для звичайного рендерингу
    }

    default void render(Mesh mesh, int shaderProgram, Matrix4f viewMatrix, Matrix4f projectionMatrix,
                        Matrix4f lightSpaceMatrix) {
        // Реалізація для рендерингу тіней
    }

    default void render(Grid grid, int shaderProgram, Matrix4f viewMatrix, Matrix4f projectionMatrix) {
        // Реалізація для рендерингу сітки
    }
}