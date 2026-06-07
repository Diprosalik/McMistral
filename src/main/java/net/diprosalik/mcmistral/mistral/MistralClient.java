package net.diprosalik.mcmistral.mistral;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.server.command.ServerCommandSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class MistralClient {

    private static final String API_URL = "https://api.mistral.ai/v1/chat/completions";

    public static boolean hasApiKey() {
        MistralConfig config = AutoConfig.getConfigHolder(MistralConfig.class).getConfig();
        return config.apiKey != null && !config.apiKey.trim().isEmpty();
    }

    public static CompletableFuture<String> queryMistral(String prompt, ServerCommandSource source) {
        return CompletableFuture.supplyAsync(() -> {
            MistralConfig config = AutoConfig.getConfigHolder(MistralConfig.class).getConfig();

            if (!hasApiKey()) {
                return "Error: No API key set! Set it in the Cloth Config screen.";
            }

            try {
                String dynamicGameContext = MinecraftWorldContext.buildContext(source);
                String systemPrompt = PromptBuilder.buildSystemPrompt(dynamicGameContext);
                String jsonBody = PromptBuilder.buildJsonPayload(config.modelName, systemPrompt, prompt);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .header("Authorization", "Bearer " + config.apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String parsedContent = ApiResponseParser.parseContent(response.body());
                    return CommandHandlerService.processResponseCommands(parsedContent, source);
                }

                return "Error from Mistral API (Status " + response.statusCode() + "): " + response.body();

            } catch (Exception e) {
                return "Error connecting to Mistral: " + e.getMessage();
            }
        });
    }
}