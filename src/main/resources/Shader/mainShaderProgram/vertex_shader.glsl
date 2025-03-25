#version 330 core

// Входные вершинные атрибуты
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec2 aTexCoord;
layout (location = 2) in vec3 aNormal;

// Выходные данные для фрагментного шейдера
out vec3 FragPos;           // Позиция фрагмента в мировом пространстве
out vec3 Normal;            // Нормаль фрагмента в мировом пространстве
out vec2 TexCoord;          // Текстурные координаты
out vec4 FragPosLightSpace; // Для теней

// Uniforms
uniform mat4 model;            // Модельная матрица
uniform mat4 mvp;              // Model-View-Projection матрица
uniform mat4 normalMatrix;     // Матрица для преобразования нормалей
uniform mat4 lightSpaceMatrix; // Матрица преобразования в пространство света

void main()
{
    gl_Position = mvp * vec4(aPos, 1.0);
    FragPos = vec3(model * vec4(aPos, 1.0));
    Normal = mat3(normalMatrix) * aNormal;
    TexCoord = aTexCoord;

    // Позиция фрагмента в пространстве источника света
    FragPosLightSpace = lightSpaceMatrix * vec4(FragPos, 1.0);
}