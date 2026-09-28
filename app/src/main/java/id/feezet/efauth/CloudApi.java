package id.feezet.efauth;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class CloudApi {
    private CloudApi() {}

    public static final class AccountIdentity {
        public final String id;
        public final String displayName;
        public final String telegramUsername;
        public final String tokenPrefix;

        public AccountIdentity(String id, String displayName, String telegramUsername, String tokenPrefix) {
            this.id = id;
            this.displayName = displayName;
            this.telegramUsername = telegramUsername;
            this.tokenPrefix = tokenPrefix;
        }
    }

    public static final class PairSession {
        public final String sessionId;
        public final String botUrl;
        public PairSession(String sessionId, String botUrl) {
            this.sessionId = sessionId;
            this.botUrl = botUrl;
        }
    }

    public static final class PairStatus {
        public final String status;
        public final String token;
        public final String telegramUsername;
        public PairStatus(String status, String token, String telegramUsername) {
            this.status = status;
            this.token = token;
            this.telegramUsername = telegramUsername;
        }
    }

    public static final class CloudFile {
        public final String category;
        public final String account;
        public CloudFile(String category, String account) {
            this.category = category;
            this.account = account;
        }
    }

    private static String base(String baseUrl) {
        String s = baseUrl == null ? "" : baseUrl.trim();
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private static HttpURLConnection connection(String baseUrl, String deviceToken, String method, String path) throws Exception {
        URL url = new URL(base(baseUrl) + path);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setRequestProperty("Accept", "application/json");
        if (deviceToken != null && !deviceToken.trim().isEmpty()) {
            c.setRequestProperty("X-eVault-Device-Token", deviceToken);
        }
        return c;
    }

    private static byte[] read(InputStream in) throws Exception {
        try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = input.read(buffer)) != -1) out.write(buffer, 0, n);
            return out.toByteArray();
        }
    }

    private static byte[] request(HttpURLConnection c, byte[] body, String contentType) throws Exception {
        if (body != null) {
            c.setDoOutput(true);
            if (contentType != null) c.setRequestProperty("Content-Type", contentType);
            try (OutputStream out = c.getOutputStream()) { out.write(body); }
        }
        int code = c.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
        byte[] data = stream == null ? new byte[0] : read(stream);
        if (code < 200 || code >= 300) {
            throw new Exception("Cloud HTTP " + code + ": " + new String(data, StandardCharsets.UTF_8));
        }
        return data;
    }

    public static PairSession startPairing(String baseUrl, String deviceId, String name, String model, String version) throws Exception {
        HttpURLConnection c = connection(baseUrl, null, "POST", "/api/evault/v1/pair/start");
        JSONObject body = new JSONObject();
        body.put("device_id", deviceId);
        body.put("name", name);
        body.put("model", model);
        body.put("app_version", version);
        JSONObject root = new JSONObject(new String(
                request(c, body.toString().getBytes(StandardCharsets.UTF_8), "application/json; charset=utf-8"),
                StandardCharsets.UTF_8));
        return new PairSession(root.getString("session_id"), root.getString("bot_url"));
    }

    public static PairStatus pairStatus(String baseUrl, String sessionId) throws Exception {
        HttpURLConnection c = connection(baseUrl, null, "GET",
                "/api/evault/v1/pair/status/" + URLEncoder.encode(sessionId, "UTF-8"));
        JSONObject root = new JSONObject(new String(request(c, null, null), StandardCharsets.UTF_8));
        return new PairStatus(
                root.optString("status", ""),
                root.optString("device_token", ""),
                root.optString("telegram_username", "")
        );
    }

    public static AccountIdentity accountMe(String baseUrl, String deviceToken) throws Exception {
        HttpURLConnection c = connection(baseUrl, deviceToken, "GET", "/api/evault/v1/account/me");
        JSONObject root = new JSONObject(new String(request(c, null, null), StandardCharsets.UTF_8));
        JSONObject account = root.optJSONObject("account");
        if (account == null) throw new Exception("Cloud account identity missing");
        return new AccountIdentity(
                account.optString("id", ""),
                account.optString("display_name", account.optString("name", "")),
                account.optString("telegram_username", ""),
                account.optString("token_prefix", "")
        );
    }

    public static void registerDevice(String baseUrl, String deviceToken, String deviceId, String name, String model, String version) throws Exception {
        HttpURLConnection c = connection(baseUrl, deviceToken, "POST", "/api/evault/v1/devices/register");
        JSONObject body = new JSONObject();
        body.put("device_id", deviceId);
        body.put("name", name);
        body.put("model", model);
        body.put("app_version", version);
        request(c, body.toString().getBytes(StandardCharsets.UTF_8), "application/json; charset=utf-8");
    }

    public static List<CloudFile> library(String baseUrl, String deviceToken) throws Exception {
        HttpURLConnection c = connection(baseUrl, deviceToken, "GET", "/api/evault/v1/library");
        JSONObject root = new JSONObject(new String(request(c, null, null), StandardCharsets.UTF_8));
        JSONArray files = root.optJSONArray("files");
        List<CloudFile> result = new ArrayList<>();
        if (files != null) {
            for (int i = 0; i < files.length(); i++) {
                JSONObject f = files.getJSONObject(i);
                result.add(new CloudFile(f.optString("category"), f.optString("account_name")));
            }
        }
        return result;
    }

    public static void uploadAuth(String baseUrl, String deviceToken, String deviceId, String category, String account, byte[] auth) throws Exception {
        HttpURLConnection c = connection(baseUrl, deviceToken, "POST", "/api/evault/v1/auth/upload");
        c.setDoOutput(true);
        c.setFixedLengthStreamingMode(auth.length);
        c.setRequestProperty("Content-Type", "application/octet-stream");
        c.setRequestProperty("X-Device-ID", deviceId);
        c.setRequestProperty("X-eVault-Category", category);
        c.setRequestProperty("X-eVault-Account", account);
        request(c, auth, "application/octet-stream");
    }

    public static void logActivity(String baseUrl, String deviceToken, String deviceId, String level, String message) throws Exception {
        HttpURLConnection c = connection(baseUrl, deviceToken, "POST", "/api/evault/v1/activity");
        JSONObject body = new JSONObject();
        body.put("level", level == null ? "INFO" : level);
        body.put("message", message);
        if (deviceId != null && !deviceId.trim().isEmpty()) c.setRequestProperty("X-Device-ID", deviceId);
        request(c, body.toString().getBytes(StandardCharsets.UTF_8), "application/json; charset=utf-8");
    }

    public static byte[] downloadAuth(String baseUrl, String deviceToken, String category, String account) throws Exception {
        String path = "/api/evault/v1/auth/" + URLEncoder.encode(category, "UTF-8").replace("+", "%20") + "/"
                + URLEncoder.encode(account, "UTF-8").replace("+", "%20") + "/download";
        HttpURLConnection c = connection(baseUrl, deviceToken, "GET", path);
        return request(c, null, null);
    }
}
