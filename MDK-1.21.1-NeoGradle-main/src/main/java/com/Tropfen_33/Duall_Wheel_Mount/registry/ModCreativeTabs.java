package com.Tropfen_33.Duall_Wheel_Mount.registry;

import com.Tropfen_33.Duall_Wheel_Mount.DuallWheelMount;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DuallWheelMount.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + DuallWheelMount.MOD_ID + ".main"))
                    .icon(() -> new ItemStack(ModItems.DUAL_WHEEL_MOUNT.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.DUAL_WHEEL_MOUNT.get());
                        // add future items here
                    })
                    .build());

    private ModCreativeTabs() {}

    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }
}
