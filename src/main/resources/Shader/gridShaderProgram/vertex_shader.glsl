#version 330 core

// Входные вершинные атрибуты
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec2 aTexCoord;
layout (location = 2) in vec3 aNormal;

// Выходные данные для фрагментного шейдера
out vec3 FragPos;      // Позиция фрагмента в мировом пространстве
out vec3 Normal;       // Нормаль фрагмента в мировом пространстве
out vec2 TexCoord;     // Текстурные координаты

// Uniforms
uniform mat4 model;         // Модельная матрица
uniform mat4 mvp;           // Model-View-Projection матрица
uniform mat4 normalMatrix;  // Матрица для преобразования нормалей

void main()
{
    // Рассчитываем позицию вершины в мировом пространстве
    FragPos = vec3(model * vec4(aPos, 1.0));

    // Трансформируем нормаль в мировое пространство
    // Используем специальную матрицу для нормалей (инвертированная транспонированная модельная матрица)
    Normal = normalize(mat3(normalMatrix) * aNormal);

    // Передаем текстурные координаты фрагментному шейдеру
    TexCoord = aTexCoord;

    // Рассчитываем final position в clip space
    gl_Position = mvp * vec4(aPos, 1.0);
}