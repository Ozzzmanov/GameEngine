package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.lwjgl.opengl.GL20.*;

public class ShaderLoader {

    public static int loadShader(String vertexShaderPath, String fragmentShaderPath) throws IOException {
        String vertexShaderSource = readResource(vertexShaderPath);
        String fragmentShaderSource = readResource(fragmentShaderPath);

        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexShaderSource);
        glCompileShader(vertexShader);
        checkCompileErrors(vertexShader, "VERTEX");

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentShaderSource);
        glCompileShader(fragmentShader);
        checkCompileErrors(fragmentShader, "FRAGMENT");

        int shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertexShader);
        glAttachShader(shaderProgram, fragmentShader);
        glLinkProgram(shaderProgram);
        checkCompileErrors(shaderProgram, "PROGRAM");

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        return shaderProgram;
    }

    private static String readResource(String resourcePath) throws IOException {
        try (InputStream inputStream = ShaderLoader.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
                return scanner.useDelimiter("\\A").next();
            }
        }
    }

    private static void checkCompileErrors(int shader, String type) {
        if (type.equals("PROGRAM")) {
            if (glGetProgrami(shader, GL_LINK_STATUS) == GL_FALSE) {
                System.err.println("ERROR: Program linking failed.");
                System.err.println(glGetProgramInfoLog(shader));
            }
        } else {
            if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
                System.err.println("ERROR: Shader compilation failed.");
                System.err.println(glGetShaderInfoLog(shader));
            }
        }
    }
}
