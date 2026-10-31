package com.leyline.harrower.checks;

import com.leyline.harrower.*;
import com.leyline.harrower.ritual.*;
import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Runs against the installed binary on a real dedicated server. Method-driven integration,
 * not a claim of human/client gameplay or natural entity-tick simulation. */
public final class RitualChecks {
 static List<String> lines;
 static ServerLevel level;static FakePlayer player;
 static final AABB AREA=new AABB(-45,95,-45,45,120,45);
 static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);lines.add("PASS ritual: "+label);}
 static List<CairnRitual> rituals(){return level.getEntitiesOfClass(CairnRitual.class,AREA,e->!e.isRemoved());}
 static ItemEntity offer(CairnFrame frame,int cell,int count){
  var pos=Vec3.atCenterOf(frame.cell(cell,1));var stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("hollow_fragment_addon:hollow_fragment")),count);
  var item=new ItemEntity(level,pos.x,pos.y,pos.z,stack);item.setThrower(player);item.tickCount=5;level.addFreshEntity(item);net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(item));return item;
 }
 static void frame(CairnFrame f){for(int u=0;u<4;u++)for(int v=0;v<3;v++)level.setBlock(f.cell(u,v),(u==0||u==3||v==0||v==2?Blocks.POLISHED_DEEPSLATE:Blocks.AIR).defaultBlockState(),3);}
 static void clean(){for(var r:rituals())r.cancel("test cleanup");for(Entity e:level.getEntitiesOfClass(Entity.class,AREA))if(e!=player)e.discard();}
 public static void run(MinecraftServer server,List<String> evidence){
  lines=evidence;level=server.overworld();server.setDifficulty(Difficulty.NORMAL,true);
  for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)level.getChunk(x,z);
  for(int x=-40;x<=40;x++)for(int z=-40;z<=40;z++){level.setBlock(new BlockPos(x,99,z),Blocks.STONE.defaultBlockState(),2);level.setBlock(new BlockPos(x,100,z),Blocks.AIR.defaultBlockState(),2);}
  player=new FakePlayer(level,new GameProfile(UUID.fromString("7a955219-9dd5-44cb-ad21-8ea4b2903671"),"RitualProbe"));player.setPos(2,100,8);player.setYRot(0);level.addNewPlayer(player);registerPlayer(server,player);
  check(level.getPlayerByUUID(player.getUUID())==player,"summoner attribution resolves a server player");
  CairnFrame f=new CairnFrame(new BlockPos(0,100,0),true);frame(f);
  check(f.valid(level),"ten-block X frame accepted");
  level.setBlock(f.cell(0,0),Blocks.AIR.defaultBlockState(),3);ItemEntity malformed=offer(f,1,2);check(rituals().isEmpty()&&malformed.getItem().getCount()==2,"incomplete frame preserves offering");frame(f);malformed.discard();
  level.setBlock(f.cell(2,1),Blocks.WATER.defaultBlockState(),3);check(!f.valid(level),"fluid in either opening rejected");frame(f);
  level.setBlock(f.cell(1,1).above(),Blocks.STONE.defaultBlockState(),3);check(!f.valid(level),"obstructed opening rejected");level.setBlock(f.cell(1,1).above(),Blocks.AIR.defaultBlockState(),3);
  check(CairnSites.find(level,player,f)!=null,"open surface has a validated arrival site");
  var unprovoked=HarrowerMod.HARROWER.get().create(level);unprovoked.setPos(2,100,24);level.addFreshEntity(unprovoked);unprovoked.combat().tick();
  check(unprovoked.combat().encounter.started&&unprovoked.getTarget()==player,"non-ritual Harrower engages nearby Survival player without being hit");
  player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);unprovoked.combat().tick();check(unprovoked.getTarget()==null,"automatic hostility excludes Creative players");
  player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);unprovoked.combat().tick();check(unprovoked.getTarget()==player,"automatic hostility reacquires player when Survival resumes");unprovoked.discard();
  var sideBlock=new BlockPos(6,100,23);level.setBlock(sideBlock,Blocks.STONE.defaultBlockState(),3);
  check(CairnSites.valid(level,player,f,new Vec3(2.5,100,23.5)),"terrain-safe site accepted with obstruction outside physical body but inside arm silhouette");level.setBlock(sideBlock,Blocks.AIR.defaultBlockState(),3);
  for(int x=-2;x<=7;x++)for(int z=20;z<=27;z++)level.setBlock(new BlockPos(x,99,z),Blocks.STONE_SLAB.defaultBlockState(),3);
  check(CairnSites.valid(level,player,f,new Vec3(2.5,99.5,23.5)),"stable slab-height arrival accepted without full-block-only floor rule");
  for(int x=-2;x<=7;x++)for(int z=20;z<=27;z++)level.setBlock(new BlockPos(x,99,z),Blocks.STONE.defaultBlockState(),3);
  var item=offer(f,1,3);check(rituals().size()==1&&item.getItem().getCount()==2,"stacked offering reserves exactly one");
  var other=offer(f,2,2);check(rituals().size()==1&&other.getItem().getCount()==2,"competing offering remains untouched");
  CairnRitual ritual=rituals().getFirst();
  CompoundTag saved=new CompoundTag();ritual.saveWithoutId(saved);var restored=CairnRegistry.TYPE.create(level);restored.load(saved);
  check(restored.age()==0&&restored.alongX()&&saved.getCompound("ReservedFragment").getInt("count")==1,"ritual NBT retains escrow, orientation, owner and arrival site");
  for(int i=0;i<259;i++)ritual.tick();check(level.getEntitiesOfClass(Harrower.class,AREA,e->e.isAlive()).isEmpty(),"arrival waits until tick 260");ritual.tick();
  var bosses=level.getEntitiesOfClass(Harrower.class,AREA,e->e.isAlive());check(bosses.size()==1&&ritual.isRemoved(),"one Harrower spawns and ritual commits once");
  var boss=bosses.getFirst();check(boss.getTarget()==player,"spawn immediately targets original summoner");
  check(boss.combat().encounter.started&&player.getUUID().equals(boss.combat().encounter.summoner),"arrival starts encounter and records summoner");
  boss.combat().tick();check(boss.getTarget()==player,"combat controller retains summoner on its next tick");check(boss.distanceTo(player)>=12&&boss.distanceTo(player)<=19,"arrival is within the approved nearby band");
  check(boss.getLookAngle().dot(player.position().subtract(boss.position()).normalize())>.99,"arrival faces summoner");
  check(!CairnLocks.get(level).near(f.center()),"successful arrival releases persistent lock");
  check(item.getItem().getCount()==2&&other.getItem().getCount()==2,"arrival never consumes a second item");clean();
  var one=offer(f,2,1);check(one.isRemoved()&&rituals().size()==1,"second opening accepts a single item");ritual=rituals().getFirst();level.setBlock(f.cell(0,0),Blocks.AIR.defaultBlockState(),3);ritual.tick();
  check(ritual.isRemoved()&&level.getEntitiesOfClass(ItemEntity.class,AREA).stream().mapToInt(e->e.getItem().getCount()).sum()==1,"broken frame refunds exactly one");
  check(!CairnLocks.get(level).near(f.center()),"cancellation releases persistent lock");
  for(var refund:level.getEntitiesOfClass(ItemEntity.class,AREA)){refund.tickCount=5;net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(refund));}check(rituals().isEmpty(),"refund does not automatically retrigger");clean();frame(f);
  ritual=begin(f);player.setPos(2,100,70);ritual.tick();check(ritual.isRemoved(),"departed summoner cancels with refund");clean();player.setPos(2,100,8);
  ritual=begin(f);CompoundTag data=new CompoundTag();ritual.saveWithoutId(data);ritual.discard();restored=CairnRegistry.TYPE.create(level);restored.load(data);level.addFreshEntity(restored);restored.tick();check(!restored.isRemoved()&&restored.age()==1,"saved active ritual reloads and resumes after revalidation");restored.cancel("test");clean();
  frame(f);ritual=begin(f);data=new CompoundTag();ritual.saveWithoutId(data);
  var arrival=BlockPos.containing(data.getDouble("SiteX"),data.getDouble("SiteY"),data.getDouble("SiteZ"));level.setBlock(arrival,Blocks.STONE.defaultBlockState(),3);
  for(int t=0;t<260;t++)ritual.tick();check(ritual.isRemoved()&&level.getEntitiesOfClass(Harrower.class,AREA,e->e.isAlive()).size()==1,"blocked reserved site relocates to another validated nearby arrival");
  check(level.getEntitiesOfClass(ItemEntity.class,AREA).isEmpty(),"successful relocation consumes only its reserved offering");level.setBlock(arrival,Blocks.AIR.defaultBlockState(),3);clean();
  ritual=begin(f);ritual.kill();check(ritual.isRemoved()&&!CairnLocks.get(level).near(f.center()),"administrative kill cancels with refund and unlocks Cairn");clean();
  frame(f);level.setBlock(f.cell(0,0),Blocks.AIR.defaultBlockState(),3);var fz=new CairnFrame(new BlockPos(0,100,0),false);frame(fz);check(fz.valid(level),"ten-block Z frame accepted");ritual=begin(fz);check(!ritual.alongX(),"Z orientation synchronized");ritual.cancel("test");clean();
  // Flood the outdoor support surface; no valid spawn can be accepted.
  for(int x=-35;x<=35;x++)for(int z=-35;z<=35;z++)if(Math.abs(x)>4||Math.abs(z)>4)level.setBlock(new BlockPos(x,100,z),Blocks.WATER.defaultBlockState(),2);
  check(CairnSites.find(level,player,fz)==null,"water-covered arrival region rejected");var failed=offer(fz,1,2);check(failed.getItem().getCount()==2&&rituals().isEmpty(),"invalid arrival leaves entire stack recoverable");clean();
  player.discard();lines.add("RITUAL_SERVER_INTEGRATION_PASS");
 }
 static void registerPlayer(MinecraftServer server,ServerPlayer player){
  try {var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");field.setAccessible(true);((java.util.Map<java.util.UUID,ServerPlayer>)field.get(server.getPlayerList())).put(player.getUUID(),player);}catch(ReflectiveOperationException e){throw new RuntimeException(e);}
 }
 static CairnRitual begin(CairnFrame f){offer(f,1,1);check(rituals().size()==1,"reusable Cairn starts with a new Fragment");return rituals().getFirst();}
}
