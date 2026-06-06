package net.diprosalik.mcmistral;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.diprosalik.mcmistral.mistral.MistralClient;
import net.diprosalik.mcmistral.mistral.MistralCommand;
import net.diprosalik.mcmistral.mistral.MistralConfig;
import net.diprosalik.mcmistral.mistral.MistralJoinHandler;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McMistral implements ModInitializer {
	public static final String MOD_ID = "mcmistral";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		AutoConfig.register(MistralConfig.class, GsonConfigSerializer::new);
		MistralCommand.register();
		MistralJoinHandler.register();
	}
}