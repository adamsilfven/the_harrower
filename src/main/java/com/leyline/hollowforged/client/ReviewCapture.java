package com.leyline.hollowforged.client;
import com.leyline.hollowforged.Hollowforged;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
@EventBusSubscriber(modid=Hollowforged.ID,value=Dist.CLIENT)
public final class ReviewCapture {
 private static int tick;
 public static void evidence(String message){if(!Boolean.getBoolean("lc_hollowforged.capture"))return;try{java.nio.file.Files.writeString(Minecraft.getInstance().gameDirectory.toPath().resolve("review-fixture.txt"),message+"\n",java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.APPEND);}catch(java.io.IOException e){throw new IllegalStateException(e);}}

 @SubscribeEvent public static void tick(ClientTickEvent.Post event){
  if(Boolean.getBoolean("lc_hollowforged.tooltip_review")||Boolean.getBoolean("lc_hollowforged.effects_review")||!Boolean.getBoolean("lc_hollowforged.capture"))return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;tick++;
  if(tick==1){AbilityControls.KEYS[0].setKey(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(72));AbilityControls.KEYS[2].setKey(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(74));net.minecraft.client.KeyMapping.resetMapping();com.mojang.logging.LogUtils.getLogger().info("HOLLOWFORGED_CAPTURE_BEGIN");evidence("CAPTURE_BEGIN");mc.options.hideGui=true;mc.options.fov().set(45);mc.options.renderDistance().set(2);}
  if(tick==80)snap("01_armor_front.png");
  if(tick==100)mc.player.connection.sendCommand("tp @s 3.8 64.5 0.0 90 2");
  if(tick==150)snap("02_armor_side.png");
  if(tick==170)mc.player.connection.sendCommand("tp @s 0.0 64.5 3.8 180 2");
  if(tick==220)snap("03_armor_rear.png");
  if(tick==240)mc.player.connection.sendCommand("tp @s 2.8 64.5 -2.8 45 2");
  if(tick==290)snap("04_armor_three_quarter.png");
  if(tick==310){mc.options.hideGui=false;mc.options.fov().set(60);mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);mc.player.connection.sendCommand("gamemode creative");mc.player.connection.sendCommand("tp @s 6.0 65.0 -5.0 180 0");}
  if(tick==322)mc.player.connection.sendCommand("item replace entity @s weapon.mainhand with minecraft:air");
  if(tick==345)snap("31_empty_hand_armor.png");
  if(tick==347)mc.player.connection.sendCommand("item replace entity @s armor.chest with minecraft:air");
  if(tick==360){snap("32_empty_hand_no_armor.png");evidence("UNARMORED_CHEST "+mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).isEmpty());}
  if(tick==361){mc.player.connection.sendCommand("item replace entity @s armor.chest with lc_hollowforged:carapace_of_the_unbroken");mc.player.connection.sendCommand("item replace entity @s weapon.mainhand with lc_hollowforged:worldshard");}
  if(tick==360){mc.getToasts().clear();mc.gui.getChat().clearMessages(true);}
  if(tick==374)mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
  if(tick==377)snap("26_swing_first_person.png");
  if(tick==370)snap("05_worldshard_first_person.png");
  if(tick==400){mc.options.hideGui=true;mc.options.fov().set(55);mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);mc.player.connection.sendCommand("tp @s 6.0 65.0 -5.0 180 0");}
  if(tick==460)snap("06_worldshard_third_person.png");
  if(tick==470){mc.player.setYRot(90);mc.player.setYHeadRot(90);}
  if(tick>=470&&tick<595){mc.player.yBodyRot=180;mc.player.yBodyRotO=180;}
  if(tick==490)snap("27_forward_carry_side.png");
  if(tick==580)snap("28_forward_strike_side.png");
  if(tick==480)mc.player.connection.sendCommand("gamemode survival");
  if(tick==500)mc.options.keyShift.setDown(true);
  if(tick==540){snap("07_armor_crouching.png");com.mojang.logging.LogUtils.getLogger().info("HOLLOWFORGED_VISUAL_CROUCH {}",mc.player.isCrouching());evidence("CROUCH "+(mc.player.isCrouching()));}
  if(tick==550)mc.options.keyShift.setDown(false);
  if(tick==565)mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
  if(tick==567)snap("23_swing_lift.png");
  if(tick==572)snap("24_swing_drive.png");
  if(tick==577)snap("25_swing_recovery.png");
  if(tick==570){snap("08_worldshard_air_swing.png");com.mojang.logging.LogUtils.getLogger().info("HOLLOWFORGED_VISUAL_AIR_SWING {}",mc.player.getAttackAnim(1)>0);evidence("AIR_SWING "+(mc.player.getAttackAnim(1)>0));}
  if(tick==600)mc.player.connection.sendCommand("tp @s 22.5 65.0 1.5 0 0");
  if(tick==610){mc.options.keySprint.setDown(true);mc.options.keyUp.setDown(true);}
  if(tick==630){snap("09_armor_swimming.png");com.mojang.logging.LogUtils.getLogger().info("HOLLOWFORGED_VISUAL_SWIM {}",mc.player.isSwimming());evidence("SWIM "+(mc.player.isSwimming()));}
  if(tick==640){mc.options.keySprint.setDown(false);mc.options.keyUp.setDown(false);}
  if(tick==650){mc.options.hideGui=false;mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);mc.player.connection.sendCommand("tp @s 6 65 -5 0 0");mc.player.connection.sendCommand("time set midnight");mc.player.connection.sendCommand("gamerule doDaylightCycle false");}
  if(tick==660){mc.player.connection.sendCommand("summon zombie 6 65 1 {NoAI:1b,Invulnerable:1b,Silent:1b,PersistenceRequired:1b}");mc.player.connection.sendCommand("fill 5 65 -2 7 68 -2 stone");}
  if(tick==680){AbilityControls.KEYS[0].setDown(true);net.minecraft.client.KeyMapping.click(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(74));}
  if(tick==684)AbilityControls.KEYS[0].setDown(false);
  if(tick==700)mc.options.keyShift.setDown(true);
  if(tick==742)mc.options.keyShift.setDown(false);
  if(tick==710){evidence("REVEAL "+java.util.stream.StreamSupport.stream(mc.level.entitiesForRendering().spliterator(),false).anyMatch(e->e instanceof net.minecraft.world.entity.monster.Zombie&&AbilityControls.revealed(e)));snap("10_crown_dark_vision_reveal.png");com.mojang.logging.LogUtils.getLogger().info("HOLLOWFORGED_VISUAL_REVEAL {}",java.util.stream.StreamSupport.stream(mc.level.entitiesForRendering().spliterator(),false).anyMatch(e->e instanceof net.minecraft.world.entity.monster.Zombie&&AbilityControls.revealed(e)));}
  if(tick==720)mc.player.connection.sendCommand("fill 5 65 -2 7 68 -2 air");
  if(tick==730){mc.options.keyUse.setDown(true);net.minecraft.client.KeyMapping.click(com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(1));}
  if(tick==734)mc.options.keyUse.setDown(false);
  if(tick==740)snap("11_fracture_wave.png");
  if(tick==760){mc.options.keyUse.setDown(true);net.minecraft.client.KeyMapping.click(com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(1));}
  if(tick==824)mc.options.keyUse.setDown(false);
  if(tick==790)snap("12_argument_windup.png");
  if(tick==830)snap("13_argument_impact.png");
  if(tick==850)AbilityControls.openMenu();
  if(tick==870)snap("14_ability_menu.png");
  if(tick==880){mc.setScreen(null);mc.options.hideGui=true;mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);mc.player.connection.sendCommand("time set day");mc.player.connection.sendCommand("summon pig 6 65 -5 {NoAI:1b,Saddle:1b}");}
  if(tick==890)mc.player.connection.sendCommand("ride @s mount @e[type=minecraft:pig,sort=nearest,limit=1]");
  if(tick==920){snap("15_armor_riding.png");com.mojang.logging.LogUtils.getLogger().info("HOLLOWFORGED_VISUAL_RIDING {}",mc.player.isPassenger());evidence("RIDING "+(mc.player.isPassenger()));}
  if(tick==940)mc.player.connection.sendCommand("ride @s dismount");
  if(tick==950){mc.player.connection.sendCommand("tp @s 6 65 -5 0 0");mc.options.keySprint.setDown(true);mc.options.keyUp.setDown(true);}
  if(tick==970){snap("16_armor_sprinting.png");com.mojang.logging.LogUtils.getLogger().info("HOLLOWFORGED_VISUAL_SPRINT {}",mc.player.isSprinting());evidence("SPRINT "+(mc.player.isSprinting()));}
  if(tick==980){mc.options.keySprint.setDown(false);mc.options.keyUp.setDown(false);}
  if(tick==990)mc.player.connection.sendCommand("tp @s 6 65 -5 0 0");
  if(tick==1000){mc.options.hideGui=false;mc.options.keyShift.setDown(true);mc.options.keyJump.setDown(true);}
  if(tick==1002){mc.options.keyShift.setDown(false);mc.options.keyJump.setDown(false);}
  if(tick==1050)AbilityControls.KEYS[0].setDown(true);
  if(tick==1074)AbilityControls.KEYS[0].setDown(false);
  if(tick==1030)snap("17_cataclysm_airborne.png");
  if(tick==1090)snap("18_cataclysm_landing.png");
  if(tick==1110){mc.player.connection.sendCommand("ride @s dismount");mc.player.connection.sendCommand("kill @e[type=minecraft:pig]");mc.player.connection.sendCommand("kill @e[type=minecraft:zombie]");mc.player.connection.sendCommand("effect clear @s");mc.player.connection.sendCommand("tp @s 6 65 -5 180 0");mc.player.connection.sendCommand("item replace entity @s hotbar.1 with lc_hollowforged:crown_of_the_hollow");mc.player.connection.sendCommand("item replace entity @s hotbar.2 with lc_hollowforged:carapace_of_the_unbroken");mc.player.connection.sendCommand("item replace entity @s hotbar.3 with lc_hollowforged:greaves_of_the_stillborn");mc.player.connection.sendCommand("item replace entity @s hotbar.4 with lc_hollowforged:treads_of_the_worldbreaker");mc.player.connection.sendCommand("item replace entity @s hotbar.5 with lc_hollowforged:hollow_core");}
  if(tick==1130)mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
  if(tick==1150)snap("19_inventory_icons.png");
  if(tick==1160){mc.setScreen(null);mc.player.connection.sendCommand("gamemode creative");mc.player.connection.sendCommand("tp @s 6 65 -5 180 0");mc.getToasts().clear();mc.gui.getChat().clearMessages(true);mc.options.hideGui=true;mc.player.getInventory().selected=8;mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);mc.options.fov().set(48);}
  if(tick==1200)snap("20_player_front.png");
  if(tick==1210)mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
  if(tick==1240)snap("21_player_back.png");
  if(tick==1250){mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);mc.player.connection.sendCommand("time set midnight");}
  if(tick==1290)snap("22_night_core.png");
  if(tick==1300){mc.player.connection.sendCommand("time set day");mc.options.fov().set(45);}
  if(tick==1320){mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);mc.player.connection.sendCommand("gamemode spectator");mc.player.connection.sendCommand("tp @s 1.5 63.9 -2.2 34 2");}
  if(tick==1340)snap("29_boot_detail.png");
  if(tick==1350)mc.player.connection.sendCommand("tp @s 1.7 65.1 -2.1 39 18");
  if(tick==1370)snap("30_gauntlet_detail.png");
  if(tick==1400){evidence("CAPTURE_END");mc.stop();}
 }
 private static void snap(String name){var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{});evidence("SNAP "+name);}
}
