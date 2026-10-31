package com.leyline.hollowforged.client;
import java.util.*;
import com.leyline.hollowforged.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
@EventBusSubscriber(modid=Hollowforged.ID,value=Dist.CLIENT)
public final class AbilityControls {
 public static final KeyMapping MENU=new KeyMapping("key.lc_hollowforged.menu",InputConstants.UNKNOWN.getValue(),"key.categories.lc_hollowforged");
 public static final KeyMapping[] KEYS=new KeyMapping[7];
 static {for(int i=0;i<7;i++)KEYS[i]=new KeyMapping("key.lc_hollowforged.ability_"+i,(i==0?org.lwjgl.glfw.GLFW.GLFW_KEY_V:i==2?org.lwjgl.glfw.GLFW.GLFW_KEY_J:InputConstants.UNKNOWN.getValue()),"key.categories.lc_hollowforged");}
 private static boolean defaultsChecked;
 /** Upgrade legacy unknown bindings once; later deliberate unbinding is respected. */
 private static void upgradeDefaultBindings(Minecraft mc){
  if(defaultsChecked||mc.options==null)return;
  defaultsChecked=true;
  var marker=mc.gameDirectory.toPath().resolve("config/lc_hollowforged-controls-v1.properties");
  try{
   var crownMarker=mc.gameDirectory.toPath().resolve("config/lc_hollowforged-crown-controls-v2.properties");
   if(!java.nio.file.Files.exists(crownMarker)){
    // Move the previous default, or an unset binding, once. Respect custom keys.
    if(KEYS[0].isUnbound()||KEYS[0].getKey().getValue()==org.lwjgl.glfw.GLFW.GLFW_KEY_H)KEYS[0].setKey(KEYS[0].getDefaultKey());
    KeyMapping.resetMapping();mc.options.save();
    java.nio.file.Files.createDirectories(crownMarker.getParent());
    java.nio.file.Files.writeString(crownMarker,"# Crown default moved from H to V. Later choices are preserved.\nversion=2\n");
   }
   if(java.nio.file.Files.exists(marker))return;
   for(int i:new int[]{0,2})if(KEYS[i].isUnbound())KEYS[i].setKey(KEYS[i].getDefaultKey());
   KeyMapping.resetMapping();mc.options.save();
   java.nio.file.Files.createDirectories(marker.getParent());
   java.nio.file.Files.writeString(marker,"# Default controls initialized. Later rebindings and unbindings are preserved.\nversion=1\n");
  }catch(java.io.IOException error){com.mojang.logging.LogUtils.getLogger().warn("Could not record Hollowforged control upgrade",error);}
 }
 private static int sightHeld,sneakHeld,launchPending;private static boolean sightWasDown,sightCommitted,sovereignCommitted,jumpWasDown;
 private static HFNetwork.View view;private static int age=1000;
 private static void review(String input){ReviewCapture.evidence("INPUT_"+input);if(Boolean.getBoolean("lc_hollowforged.capture"))com.mojang.logging.LogUtils.getLogger().info("HOLLOWFORGED_INPUT_{}",input);}
 public static void openMenu(){Minecraft.getInstance().setScreen(new AbilityScreen());}
 public static boolean revealed(Entity e){var mc=Minecraft.getInstance();return view!=null&&age<=40&&mc.level!=null&&mc.player!=null&&HFState.crown(mc.player)&&view.dimension().equals(mc.level.dimension().location().toString())&&Arrays.stream(view.reveals()).anyMatch(id->id==e.getId());}
 @SubscribeEvent public static void tick(ClientTickEvent.Post event){
  var mc=Minecraft.getInstance();upgradeDefaultBindings(mc);age++;if(mc.player==null||mc.getConnection()==null){view=null;return;}
  while(MENU.consumeClick())mc.setScreen(new AbilityScreen());
  if(mc.screen==null){
   boolean down=KEYS[0].isDown();
   if(down){if(!sightWasDown){sightHeld=0;sightCommitted=false;PacketDistributor.sendToServer(new HFEffects.Charge(true));}sightHeld++;if(sightHeld>=20&&!sightCommitted){PacketDistributor.sendToServer(new HFNetwork.Action(1));sightCommitted=true;review("SIGHT_HOLD");}}
   else if(sightWasDown&&!sightCommitted){PacketDistributor.sendToServer(new HFNetwork.Action(0));review("SIGHT_TAP");}
   if(!down&&sightWasDown)PacketDistributor.sendToServer(new HFEffects.Charge(false));
   sightWasDown=down;while(KEYS[0].consumeClick()){}
   for(int i=1;i<7;i++)while(KEYS[i].consumeClick())PacketDistributor.sendToServer(new HFNetwork.Action(i));
   boolean crouch=mc.options.keyShift.isDown();
   boolean anchored=view!=null&&(view.flags()&2)!=0;
   if(!crouch){sneakHeld=0;sovereignCommitted=false;}
   else if(HFState.legs(mc.player)&&mc.player.onGround()&&mc.player.getDeltaMovement().horizontalDistanceSqr()<.0025&&!mc.options.keyJump.isDown()&&view!=null&&view.cooldowns()[3]==0){if(!sovereignCommitted&&++sneakHeld>=20){PacketDistributor.sendToServer(new HFNetwork.Action(3));sovereignCommitted=true;review("SOVEREIGN_HOLD");}}
   else sneakHeld=0;
  }else{sightWasDown=false;sightHeld=0;sneakHeld=0;}

 }
 @SubscribeEvent public static void movement(MovementInputUpdateEvent event){
  var mc=Minecraft.getInstance();var input=event.getInput();boolean jumping=input.jumping;
  if(mc.screen==null&&jumping&&!jumpWasDown&&input.shiftKeyDown&&event.getEntity().onGround()&&HFState.boots(event.getEntity())&&view!=null&&view.cooldowns()[4]==0){PacketDistributor.sendToServer(new HFNetwork.Action(4));launchPending=3;review("CATACLYSM_JUMP");}
  jumpWasDown=jumping;if(launchPending>0){input.jumping=false;launchPending--;}
 }
 @SubscribeEvent public static void hud(RenderGuiEvent.Post event){
  if(view==null||age>40||Minecraft.getInstance().options.hideGui)return;var mc=Minecraft.getInstance();StringBuilder text=new StringBuilder();
  String[] states={"Sight","Anchored","Unbroken","Sovereign","Cataclysm","Last Argument…","Refusal"};
  for(int i=0;i<states.length;i++)if((view.flags()&(1<<i))!=0){if(!text.isEmpty())text.append(" · ");text.append(states[i]);}
  if(!text.isEmpty())event.getGuiGraphics().drawCenteredString(mc.font,text.toString(),mc.getWindow().getGuiScaledWidth()/2,mc.getWindow().getGuiScaledHeight()-96,0xE7E1D1);
  String charge="";
  if(sightWasDown&&!sightCommitted)charge="Final Witness: "+Math.min(100,sightHeld*5)+"%";
  if(sneakHeld>0&&!sovereignCommitted)charge="Sovereign Ground: "+Math.min(100,sneakHeld*5)+"%";
  if(mc.player!=null&&HFState.hammer(mc.player)&&mc.player.isUsingItem()){int held=mc.player.getTicksUsingItem();charge=held<12?"Release: Fracture · Hold: Last Argument":held<60?"Last Argument: "+Math.min(100,held*100/60)+"%":"Last Argument ready — release";}
  if(!charge.isEmpty())event.getGuiGraphics().drawCenteredString(mc.font,charge,mc.getWindow().getGuiScaledWidth()/2,mc.getWindow().getGuiScaledHeight()-120,0xD8B772);
  if(!view.targetStatus().isEmpty())event.getGuiGraphics().drawCenteredString(mc.font,view.targetStatus(),mc.getWindow().getGuiScaledWidth()/2,mc.getWindow().getGuiScaledHeight()-108,0xE7E1D1);
 }
 @EventBusSubscriber(modid=Hollowforged.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
 public static final class Registration {
  @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event){RelicEffects.install();event.register(MENU);for(var key:KEYS)event.register(key);HFNetwork.receive=value->{view=value;age=0;var player=Minecraft.getInstance().player;if(player!=null){if((value.flags()&4)!=0)HFState.until(player,"unbroken",20);else HFState.data(player).remove("unbroken");}};}
 }
 private static final class AbilityScreen extends Screen {
  private final Button[] buttons=new Button[7];
  AbilityScreen(){super(Component.literal("Hollowforged"));}
  @Override protected void init(){
   for(int i=0;i<7;i++){
    final int ability=i;int remaining=view==null?0:view.cooldowns()[i];String label=HFState.NAMES[i]+(remaining>0?" — "+((remaining+19)/20)+"s":"");
    var button=Button.builder(Component.literal(label),b->{PacketDistributor.sendToServer(new HFNetwork.Action(ability));onClose();}).bounds(width/2-150,height/2-90+i*24,300,20).build();
    button.active=view!=null&&(view.equipment()&(1<<i))!=0&&remaining==0;buttons[i]=button;addRenderableWidget(button);
   }
  }
  @Override public void tick(){for(int i=0;i<7;i++)if(buttons[i]!=null){int remaining=view==null?0:view.cooldowns()[i];buttons[i].setMessage(Component.literal(HFState.NAMES[i]+(remaining>0?" — "+((remaining+19)/20)+"s":"")));buttons[i].active=view!=null&&age<=40&&(view.equipment()&(1<<i))!=0&&remaining==0;}}
  @Override public boolean isPauseScreen(){return false;}
  @Override public void render(GuiGraphics graphics,int x,int y,float partial){super.render(graphics,x,y,partial);graphics.drawCenteredString(font,title,width/2,height/2-115,0xE7E1D1);graphics.drawCenteredString(font,"Tap/hold Sight · Hold sneak while still · Sneak+jump · Hammer right-click",width/2,height/2+88,0xB8B1A4);}
 }
}
