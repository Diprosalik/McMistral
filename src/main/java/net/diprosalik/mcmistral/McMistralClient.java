package net.diprosalik.mcmistral;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.Identifier;

public class McMistralClient implements ClientModInitializer {
    private static KeyBinding askKeyBinding;

    @Override
    public void onInitializeClient() {
        askKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mcmistral.ask",
                InputUtil.Type.KEYSYM,
                InputUtil.GLFW_KEY_M,
                new KeyBinding.Category(Identifier.of("key.mcmistral.category"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (askKeyBinding.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new ChatScreen("/mistral ask ", false));
                }
            }
        });
    }
}