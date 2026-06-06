package net.diprosalik.mcmistral.mistral;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "mcmistral")
public class MistralConfig implements ConfigData {

    @ConfigEntry.Gui.Tooltip
    public String apiKey = "";

    public String modelName = "mistral-small-latest";

    @ConfigEntry.Gui.Tooltip
    public boolean enableWelcomeGreeting = true;
}