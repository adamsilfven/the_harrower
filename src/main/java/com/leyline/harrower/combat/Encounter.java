package com.leyline.harrower.combat;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import com.leyline.harrower.Harrower;

/** Explicit opt-in roster, fixed start-time scaling, persistent progress; no automatic healing/reset. */
public final class Encounter {
 private boolean closed;private final Harrower boss;public UUID summoner;public boolean started;public int stage=1,countedPlayers=1;
 private final Set<UUID> optedIn=new LinkedHashSet<>(),combatants=new LinkedHashSet<>();
 private final Map<UUID,Float> threat=new HashMap<>();
 private final ServerBossEvent bar=new ServerBossEvent(Component.literal("The Harrower"),BossEvent.BossBarColor.WHITE,BossEvent.BossBarOverlay.PROGRESS);
 Encounter(Harrower boss){this.boss=boss;}
 public Set<UUID> roster(){return Collections.unmodifiableSet(optedIn);}
 public void setSummoner(UUID owner){if(started)throw new IllegalStateException("Encounter already started");summoner=owner;if(owner!=null)optedIn.add(owner);}
 public boolean join(ServerPlayer player){if(started||!eligible(player)||player.distanceToSqr(boss)>4096)return false;optedIn.add(player.getUUID());return true;}
 private boolean eligible(ServerPlayer p){return p.isAlive()&&!p.isRemoved()&&!p.isCreative()&&!p.isSpectator()&&p.level()==boss.level();}
 public void start(ServerPlayer attacker){
  if(started||closed||!boss.isAlive())return;if(summoner==null&&attacker!=null)setSummoner(attacker.getUUID());
  int active=0;for(UUID id:optedIn){ServerPlayer p=((ServerLevel)boss.level()).getServer().getPlayerList().getPlayer(id);if(p!=null&&eligible(p)&&p.distanceToSqr(boss)<=4096)active++;combatants.add(id);}
  countedPlayers=Math.max(1,Math.min(4,active));float fraction=boss.getHealth()/boss.getMaxHealth();
  boss.getAttribute(Attributes.MAX_HEALTH).setBaseValue(3000+1050*(countedPlayers-1));boss.setHealth(boss.getMaxHealth()*fraction);started=true;
 }
 public void received(DamageSource source,float actual){
  if(closed||!boss.isAlive())return;
  if(source.getEntity() instanceof ServerPlayer player){if(!started)start(player);combatants.add(player.getUUID());threat.merge(player.getUUID(),actual,Float::sum);}
 }
 public LivingEntity chooseTarget(){
  var server=((ServerLevel)boss.level()).getServer();ServerPlayer result=null;double score=-Double.MAX_VALUE;
  for(UUID id:combatants){ServerPlayer p=server.getPlayerList().getPlayer(id);if(p==null||!eligible(p)||p.distanceToSqr(boss)>4096)continue;
   double candidate=threat.getOrDefault(id,0F)*.12-Math.sqrt(p.distanceToSqr(boss));if(candidate>score){score=candidate;result=p;}
  }return result;
 }
 public void tick(){
  if(closed||!boss.isAlive()||boss.isRemoved()){stopBar();return;}
  // A Harrower is hostile on proximity, not an inert duel NPC waiting for first damage.
  ServerPlayer nearest=null,owner=null;double nearestDistance=4096;
  for(var candidate:((ServerLevel)boss.level()).players()){
   if(!eligible(candidate)||candidate.distanceToSqr(boss)>4096)continue;
   combatants.add(candidate.getUUID());
   if(candidate.getUUID().equals(summoner))owner=candidate;
   double distance=candidate.distanceToSqr(boss);if(distance<=nearestDistance){nearestDistance=distance;nearest=candidate;}
  }
  if(!started){ServerPlayer first=owner!=null?owner:nearest;if(first==null)return;start(first);}
  threat.replaceAll((id,value)->value*.998F);
  if(boss.tickCount%10==0){bar.setProgress(Math.max(0,boss.getHealth()/boss.getMaxHealth()));bar.setName(Component.literal("The Harrower — "+switch(stage){case 2->"The Resistance";case 3->"The Unraveling";default->"The Intrusion";}));
   var server=((ServerLevel)boss.level()).getServer();Set<ServerPlayer> visible=new HashSet<>();for(UUID id:combatants){ServerPlayer p=server.getPlayerList().getPlayer(id);if(p!=null&&eligible(p)&&p.distanceToSqr(boss)<=9216)visible.add(p);}
   for(ServerPlayer p:new ArrayList<>(bar.getPlayers()))if(!visible.contains(p))bar.removePlayer(p);for(ServerPlayer p:visible)bar.addPlayer(p);
  }
 }
 public int barViewers(){return bar.getPlayers().size();}
 public boolean barClosed(){return closed;}
 public void untrack(ServerPlayer player){bar.removePlayer(player);}
 public void stopBar(){closed=true;bar.setVisible(false);bar.removeAllPlayers();}
 public void reset(){stopBar();closed=false;bar.setVisible(true);started=false;stage=1;countedPlayers=1;combatants.clear();threat.clear();optedIn.clear();if(summoner!=null)optedIn.add(summoner);boss.getAttribute(Attributes.MAX_HEALTH).setBaseValue(3000);boss.setHealth(3000);}
 public void save(CompoundTag tag){tag.putBoolean("Started",started);tag.putInt("Stage",stage);tag.putInt("Counted",countedPlayers);if(summoner!=null)tag.putUUID("Summoner",summoner);tag.put("Roster",uuids(optedIn));tag.put("Combatants",uuids(combatants));}
 private ListTag uuids(Set<UUID> set){ListTag list=new ListTag();for(UUID id:set)list.add(StringTag.valueOf(id.toString()));return list;}
 public void load(CompoundTag tag){started=tag.getBoolean("Started");stage=Math.max(1,tag.getInt("Stage"));countedPlayers=Math.max(1,tag.getInt("Counted"));summoner=tag.hasUUID("Summoner")?tag.getUUID("Summoner"):null;read(tag.getList("Roster",Tag.TAG_STRING),optedIn);read(tag.getList("Combatants",Tag.TAG_STRING),combatants);}
 private void read(ListTag list,Set<UUID> set){for(Tag value:list)try{set.add(UUID.fromString(value.getAsString()));}catch(IllegalArgumentException ignored){}}
}
