package com.leyline.harrower.integration;

import com.google.gson.*;
import com.leyline.harrower.HarrowerMod;
import com.leyline.harrower.combat.LeylineBridge;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.advancements.AdvancementProgress;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Verified APIs of LC Core 0.9.3.7-balance.1 / 0.9.3.8. Reflection keeps the Core dependency optional.
 * Does not edit Core JARs, block states, ownership, advancements, configs or Pufferfish skills.
 */
@EventBusSubscriber(modid=HarrowerMod.ID)
public final class CoreLeylineAdapter implements LeylineBridge.Access {
 private record Lease(UUID owner,Set<String> identities,Set<String> colors,long expires){}
 private record Progress(long checked,JsonObject json){}
 private record Validation(long checked,boolean valid){}
 private final Map<String,Validation> structuralCache=new HashMap<>();
 private static CoreLeylineAdapter instance;
 private final Map<UUID,Lease> leases=new HashMap<>();private final Map<UUID,Progress> offlineProgress=new HashMap<>();
 private final Class<?> binding,landSaved,landService,sparkSaved,endgame;
 private final Method landGet,registryGet,validate,access,earned,sparkGet,sparkPlayer,completed,refresh;
 private final Field registry;
 private final Map<String,Method> getters=new HashMap<>();
 private MinecraftServer server;
 private CoreLeylineAdapter(MinecraftServer server)throws ReflectiveOperationException{
  this.server=server;ClassLoader loader=Thread.currentThread().getContextClassLoader();
  binding=Class.forName("com.leylineconvergence.core.land.LandRules$Binding",true,loader);
  landSaved=Class.forName("com.leylineconvergence.core.land.LandSavedData",true,loader);
  landService=Class.forName("com.leylineconvergence.core.land.LandService",true,loader);
  sparkSaved=Class.forName("com.leylineconvergence.core.spark.SparkSavedData",true,loader);
  endgame=Class.forName("com.leylineconvergence.core.endgame.EndgameService",true,loader);
  landGet=landSaved.getMethod("get",MinecraftServer.class);registry=landSaved.getField("registry");registryGet=registry.getType().getMethod("get",UUID.class,String.class);
  validate=landService.getMethod("validate",MinecraftServer.class,binding);access=landService.getMethod("access",ServerPlayer.class,String.class);earned=landService.getMethod("earned",ServerPlayer.class,String.class);
  sparkGet=sparkSaved.getMethod("get",MinecraftServer.class);sparkPlayer=sparkSaved.getMethod("player",UUID.class);completed=sparkPlayer.getReturnType().getMethod("completed");refresh=endgame.getMethod("refresh",ServerPlayer.class,boolean.class);
  for(String name:new String[]{"owner","color","dimension","x","y","z","leyline"})getters.put(name,binding.getMethod(name));
 }
 @SubscribeEvent public static void start(ServerStartedEvent event){
  if(!ModList.get().isLoaded("leyline_convergence"))return;
  if(!CoreFingerprint.matches()||!IntegrationFlags.coreMixinApplied){org.slf4j.LoggerFactory.getLogger("HarrowerCore").error("LC Core bytecode does not match the audited 0.9.3.7-balance.1 / 0.9.3.8 adapter. Leyline integration is disabled; Core remains unchanged.");return;}
  try{instance=new CoreLeylineAdapter(event.getServer());LeylineBridge.install(instance);org.slf4j.LoggerFactory.getLogger("HarrowerCore").info("Audited LC Core Leyline adapter enabled: {}",instance.auditedVersion());}catch(ReflectiveOperationException exception){throw new IllegalStateException("Audited LC Core API did not match",exception);}
 }
 @SubscribeEvent public static void tick(ServerTickEvent.Post event){if(instance!=null)instance.expire();}
 @SubscribeEvent public static void stop(ServerStoppingEvent event){if(instance!=null){Set<UUID> owners=new HashSet<>();for(var lease:instance.leases.values())owners.add(lease.owner);instance.leases.clear();for(UUID owner:owners)instance.refreshOwner(owner);}}
 @SubscribeEvent public static void stopped(ServerStoppedEvent event){LeylineBridge.uninstall(instance);instance=null;}
 public static boolean ownerColorSuppressed(UUID owner,String color){if(instance==null)return false;for(var lease:instance.leases.values())if(lease.owner.equals(owner)&&lease.colors.contains(color))return true;return false;}
 public static boolean bindingSuppressed(Object binding){if(instance==null||binding==null)return false;try{String identity=instance.identity(binding);for(var lease:instance.leases.values())if(lease.identities.contains(identity))return true;return false;}catch(ReflectiveOperationException exception){throw new IllegalStateException(exception);}}
 private Object get(Object b,String name)throws ReflectiveOperationException{return getters.get(name).invoke(b);}
 private String identity(Object b)throws ReflectiveOperationException{return get(b,"owner")+"/"+get(b,"color")+"/"+get(b,"dimension")+"/"+get(b,"x")+","+get(b,"y")+","+get(b,"z");}
 @Override public Collection<LeylineBridge.OwnedLeyline> ownedActive(ServerLevel level,UUID owner){
  List<LeylineBridge.OwnedLeyline> result=new ArrayList<>();ServerPlayer online=server.getPlayerList().getPlayer(owner);
  try{
   Object data=landGet.invoke(null,server),registrations=registry.get(data);
   boolean awakened=online!=null||Boolean.TRUE.equals(completed.invoke(sparkPlayer.invoke(sparkGet.invoke(null,server),owner)));if(!awakened)return result;
   for(var color:LeylineBridge.Color.values()){
    String name=color.name().toLowerCase(Locale.ROOT);Object b=registryGet.invoke(registrations,owner,name);
    if(b==null||!owner.equals(get(b,"owner"))||!Boolean.TRUE.equals(get(b,"leyline"))||!level.dimension().location().toString().equals(get(b,"dimension")))continue;
    boolean eligible=online!=null?Boolean.TRUE.equals(access.invoke(null,online,name))&&Boolean.TRUE.equals(earned.invoke(null,online,"leyline/"+name+"_access")):offlineEarned(owner,"spark/access_"+name)&&offlineEarned(owner,"leyline/"+name+"_access");
    if(!eligible)continue;String key=identity(b);long time=server.overworld().getGameTime();Validation cached=structuralCache.get(key);
    if(cached==null||time-cached.checked>=20){Object structural=validate.invoke(null,server,b);cached=new Validation(time,Boolean.TRUE.equals(structural.getClass().getMethod("valid").invoke(structural)));if(structuralCache.size()>512)structuralCache.clear();structuralCache.put(key,cached);}if(!cached.valid)continue;
    Vec3 center=new Vec3(((Number)get(b,"x")).doubleValue()+.5,((Number)get(b,"y")).doubleValue()+.5,((Number)get(b,"z")).doubleValue()+.5);
    result.add(new LeylineBridge.OwnedLeyline(identity(b),owner,color,center,true));
   }
  }catch(ReflectiveOperationException exception){throw new IllegalStateException("Audited Core ownership query failed",exception);}
  return result;
 }
 private boolean offlineEarned(UUID owner,String path){
  long now=server.overworld().getGameTime();Progress progress=offlineProgress.get(owner);
  if(progress==null||now-progress.checked>1200){Path file=server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve(owner+".json");JsonObject json=new JsonObject();try{if(Files.isRegularFile(file))try(var reader=Files.newBufferedReader(file)){json=JsonParser.parseReader(reader).getAsJsonObject();}}catch(Exception exception){org.slf4j.LoggerFactory.getLogger("HarrowerCore").warn("Cannot read offline owner advancement eligibility",exception);}progress=new Progress(now,json);offlineProgress.put(owner,progress);}
  ResourceLocation id=ResourceLocation.fromNamespaceAndPath("leyline_convergence",path);var holder=server.getAdvancements().get(id);if(holder==null||!progress.json.has(id.toString()))return false;
  var decoded=AdvancementProgress.CODEC.parse(JsonOps.INSTANCE,progress.json.get(id.toString())).result();if(decoded.isEmpty())return false;var actual=decoded.get();actual.update(holder.value().requirements());return actual.isDone();
 }
 @Override public void updateSuppression(ServerLevel level,UUID boss,Collection<LeylineBridge.OwnedLeyline> eligible,long expires){
  Lease old=leases.get(boss);Set<String> ids=new HashSet<>(),colors=new HashSet<>();UUID owner=null;
  for(var leyline:eligible){owner=leyline.owner();ids.add(leyline.identity());colors.add(leyline.color().name().toLowerCase(Locale.ROOT));}
  if(ids.isEmpty())leases.remove(boss);else leases.put(boss,new Lease(owner,Set.copyOf(ids),Set.copyOf(colors),expires));
  if(old==null?!ids.isEmpty():!old.identities.equals(ids)){if(old!=null)refreshOwner(old.owner);if(owner!=null&&(old==null||!owner.equals(old.owner)))refreshOwner(owner);else if(owner!=null&&old==null)refreshOwner(owner);}
 }
 @Override public void release(ServerLevel level,UUID boss){Lease old=leases.remove(boss);if(old!=null)refreshOwner(old.owner);}
 private void expire(){long now=server.overworld().getGameTime();Set<UUID> owners=new HashSet<>();leases.values().removeIf(lease->{if(lease.expires<=now){owners.add(lease.owner);return true;}return false;});for(UUID owner:owners)refreshOwner(owner);}
 private void refreshOwner(UUID owner){ServerPlayer player=server.getPlayerList().getPlayer(owner);if(player==null)return;offlineProgress.remove(owner);try{refresh.invoke(null,player,false);}catch(ReflectiveOperationException exception){throw new IllegalStateException("Cannot refresh suspended Core effects",exception);}}
 @Override public String auditedVersion(){return "LC Core 0.9.3.7-balance.1 / 0.9.3.8 / 82baa0b0c0396c53ac1080f43446ace99d99f7ae767b574e82b5279ec2dc684e";}
}
