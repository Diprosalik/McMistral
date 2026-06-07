package net.diprosalik.mcmistral.mistral;

public class PromptBuilder {

    public static String buildSystemPrompt(String dynamicGameContext) {
        return "You are a Minecraft assistant with server administration rights. "
                + "You can request the execution of an in-game command by formatting your response in a special way.\n\n"
                + dynamicGameContext + "\n\n"
                + "CRITICAL RULES:\n"
                + "1. STRICT RULE: You are ONLY allowed to execute a command if the player EXPLICITLY asks you to perform an action (e.g., 'teleport me', 'make it day', 'kill the sheep', 'give me a diamond').\n"
                + "2. If the player ONLY asks a question or asks for information (e.g., 'How do I craft X?', 'What is my health?', 'Where am I?'), you MUST NOT execute any commands! Just answer with text.\n"
                + "3. If an action is requested, include the exact command enclosed in [COMMAND:...] at the VERY END of your response. Examples: [COMMAND:time set day] or [COMMAND:give @s minecraft:crafting_table 1].\n"
                + "4. Your main text response must remain RAW TEXT ONLY without markdown.\n"
                + "5. Keep your explanation very short (1-2 sentences) and always in English.";
    }

    public static String buildJsonPayload(String modelName, String systemPrompt, String userPrompt) {
        return "{"
                + "\"model\": \"" + modelName + "\","
                + "\"messages\": ["
                + "  {\"role\": \"system\", \"content\": \"" + escapeJson(systemPrompt) + "\"},"
                + "  {\"role\": \"user\", \"content\": \"" + escapeJson(userPrompt) + "\"}"
                + "],"
                + "\"max_tokens\": 150"
                + "}";
    }

    private static String escapeJson(String input) {
        return input.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}