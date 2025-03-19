#version 330 core

uniform vec3 objectColor;
out vec4 FragColor;

void main()
{
    // Просто возвращаем уникальный цвет для каждого объекта
    // RGB значения представляют закодированный ID объекта
    FragColor = vec4(objectColor, 1.0);
}