#version 330 core

// Входные данные из вертексного шейдера
in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoord;

// Выходной цвет фрагмента
out vec4 FragColor;

// Структура для описания материала
struct Material {
    vec3 ambient;        // Ambient отражение
    vec3 diffuse;        // Diffuse отражение
    vec3 specular;       // Specular отражение
    float shininess;     // Коэффициент блеска
    int useTexture;      // Флаг использования текстуры (1 = использовать, 0 = не использовать)
    sampler2D diffuseMap; // Диффузная текстура
};

// Uniforms
uniform vec3 lightPos;     // Позиция источника света
uniform vec3 viewPos;      // Позиция камеры
uniform vec3 lightColor;   // Цвет источника света
uniform Material material;

void main()
{
    // Определяем базовый цвет (из текстуры или материала)
    vec3 baseColor;
    if (material.useTexture == 1) {
        baseColor = vec3(texture(material.diffuseMap, TexCoord));
    } else {
        baseColor = material.diffuse;
    }

    // Направление света (от фрагмента к источнику света)
    vec3 lightDir = normalize(lightPos - FragPos);

    // Направление обзора (от фрагмента к камере)
    vec3 viewDir = normalize(viewPos - FragPos);

    // Нормализованная нормаль фрагмента
    vec3 norm = normalize(Normal);

    // Ambient составляющая (фоновое освещение)
    vec3 ambient;
    if (material.useTexture == 1) {
        ambient = material.ambient * baseColor;
    } else {
        ambient = material.ambient * lightColor;
    }

    // Diffuse составляющая (рассеянное освещение)
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * baseColor * lightColor;

    // Specular составляющая (блики)
    vec3 reflectDir = reflect(-lightDir, norm);
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), material.shininess);
    vec3 specular = spec * material.specular * lightColor;

    // Расчет затухания (attenuation) с расстоянием
    float distance = length(lightPos - FragPos);
    float attenuation = 1.0 / (1.0 + 0.09 * distance + 0.032 * (distance * distance));

    // Применяем затухание ко всем компонентам, кроме ambient
    diffuse *= attenuation;
    specular *= attenuation;

    // Итоговый цвет фрагмента
    vec3 result = ambient + diffuse + specular;
    FragColor = vec4(result, 1.0);
}