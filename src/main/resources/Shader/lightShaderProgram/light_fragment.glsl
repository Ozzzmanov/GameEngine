#version 330 core

// Выход фрагментного шейдера
out vec4 FragColor;

// Uniform для цвета источника света
uniform vec3 lightColor;

void main()
{
    // Источник света просто светится своим цветом
    FragColor = vec4(lightColor, 1.0);
}