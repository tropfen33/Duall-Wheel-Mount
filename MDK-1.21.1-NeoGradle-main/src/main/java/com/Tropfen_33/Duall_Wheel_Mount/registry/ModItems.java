package com.Tropfen_33.Duall_Wheel_Mount.registry;

import com.Tropfen_33.Duall_Wheel_Mount.DuallWheelMount;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(DuallWheelMount.MOD_ID);

    public static final DeferredHolder<Item, BlockItem> DUAL_WHEEL_MOUNT =
            ITEMS.register("dual_wheel_mount", () ->
                    new BlockItem(ModBlocks.DUAL_WHEEL_MOUNT.get(), new Item.Properties()));

    private ModItems() {}

    public static void register(net.neoforged.bus.api.IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
