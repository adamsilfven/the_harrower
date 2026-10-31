package com.leyline.hollowforged.client;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
@EventBusSubscriber(modid="lc_hollowforged",value=Dist.CLIENT)
public final class EffectsReview {
 private static int ticks;private static final Set<Integer> received=new HashSet<>();
 public static void received(int kind){if(Boolean.getBoolean("lc_hollowforged.effects_review"))received.add(kind);}
 @SubscribeEvent public static void tick(ClientTickEvent.Post event){
  if(!Boolean.getBoolean("lc_hollowforged.effects_review"))return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;
  if(ticks==1){mc.options.hideGui=true;mc.options.fov().set(70);mc.options.renderDistance().set(4);mc.getConnection().sendCommand("tp @s 0 70 -11 0 24");}
  if(ticks<80)return;int stage=(ticks-80)/70,phase=(ticks-80)%70;
  if(stage>=20){if(received.size()!=20)throw new IllegalStateException("Missing native FX events: "+received);try{java.nio.file.Files.writeString(mc.gameDirectory.toPath().resolve("effects-review-result.txt"),"PASS all 20 effect kinds received and rendered; "+RelicEffects.metrics()+"\n");}catch(Exception failure){throw new IllegalStateException(failure);}mc.stop();return;}
  if(phase==0)mc.getConnection().sendCommand("hollowforged fxpreview "+stage);
  if(phase==(stage==15?60:8))TooltipReview.capture(String.format(java.util.Locale.ROOT,"fx_%02d.png",stage));
  if(phase==64)mc.getConnection().sendCommand("hollowforged fxstop "+stage);
 }
}
