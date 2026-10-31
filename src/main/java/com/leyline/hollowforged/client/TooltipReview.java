package com.leyline.hollowforged.client;
import com.leyline.hollowforged.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
/** Explicit opt-in native tooltip fixture. Never runs in an ordinary game. */
@EventBusSubscriber(modid=Hollowforged.ID,value=Dist.CLIENT)
public final class TooltipReview {
 public static boolean expanded;private static int ticks;private static ItemStack stack=ItemStack.EMPTY;private static String label="";
 private static void write(String name,String text){try{java.nio.file.Files.writeString(Minecraft.getInstance().gameDirectory.toPath().resolve(name),text);}catch(Exception e){throw new IllegalStateException(e);}}
 @SubscribeEvent public static void tick(ClientTickEvent.Post e){
  if(!Boolean.getBoolean("lc_hollowforged.tooltip_review"))return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;
  if(ticks==1){verifyBindings(mc);write("tooltip-test-mods.txt",net.neoforged.fml.ModList.get().getMods().stream().map(m->m.getModId()+" "+m.getVersion()).collect(java.util.stream.Collectors.joining("\n")));mc.options.hideGui=true;mc.getToasts().clear();mc.gui.getChat().clearMessages(true);AbilityControls.KEYS[0].setKey(InputConstants.Type.KEYSYM.getOrCreate(72));AbilityControls.KEYS[2].setKey(InputConstants.Type.KEYSYM.getOrCreate(74));KeyMapping.resetMapping();mc.setScreen(new Fixture());}
  if(ticks<20)return;int stage=(ticks-20)/20,phase=(ticks-20)%20;
  if(stage>=14){write("tooltip-review-result.txt","TOOLTIP_REVIEW_PASS: summaries, details, unbound and remapped keys; 14 native captures\n");mc.stop();return;}
  if(phase==0){
   int item=stage<10?stage/2:stage==10||stage==13?0:stage==11?1:4;expanded=stage<10&&stage%2==1;stack=new ItemStack(Hollowforged.RELICS.get(item).get());
   if(stage==10)AbilityControls.KEYS[0].setKey(InputConstants.UNKNOWN);
   if(stage==11)AbilityControls.KEYS[2].setKey(InputConstants.UNKNOWN);
   if(stage==12)mc.options.keyUse.setKey(InputConstants.Type.KEYSYM.getOrCreate(82));
   if(stage==13)AbilityControls.KEYS[0].setKey(InputConstants.Type.KEYSYM.getOrCreate(71));
   if(net.neoforged.fml.ModList.get().isLoaded("simplytooltips"))try{
    var registry=Class.forName("net.sweenus.simplytooltips.client.render.ItemThemeRegistry");
    if((boolean)registry.getMethod("isEnabledForStack",ItemStack.class).invoke(null,stack))throw new IllegalStateException("Simply Tooltips exclusion not applied");
    if(!(boolean)registry.getMethod("isEnabledForStack",ItemStack.class).invoke(null,new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD)))throw new IllegalStateException("Other items incorrectly excluded");
   }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
   KeyMapping.resetMapping();label=String.format(java.util.Locale.ROOT,"%02d_%s_%s",stage+1,RelicTooltips.kind(stack),stage<10?(expanded?"details":"summary"):stage<12?"unbound":"remapped");
   var lines=Screen.getTooltipFromItem(mc,stack);String text=lines.stream().map(Component::getString).collect(java.util.stream.Collectors.joining("\n"));
   if(text.contains("tooltip.lc_hollowforged.")||text.contains("Controls: bind")||text.contains("%s"))throw new IllegalStateException("Unresolved tooltip: "+text);
   if(stage==10||stage==11){if(!text.contains("UNBOUND")||!text.contains("Controls"))throw new IllegalStateException("Missing unbound hint: "+text);}
   if(stage==12&&!text.contains("[R]"))throw new IllegalStateException("Use remap not reflected");
   if(stage==13&&!text.contains("[G]"))throw new IllegalStateException("Sight remap not reflected");
   if(stage<10&&!text.contains(expanded?"Release Shift":"Hold Shift"))throw new IllegalStateException("Wrong expansion hint");
   if(stage==8&&(!text.contains("Damage: 30")||!text.contains("Attack speed: 0.8/s")||!text.contains("Attack reach: 5")))throw new IllegalStateException("Unexpected weapon stats: "+text);
   if(stage<10&&!expanded&&lines.size()>16)throw new IllegalStateException("Summary too long: "+lines.size());
   if(lines.size()*10>mc.getWindow().getGuiScaledHeight()-35)throw new IllegalStateException("Tooltip exceeds native screen: "+lines.size());
   write(label+".txt",text+"\n");
  }
  if(phase==10)capture(label+".png");
 }
 private static void verifyBindings(Minecraft mc){
  try{
   var checked=AbilityControls.class.getDeclaredField("defaultsChecked");checked.setAccessible(true);
   var upgrade=AbilityControls.class.getDeclaredMethod("upgradeDefaultBindings",Minecraft.class);upgrade.setAccessible(true);
   var marker=mc.gameDirectory.toPath().resolve("config/lc_hollowforged-controls-v1.properties");
   var crownMarker=mc.gameDirectory.toPath().resolve("config/lc_hollowforged-crown-controls-v2.properties");
   if(AbilityControls.KEYS[0].getDefaultKey().getValue()!=86||AbilityControls.KEYS[2].getDefaultKey().getValue()!=74)throw new IllegalStateException("Incorrect defaults");
   java.nio.file.Files.writeString(marker,"version=1\n");java.nio.file.Files.deleteIfExists(crownMarker);checked.setBoolean(null,false);
   AbilityControls.KEYS[0].setKey(InputConstants.Type.KEYSYM.getOrCreate(72));AbilityControls.KEYS[2].setKey(InputConstants.Type.KEYSYM.getOrCreate(71));upgrade.invoke(null,mc);
   if(AbilityControls.KEYS[0].getKey().getValue()!=86||AbilityControls.KEYS[2].getKey().getValue()!=71)throw new IllegalStateException("Legacy H migration or custom chest binding lost");
   AbilityControls.KEYS[0].setKey(InputConstants.UNKNOWN);checked.setBoolean(null,false);upgrade.invoke(null,mc);
   if(!AbilityControls.KEYS[0].isUnbound())throw new IllegalStateException("Deliberate unbind overwritten");
   java.nio.file.Files.deleteIfExists(crownMarker);AbilityControls.KEYS[0].setKey(InputConstants.Type.KEYSYM.getOrCreate(71));checked.setBoolean(null,false);upgrade.invoke(null,mc);
   if(AbilityControls.KEYS[0].getKey().getValue()!=71)throw new IllegalStateException("Custom crown key overwritten");
   AbilityControls.KEYS[0].setKey(InputConstants.Type.KEYSYM.getOrCreate(72));checked.setBoolean(null,false);upgrade.invoke(null,mc);
   if(AbilityControls.KEYS[0].getKey().getValue()!=72)throw new IllegalStateException("Later deliberate H binding overwritten");
   write("controls-review-result.txt","PASS: V crown/J chest defaults; existing v1 H upgraded to V; custom crown/chest keys preserved; later deliberate unbinding/H preserved\n");
  }catch(Exception failure){throw new IllegalStateException(failure);}
 }
 static void capture(String name){
  var mc=Minecraft.getInstance();try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
   var buffered=new java.awt.image.BufferedImage(image.getWidth(),image.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);
   for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){int c=image.getPixelRGBA(x,y);buffered.setRGB(x,y,(c&0xFF000000)|((c&255)<<16)|(c&0xFF00)|((c>>>16)&255));}
   var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(buffered,"PNG",bytes);var data=bytes.toByteArray();
   var path=mc.gameDirectory.toPath().resolve("screenshots").resolve(name);java.nio.file.Files.createDirectories(path.getParent());
   try(var channel=java.nio.channels.FileChannel.open(path,java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,java.nio.file.StandardOpenOption.WRITE)){var buffer=java.nio.ByteBuffer.wrap(data);while(buffer.hasRemaining())channel.write(buffer);channel.force(true);}
   if(java.nio.file.Files.size(path)!=data.length)throw new IllegalStateException("Incomplete screenshot");
  }catch(Exception failure){throw new IllegalStateException(failure);}
 }
 private static final class Fixture extends net.minecraft.client.gui.screens.inventory.InventoryScreen {
  Fixture(){super(Minecraft.getInstance().player);}
  @Override public boolean isPauseScreen(){return false;}
  @Override public void render(GuiGraphics g,int x,int y,float partial){g.fill(0,0,width,height,0xFF25282B);g.drawString(font,"Actual Minecraft tooltip: "+label,16,12,0xD2CDC1);if(!stack.isEmpty()){g.renderItem(stack,18,37);g.renderTooltip(font,stack,52,48);}}
 }
}
