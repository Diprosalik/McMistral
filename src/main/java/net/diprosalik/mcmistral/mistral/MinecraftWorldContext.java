package net.diprosalik.mcmistral.mistral;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MinecraftWorldContext {

    public static String buildContext(ServerCommandSource source) {
        StringBuilder context = new StringBuilder();

        var server = source.getServer();
        var world = source.getWorld();
        long rawTime = world.getTimeOfDay();
        long dailyTime = rawTime % 24000;
        long daysPlayed = rawTime / 24000;

        context.append("=== MINECRAFT WORLD CONTEXT ===\n");
        context.append("- Game Version: ").append(server.getVersion()).append("\n");
        context.append("- Dimension: ").append(world.getRegistryKey().getValue().toString()).append("\n");
        context.append("- Days Passed in World: ").append(daysPlayed).append("\n");
        context.append("- Time of Day: ").append(dailyTime).append(" ticks (0=6:00 AM sunrise, 6000=12:00 PM midday, 9000=3:00 PM sunset starts, 12000=6:00 PM dusk/darksunset, 13000=night start, 18000=12:00 AM midnight)\n");
        context.append("- Weather: ").append(world.isThundering() ? "Thunderstorm" : (world.isRaining() ? "Raining/Snowing" : "Clear/Sunny")).append("\n");
        context.append("- Difficulty: ").append(world.getDifficulty().getName()).append("\n");

        if (source.getEntity() instanceof ServerPlayerEntity player) {
            Vec3d pos = player.getPos();
            BlockPos blockPos = player.getBlockPos();

            context.append("\n=== PLAYER STATUS ===\n");
            context.append("- Name: ").append(player.getName().getString()).append("\n");
            context.append("- Position: X: ").append(String.format("%.1f", pos.x))
                    .append(", Y: ").append(String.format("%.1f", pos.y))
                    .append(", Z: ").append(String.format("%.1f", pos.z)).append("\n");
            context.append("- Facing Direction: ").append(player.getHorizontalFacing().getName().toUpperCase()).append("\n");
            context.append("- Gamemode: ").append(player.interactionManager.getGameMode().getName()).append("\n");
            context.append("- Is on Ground: ").append(player.isOnGround()).append("\n");
            context.append("- Is Swimming: ").append(player.isSwimming()).append("\n");
            context.append("- Is Sneaking: ").append(player.isSneaking()).append("\n");

            context.append("- Health: ").append(String.format("%.1f", player.getHealth())).append("/").append(player.getMaxHealth()).append("\n");
            context.append("- Food Level: ").append(player.getHungerManager().getFoodLevel()).append("/20 (Saturation: ")
                    .append(String.format("%.1f", player.getHungerManager().getSaturationLevel())).append(")\n");
            context.append("- Experience: Level ").append(player.experienceLevel).append(" (Progress: ")
                    .append(String.format("%.1f", player.experienceProgress * 100)).append("%)\n");

            var biomeKey = world.getRegistryManager().get(RegistryKeys.BIOME).getId(world.getBiome(blockPos).value());
            context.append("- Current Biome: ").append(biomeKey != null ? biomeKey.toString() : "Unknown").append("\n");

            if (!player.getStatusEffects().isEmpty()) {
                String effects = player.getStatusEffects().stream()
                        .map(effect -> {
                            Identifier effectId = Registries.STATUS_EFFECT.getId(effect.getEffectType().value());
                            String name = (effectId != null) ? effectId.toString() : "unknown";
                            return name + " (Amp: " + effect.getAmplifier() + ", Duration: " + (effect.getDuration() / 20) + "s)";
                        })
                        .collect(Collectors.joining(", "));
                context.append("- Active Status Effects: ").append(effects).append("\n");
            } else {
                context.append("- Active Status Effects: None\n");
            }

            context.append("- Main Hand: ").append(Registries.ITEM.getId(player.getMainHandStack().getItem()).toString())
                    .append(" (Count: ").append(player.getMainHandStack().getCount()).append(")\n");
            context.append("- Off Hand: ").append(Registries.ITEM.getId(player.getOffHandStack().getItem()).toString()).append("\n");

            context.append("- Armor: [Helmet: ").append(Registries.ITEM.getId(player.getInventory().getArmorStack(3).getItem()).toString())
                    .append(", Chestplate: ").append(Registries.ITEM.getId(player.getInventory().getArmorStack(2).getItem()).toString())
                    .append(", Leggings: ").append(Registries.ITEM.getId(player.getInventory().getArmorStack(1).getItem()).toString())
                    .append(", Boots: ").append(Registries.ITEM.getId(player.getInventory().getArmorStack(0).getItem()).toString()).append("]\n");

            context.append("\n=== FULL PLAYER INVENTORY ===\n");
            var inventory = player.getInventory();
            boolean hasItems = false;
            for (int i = 0; i < inventory.size(); i++) {
                if (i >= 36) continue;
                ItemStack stack = inventory.getStack(i);
                if (!stack.isEmpty()) {
                    hasItems = true;
                    Identifier itemId = Registries.ITEM.getId(stack.getItem());
                    context.append("- Slot ").append(i).append(": ").append(itemId.toString()).append(" (Count: ").append(stack.getCount()).append(")");

                    if (stack.isDamageable()) {
                        context.append(" [Durability: ").append(stack.getMaxDamage() - stack.getDamage()).append("/").append(stack.getMaxDamage()).append("]");
                    }
                    context.append("\n");
                }
            }
            if (!hasItems) {
                context.append("- Inventory is completely empty.\n");
            }

            context.append("\n=== CRAFTING KNOWLEDGE ===\n");
            context.append("- Available Crafting Recipes: ");
            var recipeManager = world.getRecipeManager();
            var craftingRecipes = recipeManager.listAllOfType(RecipeType.CRAFTING);
            List<String> possibleCrafts = new ArrayList<>();

            for (RecipeEntry<?> recipeEntry : craftingRecipes) {
                var recipe = recipeEntry.value();
                ItemStack resultStack = recipe.getResult(world.getRegistryManager());

                if (!resultStack.isEmpty()) {
                    Identifier resultId = Registries.ITEM.getId(resultStack.getItem());
                    if (!resultId.equals(Registries.ITEM.getId(net.minecraft.item.Items.AIR))) {
                        String resultString = resultId.toString();
                        if (!possibleCrafts.contains(resultString)) {
                            if (possibleCrafts.size() < 25) {
                                possibleCrafts.add(resultString);
                            } else {
                                break;
                            }
                        }
                    }
                }
            }
            context.append(String.join(", ", possibleCrafts)).append("\n");

            context.append("\n=== IMMEDIATE ENVIRONMENT ===\n");
            context.append("- Block at Feet: ").append(Registries.BLOCK.getId(world.getBlockState(blockPos).getBlock()).toString()).append("\n");
            context.append("- Block below Feet: ").append(Registries.BLOCK.getId(world.getBlockState(blockPos.down()).getBlock()).toString()).append("\n");
            context.append("- Block above Head: ").append(Registries.BLOCK.getId(world.getBlockState(blockPos.up(2)).getBlock()).toString()).append("\n");
            context.append("- Can see Sky: ").append(world.isSkyVisible(blockPos)).append("\n");
        } else {
            context.append("\n- Executed by Console or Non-Player Entity.\n");
        }

        context.append("===============================\n");
        return context.toString();
    }
}