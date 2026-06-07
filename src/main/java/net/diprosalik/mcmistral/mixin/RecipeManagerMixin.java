package net.diprosalik.mcmistral.mixin;

import net.minecraft.recipe.RecipeManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.diprosalik.mcmistral.mistral.ModRecipeStorage;
import java.util.Map;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {

    @Inject(method = "apply", at = @At("TAIL"))
    private void onRecipesLoaded(Map<Identifier, Object> map, ResourceManager resourceManager, Profiler profiler, CallbackInfo ci) {
        ModRecipeStorage.ALL_MOD_RECIPES.clear();
        for (Identifier id : map.keySet()) {
            if (!id.getNamespace().equals("minecraft")) {
                ModRecipeStorage.ALL_MOD_RECIPES.add(id.toString());
            }
        }
    }
}