package net.diprosalik.mcmistral.mistral;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
                return "Error: No API key set! Set it in the Cloth Config screen.";
            }

            try {
                String dynamicGameContext = MinecraftWorldContext.buildContext(source);
                HttpClient client = HttpClient.newHttpClient();

                String systemPrompt = "You are a Minecraft assistant with server administration rights. "
                        + "You can request the execution of an in-game command by formatting your response in a special way.\n\n"
                        + dynamicGameContext + "\n\n"
                        + "CRITICAL RULES:\n"
                        + "1. STRICT RULE: You are ONLY allowed to execute a command if the player EXPLICITLY asks you to perform an action (e.g., 'teleport me', 'make it day', 'kill the sheep', 'give me a diamond').\n"
                        + "2. If the player ONLY asks a question or asks for information (e.g., 'How do I craft X?', 'What is my health?', 'Where am I?'), you MUST NOT execute any commands! Just answer with text.\n"
                        + "3. If an action is requested, include the exact command enclosed in [COMMAND:...] at the VERY END of your response. Examples: [COMMAND:time set day] or [COMMAND:give @s minecraft:crafting_table 1].\n"
                        + "4. Your main text response must remain RAW TEXT ONLY without markdown.\n"
                        + "5. Keep your explanation very short (1-2 sentences) and always in English.";

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
                    String rawResponse = parseResponse(response.body());

                    return handleCommandExecution(rawResponse, source);
                } else {
                    return "Error from Mistral API (Status " + response.statusCode() + "): " + response.body();
                }

            } catch (Exception e) {
                return "Error connecting to Mistral: " + e.getMessage();
            }
        });
    }

    private static String handleCommandExecution(String responseText, ServerCommandSource source) {
        if (responseText.contains("[COMMAND:")) {
            int start = responseText.indexOf("[COMMAND:");
            int end = responseText.indexOf("]", start);

            if (end != -1) {
                String command = responseText.substring(start + 9, end).trim();
                String cleanText = responseText.substring(0, start).trim();

                source.getServer().execute(() -> {
                    try {
                        ServerCommandSource adminPlayerSource = source
                                .withLevel(4)
                                .withSilent();

                        source.getServer().getCommandManager().executeWithPrefix(adminPlayerSource, command);

                        source.sendFeedback(() -> Text.literal("[Mistral executed: /" + command + "]").formatted(Formatting.GREEN), false);
                    } catch (Exception e) {
                        source.sendError(Text.literal("Failed to execute command: " + e.getMessage()));
                    }
                });

                return cleanText;
            }
        }
        return responseText;
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

                    result = result
                            .replace("\\n", "\n")
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\");

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