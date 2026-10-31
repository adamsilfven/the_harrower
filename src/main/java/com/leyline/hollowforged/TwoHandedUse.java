package com.leyline.hollowforged;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
public final class TwoHandedUse {
 public static boolean denied(LivingEntity entity,InteractionHand hand){return hand==InteractionHand.OFF_HAND&&entity.getMainHandItem().is(Hollowforged.WORLDSHARD.get());}
 public static void register(){
  var b=NeoForge.EVENT_BUS;
  b.addListener(EventPriority.LOWEST,(PlayerInteractEvent.RightClickItem e)->{if(denied(e.getEntity(),e.getHand())){e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);}});
  b.addListener(EventPriority.LOWEST,(PlayerInteractEvent.RightClickBlock e)->{if(denied(e.getEntity(),e.getHand())){e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);}});
  b.addListener(EventPriority.LOWEST,(PlayerInteractEvent.EntityInteract e)->{if(denied(e.getEntity(),e.getHand())){e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);}});
  b.addListener(EventPriority.LOWEST,(PlayerInteractEvent.EntityInteractSpecific e)->{if(denied(e.getEntity(),e.getHand())){e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);}});
  b.addListener(EventPriority.LOWEST,(LivingEntityUseItemEvent.Start e)->{if(denied(e.getEntity(),e.getHand()))e.setCanceled(true);});
  b.addListener(EventPriority.LOWEST,(LivingEntityUseItemEvent.Tick e)->{if(denied(e.getEntity(),e.getHand()))e.setCanceled(true);});
  b.addListener((PlayerTickEvent.Pre e)->{if(e.getEntity().isUsingItem()&&denied(e.getEntity(),e.getEntity().getUsedItemHand()))e.getEntity().stopUsingItem();});
 }
}
