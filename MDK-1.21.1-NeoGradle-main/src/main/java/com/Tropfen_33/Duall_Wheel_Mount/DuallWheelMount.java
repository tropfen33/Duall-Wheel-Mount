package com.Tropfen_33.Duall_Wheel_Mount;

import com.Tropfen_33.Duall_Wheel_Mount.registry.ModBlockEntities;
import com.Tropfen_33.Duall_Wheel_Mount.events.DualWheelMountCommonEvents;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import com.Tropfen_33.Duall_Wheel_Mount.registry.ModBlocks;
import com.Tropfen_33.Duall_Wheel_Mount.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(DuallWheelMount.MOD_ID)
public class DuallWheelMount {
    public static final String MOD_ID = "duall_wheel_mount";

    public DuallWheelMount(IEventBus modEventBus) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        SableEventPlatform.INSTANCE.onPhysicsTick(DualWheelMountCommonEvents::physicsTick);
    }
}
