package edu.ump.timetabling.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Serves static web frontend assets (HTML, CSS, JavaScript).
 */
public class StaticFileHandler implements HttpHandler {

    private final String resourceBasePath;
    private final Path fileBasePath;
    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put("html", "text/html; charset=UTF-8");
        MIME_TYPES.put("htm", "text/html; charset=UTF-8");
        MIME_TYPES.put("css", "text/css; charset=UTF-8");
        MIME_TYPES.put("js", "application/javascript; charset=UTF-8");
        MIME_TYPES.put("json", "application/json; charset=UTF-8");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("ico", "image/x-icon");
        MIME_TYPES.put("properties", "text/plain; charset=UTF-8");
    }

    public StaticFileHandler(String resourceBasePath, String filesystemPath) {
        this.resourceBasePath = resourceBasePath.endsWith("/") ? resourceBasePath : resourceBasePath + "/";
        this.fileBasePath = Paths.get(filesystemPath);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }

        // Security check: avoid directory traversal
        if (path.contains("..")) {
            sendError(exchange, 403, "Forbidden");
            return;
        }

        String subPath = path.startsWith("/") ? path.substring(1) : path;
        byte[] content = null;

        // Try filesystem first (for instant dev reloads)
        Path localFile = fileBasePath.resolve(subPath);
        if (Files.exists(localFile) && !Files.isDirectory(localFile)) {
            content = Files.readAllBytes(localFile);
        } else {
            // Fallback to classpath resource
            String resourcePath = resourceBasePath + subPath;
            try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                if (is != null) {
                    content = is.readAllBytes();
                }
            }
        }

        if (content == null) {
            sendError(exchange, 404, "File Not Found: " + path);
            return;
        }

        String ext = getExtension(path);
        String mimeType = MIME_TYPES.getOrDefault(ext, "application/octet-stream");

        exchange.getResponseHeaders().set("Content-Type", mimeType);
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
        exchange.sendResponseHeaders(200, content.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(content);
        }
    }

    private void sendError(HttpExchange exchange, int status, String msg) throws IOException {
        byte[] bytes = msg.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String getExtension(String path) {
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot + 1).toLowerCase() : "";
    }
}
