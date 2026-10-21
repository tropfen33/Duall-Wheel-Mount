package com.Tropfen_33.Duall_Wheel_Mount.client;

import com.Tropfen_33.Duall_Wheel_Mount.DuallWheelMount;
import com.Tropfen_33.Duall_Wheel_Mount.registry.ModBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = DuallWheelMount.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DualWheelMountClient {
    private DualWheelMountClient() {}
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.DUAL_WHEEL_MOUNT.get(), DualWheelMountRenderer::new);
    }
}
