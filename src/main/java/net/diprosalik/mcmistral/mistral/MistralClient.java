package net.diprosalik.mcmistral.mistral;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class MistralClient {

    public static boolean hasApiKey() {
        MistralConfig config = AutoConfig.getConfigHolder(MistralConfig.class).getConfig();
        return config.apiKey != null && !config.apiKey.trim().isEmpty();
    }

    public static CompletableFuture<String> queryMistral(String prompt, ServerCommandSource source) {
        return CompletableFuture.supplyAsync(() -> {
            MistralConfig config = AutoConfig.getConfigHolder(MistralConfig.class).getConfig();
            String apiKey = config.apiKey;

            if (!hasApiKey()) {
                return "Error: No API key set! Set it in the Mod Menu / Cloth Config screen.";
            }

            if ("testapikey".equals(apiKey)) {
                try {
                    Thread.sleep(2000);
                    return "Simulation Mode Active! Your code works perfectly. You asked: \"" + prompt + "\"";
                } catch (InterruptedException e) {
                    return "Error during simulation.";
                }
            }

            try {
                String mcVersion = source.getServer().getVersion();
                long worldTime = source.getWorld().getTimeOfDay() % 24000;
                boolean isRaining = source.getWorld().isRaining();

                String playerStatus = "Unknown";
                String coordinates = "Unknown";
                if (source.getEntity() instanceof ServerPlayerEntity player) {
                    playerStatus = String.format("Name: %s, Health: %.1f/20, Hunger: %d/20, Level: %d",
                            player.getName().getString(),
                            player.getHealth(),
                            player.getHungerManager().getFoodLevel(),
                            player.experienceLevel);

                    Vec3d pos = player.getPos();
                    coordinates = String.format("X: %.1f, Y: %.1f, Z: %.1f", pos.x, pos.y, pos.z);
                }

                HttpClient client = HttpClient.newHttpClient();

                String systemPrompt = "You are an omniscient Minecraft expert and a helpful in-game chatbot. "
                        + "CURRENT GAME STATUS:\n"
                        + "- Minecraft Version: " + mcVersion + "\n"
                        + "- Ingame Time: " + worldTime + " ticks (0=morning, 6000=noon, 12000=sunset, 18000=midnight)\n"
                        + "- Weather: " + (isRaining ? "Raining/Snowing" : "Sunny/Clear") + "\n"
                        + "- Player Status: " + playerStatus + "\n"
                        + "- Player Coordinates: " + coordinates + "\n\n"
                        + "CRITICAL: Always reply in RAW TEXT ONLY. Do not use markdown like asterisks (**), hashtags, bullet points, or any other formatting. "
                        + "Always reply in English. Use the game status and coordinates only if it's relevant to the player's question. "
                        + "Keep your answers precise, helpful, and short (maximum 2-3 sentences) to fit perfectly into the Minecraft chat.";

                String jsonBody = "{"
                        + "\"model\": \"" + config.modelName + "\","
                        + "\"messages\": ["
                        + "  {\"role\": \"system\", \"content\": \"" + systemPrompt.replace("\"", "\\\"").replace("\n", "\\n") + "\"},"
                        + "  {\"role\": \"user\", \"content\": \"" + prompt.replace("\"", "\\\"") + "\"}"
                        + "],"
                        + "\"max_tokens\": 150"
                        + "}";

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.mistral.ai/v1/chat/completions"))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return parseResponse(response.body());
                } else {
                    return "Error from Mistral API (Status " + response.statusCode() + "): " + response.body();
                }

            } catch (Exception e) {
                return "Error connecting to Mistral: " + e.getMessage();
            }
        });
    }

    private static String parseResponse(String responseBody) {
        try {
            String target = "\"content\":\"";
            int textIndex = responseBody.indexOf(target);

            if (textIndex == -1) {
                target = "\"content\": \"";
                textIndex = responseBody.indexOf(target);
            }

            if (textIndex != -1) {
                int start = textIndex + target.length();
                int end = start;

                while (end < responseBody.length()) {
                    end = responseBody.indexOf("\"", end);
                    if (end == -1) break;

                    if (responseBody.charAt(end - 1) != '\\') {
                        break;
                    }
                    end++;
                }

                if (end != -1) {
                    String result = responseBody.substring(start, end);

                    // JSON-Zeichen bereinigen
                    result = result
                            .replace("\\n", "\n")
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\");

                    // 3. Letzte Instanz: Filtert alle Markdown-Überbleibsel (Sterne, Rauten) für Raw Text heraus
                    return result
                            .replace("**", "")
                            .replace("*", "")
                            .replace("`", "")
                            .replace("#", "");
                }
            }
        } catch (Exception e) {
            return "Error reading response: " + e.getMessage();
        }
        return "Unexpected response structure. Raw API output: " + responseBody;
    }
}