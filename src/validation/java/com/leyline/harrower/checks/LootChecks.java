package com.leyline.harrower.checks;
import com.leyline.harrower.*;
import com.leyline.harrower.combat.LeylineBridge;
import com.leyline.hollowforged.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.*;

public final class LootChecks {
 static List<String> lines;static MinecraftServer server;static ServerLevel level;
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);lines.add("PASS loot/core: "+message);}
 static int amount(List<ItemStack> stacks,Item item){return stacks.stream().filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum();}
 static Map<String,Integer> ench(ItemStack stack){var component=stack.get(DataComponents.ENCHANTMENTS);Map<String,Integer> result=new TreeMap<>();if(component!=null)for(var e:component.entrySet())result.put(e.getKey().unwrapKey().orElseThrow().location().getPath(),e.getIntValue());return result;}
 static boolean weapon(ItemStack s){return s.is(Items.NETHERITE_SWORD)||s.is(Items.MACE)||s.is(Items.TRIDENT)||s.is(Items.BOW)||s.is(Items.CROSSBOW)||s.is(Items.NETHERITE_AXE)&&ench(s).containsKey("sharpness");}
 static boolean tool(ItemStack s){return (s.is(Items.NETHERITE_PICKAXE)||s.is(Items.NETHERITE_AXE)||s.is(Items.NETHERITE_SHOVEL))&&!weapon(s);}
 static void enchantments(ItemStack stack){
  var component=stack.get(DataComponents.ENCHANTMENTS);if(component==null||component.isEmpty()||!stack.hasFoil())throw new AssertionError("missing enchants/glint "+stack);
  var keys=new ArrayList<>(component.keySet());for(var enchant:keys){if(!enchant.value().canEnchant(stack)||component.getLevel(enchant)!=enchant.value().getMaxLevel())throw new AssertionError("wrong item or non-max enchantment "+stack);}
  for(var a:keys)for(var b:keys)if(a!=b&&!Enchantment.areCompatible(a,b))throw new AssertionError("incompatible enchantments "+stack);
  var e=ench(stack);if(e.getOrDefault("unbreaking",0)!=3||(!e.containsKey("infinity")&&e.getOrDefault("mending",0)!=1))throw new AssertionError("quality floor");
  if(stack.is(Items.MACE)&&e.containsKey("density")==e.containsKey("breach"))throw new AssertionError("mace exclusivity");
  if(stack.is(Items.TRIDENT)&&e.containsKey("riptide")==e.containsKey("loyalty"))throw new AssertionError("trident exclusivity");
  if(stack.is(Items.BOW)&&e.containsKey("infinity")==e.containsKey("mending"))throw new AssertionError("bow exclusivity");
  if(stack.is(Items.CROSSBOW)&&e.containsKey("piercing")==e.containsKey("multishot"))throw new AssertionError("crossbow exclusivity");
  if(tool(stack)&&e.containsKey("fortune")==e.containsKey("silk_touch"))throw new AssertionError("tool exclusivity");
  if(stack.is(Items.NETHERITE_BOOTS)&&e.containsKey("depth_strider")==e.containsKey("frost_walker"))throw new AssertionError("boots exclusivity");
 }
 public static void run(MinecraftServer host,List<String> evidence){
  server=host;level=host.overworld();lines=evidence;level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT).set(true,server);
  var type=HarrowerMod.HARROWER.get();var boss=type.create(level);boss.setPos(2,100,20);
  var key=ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.parse("lc_harrower:entities/harrower"));var table=server.reloadableRegistries().getLootTable(key);
  check(table!=LootTable.EMPTY,"native Harrower entity loot table loaded");
  var params=new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY,boss).withParameter(LootContextParams.ORIGIN,boss.position()).withParameter(LootContextParams.DAMAGE_SOURCE,boss.damageSources().generic()).create(LootContextParamSets.ENTITY);
  int n=10000;int[] successes=new int[9],slots=new int[4],families=new int[6];int fortune=0,silk=0,maxRows=0;
  Set<Item> allowed=new HashSet<>(List.of(Items.ENCHANTED_GOLDEN_APPLE,Items.NETHERITE_INGOT,Items.NETHERITE_SWORD,Items.NETHERITE_HELMET,Items.NETHERITE_CHESTPLATE,Items.NETHERITE_LEGGINGS,Items.NETHERITE_BOOTS,Items.NETHER_STAR,Items.ELYTRA,Items.TOTEM_OF_UNDYING,Items.HEAVY_CORE,Items.NETHERITE_PICKAXE,Items.NETHERITE_AXE,Items.NETHERITE_SHOVEL,Items.MACE,Items.TRIDENT,Items.BOW,Items.CROSSBOW,Items.ENCHANTED_BOOK,Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE,Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE,Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE,Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE,Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE,Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE,Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
  for(int i=1;i<=n;i++){
   var stacks=table.getRandomItems(params,i);int apples=amount(stacks,Items.ENCHANTED_GOLDEN_APPLE),ingots=amount(stacks,Items.NETHERITE_INGOT);
   if(apples<2||apples>8||ingots<4||ingots>8)throw new AssertionError("guaranteed ranges");
   int[] row={stacks.stream().filter(LootChecks::weapon).mapToInt(ItemStack::getCount).sum(),amount(stacks,Items.NETHERITE_HELMET)+amount(stacks,Items.NETHERITE_CHESTPLATE)+amount(stacks,Items.NETHERITE_LEGGINGS)+amount(stacks,Items.NETHERITE_BOOTS),apples>=5?1:0,amount(stacks,Items.NETHER_STAR),amount(stacks,Items.ELYTRA),amount(stacks,Items.TOTEM_OF_UNDYING),amount(stacks,Items.HEAVY_CORE),stacks.stream().filter(LootChecks::tool).mapToInt(ItemStack::getCount).sum(),stacks.stream().filter(s->!s.isDamageableItem()&&!s.is(Items.ENCHANTED_GOLDEN_APPLE)&&!s.is(Items.NETHERITE_INGOT)&&!s.is(Items.NETHER_STAR)&&!s.is(Items.TOTEM_OF_UNDYING)&&!s.is(Items.HEAVY_CORE)).mapToInt(ItemStack::getCount).sum()};
   // Apple bonus can total four; count that row separately by a deterministic all-success sample below.
   int rows=0;for(int r=0;r<9;r++)if(row[r]>0){successes[r]++;rows++;}maxRows=Math.max(maxRows,rows);
   if(row[3]>2||row[5]>4||row[5]==1)throw new AssertionError("bonus quantity");
   for(ItemStack stack:stacks){if(!allowed.contains(stack.getItem())||stack.getCount()>stack.getMaxStackSize())throw new AssertionError("forbidden/uncollectible loot "+stack);
    if(stack.isDamageableItem()&&!stack.is(Items.ELYTRA))enchantments(stack);
    Item[] armor={Items.NETHERITE_HELMET,Items.NETHERITE_CHESTPLATE,Items.NETHERITE_LEGGINGS,Items.NETHERITE_BOOTS};for(int j=0;j<4;j++)if(stack.is(armor[j]))slots[j]++;
    if(weapon(stack)){Item[] ws={Items.NETHERITE_SWORD,Items.NETHERITE_AXE,Items.MACE,Items.TRIDENT,Items.BOW,Items.CROSSBOW};for(int j=0;j<6;j++)if(stack.is(ws[j]))families[j]++;}
    if(stack.is(Items.ENCHANTED_BOOK)){var book=stack.get(DataComponents.STORED_ENCHANTMENTS);if(book==null||book.size()!=1||!stack.hasFoil())throw new AssertionError("invalid enchanted book");}
    if(tool(stack)){if(ench(stack).containsKey("fortune"))fortune++;else silk++;}
   }
  }
  double[] probabilities={.65,.8,0,.5,.35,.75,.4,.45,.35};
  for(int r=0;r<9;r++)if(r!=2)check(Math.abs(successes[r]/(double)n-probabilities[r])<.025,"10000 seeded native rolls: row "+r+" observed "+successes[r]+" expected "+probabilities[r]);
  for(int f:families)check(Math.abs(f/(double)successes[0]-1.0/6)<.03,"weapon family equal weighting ~1/6");
  check(maxRows>=7,"independent bonus rows co-occur, not exclusive tiers");
  for(int count:slots)check(Math.abs(count/(double)successes[1]-.25)<.03,"netherite armor slot weighting ~25%");
  check(Math.abs(fortune/(double)(fortune+silk)-.5)<.03,"Fortune/Silk Touch split ~50%, never combined");
  check(true,"10000 rolls: vanilla-only items, valid compatible enchantments/glint, collectible stack limits, guaranteed ranges");
  // Inspect exact pool chance for apples, whose base and bonus quantities can overlap.
  try(var in=LootChecks.class.getClassLoader().getResourceAsStream("data/lc_harrower/loot_table/entities/harrower.json")){
   var json=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in)).getAsJsonObject();check(json.getAsJsonArray("pools").get(4).getAsJsonObject().getAsJsonArray("conditions").get(0).getAsJsonObject().get("chance").getAsDouble()==.6,"apple bonus is an independent 60% pool");
  }catch(Exception e){throw new RuntimeException(e);}
  var core=new ItemStack(Hollowforged.HOLLOW_CORE.get());var tip=core.getTooltipLines(Item.TooltipContext.of(level),null,TooltipFlag.NORMAL);
  check(tip.stream().anyMatch(c->c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t&&t.getKey().equals("tooltip.lc_hollowforged.hollow_core.use")),"Core tooltip describes recipe use");
  var lucky=new LootParams.Builder(level).withLuck(100).withParameter(LootContextParams.THIS_ENTITY,boss).withParameter(LootContextParams.ORIGIN,boss.position()).withParameter(LootContextParams.DAMAGE_SOURCE,boss.damageSources().generic()).create(LootContextParamSets.ENTITY);
  for(int seed=1;seed<=100;seed++){var normal=table.getRandomItems(params,seed);var boosted=table.getRandomItems(lucky,seed);if(normal.size()!=boosted.size())throw new AssertionError("Luck modified quantity");for(int i=0;i<normal.size();i++)if(!ItemStack.matches(normal.get(i),boosted.get(i)))throw new AssertionError("Luck modified loot");}
  check(true,"100 deterministic seeds produce identical loot at Luck 0 and Luck 100");
  deathTests();lines.add("HOLLOW_CORE_LOOT_PASS");
 }
 static void deathTests(){
  var player=RitualChecks.player;player.setPos(2,100,8);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
  var bridge=new TestBridge(player.getUUID());var existing=LeylineBridge.access();if(existing!=null)throw new AssertionError("standalone test expects no LC adapter");LeylineBridge.install(bridge);
  try {
   for(int colors:new int[]{0,1,5}){
    var boss=HarrowerMod.HARROWER.get().create(level);boss.setPos(2,100,20);bridge.center=boss.position();bridge.colors=0;bridge.active=true;
    // Change between summon/setup and death: only the instant-of-death value counts.
    bridge.colors=colors;boss.setHealth(0);NeoForge.EVENT_BUS.post(new LivingDeathEvent(boss,player.damageSources().playerAttack(player)));
    var d=boss.getPersistentData().getCompound("hollowforged_encounter");check(d.getInt("death_colors")==colors,"death snapshot observes "+colors+" current active Leylines");
    bridge.colors=0;bridge.active=false;var drops=new ArrayList<ItemEntity>();NeoForge.EVENT_BUS.post(new LivingDropsEvent(boss,player.damageSources().playerAttack(player),drops,true));
    check(drops.isEmpty()&&amount(escrow(boss),Hollowforged.HOLLOW_CORE.get())==d.getInt("death_cores"),"Core drop uses frozen death snapshot after Leylines change");
    var repeated=new ArrayList<ItemEntity>();NeoForge.EVENT_BUS.post(new LivingDropsEvent(boss,player.damageSources().playerAttack(player),repeated,true));check(repeated.isEmpty(),"repeated drop event cannot award another Core/XP");
    check(d.getInt("death_xp")>=1000&&d.getInt("death_xp")<=1500,"XP snapshot in locked range");
   }
   bridge.colors=5;bridge.active=true;bridge.center=new Vec3(1000,100,1000);var outside=HarrowerMod.HARROWER.get().create(level);outside.setPos(2,100,20);outside.setHealth(0);NeoForge.EVENT_BUS.post(new LivingDeathEvent(outside,player.damageSources().playerAttack(player)));check(EncounterRewards.qualifying(outside)==0,"outside all Leyline radii gives no bonus");
  bridge.center=new Vec3(2,100,20).add(64,0,0);check(com.leyline.harrower.loot.LootLeylines.atDeath(level,outside,player.getUUID())==5,"64-block boundary includes active owner Leylines");bridge.center=bridge.center.add(.01,0,0);check(com.leyline.harrower.loot.LootLeylines.atDeath(level,outside,player.getUUID())==0,"outside 64-block boundary excludes bonus");
   bridge.center=outside.position();check(com.leyline.harrower.loot.LootLeylines.atDeath(level,outside,UUID.randomUUID())==0,"another owner's active Leylines cannot qualify the killer");
  }finally{LeylineBridge.uninstall(bridge);}
  // One actual native death: real loot entities, XP total, and no second rewards over death ticks.
  for(Entity e:level.getEntitiesOfClass(Entity.class,RitualChecks.AREA))if(e instanceof ExperienceOrb||e instanceof ItemEntity||e instanceof Harrower)e.discard();
  var real=HarrowerMod.HARROWER.get().create(level);real.setPos(2,100,20);real.setHealth(1);level.addFreshEntity(real);real.hurt(player.damageSources().playerAttack(player),10000);
  check(real.isDeadOrDying(),"native lethal damage completes death without LC installed");
  check(level.getEntitiesOfClass(ItemEntity.class,RitualChecks.AREA,e->!e.isRemoved()).isEmpty()&&xp()==0,"no items or XP appear at death");
  var committed=escrow(real);check(amount(committed,Hollowforged.HOLLOW_CORE.get())==1&&amount(committed,Items.ENCHANTED_GOLDEN_APPLE)>=2,"real native rolls including Core stored intact");
  // Resume an actual saved corpse at its original death clock; no reroll or fresh payout.
  for(int i=0;i<200;i++)deathTick(real);CompoundTag saved=new CompoundTag();real.saveWithoutId(saved);real.discard();var restored=HarrowerMod.HARROWER.get().create(level);restored.load(saved);level.addFreshEntity(restored);
  check(restored.deathTime==200&&same(committed,escrow(restored)),"saved corpse retains exact items, enchantments and death clock");
  while(restored.deathTime<409)deathTick(restored);
  check(level.getEntitiesOfClass(ItemEntity.class,RitualChecks.AREA,e->!e.isRemoved()).isEmpty()&&xp()==0,"no early drops through fall and pre-release crumble tick 409");
  deathTick(restored);var items=level.getEntitiesOfClass(ItemEntity.class,RitualChecks.AREA,e->!e.isRemoved()).stream().map(ItemEntity::getItem).toList();
  check(same(committed,items),"tick 410 releases the exact pre-rolled reward stacks from the remains");
  int xp=xp();check(xp>=1000&&xp<=1500,"delayed XP total paid once: "+xp);
  int before=items.stream().mapToInt(ItemStack::getCount).sum();for(int i=0;i<40;i++)deathTick(restored);
  check(xp()==xp&&level.getEntitiesOfClass(ItemEntity.class,RitualChecks.AREA,e->!e.isRemoved()).stream().mapToInt(e->e.getItem().getCount()).sum()==before,"later death ticks cannot duplicate payout");
  CompoundTag paid=new CompoundTag();restored.saveWithoutId(paid);var again=HarrowerMod.HARROWER.get().create(level);again.load(paid);com.leyline.harrower.loot.DelayedDeathLoot.release(again);check(xp()==xp&&escrow(again).isEmpty(),"paid flag persists through reload");
  var pickup=level.getEntitiesOfClass(ItemEntity.class,RitualChecks.AREA,e->!e.isRemoved()).stream().filter(e->e.getItem().is(Hollowforged.HOLLOW_CORE.get())).findFirst().orElseThrow();int inv=player.getInventory().countItem(Hollowforged.HOLLOW_CORE.get());pickup.setNoPickUpDelay();pickup.playerTouch(player);check(player.getInventory().countItem(Hollowforged.HOLLOW_CORE.get())==inv+1,"delayed Core remains collectible");

 }
 static List<ItemStack> escrow(Harrower boss){var list=boss.getPersistentData().getCompound(com.leyline.harrower.loot.DelayedDeathLoot.KEY).getList("Items",net.minecraft.nbt.Tag.TAG_COMPOUND);List<ItemStack> result=new ArrayList<>();for(int i=0;i<list.size();i++)result.add(ItemStack.parseOptional(level.registryAccess(),list.getCompound(i).getCompound("Item")));return result;}
 static void deathTick(Harrower boss){boss.tick();NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(boss));}
 static boolean same(List<ItemStack> a,List<ItemStack> b){List<ItemStack> left=new ArrayList<>(b);for(var x:a){int found=-1;for(int i=0;i<left.size();i++)if(ItemStack.matches(x,left.get(i))){found=i;break;}if(found<0)return false;left.remove(found);}return left.isEmpty();}
 static int xp(){int total=0;for(var orb:level.getEntitiesOfClass(ExperienceOrb.class,RitualChecks.AREA,e->!e.isRemoved())){CompoundTag tag=new CompoundTag();orb.saveWithoutId(tag);total+=tag.getInt("Value")*tag.getInt("Count");}return total;}
 static final class TestBridge implements LeylineBridge.Access {
  final UUID owner;int colors;boolean active=true;Vec3 center=Vec3.ZERO;TestBridge(UUID owner){this.owner=owner;}
  public Collection<LeylineBridge.OwnedLeyline> ownedActive(ServerLevel level,UUID who){List<LeylineBridge.OwnedLeyline> result=new ArrayList<>();for(int i=0;i<colors;i++)result.add(new LeylineBridge.OwnedLeyline("test"+i,owner,LeylineBridge.Color.values()[i],center,active));return result;}
  public void updateSuppression(ServerLevel level,UUID boss,Collection<LeylineBridge.OwnedLeyline> eligible,long expires){}public void release(ServerLevel level,UUID boss){}public String auditedVersion(){return "test-only";}
 }
}
