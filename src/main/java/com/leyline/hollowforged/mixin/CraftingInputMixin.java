package com.leyline.hollowforged.mixin;
import java.util.List;
import com.leyline.hollowforged.GridOrigin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(CraftingInput.class)
public abstract class CraftingInputMixin implements GridOrigin {
 @Unique private int hollowforged$width=-1,hollowforged$height=-1,hollowforged$left=-1,hollowforged$top=-1;
 @Override public void hollowforged$origin(int w,int h,int l,int t){hollowforged$width=w;hollowforged$height=h;hollowforged$left=l;hollowforged$top=t;}
 @Override public boolean hollowforged$matchesOrigin(int w,int h,int l,int t){return hollowforged$width==w&&hollowforged$height==h&&hollowforged$left==l&&hollowforged$top==t;}
 @Inject(method="ofPositioned",at=@At("RETURN"))
 private static void hollowforged$retainOrigin(int width,int height,List<ItemStack> items,CallbackInfoReturnable<CraftingInput.Positioned> cir){
  var p=cir.getReturnValue();if(!p.input().isEmpty())((GridOrigin)(Object)p.input()).hollowforged$origin(width,height,p.left(),p.top());
 }
}
