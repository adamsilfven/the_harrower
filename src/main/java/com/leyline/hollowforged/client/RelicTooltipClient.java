package com.leyline.hollowforged.client;
import java.util.*;
import com.leyline.hollowforged.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
/** Client-only presentation. JEI and inventory use the same ItemTooltipEvent path. */
@EventBusSubscriber(modid=Hollowforged.ID,value=Dist.CLIENT)
public final class RelicTooltipClient {
 @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST)
 public static void tooltip(ItemTooltipEvent event){
  if(!Hollowforged.isRelic(event.getItemStack()))return;
  boolean expanded=Screen.hasShiftDown();
  if(Boolean.getBoolean("lc_hollowforged.tooltip_review"))expanded=TooltipReview.expanded;
  var lines=event.getToolTip();lines.removeIf(RelicTooltipClient::replacedVanillaLine);
  // Remove empty separators left by vanilla's now-replaced attribute sections.
  for(int i=lines.size()-1;i>0;i--)if(lines.get(i).getString().isBlank())lines.remove(i);
  var body=RelicTooltips.describe(event.getItemStack(),expanded,RelicTooltipClient::binding,RelicTooltipClient::unbound);
  var content=new ArrayList<Component>();content.add(body.get(0));content.addAll(stats(event.getItemStack()));content.add(Component.empty());content.addAll(body.subList(1,body.size()));
  int width=Math.min(expanded?340:240,Math.max(160,Minecraft.getInstance().getWindow().getGuiScaledWidth()-40));
  for(var component:content)lines.addAll(wrap(component,width));
 }
 private static boolean replacedVanillaLine(Component c){
  if(c.getContents() instanceof TranslatableContents t){String key=t.getKey();if(key.equals("item.unbreakable")||key.startsWith("item.modifiers.")||key.startsWith("attribute.modifier.")||key.startsWith("neoforge.modifier."))return true;}
  return c.getSiblings().stream().anyMatch(RelicTooltipClient::replacedVanillaLine);
 }
 private static KeyMapping mapping(int i){var options=Minecraft.getInstance().options;return i==-1?options.keyUse:i==-2?options.keyShift:i==-3?options.keyJump:AbilityControls.KEYS[i];}
 public static String binding(int i){return unbound(i)?Component.translatable(RelicTooltips.PREFIX+"unbound").getString():mapping(i).getTranslatedKeyMessage().getString();}
 public static boolean unbound(int i){return mapping(i).isUnbound();}
 /** Item's own attribute/enchantment modifiers plus player base, without counting held gear twice. */
 private static double value(ItemStack stack,EquipmentSlot slot,Holder<Attribute> attribute,double fallback){
  var player=Minecraft.getInstance().player;double base=player==null?fallback:player.getAttributeBaseValue(attribute);
  double[] sums={0,0,1};stack.forEachModifier(slot,(kind,modifier)->{if(!kind.equals(attribute))return;switch(modifier.operation()){case ADD_VALUE->sums[0]+=modifier.amount();case ADD_MULTIPLIED_BASE->sums[1]+=modifier.amount();case ADD_MULTIPLIED_TOTAL->sums[2]*=1+modifier.amount();}});
  return (base+sums[0])*(1+sums[1])*sums[2];
 }
 private static String number(double value){return String.format(Locale.ROOT,"%.2f",value).replaceAll("0+$","").replaceAll("\\.$","");}
 private static Component stat(String key,Object... args){return Component.translatable(RelicTooltips.PREFIX+"stats."+key,args).withStyle(ChatFormatting.GRAY);}
 public static List<Component> stats(ItemStack stack){
  if(stack.getItem() instanceof ArmorItem armor){var slot=armor.getEquipmentSlot();return List.of(stat("armor",number(value(stack,slot,Attributes.ARMOR,0)),number(value(stack,slot,Attributes.ARMOR_TOUGHNESS,0))),stat("knockback",number(value(stack,slot,Attributes.KNOCKBACK_RESISTANCE,0)*100)));}
  return List.of(stat("weapon",number(value(stack,EquipmentSlot.MAINHAND,Attributes.ATTACK_DAMAGE,1)),number(value(stack,EquipmentSlot.MAINHAND,Attributes.ATTACK_SPEED,4))),stat("reach",number(value(stack,EquipmentSlot.MAINHAND,Attributes.ENTITY_INTERACTION_RANGE,3))));
 }
 public static List<Component> wrap(Component component,int width){
  if(component.getString().isEmpty())return List.of(Component.empty());var result=new ArrayList<Component>();
  for(var sequence:Minecraft.getInstance().font.split(component,width)){
   var line=Component.empty();sequence.accept((index,style,codepoint)->{line.append(Component.literal(new String(Character.toChars(codepoint))).setStyle(style));return true;});result.add(line);
  }return result;
 }
}
