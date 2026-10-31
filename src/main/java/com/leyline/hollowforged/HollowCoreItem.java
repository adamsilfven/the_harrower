package com.leyline.hollowforged;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
public final class HollowCoreItem extends Item {
 public HollowCoreItem(Properties properties){super(properties);}
 @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
  lines.add(Component.translatable("tooltip.lc_hollowforged.hollow_core.origin").withStyle(ChatFormatting.DARK_GRAY,ChatFormatting.ITALIC));
  lines.add(Component.translatable("tooltip.lc_hollowforged.hollow_core.use").withStyle(ChatFormatting.GRAY));
 }
}
