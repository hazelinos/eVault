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

    public static final class CloudFile {
        public final String category;
        public final String account;
        public CloudFile(String category, String account) { this.category = category; this.account = account; }
    }

    private static String base(String baseUrl) {
        String s = baseUrl == null ? "" : baseUrl.trim();
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private static HttpURLConnection connection(String baseUrl, String apiKey, String method, String path) throws Exception {
        URL url = new URL(base(baseUrl) + path);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("X-eVault-API-Key", apiKey == null ? "" : apiKey);
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
        if (code < 200 || code >= 300) throw new Exception("Cloud HTTP " + code + ": " + new String(data, StandardCharsets.UTF_8));
        return data;
    }

    public static void registerDevice(String baseUrl, String apiKey, String deviceId, String name, String model, String version) throws Exception {
        HttpURLConnection c = connection(baseUrl, apiKey, "POST", "/api/evault/v1/devices/register");
        JSONObject body = new JSONObject();
        body.put("device_id", deviceId); body.put("name", name); body.put("model", model); body.put("app_version", version);
        request(c, body.toString().getBytes(StandardCharsets.UTF_8), "application/json; charset=utf-8");
    }

    public static List<CloudFile> library(String baseUrl, String apiKey) throws Exception {
        HttpURLConnection c = connection(baseUrl, apiKey, "GET", "/api/evault/v1/library");
        JSONObject root = new JSONObject(new String(request(c, null, null), StandardCharsets.UTF_8));
        JSONArray files = root.optJSONArray("files");
        List<CloudFile> result = new ArrayList<>();
        if (files != null) for (int i = 0; i < files.length(); i++) {
            JSONObject f = files.getJSONObject(i);
            result.add(new CloudFile(f.optString("category"), f.optString("account_name")));
        }
        return result;
    }

    public static void uploadAuth(String baseUrl, String apiKey, String deviceId, String category, String account, byte[] auth) throws Exception {
        String path = "/api/evault/v1/auth/upload";
        HttpURLConnection c = connection(baseUrl, apiKey, "POST", path);
        c.setDoOutput(true);
        c.setFixedLengthStreamingMode(auth.length);
        c.setRequestProperty("Content-Type", "application/octet-stream");
        c.setRequestProperty("X-Device-ID", deviceId);
        c.setRequestProperty("X-eVault-Category", category);
        c.setRequestProperty("X-eVault-Account", account);
        request(c, auth, "application/octet-stream");
    }

    public static byte[] downloadAuth(String baseUrl, String apiKey, String category, String account) throws Exception {
        String path = "/api/evault/v1/auth/" + URLEncoder.encode(category, "UTF-8").replace("+", "%20") + "/"
                + URLEncoder.encode(account, "UTF-8").replace("+", "%20") + "/download";
        HttpURLConnection c = connection(baseUrl, apiKey, "GET", path);
        return request(c, null, null);
    }
}