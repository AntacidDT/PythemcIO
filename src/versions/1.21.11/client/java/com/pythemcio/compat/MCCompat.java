package com.pythemcio.compat;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class MCCompat {

    private MCCompat() {
    }

    public static String blockName(BlockState state) {
        return resourceKeyName(state.getBlock().builtInRegistryHolder().key());
    }

    public static String itemName(ItemStack stack) {
        return resourceKeyName(stack.getItem().builtInRegistryHolder().key());
    }

    public static String entityTypeName(EntityType<?> type) {
        return resourceKeyName(type.builtInRegistryHolder().key());
    }

    public static String resourceKeyName(ResourceKey<?> key) {
        return key.identifier().toString();
    }

    public static ItemStack selectedItem(Inventory inv) {
        return inv.getSelectedItem();
    }
}
