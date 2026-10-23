package com.Tropfen_33.Duall_Wheel_Mount.client;

import com.Tropfen_33.Duall_Wheel_Mount.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Draws the mount as an item: the block model, the shaft and the suspension
 * (telescopes + springs) of both sides at their resting extension.
 */
public class DualWheelMountItemRenderer extends BlockEntityWithoutLevelRenderer {
    /** Same value as NO_WHEEL_EXTENSION in DualWheelMountBlockEntity. */
    private static final double REST_EXTENSION = 0.5;

    public DualWheelMountItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack ms,
                             MultiBufferSource buffer, int light, int overlay) {
        BlockState state = ModBlocks.DUAL_WHEEL_MOUNT.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);

        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, ms, buffer, light, overlay);

        VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());
        CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, Direction.UP)
                .light(light).renderInto(ms, vb);

        Direction facing = Direction.NORTH;
        DualWheelMountRenderer.renderWheelAssembly(null, ItemStack.EMPTY, 0f, REST_EXTENSION,
                facing.getCounterClockWise(), 0.0, true, state, ms, buffer, vb, light, overlay);
        DualWheelMountRenderer.renderWheelAssembly(null, ItemStack.EMPTY, 0f, REST_EXTENSION,
                facing.getClockWise(), 0.0, false, state, ms, buffer, vb, light, overlay);
    }
}
