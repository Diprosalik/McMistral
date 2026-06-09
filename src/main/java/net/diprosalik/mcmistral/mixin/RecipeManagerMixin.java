package net.diprosalik.mcmistral.mixin;

import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.resource.featuretoggle.FeatureSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.diprosalik.mcmistral.mistral.ModRecipeStorage;
import java.util.Collection;

@Mixin(ServerRecipeManager.class)
public class RecipeManagerMixin {

    @Shadow
    public Collection<RecipeEntry<?>> values() {
        return null;
    }

    @Inject(method = "initialize", at = @At("TAIL"))
    private void onRecipesInitialized(FeatureSet features, CallbackInfo ci) {
        ModRecipeStorage.ALL_MOD_RECIPES.clear();

        for (RecipeEntry<?> entry : this.values()) {
            net.minecraft.util.Identifier id = entry.id().getValue();
            if (id != null && !id.getNamespace().equals("minecraft")) {
                ModRecipeStorage.ALL_MOD_RECIPES.add(id.toString());
            }
        }
    }
}