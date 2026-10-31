package com.leyline.hollowforged;
import java.util.*;
import java.util.function.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
/** Shared, renderer-independent tooltip content. No client classes or gameplay changes. */
public final class RelicTooltips {
 public static final String PREFIX="tooltip.lc_hollowforged.";
 public static String kind(ItemStack stack){if(stack.is(Hollowforged.CROWN.get()))return "crown";if(stack.is(Hollowforged.CARAPACE.get()))return "carapace";if(stack.is(Hollowforged.GREAVES.get()))return "greaves";if(stack.is(Hollowforged.TREADS.get()))return "treads";return "worldshard";}
 public static List<Component> describe(ItemStack stack,boolean expanded,IntFunction<String> bindings,IntPredicate unbound){
  var result=new ArrayList<Component>();String kind=kind(stack);var role=Component.translatable(PREFIX+kind+".role").withStyle(ChatFormatting.GOLD);result.add(role);
  int count=switch(kind){case "crown"->expanded?13:5;case "carapace"->expanded?12:4;case "greaves"->expanded?17:5;case "treads"->expanded?12:5;default->expanded?23:6;};
  String mode=expanded?"detail":"summary";
  for(int i=0;i<count;i++){
   String key=PREFIX+kind+"."+mode+"."+i;
   // Lines with controls use translated key names, including remapped Use/Sneak/Jump.
   Object[] args=switch(kind){case "crown"->new Object[]{key(bindings,unbound,0)};case "carapace"->new Object[]{key(bindings,unbound,2)};case "greaves"->new Object[]{key(bindings,unbound,-2)};case "treads"->new Object[]{key(bindings,unbound,-2),key(bindings,unbound,-3)};default->new Object[]{key(bindings,unbound,-1)};};
   boolean heading=expanded&&switch(kind){case "crown"->i==0||i==5||i==9;case "carapace"->i==0||i==4||i==7;case "greaves"->i==0||i==4||i==10;case "treads"->i==0||i==3||i==6;default->i==0||i==6||i==11||i==19;};
   result.add(Component.translatable(key,args).withStyle(heading?ChatFormatting.GOLD:ChatFormatting.GRAY));
  }
  if((kind.equals("crown")&&unbound.test(0))||(kind.equals("carapace")&&unbound.test(2))||(kind.equals("greaves")&&unbound.test(-2))||(kind.equals("treads")&&(unbound.test(-2)||unbound.test(-3)))||(kind.equals("worldshard")&&unbound.test(-1)))result.add(Component.translatable(PREFIX+"unbound_hint").withStyle(ChatFormatting.RED));
  result.add(Component.translatable(PREFIX+(expanded?"collapse":"expand")).withStyle(ChatFormatting.YELLOW));return result;
 }
 private static Component key(IntFunction<String> bindings,IntPredicate unbound,int i){return Component.literal(bindings.apply(i)).withStyle(unbound.test(i)?ChatFormatting.RED:ChatFormatting.AQUA);}
}
