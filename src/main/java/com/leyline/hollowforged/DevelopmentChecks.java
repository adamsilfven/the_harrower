package com.leyline.hollowforged;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
/** Opt-in assertions against real loaded registries, crafting menus and transformed classes. */
public final class DevelopmentChecks {
 private static int checks;
 private static final List<String> evidence=new ArrayList<>();
 static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);evidence.add("PASS "+message);}
 static void near(double actual,double expected,String message){check(Math.abs(actual-expected)<.0001,message+" ("+actual+")");}
 public static void started(ServerStartedEvent event){
  if(!Boolean.getBoolean("lc_hollowforged.verify"))return;
  try{
   var server=event.getServer();var level=server.overworld();var fake=FakePlayerFactory.getMinecraft(level);
   int[] armor={5,12,9,4},toughness={5,6,5,4};double totalArmor=0,totalToughness=0,totalKnockback=0;
   for(int i=0;i<Hollowforged.RELICS.size();i++){
    Item item=Hollowforged.RELICS.get(i).get();ItemStack stack=new ItemStack(item);
    check(stack.has(DataComponents.UNBREAKABLE),item+" native unbreakable component");check(!stack.isDamageableItem(),item+" is not damageable");
    check(stack.isEnchantable(),item+" native enchanting eligibility");near(stack.getEnchantmentValue(),20,item+" enchantability");
    stack.hurtAndBreak(100000,fake,EquipmentSlot.MAINHAND);check(stack.getCount()==1&&stack.getDamageValue()==0,item+" survives 100000 durability damage");
    var saved=stack.save(server.registryAccess());var restored=ItemStack.parseOptional(server.registryAccess(),(net.minecraft.nbt.CompoundTag)saved);
    check(restored.has(DataComponents.UNBREAKABLE)&&!restored.isDamageableItem(),item+" unbreakability survives NBT round trip");
    if(item instanceof ArmorItem a){
     near(a.getDefense(),armor[i],item+" armor");near(a.getToughness(),toughness[i],item+" toughness");
     totalArmor+=a.getDefense();totalToughness+=a.getToughness();totalKnockback+=a.getMaterial().value().knockbackResistance();
     check(stack.is(ItemTags.ARMOR_ENCHANTABLE),item+" armor enchanting tag");
     stack.forEachModifier(a.getEquipmentSlot(),(attribute,modifier)->fake.getAttribute(attribute).addTransientModifier(modifier));
    }
    var lookup=server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);var key=item instanceof ArmorItem?Enchantments.PROTECTION:Enchantments.SHARPNESS;
    check(lookup.getOrThrow(key).value().canEnchant(stack),item+" compatible protection/weapon enchantment");stack.enchant(lookup.getOrThrow(key),4);check(stack.isEnchanted(),item+" accepts enchantment");
   }
   near(totalArmor,30,"set armor total");near(totalToughness,20,"set toughness total");near(totalKnockback,.70,"set native knockback total");
   near(fake.getArmorValue(),30,"actual player attribute armor total");near(fake.getAttributeValue(Attributes.ARMOR_TOUGHNESS),20,"actual player attribute toughness total");near(fake.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE),.70,"actual player attribute knockback total");
   var hammer=new ItemStack(Hollowforged.WORLDSHARD.get());final double[] values={1,4,3};
   hammer.forEachModifier(EquipmentSlot.MAINHAND,(attribute,modifier)->{
    if(attribute.equals(Attributes.ATTACK_DAMAGE))values[0]+=modifier.amount();if(attribute.equals(Attributes.ATTACK_SPEED))values[1]+=modifier.amount();if(attribute.equals(Attributes.ENTITY_INTERACTION_RANGE))values[2]+=modifier.amount();
   });
   near(values[0],30,"Worldshard total player attack damage");near(values[1],.8,"Worldshard total player attack speed");near(values[2],5,"Worldshard native entity reach");
   hammer.forEachModifier(EquipmentSlot.MAINHAND,(attribute,modifier)->fake.getAttribute(attribute).addTransientModifier(modifier));
   near(fake.getAttributeValue(Attributes.ATTACK_DAMAGE),30,"actual player Worldshard attack damage");near(fake.getAttributeValue(Attributes.ATTACK_SPEED),.8,"actual player Worldshard attack speed");near(fake.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE),5,"actual player Worldshard reach");
   fake.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,hammer);fake.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));
   var air=new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem(fake,net.minecraft.world.InteractionHand.OFF_HAND);net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(air);check(air.isCanceled(),"Worldshard blocks ordinary offhand use event");
   var use=new net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Start(fake,fake.getOffhandItem(),net.minecraft.world.InteractionHand.OFF_HAND,72000);net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(use);check(use.isCanceled(),"Worldshard blocks use-start fallback event");
   check(fake.getOffhandItem().is(Items.SHIELD),"Worldshard preserves offhand inventory");check(!TwoHandedUse.denied(fake,net.minecraft.world.InteractionHand.MAIN_HAND),"Worldshard permits main hand");
   fake.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
   var released=new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem(fake,net.minecraft.world.InteractionHand.OFF_HAND);net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(released);check(!released.isCanceled(),"offhand use resumes when Worldshard is unequipped");fake.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,ItemStack.EMPTY);
   for(var relic:Hollowforged.RELICS){
    String name=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(relic.get()).getPath();var holder=server.getRecipeManager().byKey(Hollowforged.id(name)).orElseThrow();
    check(holder.value() instanceof ShapedRecipe,name+" remains a native shaped recipe");var recipe=(ShapedRecipe)holder.value();
    check(recipe.getWidth()==3&&recipe.getHeight()==3,name+" public 3x3 display dimensions");check(recipe.getIngredients().size()==9,name+" nine display slots");check(!recipe.canCraftInDimensions(2,2),name+" rejected by 2x2 inventory grid");
    List<ItemStack> grid=new ArrayList<>();for(var ingredient:recipe.getIngredients())grid.add(ingredient.isEmpty()?ItemStack.EMPTY:ingredient.getItems()[0].copy());
    check(grid.get(4).is(Hollowforged.HOLLOW_CORE.get()),name+" core in center display slot");var valid=CraftingInput.of(3,3,grid);check(recipe.matches(valid,level),name+" approved grid accepted");
    var output=recipe.assemble(valid,server.registryAccess());check(output.is(relic.get())&&output.getCount()==1,name+" output exactly one registered relic");
    fake.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
    var menu=new net.minecraft.world.inventory.CraftingMenu(1,fake.getInventory(),net.minecraft.world.inventory.ContainerLevelAccess.create(level,net.minecraft.core.BlockPos.ZERO));fake.containerMenu=menu;
    for(int slot=0;slot<9;slot++)menu.getSlot(slot+1).set(grid.get(slot).copy());check(menu.getSlot(0).getItem().is(relic.get()),name+" actual crafting-menu preview");
    menu.clicked(0,0,net.minecraft.world.inventory.ClickType.PICKUP,fake);check(menu.getCarried().is(relic.get())&&menu.getCarried().getCount()==1,name+" actual survival pickup output");
    check(java.util.stream.IntStream.range(1,10).allMatch(s->menu.getSlot(s).getItem().isEmpty()),name+" actual pickup consumes exactly one recipe");menu.setCarried(ItemStack.EMPTY);fake.getInventory().clearContent();
    for(int slot=0;slot<9;slot++)menu.getSlot(slot+1).set(grid.get(slot).isEmpty()?ItemStack.EMPTY:grid.get(slot).copyWithCount(2));
    menu.clicked(0,0,net.minecraft.world.inventory.ClickType.QUICK_MOVE,fake);check(fake.getInventory().countItem(relic.get())==2,name+" actual shift crafting produces two outputs");
    check(java.util.stream.IntStream.range(1,10).allMatch(s->menu.getSlot(s).getItem().isEmpty()),name+" actual shift crafting consumes both recipes");fake.getInventory().clearContent();fake.containerMenu=fake.inventoryMenu;
    for(int slot=0;slot<9;slot++)if(slot!=4){List<ItemStack> wrong=new ArrayList<>(grid);Collections.swap(wrong,4,slot);check(!recipe.matches(CraftingInput.of(3,3,wrong),level),name+" rejects displaced core at slot "+slot);}
    var wrongSize=new ArrayList<>(Collections.nCopies(12,ItemStack.EMPTY));for(int y=0;y<3;y++)for(int x=0;x<3;x++)wrongSize.set(y*4+x,grid.get(y*3+x));check(!recipe.matches(CraftingInput.of(4,3,wrongSize),level),name+" rejects larger unapproved grid");
    if(relic==Hollowforged.TREADS){
     List<ItemStack> shifted=new ArrayList<>(grid.subList(3,9));shifted.addAll(Collections.nCopies(3,ItemStack.EMPTY));var up=CraftingInput.of(3,3,shifted);check(!recipe.matches(up,level),"boots reject upward shift despite same normalized ingredients");
     var cache=new RecipeCache(10);check(cache.get(level,valid).isPresent(),"boots cache valid-first accepts approved placement");check(cache.get(level,up).isEmpty(),"boots cache valid-first rejects shifted placement");
     cache=new RecipeCache(10);check(cache.get(level,up).isEmpty(),"boots cache invalid-first rejects shifted placement");check(cache.get(level,valid).isPresent(),"boots cache invalid-first accepts approved placement");
    }
   }
   // Exercise actual effect application/removal, attribute cleanup, and damage event routing.
   fake.getInventory().clearContent();
   double ordinarySpeed=fake.getAttributeValue(Attributes.MOVEMENT_SPEED);
   fake.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS,200));
   check(fake.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS),"blindness applies before crown equip");
   fake.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Hollowforged.CROWN.get()));
   fake.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Hollowforged.CARAPACE.get()));
   fake.setItemSlot(EquipmentSlot.LEGS,new ItemStack(Hollowforged.GREAVES.get()));
   fake.setItemSlot(EquipmentSlot.FEET,new ItemStack(Hollowforged.TREADS.get()));
   LockedPassives.update(fake);
   check(!fake.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS),"crown clears pre-existing blindness");
   for(var effect:List.of(net.minecraft.world.effect.MobEffects.BLINDNESS,net.minecraft.world.effect.MobEffects.DARKNESS,net.minecraft.world.effect.MobEffects.POISON,net.minecraft.world.effect.MobEffects.WITHER,net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN)){
    check(!fake.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect,200)),"equipped passive rejects "+effect);
   }
   near(fake.getAttributeValue(Attributes.MOVEMENT_SPEED),ordinarySpeed*1.3,"treads increase movement speed by 30 percent");
   LockedPassives.update(fake);near(fake.getAttributeValue(Attributes.MOVEMENT_SPEED),ordinarySpeed*1.3,"repeated passive tick does not stack speed");
   for(var source:List.of(fake.damageSources().fall(),fake.damageSources().lava(),fake.damageSources().onFire(),fake.damageSources().wither())){
    var hit=new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(fake,new net.neoforged.neoforge.common.damagesource.DamageContainer(source,10));
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(hit);check(hit.isCanceled(),"equipped passive cancels "+source.getMsgId());
   }
   for(var source:List.of(fake.damageSources().fellOutOfWorld(),fake.damageSources().generic())){
    var hit=new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(fake,new net.neoforged.neoforge.common.damagesource.DamageContainer(source,10));
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(hit);check(!hit.isCanceled(),"passives preserve "+source.getMsgId());
   }
   for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))fake.setItemSlot(slot,ItemStack.EMPTY);
   LockedPassives.update(fake);near(fake.getAttributeValue(Attributes.MOVEMENT_SPEED),ordinarySpeed,"unequipping treads removes movement bonus");
   check(fake.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS,200)),"unequipping crown restores ordinary effect application");fake.removeAllEffects();
   var tableGrid=new ArrayList<>(Collections.nCopies(9,ItemStack.EMPTY));for(int index:new int[]{4,5,7,8})tableGrid.set(index,new ItemStack(Items.OAK_PLANKS));
   var ordinary=server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,CraftingInput.of(3,3,tableGrid),level);check(ordinary.isPresent()&&ordinary.get().value().getResultItem(server.registryAccess()).is(Items.CRAFTING_TABLE),"unrelated shifted vanilla crafting table still matches");
   // The standard FakePlayer overrides isInvulnerableTo to always true. Use a damageable fixture.
   var damageable=new net.neoforged.neoforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"PassiveDamage")){
    @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source){return false;}
   };
   damageable.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
   damageable.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Hollowforged.CARAPACE.get()));damageable.setItemSlot(EquipmentSlot.FEET,new ItemStack(Hollowforged.TREADS.get()));
   damageable.setHealth(20);damageable.invulnerableTime=0;float beforeFall=damageable.getHealth();
   damageable.hurt(damageable.damageSources().fall(),100);near(damageable.getHealth(),beforeFall,"actual fall damage leaves equipped player health unchanged");
   damageable.setRemainingFireTicks(100);LockedPassives.update(damageable);check(!damageable.isOnFire(),"carapace extinguishes existing fire");
   damageable.invulnerableTime=0;damageable.hurt(damageable.damageSources().fellOutOfWorld(),1000);check(damageable.isDeadOrDying(),"actual void damage remains lethal with carapace and treads equipped");
   CombatChecks.run(server);
   evidence.add("TOTAL "+checks+" assertions passed");Files.write(Path.of("hollowforged-foundation-checks.txt"),evidence);LogUtils.getLogger().info("HOLLOWFORGED_FOUNDATION_PASS {} checks",checks);
  }catch(Throwable failure){
   LogUtils.getLogger().error("HOLLOWFORGED_FOUNDATION_FAIL",failure);try{evidence.add("FAIL "+failure);Files.write(Path.of("hollowforged-foundation-checks.txt"),evidence);}catch(Exception ignored){}throw new IllegalStateException("Hollowforged foundation checks failed",failure);
  }finally{event.getServer().halt(false);}
 }
}
