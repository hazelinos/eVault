package id.feezet.efauth;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class CloudApi {
    private static final String BASE_URL = "https://airdrop-hunter-production-c028.up.railway.app";
    private CloudApi() {}

    public static String uploadAuth(String token, String deviceId, String category, String account, byte[] data) throws IOException {
        HttpURLConnection c = open("/api/evault/v1/auth/upload", "POST", token, deviceId);
        c.setRequestProperty("X-eVault-Category", category);
        c.setRequestProperty("X-eVault-Account", account);
        c.setRequestProperty("Content-Type", "application/octet-stream");
        c.setFixedLengthStreamingMode(data.length);
        try (OutputStream out = c.getOutputStream()) { out.write(data); }
        return readResponse(c);
    }

    public static String library(String token, String deviceId) throws IOException {
        return readResponse(open("/api/evault/v1/library", "GET", token, deviceId));
    }

    public static String account(String token, String deviceId) throws IOException {
        return readResponse(open("/api/evault/v1/account/me", "GET", token, deviceId));
    }

    private static HttpURLConnection open(String path, String method, String token, String deviceId) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(BASE_URL + path).openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(15000);
        c.setReadTimeout(60000);
        c.setUseCaches(false);
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("X-eVault-Device-Token", token);
        c.setRequestProperty("X-Device-ID", deviceId == null ? "android" : deviceId);
        if ("POST".equals(method)) c.setDoOutput(true);
        return c;
    }

    private static String readResponse(HttpURLConnection c) throws IOException {
        int code = c.getResponseCode();
        InputStream in = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
        StringBuilder body = new StringBuilder();
        if (in != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) body.append(line);
            }
        }
        if (code < 200 || code >= 300) throw new IOException("HTTP " + code + (body.length() == 0 ? "" : ": " + body));
        return body.toString();
    }
}
