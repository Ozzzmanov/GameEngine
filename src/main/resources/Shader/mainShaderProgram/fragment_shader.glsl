#version 330 core

// Входные данные из вертексного шейдера
in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoord;
in vec4 FragPosLightSpace; // Для теней

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
uniform float lightIntensity; // Интенсивность источника света
uniform Material material;
uniform sampler2D shadowMap; // Карта теней

float ShadowCalculation(vec4 fragPosLightSpace) {
    // Преобразуем координаты в нормализованные координаты устройства
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;

    // Преобразуем диапазон [-1,1] в [0,1]
    projCoords = projCoords * 0.5 + 0.5;

    // Получаем ближайшую глубину от источника света
    float closestDepth = texture(shadowMap, projCoords.xy).r;

    // Получаем глубину текущего фрагмента от источника света
    float currentDepth = projCoords.z;

    // Добавляем смещение для борьбы с акне теней
    float bias = 0.005;

    // Проверяем, находится ли фрагмент в тени
    float shadow = 0.0;

    // PCF (Percentage Closer Filtering) для сглаживания теней
    vec2 texelSize = 1.0 / vec2(textureSize(shadowMap, 0));
    for(int x = -1; x <= 1; ++x) {
        for(int y = -1; y <= 1; ++y) {
            float pcfDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
            shadow += currentDepth - bias > pcfDepth ? 1.0 : 0.0;
        }
    }
    shadow /= 9.0;

    // Проверяем, находится ли вообще фрагмент в пределах карты теней
    if(projCoords.z > 1.0)
    shadow = 0.0;

    return shadow;
}

void main() {
    // Нормализуем нормаль (она может быть не нормализованной из-за интерполяции)
    vec3 norm = normalize(Normal);

    // Направление к источнику света
    vec3 lightDir = normalize(lightPos - FragPos);

    // Направление обзора (от фрагмента к камере)
    vec3 viewDir = normalize(viewPos - FragPos);

    // Направление отражения (для зеркального освещения)
    vec3 reflectDir = reflect(-lightDir, norm);

    // Рассчитываем коэффициенты освещения

    // Фоновое освещение (ambient)
    vec3 ambient;
    if (material.useTexture == 1) {
        ambient = material.ambient * texture(material.diffuseMap, TexCoord).rgb;
    } else {
        ambient = material.ambient;
    }

    // Диффузное освещение (diffuse)
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse;
    if (material.useTexture == 1) {
        diffuse = diff * material.diffuse * texture(material.diffuseMap, TexCoord).rgb;
    } else {
        diffuse = diff * material.diffuse;
    }

    // Зеркальное освещение (specular)
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), material.shininess);
    vec3 specular = spec * material.specular;

    // Рассчитываем коэффициент тени
    float shadow = ShadowCalculation(FragPosLightSpace);

    // Итоговый цвет с учетом теней
    vec3 result = ambient + (1.0 - shadow) * (diffuse + specular) * lightColor * lightIntensity;

    FragColor = vec4(result, 1.0);
}