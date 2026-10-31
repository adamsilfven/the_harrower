package com.leyline.harrower.loot;
import com.leyline.harrower.*;
import com.leyline.harrower.combat.LeylineBridge;
import java.util.*;
import java.lang.reflect.*;
import java.security.*;
import net.minecraft.server.level.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

/** Read-only death-time query. Optional Core classes are resolved only when installed.
 * Separate from the combat adapter: this never installs suppression mixins or changes Core. */
public final class LootLeylines {
 private static boolean warned;
 public static int atDeath(ServerLevel level,Harrower boss,UUID owner){
  if(owner==null)return 0;
  Collection<LeylineBridge.OwnedLeyline> lines;
  if(ModList.get().isLoaded("leyline_convergence")){
   try{lines=core(level,owner);}catch(Exception failure){
    if(!warned){warned=true;org.slf4j.LoggerFactory.getLogger("HarrowerLoot").warn("Optional Core death-time API could not be audited; no bonus Core will be inferred",failure);}return 0;
   }
  }else{var bridge=LeylineBridge.access();if(bridge==null)return 0;lines=bridge.ownedActive(level,owner);}
  Set<LeylineBridge.Color> active=EnumSet.noneOf(LeylineBridge.Color.class);
  for(var line:lines)if(owner.equals(line.owner())&&line.active()&&line.center().distanceToSqr(boss.position())<=64*64)active.add(line.color());
  return active.size();
 }
 private static Collection<LeylineBridge.OwnedLeyline> core(ServerLevel level,UUID owner)throws Exception {
  ClassLoader loader=Thread.currentThread().getContextClassLoader();
  String serviceName="com.leylineconvergence.core.land.LandService",bindingName="com.leylineconvergence.core.land.LandRules$Binding";
  String pair=hash(loader,serviceName)+"/"+hash(loader,bindingName);
  if(!AUDITED.contains(pair))throw new IllegalStateException("Unreviewed Core land API fingerprint: "+pair);
  Class<?> service=Class.forName(serviceName,true,loader),binding=Class.forName(bindingName,true,loader),saved=Class.forName("com.leylineconvergence.core.land.LandSavedData",true,loader);
  Object data=saved.getMethod("get",net.minecraft.server.MinecraftServer.class).invoke(null,level.getServer()),registry=saved.getField("registry").get(data);
  Method get=registry.getClass().getMethod("get",UUID.class,String.class),validate=service.getMethod("validate",net.minecraft.server.MinecraftServer.class,binding);
  ServerPlayer player=level.getServer().getPlayerList().getPlayer(owner);
  com.google.gson.JsonObject progress=null;
  if(player==null){
   String sparkName="com.leylineconvergence.core.spark.SparkSavedData";
   if(!hash(loader,sparkName).equals("d6a3c4759ac1f645ef477008686241ca423f32fd2bf85d75b6ff614f23b388ef"))throw new IllegalStateException("Unreviewed offline Spark API");
   Class<?> spark=Class.forName(sparkName,true,loader);Object entry=spark.getMethod("player",UUID.class).invoke(spark.getMethod("get",net.minecraft.server.MinecraftServer.class).invoke(null,level.getServer()),owner);
   if(!Boolean.TRUE.equals(entry.getClass().getMethod("completed").invoke(entry)))return List.of();
   java.nio.file.Path file=level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve(owner+".json");
   if(!java.nio.file.Files.isRegularFile(file))return List.of();
   try(var reader=java.nio.file.Files.newBufferedReader(file)){progress=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();}
  }
  List<LeylineBridge.OwnedLeyline> result=new ArrayList<>();
  for(var color:LeylineBridge.Color.values()){
   String name=color.name().toLowerCase(Locale.ROOT);Object b=get.invoke(registry,owner,name);if(b==null)continue;
   if(!owner.equals(binding.getMethod("owner").invoke(b))||!Boolean.TRUE.equals(binding.getMethod("leyline").invoke(b))||!level.dimension().location().toString().equals(binding.getMethod("dimension").invoke(b)))continue;
   boolean earned=player!=null?Boolean.TRUE.equals(service.getMethod("access",ServerPlayer.class,String.class).invoke(null,player,name))&&Boolean.TRUE.equals(service.getMethod("earned",ServerPlayer.class,String.class).invoke(null,player,"leyline/"+name+"_access")):offlineEarned(level,progress,"spark/access_"+name)&&offlineEarned(level,progress,"leyline/"+name+"_access");
   if(!earned)continue;
   Object validation=validate.invoke(null,level.getServer(),b);if(!Boolean.TRUE.equals(validation.getClass().getMethod("valid").invoke(validation)))continue;
   double x=((Number)binding.getMethod("x").invoke(b)).doubleValue()+.5,y=((Number)binding.getMethod("y").invoke(b)).doubleValue()+.5,z=((Number)binding.getMethod("z").invoke(b)).doubleValue()+.5;
   result.add(new LeylineBridge.OwnedLeyline(name,owner,color,new Vec3(x,y,z),true));
  }return result;
 }
 private static boolean offlineEarned(ServerLevel level,com.google.gson.JsonObject progress,String path){
  var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("leyline_convergence",path);var holder=level.getServer().getAdvancements().get(id);
  if(holder==null||!progress.has(id.toString()))return false;
  var decoded=net.minecraft.advancements.AdvancementProgress.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,progress.get(id.toString())).result();
  if(decoded.isEmpty())return false;var actual=decoded.get();actual.update(holder.value().requirements());return actual.isDone();
 }
 private static String hash(ClassLoader loader,String name)throws Exception{try(var input=loader.getResourceAsStream(name.replace('.','/')+".class")){if(input==null)throw new ClassNotFoundException(name);return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.readAllBytes()));}}
 private static final Set<String> AUDITED=Set.of(
  "9f1a6717afd6b71edc6d3f43bb24e9e40d2b7c15da8ec8e828909dc8cbe5fe76/e36eb5bcc9f1820520171d6842c2b2681599720fec443dd6659339822d9df8aa");
 private LootLeylines(){}
}
