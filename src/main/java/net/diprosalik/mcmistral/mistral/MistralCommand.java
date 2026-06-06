package net.diprosalik.mcmistral.mistral;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class MistralCommand {
    private static String currentKeyForStatus = "";

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("mistral")
                    .then(CommandManager.literal("apikey")
                            .then(CommandManager.argument("key", StringArgumentType.string())
                                    .executes(context -> {
                                        String key = StringArgumentType.getString(context, "key");
                                        MistralClient.setApiKey(key);
                                        currentKeyForStatus = key;

                                        if ("testapikey".equals(key)) {
                                            context.getSource().sendFeedback(() ->
                                                    Text.literal("Simulation mode activated!").formatted(Formatting.YELLOW), false);
                                        } else {
                                            context.getSource().sendFeedback(() ->
                                                    Text.literal("API key successfully set!").formatted(Formatting.GREEN), false);
                                        }
                                        return 1;
                                    })
                            )
                    )
                    .then(CommandManager.literal("ask")
                            .then(CommandManager.argument("prompt", StringArgumentType.greedyString())
                                    .executes(context -> {
                                        ServerCommandSource source = context.getSource();
                                        String prompt = StringArgumentType.getString(context, "prompt");

                                        if (!MistralClient.hasApiKey()) {
                                            source.sendError(Text.literal("Please set an API key first using /mistral apikey <key>"));
                                            return 0;
                                        }

                                        source.sendFeedback(() -> Text.literal("Mistral is thinking...").formatted(Formatting.GRAY), false);

                                        MistralClient.queryMistral(prompt, source).thenAccept(response -> {
                                            source.sendFeedback(() ->
                                                    Text.literal("[Mistral]: ").formatted(Formatting.DARK_PURPLE)
                                                            .append(Text.literal(response).formatted(Formatting.WHITE)), false);
                                        });

                                        return 1;
                                    })
                            )
                    )
                    .then(CommandManager.literal("status")
                            .executes(context -> {
                                if (!MistralClient.hasApiKey()) {
                                    context.getSource().sendFeedback(() -> Text.literal("Status: No API key set.").formatted(Formatting.RED), false);
                                } else if ("testapikey".equals(currentKeyForStatus)) {
                                    context.getSource().sendFeedback(() -> Text.literal("Status: Simulation Mode Active.").formatted(Formatting.YELLOW), false);
                                } else {
                                    context.getSource().sendFeedback(() -> Text.literal("Status: Connected (Ready to use).").formatted(Formatting.GREEN), false);
                                }
                                return 1;
                            })
                    )
                    .then(CommandManager.literal("clear")
                            .executes(context -> {
                                MistralClient.setApiKey("");
                                currentKeyForStatus = "";
                                context.getSource().sendFeedback(() -> Text.literal("API key has been cleared from memory.").formatted(Formatting.RED), false);
                                return 1;
                            })
                    )
                    .then(CommandManager.literal("help")
                            .executes(context -> {
                                context.getSource().sendFeedback(() -> Text.literal("=== Mistral Mod Commands ===").formatted(Formatting.GOLD), false);
                                context.getSource().sendFeedback(() -> Text.literal("/mistral apikey <key> - Set your token (or use 'testapikey')").formatted(Formatting.GRAY), false);
                                context.getSource().sendFeedback(() -> Text.literal("/mistral ask <prompt> - Ask the AI chatbot a question").formatted(Formatting.GRAY), false);
                                context.getSource().sendFeedback(() -> Text.literal("/mistral status        - Check connection status").formatted(Formatting.GRAY), false);
                                context.getSource().sendFeedback(() -> Text.literal("/mistral clear         - Remove the current API key").formatted(Formatting.GRAY), false);
                                return 1;
                            })
                    )
            );
        });
    }
}