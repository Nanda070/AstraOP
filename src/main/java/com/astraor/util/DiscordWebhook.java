package com.astraor.util;

import com.astraor.AstraORPlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class DiscordWebhook {

    private final AstraORPlugin plugin;

    public DiscordWebhook(AstraORPlugin plugin) {
        this.plugin = plugin;
    }

    public void logClassSelection(String playerName, String className) {
        String description = "**" + playerName + "** has chosen the **" + className + "** class.";
        sendWebhook("Initial Class Selection", description, 0x00FF00); // Green color
    }

    public void logClassChange(String playerName, String oldClass, String newClass) {
        String description = "**" + playerName + "** changed their class from **" + oldClass + "** to **" + newClass + "**.";
        sendWebhook("Endgame Class Change", description, 0xFFA500); // Orange color
    }

    private void sendWebhook(String title, String description, int colorHex) {
        String webhookUrl = plugin.getSettings().getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("your-webhook-id")) {
            return; // Webhook not configured
        }

        CompletableFuture.runAsync(() -> {
            try {
                URL url = java.net.URI.create(webhookUrl).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                // Build simple JSON structure manually to avoid external dependencies like Gson if possible,
                // but Paper includes Gson so we could use it. Here we just use a safe string format.
                String timestamp = Instant.now().toString();
                
                String jsonPayload = "{"
                        + "\"silent\": true,"
                        + "\"embeds\": [{"
                        + "\"title\": \"" + escapeJson(title) + "\","
                        + "\"description\": \"" + escapeJson(description) + "\","
                        + "\"color\": " + colorHex + ","
                        + "\"timestamp\": \"" + timestamp + "\""
                        + "}]"
                        + "}";

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    plugin.getLogger().warning("Discord webhook responded with code: " + responseCode);
                }

                connection.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send Discord webhook: " + e.getMessage());
            }
        });
    }

    private String escapeJson(String text) {
        return text.replace("\"", "\\\"").replace("\n", "\\n");
    }
}
