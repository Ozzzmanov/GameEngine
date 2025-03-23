package org.example;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.io.IOException;
import java.io.InputStream;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;

public class TextureLoader {
    private final int id;
    private static int width;
    private static int height;

    public TextureLoader(String path) throws IOException {
        // Завантаження текстури з ресурсів
        id = loadTexture(path);
    }

    private static int loadTexture(String path) throws IOException {
        // Завантаження зображення використовуючи STB бібліотеку
        ByteBuffer imageBuffer;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            InputStream is = TextureLoader.class.getResourceAsStream(path);
            if (is == null) {
                throw new IOException("Не вдалось знайти ресурс: " + path);
            }

            // Зчитуємо дані в байтовий буфер
            byte[] imageData = is.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(imageData.length);
            buffer.put(imageData);
            buffer.flip();

            // Параметри зображення
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            // Встановлюємо прапор STB для перевертання зображення
            stbi_set_flip_vertically_on_load(true);

            // Завантажуємо зображення
            imageBuffer = stbi_load_from_memory(buffer, w, h, channels, 4);
            if (imageBuffer == null) {
                throw new IOException("Не вдалось завантажити зображення: " + stbi_failure_reason());
            }

            width = w.get(0);
            height = h.get(0);
        }

        // Створюємо OpenGL текстуру
        int textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        // Встановлюємо параметри текстури
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT); // X repeat
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT); // Y repeat
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR); //GL_NEAREST - квадратні пікселі GL_LINEAR - билинейная интерполяция

        // Завантажуємо дані зображення в текстуру
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, imageBuffer);
        glGenerateMipmap(GL_TEXTURE_2D);

        // Звільняємо пам'ять стороннього буфера
        stbi_image_free(imageBuffer);

        return textureID;
    }

    public void bind() {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, id);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void cleanup() {
        glDeleteTextures(id);
    }

    public int getId() {
        return id;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}