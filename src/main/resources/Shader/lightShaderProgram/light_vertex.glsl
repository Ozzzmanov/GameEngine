#version 330 core

// Входные вершинные атрибуты
layout (location = 0) in vec3 aPos;

// Uniform переменные
uniform mat4 mvp; // Model-View-Projection матрица

void main()
{
    // Просто преобразуем позицию в clip space
    gl_Position = mvp * vec4(aPos, 1.0);
}