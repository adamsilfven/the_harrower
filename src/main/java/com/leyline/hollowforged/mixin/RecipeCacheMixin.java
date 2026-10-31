package com.leyline.hollowforged.mixin;
import java.util.Optional;
import com.leyline.hollowforged.Hollowforged;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Vanilla cache keys omit original grid offsets. Never reuse that cache for a core grid. */
@Mixin(RecipeCache.class)
public abstract class RecipeCacheMixin {
 @Inject(method="get",at=@At("HEAD"),cancellable=true)
 private void hollowforged$uncachedCore(Level level,CraftingInput input,CallbackInfoReturnable<Optional<RecipeHolder<CraftingRecipe>>> cir){
  if(input.items().stream().anyMatch(s->s.is(Hollowforged.HOLLOW_CORE.get())))cir.setReturnValue(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level));
 }
}
