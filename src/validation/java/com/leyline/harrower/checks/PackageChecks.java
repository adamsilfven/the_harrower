package com.leyline.harrower.checks;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Mob;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
@Mod("harrower_package_checks")
public final class PackageChecks {
 private final List<String> lines=new ArrayList<>();
 public PackageChecks(){NeoForge.EVENT_BUS.register(this);}
 private void check(boolean value,String label){if(!value)throw new AssertionError(label);lines.add("PASS "+label);}
 @SubscribeEvent(priority=EventPriority.LOWEST)
 public void started(ServerStartedEvent event) {
  var server=event.getServer();boolean deferred=false;
  try {
   var mods=ModList.get();
   var owners=new HashSet<Object>();
   for(String id:List.of("lc_harrower","lc_hollowforged","hollow_fragment_addon")) {
    check(mods.isLoaded(id),"module loaded "+id);
    var info=mods.getModContainerById(id).orElseThrow().getModInfo();
    owners.add(info.getOwningFile().getFile().getFilePath());
   }
   check(owners.size()==1,"all three modules came from ONE physical JAR: "+owners);
   for(String id:List.of("lc_harrower:harrower_spawn_egg","lc_hollowforged:hollow_core","lc_hollowforged:crown_of_the_hollow","lc_hollowforged:carapace_of_the_unbroken","lc_hollowforged:greaves_of_the_stillborn","lc_hollowforged:treads_of_the_worldbreaker","lc_hollowforged:worldshard","hollow_fragment_addon:hollow_fragment")) {
    var key=ResourceLocation.parse(id);check(BuiltInRegistries.ITEM.containsKey(key),"original item ID registered "+id);
    var stack=new ItemStack(BuiltInRegistries.ITEM.get(key));
    if(id.startsWith("lc_hollowforged:")&&!id.endsWith("hollow_core"))check(stack.has(DataComponents.UNBREAKABLE),"relic remains unbreakable "+id);
    if(id.equals("hollow_fragment_addon:hollow_fragment"))check(stack.getMaxStackSize()==16,"fragment stack limit remains 16");
   }
   var entityKey=ResourceLocation.parse("lc_harrower:harrower");
   check(BuiltInRegistries.ENTITY_TYPE.containsKey(entityKey),"original Harrower entity ID registered");
   var type=BuiltInRegistries.ENTITY_TYPE.get(entityKey);
   var boss=type.create(server.overworld());
   check(boss instanceof Mob,"Harrower entity instantiates");
   check(Math.abs(boss.getBbWidth()-8.8)<.001&&Math.abs(boss.getBbHeight()-11.5)<.001,"Harrower dimensions remain 8.8 by 11.5");
   for(String cmd:List.of("harrower","hollowforged"))check(server.getCommands().getDispatcher().getRoot().getChild(cmd)!=null,"command registered "+cmd);
   var foundation=Path.of("hollowforged-foundation-checks.txt");
   check(Files.exists(foundation),"foundation evidence exists");
   var text=Files.readString(foundation);check(text.contains("assertions passed")&&!text.contains("FAIL "),"existing Hollowforged foundation suite passes");
   RitualChecks.run(server,lines);
   LootChecks.run(server,lines);
   lines.add("PACKAGE_PASS "+lines.size()+" checks");
   if(Boolean.getBoolean("lc_harrower.delayedVerify")){NaturalDelayedDropCheck.start(server,lines,()->finish(server));deferred=true;}else if(!Boolean.getBoolean("lc_hollowforged.verify")){NaturalRitualCheck.start(server,lines,()->finish(server));deferred=true;}
  } catch(Throwable failure) {
   var detail=new java.io.StringWriter();failure.printStackTrace(new java.io.PrintWriter(detail));lines.add("PACKAGE_FAIL "+detail);
  } finally {if(!deferred)finish(server);}
 }
 private void finish(net.minecraft.server.MinecraftServer server){
   try {Files.write(Path.of("harrower-package-checks.txt"),lines);}catch(Exception e){throw new RuntimeException(e);}
   org.slf4j.LoggerFactory.getLogger("HarrowerPackageChecks").info(String.join("\n",lines));
   server.halt(false);
 }
}
