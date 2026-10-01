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

    /** Appends ?wait=true (or &wait=true) so Discord returns the created message. */
    private static String withWait(String webhookUrl) {
        return webhookUrl + (webhookUrl.contains("?") ? "&wait=true" : "?wait=true");
    }

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
            String id = request(withWait(webhookUrl), "POST", content, false);
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
            String id = request(withWait(webhookUrl), "POST", payload, true);
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
        StringBuilder embed = new StringBuilder();
        embed.append("\"title\":\"").append(escapeJson(title == null ? "" : title)).append("\"");
        embed.append(",\"description\":\"").append(escapeJson(description == null ? "" : description)).append("\"");
        if (color > 0) embed.append(",\"color\":").append(color);
        if (fieldsJson != null && !fieldsJson.trim().isEmpty()) {
            embed.append(",\"fields\":").append(fieldsJson);
        }
        return "{\"embeds\":[{" + embed + "}]}";
    }

    private static String request(String url, String method, String payload, boolean rawJson) {
        String body = rawJson
                ? payload
                : "{\"content\":\"" + escapeJson(payload) + "\"}";
        // One retry on HTTP 429, honoring Discord's Retry-After header.
        for (int attempt = 0; attempt < 2; attempt++) {
            HttpURLConnection connection = null;
            try {
                URL target = new URL(url);
                connection = (HttpURLConnection) target.openConnection();
                connection.setRequestMethod(method);
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(body.getBytes(StandardCharsets.UTF_8));
                }
                int code = connection.getResponseCode();
                if (code == 429 && attempt == 0) {
                    long retryAfterMs = 1000L;
                    try {
                        String header = connection.getHeaderField("Retry-After");
                        if (header != null) retryAfterMs = Math.min(5000L, (long) (Double.parseDouble(header) * 1000L));
                    } catch (Exception ignored) {}
                    try { Thread.sleep(Math.max(250L, retryAfterMs)); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                    continue;
                }
                if (code < 200 || code >= 300) return null;
                return extractId(readStream(connection.getInputStream()));
            } catch (Exception ignored) {
                return null;
            } finally {
                if (connection != null) connection.disconnect();
            }
        }
        return null;
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
        // Only accept the top-level message "id" (JSON depth 1), never a nested
        // author/embed/field id, so KEY_AL_WEBHOOK_MSG_ID stays correct.
        int depth = 0;
        boolean inString = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                if (c == '\\') { i++; continue; }
                if (c == '"') inString = false;
                continue;
            }
            if (c == '"') {
                if (depth == 1 && matchesKeyAt(json, i)) {
                    String id = stringValueAfter(json, i);
                    if (id != null) return id;
                }
                inString = true;
            } else if (c == '{' || c == '[') {
                depth++;
            } else if (c == '}' || c == ']') {
                depth--;
            }
        }
        return null;
    }

    /** True if the quote at index i begins the object key "id". */
    private static boolean matchesKeyAt(String json, int i) {
        if (i + 4 >= json.length()) return false;
        if (json.charAt(i + 1) != 'i' || json.charAt(i + 2) != 'd' || json.charAt(i + 3) != '"') return false;
        int colon = i + 4;
        while (colon < json.length() && Character.isWhitespace(json.charAt(colon))) colon++;
        return colon < json.length() && json.charAt(colon) == ':';
    }

    /** Reads the JSON string value following the key at index i, or null. */
    private static String stringValueAfter(String json, int i) {
        int colon = json.indexOf(':', i);
        if (colon < 0) return null;
        int firstQuote = json.indexOf('"', colon + 1);
        if (firstQuote < 0) return null;
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) return null;
        return json.substring(firstQuote + 1, secondQuote);
    }

    /** JSON string escaper shared with MainActivity's embed field builder. */
    static String escapeJson(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\': out.append("\\\\"); break;
                case '"': out.append("\\\""); break;
                case '\r': out.append("\\r"); break;
                case '\n': out.append("\\n"); break;
                case '\t': out.append("\\t"); break;
                case '\b': out.append("\\b"); break;
                case '\f': out.append("\\f"); break;
                default:
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
            }
        }
        return out.toString();
    }

    interface Callback {
        void onResult(String messageIdOrNull);
    }
}
