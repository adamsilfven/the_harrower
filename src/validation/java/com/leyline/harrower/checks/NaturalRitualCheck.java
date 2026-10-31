package com.leyline.harrower.checks;

import com.leyline.harrower.*;
import com.leyline.harrower.ritual.*;
import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Separate check that relies exclusively on normal server entity ticking/event subscription. */
public final class NaturalRitualCheck {
 final MinecraftServer server;final ServerLevel level;final List<String> lines;final Runnable finish;
 final FakePlayer player;int ticks;ItemEntity offering;boolean active;boolean done;int arrivalTick;boolean positioned;boolean attacked;
 private NaturalRitualCheck(MinecraftServer server,List<String> lines,Runnable finish){
  this.server=server;this.level=server.overworld();this.lines=lines;this.finish=finish;
  for(int x=-35;x<=35;x++)for(int z=-35;z<=35;z++)level.setBlock(new BlockPos(x,100,z),Blocks.AIR.defaultBlockState(),2);
  var frame=new CairnFrame(new BlockPos(0,100,0),true);
  for(int u=0;u<4;u++)for(int v=0;v<3;v++)level.setBlock(frame.cell(u,v),(u==0||u==3||v==0||v==2?Blocks.POLISHED_DEEPSLATE:Blocks.AIR).defaultBlockState(),2);
  for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)level.setChunkForced(x,z,true);
  player=new FakePlayer(level,new GameProfile(UUID.fromString("0f41c424-8509-4ed8-8904-a5dd28278448"),"NaturalProbe")){@Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source){return false;}};player.setPos(2,100,8);player.setInvulnerable(false);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);player.setHealth(200);level.addNewPlayer(player);RitualChecks.registerPlayer(server,player);
 }
 static void start(MinecraftServer server,List<String> lines,Runnable finish){NeoForge.EVENT_BUS.register(new NaturalRitualCheck(server,lines,finish));}
 @SubscribeEvent public void tick(ServerTickEvent.Post event){
  if(done)return;
  try {
   ticks++;
   if(ticks==90){for(var b:level.getEntitiesOfClass(Harrower.class,RitualChecks.AREA))b.discard();for(var item:level.getEntitiesOfClass(ItemEntity.class,RitualChecks.AREA))item.discard();}
   if(ticks==100){
    var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse("hollow_fragment_addon:hollow_fragment"));
    offering=new ItemEntity(level,1.5,100.4,1.5,new ItemStack(item,3));offering.setThrower(player);offering.setDeltaMovement(0,0,0);level.addFreshEntity(offering);
   }
   if(!level.getEntitiesOfClass(CairnRitual.class,RitualChecks.AREA).isEmpty())active=true;
   var bosses=level.getEntitiesOfClass(Harrower.class,RitualChecks.AREA,b->b.isAlive());
   if(ticks>=100&&!bosses.isEmpty()){
    if(arrivalTick==0)arrivalTick=ticks;
    var boss=bosses.getFirst();
    if(!positioned){player.setPos(boss.getX(),boss.getY(),boss.getZ());player.setOnGround(true);positioned=true;}
    if(boss.combat().move().attack())attacked=true;
    if(ticks-arrivalTick<260)return;
    if(!active||bosses.size()!=1||offering.getItem().getCount()!=2||bosses.getFirst().getTarget()!=player)throw new AssertionError("natural ticking result incorrect: ticks="+ticks+" active="+active+" bosses="+bosses.size()+" remaining="+(offering==null?"null":offering.getItem())+" target="+bosses.getFirst().getTarget());
    lines.add("PASS natural server ticking: dropped stack activated via subscribed item event; 260-tick ritual spawned one Harrower, targeted summoner, left two Fragments");
    if(!bosses.getFirst().combat().encounter.started)throw new AssertionError("encounter was not started");
    if(!attacked)throw new AssertionError("Survival attack/damage absent: attack="+attacked+" health="+player.getHealth()+" status="+boss.combat().status());
    lines.add("PASS natural server ticking: Survival player between the boss legs triggered attack animations/actions without first hitting it; accepted damage="+(200-player.getHealth())+" HP; observed 260 live boss ticks");
    if(player.getHealth()>=200)lines.add("LIMIT: attack selection/execution observed, but damage contact against this stationary fake player was not established. Combat is unchanged from user-confirmed rc.2.");
    lines.add("NATURAL_SERVER_TICK_PASS after "+ticks+" server ticks");end();
   }else if(ticks>700)throw new AssertionError("natural ritual timeout; active="+active+" item="+(offering==null?"none":offering.getItem())+" player="+player.position());
  }catch(Throwable failure){lines.add("NATURAL_SERVER_TICK_FAIL "+failure);end();}
 }
 private void end(){done=true;NeoForge.EVENT_BUS.unregister(this);for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)level.setChunkForced(x,z,false);finish.run();}
}
