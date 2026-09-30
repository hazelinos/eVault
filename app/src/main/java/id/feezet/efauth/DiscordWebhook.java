package id.feezet.efauth;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class DiscordWebhook {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private DiscordWebhook() {}

    static boolean isValidUrl(String value) {
        return value != null && (
                value.startsWith("https://discord.com/api/webhooks/")
                || value.startsWith("https://discordapp.com/api/webhooks/"));
    }

    /** Fire-and-forget plain message. Kept for backward compatibility. */
    static void send(String webhookUrl, String content) {
        if (!isValidUrl(webhookUrl) || content == null || content.trim().isEmpty()) return;
        EXECUTOR.execute(() -> request(webhookUrl, "POST", content, false));
    }

    static void sendTransaction(String webhookUrl, String content, Callback callback) {
        if (!isValidUrl(webhookUrl) || content == null || content.trim().isEmpty()) {
            if (callback != null) callback.onResult(null);
            return;
        }
        EXECUTOR.execute(() -> {
            String id = request(webhookUrl + "?wait=true", "POST", content, false);
            if (callback != null) callback.onResult(id);
        });
    }

    static void edit(String webhookUrl, String messageId, String content, Callback callback) {
        if (!isValidUrl(webhookUrl) || messageId == null || messageId.isEmpty()
                || content == null || content.trim().isEmpty()) {
            if (callback != null) callback.onResult(null);
            return;
        }
        EXECUTOR.execute(() -> {
            String id = request(webhookUrl + "/messages/" + messageId, "PATCH", content, false);
            if (callback != null) callback.onResult(id);
        });
    }

    /** Sends one Discord embed and returns its message id. */
    static void sendEmbedTransaction(String webhookUrl, String title, String description,
                                      int color, String fieldsJson, Callback callback) {
        if (!isValidUrl(webhookUrl)) {
            if (callback != null) callback.onResult(null);
            return;
        }
        EXECUTOR.execute(() -> {
            String payload = buildEmbedPayload(title, description, color, fieldsJson);
            String id = request(webhookUrl + "?wait=true", "POST", payload, true);
            if (callback != null) callback.onResult(id);
        });
    }

    /** Replaces the existing webhook message with one embed. */
    static void editEmbed(String webhookUrl, String messageId, String title, String description,
                          int color, String fieldsJson, Callback callback) {
        if (!isValidUrl(webhookUrl) || messageId == null || messageId.isEmpty()) {
            if (callback != null) callback.onResult(null);
            return;
        }
        EXECUTOR.execute(() -> {
            String payload = buildEmbedPayload(title, description, color, fieldsJson);
            String id = request(webhookUrl + "/messages/" + messageId, "PATCH", payload, true);
            if (callback != null) callback.onResult(id);
        });
    }

    private static String buildEmbedPayload(String title, String description,
                                             int color, String fieldsJson) {
        StringBuilder body = new StringBuilder();
        body.append("{\"embeds\":[{");
        body.append("\"title\":\"").append(escapeJson(title == null ? "" : title)).append("\",");
        body.append("\"description\":\"").append(escapeJson(description == null ? "" : description)).append("\",");
        if (color > 0) body.append(",\"color\":").append(color);
        if (fieldsJson != null && !fieldsJson.trim().isEmpty()) {
            body.append(",\"fields\":").append(fieldsJson);
        }
        body.append("}]}");
        return body.toString();
    }

    private static String request(String url, String method, String payload, boolean rawJson) {
        HttpURLConnection connection = null;
        try {
            URL target = new URL(url);
            connection = (HttpURLConnection) target.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            String body = rawJson
                    ? payload
                    : "{\"content\":\"" + escapeJson(payload) + "\"}";
            try (OutputStream out = connection.getOutputStream()) {
                out.write(body.getBytes(StandardCharsets.UTF_8));
            }
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) return null;
            return extractId(readStream(connection.getInputStream()));
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String readStream(InputStream in) {
        if (in == null) return null;
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        } catch (Exception ignored) {
            return null;
        }
        return sb.toString();
    }

    private static String extractId(String json) {
        if (json == null) return null;
        int key = json.indexOf("\"id\"");
        if (key < 0) return null;
        int colon = json.indexOf(':', key);
        if (colon < 0) return null;
        int firstQuote = json.indexOf('"', colon + 1);
        if (firstQuote < 0) return null;
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) return null;
        return json.substring(firstQuote + 1, secondQuote);
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    interface Callback {
        void onResult(String messageIdOrNull);
    }
}
