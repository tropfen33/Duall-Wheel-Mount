package com.Tropfen_33.Duall_Wheel_Mount.client;

import com.Tropfen_33.Duall_Wheel_Mount.content.DualWheelMountBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.ryanhcode.offroad.content.components.TireLike;
import dev.ryanhcode.offroad.index.OffroadDataComponents;
import dev.ryanhcode.offroad.index.OffroadPartialModels;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2d;

/**
 * Dual-wheel renderer rebuilt around the original Offroad Wheel Mount's render
 * transforms. Each side gets its own complete wheel/telescope/spring assembly.
 * This class intentionally contains no physics or block-entity state updates.
 */
public class DualWheelMountRenderer extends KineticBlockEntityRenderer<DualWheelMountBlockEntity> {
    public DualWheelMountRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(DualWheelMountBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        BlockState state = getRenderedBlockState(be);
        renderRotatingBuffer(be, getRotatedModel(be, state), ms, buffer.getBuffer(getRenderType(be, state)), light);
        FilteringRenderer.renderOnBlockEntity(be, partialTicks, ms, buffer, light, overlay);

        BlockState blockState = be.getBlockState();
        VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());
        Direction facing = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);

        // Consistent visual convention for every facing: the RIGHT wheel slot
        // is rendered on the mount's left side; the LEFT wheel slot on its right.
        renderWheelAssembly(be, be.getRightWheel(), be.getLerpedRightAngle(partialTicks),
                be.getLerpedRightExtension(partialTicks), facing.getCounterClockWise(),
                be.getSteeringYawForRendering(), true, blockState, ms, buffer, vb, light, overlay);
        renderWheelAssembly(be, be.getLeftWheel(), be.getLerpedLeftAngle(partialTicks),
                be.getLerpedLeftExtension(partialTicks), facing.getClockWise(),
                be.getSteeringYawForRendering(), false, blockState, ms, buffer, vb, light, overlay);
    }

    public static void renderWheelAssembly(DualWheelMountBlockEntity be, ItemStack stack, float wheelAngle,
                                     double extension, Direction wheelDirection, double steeringYaw, boolean leftWheel,
                                     BlockState state, PoseStack ms, MultiBufferSource buffer,
                                     VertexConsumer vb, int light, int overlay) {
        // Keep the suspension assembly visible even when this wheel slot is empty.
        // Only the tire-specific rendering below is conditional.
        TireLike tire = stack.get(OffroadDataComponents.TIRE);

        // These dimensions and pivot calculations are ported from the original
        // WheelMountRenderer. Only the assembly's facing and side offset differ.
        final double wheelPivotOffsetHor = 10.0 / 16.0;
        final double springWheelPivotOffsetHor = 12.0 / 16.0;
        final double springWheelPivotOffsetVer = -2.0 / 16.0;
        final double horizontalWheelPosition = 22.0 / 16.0;
        final double verticalWheelPosition = -extension;
        final double teleMountHor = 0.0;
        final double teleMountVer = -6.0 / 16.0;
        final double springMountHor = 7.0 / 16.0;
        final double springMountVer = 7.0 / 16.0;

        final double teleAngle = Math.atan2(verticalWheelPosition - teleMountVer,
                horizontalWheelPosition - wheelPivotOffsetHor - teleMountHor);
        final double teleDistance = new Vector2d(verticalWheelPosition - teleMountVer,
                horizontalWheelPosition - wheelPivotOffsetHor - teleMountHor).length();
        final double springAngle = Math.atan2(verticalWheelPosition - springWheelPivotOffsetVer - springMountVer,
                horizontalWheelPosition - springWheelPivotOffsetHor - springMountHor);
        final double springDistance = new Vector2d(verticalWheelPosition - springWheelPivotOffsetVer - springMountVer,
                horizontalWheelPosition - springWheelPivotOffsetHor - springMountHor).length();

        SuperByteBuffer teleOuter = CachedBuffers.partial(OffroadPartialModels.TELE_OUTER, state);
        SuperByteBuffer teleInner = CachedBuffers.partial(OffroadPartialModels.TELE_INNER, state);
        SuperByteBuffer teleMount = CachedBuffers.partial(OffroadPartialModels.TELE_MOUNT, state);
        SuperByteBuffer springTop = CachedBuffers.partial(OffroadPartialModels.SPRING_UPPER, state);
        SuperByteBuffer springMiddle = CachedBuffers.partial(OffroadPartialModels.SPRING_MIDDLE, state);
        SuperByteBuffer springBottom = CachedBuffers.partial(OffroadPartialModels.SPRING_LOWER, state);

        ms.pushPose();
        TransformStack.of(ms).center()
                .rotateYDegrees(AngleHelper.horizontalAngle(wheelDirection))
                .rotateXDegrees(AngleHelper.verticalAngle(wheelDirection))
                .uncenter();

        // Shift the whole original assembly outward so left and right do not overlap.
        ms.translate(0, 0, 0);

        // Telescope: the outer tube stays attached to the mount; the inner tube
        // extends with the wheel, following the original renderer's distance math.
        ms.pushPose();
        ms.translate(0, -6.0 / 16.0, 0);
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Axis.XP.rotation((float) teleAngle));
        ms.translate(-0.5, -0.5, -0.5);
        teleOuter.light(light).renderInto(ms, vb);
        ms.translate(0, 0, -(teleDistance - 1.0));
        teleInner.light(light).renderInto(ms, vb);
        ms.popPose();

        // Wheel carrier and tire pivot move by the interpolated suspension extension.
        ms.pushPose();
        ms.translate(0, verticalWheelPosition, 26.0 / 16.0 - horizontalWheelPosition);
        ms.translate(0.5, 0.5, 0.5);
        ms.rotateAround(Axis.YP.rotation((float) steeringYaw), 0, 0,
                (float) (-horizontalWheelPosition + 6.0 / 16.0));
        ms.translate(-0.5, -0.5, -0.5);
        teleMount.light(light).renderInto(ms, vb);

        ms.translate(0.5, 0.5, 0.5);
        ms.translate(0, 0, -26.0 / 16.0);
        // Match the original Offroad renderer's correction to the direction
        // that actually rotated this wheel assembly above. The two wheel
        // assemblies face opposite local directions, so using the block facing
        // for both makes one wheel roll backward. This is a coordinate-frame
        // correction, not an arbitrary left/right flip.
        Direction spinDirection = wheelDirection;
        double spinSign = (spinDirection.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0 : -1.0)
                * (spinDirection.getAxis() == Direction.Axis.X ? 1.0 : -1.0);
        // Keep the established NORTH correction and correct the WEST-facing
        // mount as well; its local wheel frame rolls opposite to the other facings.
        Direction mountFacing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (mountFacing == Direction.NORTH || mountFacing == Direction.WEST) {
            spinSign *= -1.0;
        }
        ms.mulPose(Axis.ZP.rotation((float) (-wheelAngle * spinSign)));

        if (tire != null) {
            Vec3 rotation = tire.rotation();
            ms.mulPose(Axis.XP.rotation((float) Math.toRadians(rotation.x)));
            ms.mulPose(Axis.YP.rotation((float) Math.toRadians(rotation.y)));
            ms.mulPose(Axis.ZP.rotation((float) Math.toRadians(rotation.z)));
            ms.translate(tire.offset().x, tire.offset().y, tire.offset().z);

            if (tire.model().isPresent()) {
                ResourceLocation model = tire.model().get();
                CachedBuffers.partial(PartialModel.of(model), state).light(light)
                        .translate(-0.5f, 0, -0.5f).renderInto(ms, vb);
            } else {
                Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE,
                        light, overlay, ms, buffer, be != null ? be.getLevel() : null, 0);
            }
        }
        ms.popPose();

        // Spring is recalculated from the same wheel extension, so it follows
        // the wheel instead of remaining a static decoration.
        ms.pushPose();
        ms.translate(0.5, 0.5 + springMountVer, 0.5 - springMountHor);
        ms.mulPose(Axis.XP.rotation((float) springAngle + Mth.PI / 2.0f));
        ms.translate(-0.5, -0.5 - springMountVer, -0.5 + springMountHor);
        float springSpan = (float) springDistance - 4.0f / 16.0f;
        springTop.light(light).renderInto(ms, vb);
        springMiddle.light(light)
                .translate(0, 13.0f / 16.0f, 0)
                .scale(1, springSpan / (14.0f / 16.0f), 1)
                .translateBack(0, 13.0f / 16.0f, 0).renderInto(ms, vb);
        springBottom.light(light)
                .translate(0, -(springSpan - 14.0 / 16.0), 0).renderInto(ms, vb);
        ms.popPose();

        ms.popPose();
    }

    @Override
    public int getViewDistance() {
        return 512;
    }

    @Override
    protected SuperByteBuffer getRotatedModel(DualWheelMountBlockEntity te, BlockState state) {
        // The kinetic input is on the top face, so orient the visible shaft vertically.
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, Direction.UP);
    }
}
