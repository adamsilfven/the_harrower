package com.leylineconvergence.hollowfragment;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.List;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(HollowFragmentAddon.MOD_ID)
public final class HollowFragmentAddon {
    public static final String MOD_ID = "hollow_fragment_addon";
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredItem<Item> HOLLOW_FRAGMENT = ITEMS.register(
        "hollow_fragment", () -> new Item(new Item.Properties().stacksTo(16)) {
            @Override
            public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                        List<Component> tooltip, TooltipFlag flag) {
                super.appendHoverText(stack, context, tooltip, flag);
                tooltip.add(Component.translatable("tooltip.hollow_fragment_addon.hollow_fragment")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
        });

    public HollowFragmentAddon(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
