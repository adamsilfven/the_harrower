package com.leyline.hollowforged;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
/** Explicit opt-in development showroom. Never called in an ordinary world. */
public final class ReviewScene {
 public static void started(ServerStartedEvent event){
  if(!Boolean.getBoolean("lc_hollowforged.review"))return;var level=event.getServer().overworld();
  for(BlockPos p:BlockPos.betweenClosed(-26,64,-26,26,64,26))level.setBlock(p,Blocks.SMOOTH_STONE.defaultBlockState(),3);
  for(BlockPos p:BlockPos.betweenClosed(19,64,-7,28,64,13))level.setBlock(p,Blocks.SMOOTH_STONE.defaultBlockState(),3);
  for(BlockPos p:BlockPos.betweenClosed(19,65,-7,28,66,13))if(p.getX()==19||p.getX()==28||p.getZ()==-7||p.getZ()==13)level.setBlock(p,Blocks.SMOOTH_STONE.defaultBlockState(),3);
  for(BlockPos p:BlockPos.betweenClosed(20,65,-6,27,66,12))level.setBlock(p,Blocks.WATER.defaultBlockState(),3);
  level.setDayTime(6000);level.setWeatherParameters(0,24000,false,false);
  ArmorStand stand=EntityType.ARMOR_STAND.create(level);stand.moveTo(0,65,0,180,0);stand.setYBodyRot(180);
  stand.setNoGravity(true);stand.setInvulnerable(true);stand.setShowArms(true);equip(stand);level.addFreshEntity(stand);
 }
 public static void joined(PlayerEvent.PlayerLoggedInEvent event){
  if(!Boolean.getBoolean("lc_hollowforged.review")||!(event.getEntity() instanceof ServerPlayer p))return;
  p.getServer().getPlayerList().op(p.getGameProfile());p.setGameMode(GameType.SPECTATOR);p.teleportTo(0,64.5,-3.8);p.connection.teleport(0,64.5,-3.8,0,2);equip(p);
 }
 private static void equip(net.minecraft.world.entity.LivingEntity e){
  e.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Hollowforged.CROWN.get()));e.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Hollowforged.CARAPACE.get()));
  e.setItemSlot(EquipmentSlot.LEGS,new ItemStack(Hollowforged.GREAVES.get()));e.setItemSlot(EquipmentSlot.FEET,new ItemStack(Hollowforged.TREADS.get()));
  if(e instanceof ServerPlayer)e.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Hollowforged.WORLDSHARD.get()));
 }
}
