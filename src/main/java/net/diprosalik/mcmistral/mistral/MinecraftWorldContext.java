package net.diprosalik.mcmistral.mistral;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MinecraftWorldContext {

    public static String buildContext(ServerCommandSource source) {
        StringBuilder context = new StringBuilder();
        var server = source.getServer();
        var world = source.getWorld();

        long dailyTime = world.getTimeOfDay() % 24000;
        if (dailyTime < 0) dailyTime += 24000;
        long daysPlayed = world.getTimeOfDay() / 24000;

        String worldPhase = evaluateWorldPhase(dailyTime);
        double mspt = server.getAverageTickTime();
        double tps = Math.min(20.0, 1000.0 / mspt);

        context.append("=== MINECRAFT WORLD CONTEXT (DEBUG/F3 ACTIVE) ===\n");
        context.append("- Game Version: ").append(server.getVersion()).append("\n");
        context.append("- Server Performance: ").append(String.format("%.1f", tps)).append(" TPS / ").append(String.format("%.1f", mspt)).append(" MSPT\n");
        context.append("- Dimension: ").append(world.getRegistryKey().getValue().toString()).append("\n");
        context.append("- Days Passed in World: ").append(daysPlayed).append("\n");
        context.append("- Time Ticks: ").append(dailyTime).append(" / 24000\n");
        context.append("- World Phase: ").append(worldPhase).append("\n");
        context.append("- Weather: ").append(world.isThundering() ? "Thunderstorm" : (world.isRaining() ? "Raining/Snowing" : "Clear/Sunny")).append("\n");
        context.append("- Difficulty: ").append(world.getDifficulty().getName()).append("\n");
        context.append(" -Seed: ").append(world.getSeed()).append("\n");

        if (source.getEntity() instanceof ServerPlayerEntity player) {
            Vec3d pos = player.getPos();
            BlockPos blockPos = player.getBlockPos();
            ChunkPos chunkPos = new ChunkPos(blockPos);

            int skyLight = world.getLightLevel(LightType.SKY, blockPos);
            int blockLight = world.getLightLevel(LightType.BLOCK, blockPos);
            int totalLight = world.getLightLevel(blockPos);
            int seaLevel = world.getSeaLevel();
            boolean isInCave = skyLight == 0 && blockPos.getY() < seaLevel && !world.getDimensionEntry().value().hasCeiling();

            context.append("\n=== F3 DEBUG NAVIGATION & LOCATION ===\n");
            context.append(String.format("- XYZ Coordinates: X: %.3f, Y: %.5f, Z: %.3f\n", pos.x, pos.y, pos.z));
            context.append(String.format("- Block Pos: [%d, %d, %d]\n", blockPos.getX(), blockPos.getY(), blockPos.getZ()));
            context.append(String.format("- Chunk Pos: [%d, %d] (In Chunk Local: X: %d, Y: %d, Z: %d)\n", chunkPos.x, chunkPos.z, blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15));
            context.append("- Facing Direction: ").append(player.getHorizontalFacing().getName().toUpperCase()).append(" (Yaw: ").append(String.format("%.1f", player.getYaw())).append(" / Pitch: ").append(String.format("%.1f", player.getPitch())).append(")\n");
            context.append(String.format("- F3 Light Level: %d (Sky: %d, Block: %d)\n", totalLight, skyLight, blockLight));
            context.append("- Is in Cave/Underground: ").append(isInCave).append("\n");
            context.append("- Sea Level Reference: ").append(seaLevel).append("\n");

            context.append("\n=== PLAYER STATUS ===\n");
            context.append("- Name: ").append(player.getName().getString()).append("\n");
            context.append("- Has permission Level 2: ").append(source.hasPermissionLevel(2)).append("\n");
            context.append("- Gamemode: ").append(player.interactionManager.getGameMode().getName()).append("\n");
            context.append("- Is on Ground: ").append(player.isOnGround()).append("\n");
            context.append("- Is Swimming: ").append(player.isSwimming()).append("\n");
            context.append("- Is Sneaking: ").append(player.isSneaking()).append("\n");
            context.append("- Health: ").append(String.format("%.1f", player.getHealth())).append("/").append(player.getMaxHealth()).append("\n");
            context.append("- Food Level: ").append(player.getHungerManager().getFoodLevel()).append("/20 (Saturation: ").append(String.format("%.1f", player.getHungerManager().getSaturationLevel())).append(")\n");
            context.append("- Experience: Level ").append(player.experienceLevel).append(" (Progress: ").append(String.format("%.1f", player.experienceProgress * 100)).append("%)\n");

            var biomeKey = world.getRegistryManager().get(RegistryKeys.BIOME).getId(world.getBiome(blockPos).value());
            context.append("- Current Biome: ").append(biomeKey != null ? biomeKey.toString() : "Unknown").append("\n");

            appendStatusEffects(context, player);

            context.append("- Main Hand: ").append(Registries.ITEM.getId(player.getMainHandStack().getItem()).toString()).append(" (Count: ").append(player.getMainHandStack().getCount()).append(")\n");
            context.append("- Off Hand: ").append(Registries.ITEM.getId(player.getOffHandStack().getItem()).toString()).append("\n");
            context.append("- Armor: [Helmet: ").append(Registries.ITEM.getId(player.getInventory().getArmorStack(3).getItem()).toString()).append(", Chestplate: ").append(Registries.ITEM.getId(player.getInventory().getArmorStack(2).getItem()).toString()).append(", Leggings: ").append(Registries.ITEM.getId(player.getInventory().getArmorStack(1).getItem()).toString()).append(", Boots: ").append(Registries.ITEM.getId(player.getInventory().getArmorStack(0).getItem()).toString()).append("]\n");

            appendInventory(context, player);
            appendModRecipes(context);
            appendInstalledMods(context);
            appendTargetedBlock(context, player, world);

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

    private static String evaluateWorldPhase(long dailyTime) {
        if (dailyTime >= 0 && dailyTime < 9000) return "DAYTIME (Safe, no surface monster spawns)";
        if (dailyTime >= 9000 && dailyTime < 12000) return "SUNSET (Dusk, light dropping)";
        if (dailyTime >= 12000 && dailyTime < 13000) return "LATE SUNSET (Beds become usable)";
        if (dailyTime >= 13000 && dailyTime < 23000) return "NIGHTTIME (Hostile monsters spawn on surface, Beds are usable)";
        return "SUNRISE (Dawn, monsters start burning)";
    }

    private static void appendStatusEffects(StringBuilder context, ServerPlayerEntity player) {
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
    }

    private static void appendInventory(StringBuilder context, ServerPlayerEntity player) {
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
        if (!hasItems) context.append("- Inventory is completely empty.\n");
    }

    private static void appendModRecipes(StringBuilder context) {
        context.append("\n=== CRAFTING KNOWLEDGE ===\n");
        context.append("- Available Mod Recipes: ");
        List<String> modRecipes = ModRecipeStorage.ALL_MOD_RECIPES;

        if (modRecipes.isEmpty()) {
            context.append("None detected.\n");
        } else {
            String recipeString = modRecipes.stream().limit(30).collect(Collectors.joining(", "));
            context.append(recipeString);
            if (modRecipes.size() > 30) {
                context.append("... (and ").append(modRecipes.size() - 30).append(" more)");
            }
            context.append("\n");
        }
    }

    private static void appendInstalledMods(StringBuilder context) {
        context.append("\n=== INSTALLED MODS & ITEMS ===\n");
        Map<String, List<String>> itemsByMod = new HashMap<>();
        for (Item item : Registries.ITEM) {
            Identifier id = Registries.ITEM.getId(item);
            String namespace = id.getNamespace();
            if (!namespace.equals("minecraft") && !namespace.equals("brigadier")) {
                itemsByMod.computeIfAbsent(namespace, k -> new ArrayList<>()).add(id.getPath());
            }
        }

        if (itemsByMod.isEmpty()) {
            context.append("- No external custom item mods detected.\n");
        } else {
            for (Map.Entry<String, List<String>> entry : itemsByMod.entrySet()) {
                context.append("- Mod [").append(entry.getKey()).append("] provides items: ");
                List<String> items = entry.getValue();
                if (items.size() > 40) {
                    context.append(items.stream().limit(40).collect(Collectors.joining(", "))).append("... (and ").append(items.size() - 40).append(" more items)");
                } else {
                    context.append(String.join(", ", items));
                }
                context.append("\n");
            }
        }
    }

    private static void appendTargetedBlock(StringBuilder context, ServerPlayerEntity player, net.minecraft.world.World world) {
        net.minecraft.util.hit.BlockHitResult hit = (net.minecraft.util.hit.BlockHitResult) player.raycast(5.0, 0.0f, false);
        if (hit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
            BlockPos targetedPos = hit.getBlockPos();
            String targetedBlock = Registries.BLOCK.getId(world.getBlockState(targetedPos).getBlock()).toString();
            context.append("- Looking at Block: ").append(targetedBlock).append("\n");
        }
    }
}