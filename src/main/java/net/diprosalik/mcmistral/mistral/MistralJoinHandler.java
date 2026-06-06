package net.diprosalik.mcmistral.mistral;

import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class MistralJoinHandler {

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            MistralConfig config = AutoConfig.getConfigHolder(MistralConfig.class).getConfig();

            if (!config.enableWelcomeGreeting) {
                return;
            }

            ServerPlayerEntity player = handler.getPlayer();
            var source = player.getCommandSource();

            int gamesLeft = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.LEAVE_GAME));

            String prompt;
            if (gamesLeft == 0) {
                prompt = "The player " + player.getName().getString() + " just joined this world for the VERY FIRST TIME. Introduce yourself briefly as their all-knowing AI companion, greet them warmly, and tell them you are ready to help them survive.";
            } else {
                prompt = "The player " + player.getName().getString() + " just rejoined their existing world. Give them a very short, welcoming one-sentence welcome back greeting.";
            }

            MistralClient.queryMistral(prompt, source).thenAccept(response -> {
                player.sendMessage(Text.literal("[Mistral]: ").formatted(Formatting.GOLD).append(Text.literal(response).formatted(Formatting.WHITE)), false);
            });
        });
    }
}