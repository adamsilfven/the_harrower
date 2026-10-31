package com.leyline.hollowforged;
import java.util.UUID;
import com.leyline.harrower.Harrower;
import com.leyline.harrower.loot.LootLeylines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.*;

/** Guaranteed standalone Core and XP, alongside native datapack-controlled vanilla loot.
 * Persist death-time eligibility and reward flags on the boss; never use historical suppression duration. */
public final class EncounterRewards {
 public enum Color {WHITE,BLUE,BLACK,RED,GREEN}
 private static final String KEY="hollowforged_encounter";
 public static void begin(LivingEntity boss,UUID summoner,UUID encounter){
  if(boss.level().isClientSide()||summoner==null||encounter==null)throw new IllegalArgumentException("Server encounter identity required");
  var d=boss.getPersistentData().getCompound(KEY);if(d.contains("encounter"))throw new IllegalStateException("Encounter already registered");
  d.putUUID("summoner",summoner);d.putUUID("encounter",encounter);d.putBoolean("legitimate",true);boss.getPersistentData().put(KEY,d);
 }
 /** Retained public legacy hook for compatibility. Duration no longer controls loot eligibility. */
 public static void suppression(LivingEntity boss,UUID owner,Color color,boolean active){}
 public static int qualifying(LivingEntity boss){return boss.getPersistentData().getCompound(KEY).getInt("death_colors");}
 public static int count(int colors,double roll){return 1+(roll<.05*Math.clamp(colors,0,5)?1:0);}
 private static void death(LivingDeathEvent event){
  if(event.isCanceled()||!(event.getEntity() instanceof Harrower boss)||!(boss.level() instanceof ServerLevel level))return;
  var d=boss.getPersistentData().getCompound(KEY);if(d.getBoolean("rewarded"))return;
  UUID owner=null;Entity credited=event.getSource().getEntity();
  if(credited instanceof ServerPlayer p)owner=p.getUUID();
  else if(credited instanceof OwnableEntity pet)owner=pet.getOwnerUUID();
  if(owner==null&&boss.getKillCredit() instanceof ServerPlayer p)owner=p.getUUID();
  if(owner==null)owner=boss.combat().encounter.summoner;
  d.putInt("death_colors",LootLeylines.atDeath(level,boss,owner));
  d.putInt("death_cores",count(d.getInt("death_colors"),boss.getRandom().nextDouble()));
  d.putInt("death_xp",1000+boss.getRandom().nextInt(501));d.putBoolean("death_snapshot",true);
  if(owner!=null)d.putUUID("loot_owner",owner);boss.getPersistentData().put(KEY,d);
 }
 private static void drops(LivingDropsEvent event){
  if(event.isCanceled()||!(event.getEntity() instanceof Harrower boss)||!(boss.level() instanceof ServerLevel level)||!level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT))return;
  if(!boss.isDeadOrDying())return;
  var d=boss.getPersistentData().getCompound(KEY);if(d.getBoolean("rewarded"))return;
  int cores=d.getBoolean("death_snapshot")?d.getInt("death_cores"):1;
  int xp=d.getBoolean("death_snapshot")?d.getInt("death_xp"):1000+boss.getRandom().nextInt(501);
  event.getDrops().removeIf(item->item.getItem().is(Hollowforged.HOLLOW_CORE.get()));
  event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(level,boss.getX(),boss.getY(),boss.getZ(),new ItemStack(Hollowforged.HOLLOW_CORE.get(),cores)));
  d.putBoolean("rewarded",true);boss.getPersistentData().put(KEY,d);
  // Snapshot the existing native rolls and fixed XP now; deliver them at the crumble.
  com.leyline.harrower.loot.DelayedDeathLoot.capture(boss,event.getDrops(),xp);
 }
 public static void register(){NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,EncounterRewards::death);NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,EncounterRewards::drops);}
}
