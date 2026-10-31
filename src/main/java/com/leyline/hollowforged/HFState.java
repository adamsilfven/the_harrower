package com.leyline.hollowforged;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
public final class HFState {
 public static final TagKey<net.minecraft.world.damagesource.DamageType> VOID=TagKey.create(Registries.DAMAGE_TYPE,Hollowforged.id("void_equivalent"));
 public static final TagKey<net.minecraft.world.effect.MobEffect> STUN_EFFECTS=TagKey.create(Registries.MOB_EFFECT,Hollowforged.id("stun_effects"));
 public static final TagKey<EntityType<?>> CONTROL_IMMUNE=TagKey.create(Registries.ENTITY_TYPE,Hollowforged.id("control_immune"));
 public static CompoundTag data(Entity e){var all=e.getPersistentData();if(!all.contains(Hollowforged.ID,10))all.put(Hollowforged.ID,new CompoundTag());return all.getCompound(Hollowforged.ID);}
 public static long now(Entity e){return e.level().isClientSide()?e.level().getGameTime():e.getServer().overworld().getGameTime();}
 public static boolean active(Entity e,String key){return data(e).getLong(key)>now(e);}
 public static void until(Entity e,String key,long ticks){data(e).putLong(key,now(e)+ticks);}
 public static boolean worn(LivingEntity e,EquipmentSlot slot,Item item){return e.getItemBySlot(slot).is(item);}
 public static boolean crown(LivingEntity e){return worn(e,EquipmentSlot.HEAD,Hollowforged.CROWN.get());}
 public static boolean chest(LivingEntity e){return worn(e,EquipmentSlot.CHEST,Hollowforged.CARAPACE.get());}
 public static boolean legs(LivingEntity e){return worn(e,EquipmentSlot.LEGS,Hollowforged.GREAVES.get());}
 public static boolean boots(LivingEntity e){return worn(e,EquipmentSlot.FEET,Hollowforged.TREADS.get());}
 public static boolean hammer(LivingEntity e){return e.getMainHandItem().is(Hollowforged.WORLDSHARD.get());}
 public static boolean voidDamage(DamageSource source){return source.is(DamageTypes.FELL_OUT_OF_WORLD)||source.is(VOID);}
 public static boolean forceImmune(Entity e){return e instanceof LivingEntity l&&(legs(l)||chest(l)&&active(l,"unbroken"));}
 public static boolean controlImmune(LivingEntity e){return e instanceof Player||e.getType().is(CONTROL_IMMUNE)||e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon||e instanceof net.minecraft.world.entity.boss.wither.WitherBoss||e.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)>=1;}
 public static boolean hostile(LivingEntity e){return e.isAlive()&&!e.isSpectator()&&(e instanceof net.minecraft.world.entity.monster.Enemy||e.getType().is(CombatAbilities.HOSTILES));}
 public static boolean equipped(Player p,int ability){return switch(ability){case 0,1->crown(p);case 2->chest(p);case 3->legs(p);case 4->boots(p);case 5,6->hammer(p);default->false;};}
 public static final String[] NAMES={"Absolute Sight","The Final Witness","The World Cannot Break Me","Sovereign Ground","Cataclysm","Fracture the Earth","The Last Argument"};
 public static final int[] COOLDOWNS={0,1800,3600,2400,1200,600,6000};
 public static String mark(Player owner,String kind){return kind+"_"+owner.getUUID();}
 public static void clearMovement(Entity e){var d=data(e);for(String s:new String[]{"anchor","still","cataclysm","windup","last_seen_tick"})d.remove(s);d.remove("gaze");d.remove("gaze_ticks");}
}
