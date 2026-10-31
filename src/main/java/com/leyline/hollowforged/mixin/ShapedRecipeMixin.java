package com.leyline.hollowforged.mixin;
import com.leyline.hollowforged.GridOrigin;
import com.leyline.hollowforged.Hollowforged;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ShapedRecipe.class)
public abstract class ShapedRecipeMixin {
 @Shadow @Final ItemStack result;
 @Shadow @Final public ShapedRecipePattern pattern;
 @Inject(method="matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",at=@At("HEAD"),cancellable=true)
 private void hollowforged$exactGrid(CraftingInput input,Level level,CallbackInfoReturnable<Boolean> cir){
  if(Hollowforged.isRelic(result)&&!((GridOrigin)(Object)input).hollowforged$matchesOrigin(3,3,0,result.is(Hollowforged.TREADS.get())?1:0))cir.setReturnValue(false);
 }
 @Inject(method="canCraftInDimensions",at=@At("HEAD"),cancellable=true)
 private void hollowforged$tableOnly(int width,int height,CallbackInfoReturnable<Boolean> cir){if(Hollowforged.isRelic(result))cir.setReturnValue(width==3&&height==3);}
 @Inject(method="getHeight",at=@At("HEAD"),cancellable=true)
 private void hollowforged$bootHeight(CallbackInfoReturnable<Integer> cir){if(result.is(Hollowforged.TREADS.get()))cir.setReturnValue(3);}
 @Inject(method="getIngredients",at=@At("HEAD"),cancellable=true)
 private void hollowforged$bootDisplay(CallbackInfoReturnable<NonNullList<Ingredient>> cir){
  if(result.is(Hollowforged.TREADS.get())&&pattern.height()==2&&pattern.width()==3){
   var display=NonNullList.withSize(9,Ingredient.EMPTY);for(int i=0;i<6;i++)display.set(i+3,pattern.ingredients().get(i));cir.setReturnValue(display);
  }
 }
}
