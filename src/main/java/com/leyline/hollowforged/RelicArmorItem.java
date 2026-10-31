package com.leyline.hollowforged;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
public final class RelicArmorItem extends ArmorItem {
 public RelicArmorItem(Holder<ArmorMaterial> material,Type type,Properties properties){super(material,type,properties);}
 @Override public boolean canWalkOnPowderedSnow(ItemStack stack,LivingEntity wearer){return getType()==Type.BOOTS;}
}
