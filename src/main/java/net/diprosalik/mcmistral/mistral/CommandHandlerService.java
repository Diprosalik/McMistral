package net.diprosalik.mcmistral.mistral;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class CommandHandlerService {

    public static String processResponseCommands(String responseText, ServerCommandSource source) {
        if (!responseText.contains("[COMMAND:")) {
            return responseText;
        }

        int start = responseText.indexOf("[COMMAND:");
        int end = responseText.indexOf("]", start);

        if (end == -1) {
            return responseText;
        }

        String command = responseText.substring(start + 9, end).trim();
        String cleanText = responseText.substring(0, start).trim();

        boolean isProtectedCommand = command.startsWith("teleport") || command.startsWith("tp")
                || command.startsWith("gamemode") || command.startsWith("give");

        if (isProtectedCommand && !source.hasPermissionLevel(2)) {
            source.sendError(Text.literal("Mistral tried to execute an admin command, but you lack Permission Level 2 (OP)!"));
            return cleanText;
        }

        source.getServer().execute(() -> {
            try {
                ServerCommandSource adminPlayerSource = source.withLevel(4).withSilent();
                source.getServer().getCommandManager().executeWithPrefix(adminPlayerSource, command);
                source.sendFeedback(() -> Text.literal("[Mistral executed: /" + command + "]").formatted(Formatting.GREEN), false);
            } catch (Exception e) {
                source.sendError(Text.literal("Failed to execute command: " + e.getMessage()));
            }
        });

        return cleanText;
    }
}