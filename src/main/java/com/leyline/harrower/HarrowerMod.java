package com.leyline.harrower;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
@Mod(HarrowerMod.ID)
public final class HarrowerMod {
 public static final String ID="lc_harrower";
 public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,ID);
 public static final DeferredHolder<EntityType<?>,EntityType<Harrower>> HARROWER=ENTITIES.register("harrower",()->EntityType.Builder.of(Harrower::new,MobCategory.MONSTER).fireImmune().sized(8.8F,11.5F).eyeHeight(9.4F).clientTrackingRange(16).updateInterval(2).build(ID+":harrower"));
 public static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS=DeferredRegister.create(Registries.SOUND_EVENT,ID);
 public static final DeferredHolder<net.minecraft.sounds.SoundEvent,net.minecraft.sounds.SoundEvent> STEP=sound("heavy_step"),GRIND=sound("stone_grind"),BREATH=sound("mountain_breath"),IMPACT=sound("world_impact"),FIST_CUE=sound("fist_warning"),SWEEP_CUE=sound("sweep_warning"),FOOT_CUE=sound("foot_warning"),RUPTURE_CUE=sound("rupture_warning"),CHARGE_CUE=sound("charge_warning"),GRASP_CUE=sound("grasp_warning"),HOLLOW_CUE=sound("hollowing_warning"),PHASE_CUE=sound("phase_strain");
 public static net.minecraft.sounds.SoundEvent cue(String group){return switch(group){case "fist"->FIST_CUE.get();case "sweep"->SWEEP_CUE.get();case "heel","stomp"->FOOT_CUE.get();case "rupture"->RUPTURE_CUE.get();case "charge"->CHARGE_CUE.get();case "grasp"->GRASP_CUE.get();case "hollowing"->HOLLOW_CUE.get();case "phase"->PHASE_CUE.get();default->GRIND.get();};}
 private static DeferredHolder<net.minecraft.sounds.SoundEvent,net.minecraft.sounds.SoundEvent> sound(String name){return SOUNDS.register(name,()->net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ID,name)));}
 public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,ID);
 public static final DeferredHolder<Item,DeferredSpawnEggItem> EGG=ITEMS.register("harrower_spawn_egg",()->new DeferredSpawnEggItem(HARROWER,0x252528,0xB0A898,new Item.Properties()));
 public HarrowerMod(IEventBus bus){ENTITIES.register(bus);ITEMS.register(bus);SOUNDS.register(bus);bus.addListener((EntityAttributeCreationEvent e)->e.put(HARROWER.get(),Harrower.attributes().build()));}
}
