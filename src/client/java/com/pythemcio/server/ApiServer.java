package com.pythemcio.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pythemcio.PythemcIO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ApiServer {

    private static HttpServer server;
    private static int port = 8080;
    private static String apiKey = "pythemcio";
    private static boolean running = false;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void start(Path configDir) {
        loadConfig(configDir);

        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            server.createContext("/event", new EventHandler());
            server.createContext("/status", new StatusHandler());
            server.setExecutor(null);
            server.start();
            running = true;

            PythemcIO.LOGGER.info("[PythemcIO] API server started on http://127.0.0.1:{}/", port);
            PythemcIO.LOGGER.info("[PythemcIO] API key: {}", apiKey);
        } catch (IOException e) {
            PythemcIO.LOGGER.error("[PythemcIO] Failed to start API server on port {}", port, e);
        }
    }

    public static void stop() {
        if (server != null && running) {
            server.stop(0);
            running = false;
            PythemcIO.LOGGER.info("[PythemcIO] API server stopped.");
        }
    }

    public static boolean isRunning() {
        return running;
    }

    public static int getPort() {
        return port;
    }

    public static String getApiKey() {
        return apiKey;
    }

    private static void loadConfig(Path configDir) {
        Path file = configDir.resolve("server.json");
        if (Files.exists(file)) {
            try (BufferedReader reader = Files.newBufferedReader(file)) {
                JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();
                if (config.has("port")) port = config.get("port").getAsInt();
                if (config.has("api_key")) apiKey = config.get("api_key").getAsString();
            } catch (Exception e) {
                PythemcIO.LOGGER.warn("[PythemcIO] Failed to load server config, using defaults");
            }
        } else {
            saveConfig(configDir);
        }
    }

    private static void saveConfig(Path configDir) {
        Path file = configDir.resolve("server.json");
        try (Writer writer = Files.newBufferedWriter(file)) {
            JsonObject config = new JsonObject();
            config.addProperty("port", port);
            config.addProperty("api_key", apiKey);
            config.addProperty("enabled", true);
            GSON.toJson(config, writer);
        } catch (IOException e) {
            PythemcIO.LOGGER.error("[PythemcIO] Failed to save server config", e);
        }
    }

    private static class EventHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405, "{\"status\":\"error\",\"message\":\"POST required\"}");
                return;
            }

            String authHeader = exchange.getRequestHeaders().getFirst("X-Api-Key");
            if (authHeader == null || !authHeader.equals(apiKey)) {
                sendResponse(exchange, 403, "{\"status\":\"error\",\"message\":\"Invalid API key\"}");
                return;
            }

            String body;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                body = sb.toString();
            }

            String response = GameActionHandler.handle(body, apiKey);
            sendResponse(exchange, 200, response);
        }
    }

    private static class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String authHeader = exchange.getRequestHeaders().getFirst("X-Api-Key");
            if (authHeader == null || !authHeader.equals(apiKey)) {
                sendResponse(exchange, 403, "{\"status\":\"error\",\"message\":\"Invalid API key\"}");
                return;
            }

            JsonObject status = new JsonObject();
            status.addProperty("status", "ok");
            status.addProperty("server", "PythemcIO");
            status.addProperty("port", port);
            status.addProperty("running", running);
            sendResponse(exchange, 200, GSON.toJson(status));
        }
    }

    private static void sendResponse(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
