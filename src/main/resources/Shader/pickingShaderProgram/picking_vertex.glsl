#version 330 core

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;

uniform mat4 mvp;

void main()
{
    // Просто преобразуем вершины используя MVP матрицу
    gl_Position = mvp * vec4(aPos, 1.0);
}