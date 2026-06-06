package net.diprosalik.mcmistral;

import net.diprosalik.mcmistral.mistral.MistralClient;
import net.diprosalik.mcmistral.mistral.MistralCommand;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McMistral implements ModInitializer {
	public static final String MOD_ID = "mcmistral";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		MistralCommand.register();
	}
}