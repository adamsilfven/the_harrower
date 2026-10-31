package com.leyline.harrower.ritual;

import com.leyline.harrower.HarrowerMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid=HarrowerMod.ID,bus=EventBusSubscriber.Bus.MOD)
public final class CairnRegistry {
 public static EntityType<CairnRitual> TYPE;
 @SubscribeEvent public static void register(RegisterEvent event){
  event.register(Registries.ENTITY_TYPE,helper->{
   TYPE=EntityType.Builder.<CairnRitual>of(CairnRitual::new,MobCategory.MISC).sized(2,1).clientTrackingRange(8).updateInterval(1).fireImmune().build("lc_harrower:cairn_ritual");
   helper.register(ResourceLocation.fromNamespaceAndPath(HarrowerMod.ID,"cairn_ritual"),TYPE);
  });
 }
 private CairnRegistry(){}
}
