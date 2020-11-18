package me.steven.indrev.blockentities.solarpowerplant

import net.minecraft.block.Blocks
import net.minecraft.block.HorizontalConnectingBlock
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.RenderLayers
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Vector3f

class SolarReflectorBlockEntityRenderer(dispatcher: BlockEntityRenderDispatcher) : BlockEntityRenderer<SolarReflectorBlockEntity>(dispatcher) {
    override fun render(
        entity: SolarReflectorBlockEntity?,
        tickDelta: Float,
        matrices: MatrixStack?,
        vertexConsumers: VertexConsumerProvider?,
        light: Int,
        overlay: Int
    ) {
        if (entity == null) return

        matrices?.run {
            push()
            val state = Blocks.GLASS_PANE.defaultState.with(HorizontalConnectingBlock.WEST, true).with(HorizontalConnectingBlock.EAST, true)
            matrices.translate(0.5, 0.5, 0.5)
            matrices.multiply(Vector3f.NEGATIVE_Y.getDegreesQuaternion(entity.yaw))
            matrices.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(entity.pitch))
            matrices.translate(-0.5, -0.5, -0.5)
            val buffer = vertexConsumers?.getBuffer(RenderLayers.getBlockLayer(state))
            MinecraftClient.getInstance().blockRenderManager.renderBlock(state, entity.pos, entity.world, this, buffer, false, entity.world!!.random)
            pop()
        }
    }
}