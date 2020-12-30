package me.steven.indrev.utils

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume
import com.google.gson.JsonObject
import dev.technici4n.fasttransferlib.api.Simulation
import dev.technici4n.fasttransferlib.api.energy.EnergyIo
import me.shedaniel.math.Point
import me.shedaniel.rei.api.widgets.Widgets
import me.shedaniel.rei.gui.widget.Widget
import me.steven.indrev.IndustrialRevolution
import me.steven.indrev.gui.widgets.machines.WFluid
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry
import net.fabricmc.fabric.impl.screenhandler.ExtendedScreenHandlerType
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluid
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.text.LiteralText
import net.minecraft.text.OrderedText
import net.minecraft.util.Identifier
import net.minecraft.util.JsonHelper
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.registry.Registry
import net.minecraft.world.World
import java.util.*

val EMPTY_INT_ARRAY = intArrayOf()

fun identifier(id: String) = Identifier(IndustrialRevolution.MOD_ID, id)

fun Identifier.block(block: Block): Identifier {
    Registry.register(Registry.BLOCK, this, block)
    return this
}

fun Identifier.fluid(fluid: Fluid): Identifier {
    Registry.register(Registry.FLUID, this, fluid)
    return this
}

fun Identifier.item(item: Item): Identifier {
    Registry.register(Registry.ITEM, this, item)
    return this
}

fun Identifier.blockEntityType(entityType: BlockEntityType<*>): Identifier {
    Registry.register(Registry.BLOCK_ENTITY_TYPE, this, entityType)
    return this
}

fun itemSettings(): FabricItemSettings = FabricItemSettings().group(IndustrialRevolution.MOD_GROUP)

fun <T : ScreenHandler> Identifier.registerScreenHandler(
    f: (Int, PlayerInventory, ScreenHandlerContext) -> T
): ExtendedScreenHandlerType<T> =
    ScreenHandlerRegistry.registerExtended(this) { syncId, inv, buf ->
        f(syncId, inv, ScreenHandlerContext.create(inv.player.world, buf.readBlockPos()))
    } as ExtendedScreenHandlerType<T>

fun BlockPos.toVec3d() = Vec3d(x.toDouble(), y.toDouble(), z.toDouble())

fun ChunkPos.asString() = "$x,$z"

fun getChunkPos(s: String): ChunkPos? {
    val split = s.split(",")
    val x = split[0].toIntOrNull() ?: return null
    val z = split[1].toIntOrNull() ?: return null
    return ChunkPos(x, z)
}

fun getFluidFromJson(json: JsonObject): FluidVolume {
    val fluidId = json.get("fluid").asString
    val fluidKey = FluidKeys.get(Registry.FLUID.get(Identifier(fluidId)))
    val amount = JsonHelper.getLong(json, "count", 1)
    val fluidAmount = when (val type = json.get("type").asString) {
        "nugget" -> NUGGET_AMOUNT
        "ingot" -> INGOT_AMOUNT
        "block" -> BLOCK_AMOUNT
        "bucket" -> FluidAmount.BUCKET
        "scrap" -> SCRAP_AMOUNT
        "bottle" -> FluidAmount.BOTTLE
        else -> throw IllegalArgumentException("unknown amount type $type")
    }.mul(amount)
    return fluidKey.withAmount(fluidAmount)
}

inline fun Box.any(f: (Int, Int, Int) -> Boolean): Boolean {
    for (x in minX.toInt()..maxX.toInt())
        for (y in minY.toInt()..maxY.toInt())
            for (z in minZ.toInt()..maxZ.toInt())
                if (f(x, y, z)) return true
    return false
}

inline fun Box.forEach(f: (Int, Int, Int) -> Unit) {
    for (x in minX.toInt() until maxX.toInt())
        for (y in minY.toInt() until maxY.toInt())
            for (z in minZ.toInt() until maxZ.toInt())
                f(x, y, z)
}

inline fun <T> Box.map(f: (Int, Int, Int) -> T): MutableList<T> {
    val list = mutableListOf<T>()
    for (x in minX.toInt() until maxX.toInt())
        for (y in minY.toInt() until maxY.toInt())
            for (z in minZ.toInt() until maxZ.toInt())
                list.add(f(x, y, z))
    return list
}

operator fun Box.contains(pos: BlockPos): Boolean {
    return contains(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
}

inline fun Box.firstOrNull(f: (Int, Int, Int) -> Boolean): BlockPos? {
    for (x in minX.toInt()..maxX.toInt())
        for (y in minY.toInt()..maxY.toInt())
            for (z in minZ.toInt()..maxZ.toInt())
                if (f(x, y, z)) return BlockPos(x, y, z)
    return null
}

fun createREIFluidWidget(widgets: MutableList<Widget>, startPoint: Point, fluid: FluidVolume) {
    widgets.add(Widgets.createTexturedWidget(WFluid.ENERGY_EMPTY, startPoint.x, startPoint.y, 0f, 0f, 16, 52, 16, 52))
    widgets.add(Widgets.createDrawableWidget { _, matrices, mouseX, mouseY, _ ->
        fluid.renderGuiRect(startPoint.x + 2.0, startPoint.y.toDouble() + 1.5, startPoint.x.toDouble() + 14, startPoint.y.toDouble() + 50)
        if (mouseX > startPoint.x && mouseX < startPoint.x + 16 && mouseY > startPoint.y && mouseY < startPoint.y + 52) {
            val information = mutableListOf<OrderedText>()
            information.addAll(fluid.fluidKey.fullTooltip.map { it.asOrderedText() })
            information.add(LiteralText("${(fluid.amount().asInexactDouble() * 1000).toInt()} mB").asOrderedText())
            MinecraftClient.getInstance().currentScreen?.renderOrderedTooltip(matrices, information, mouseX, mouseY)
        }
    })
}

inline fun IntArray.associateStacks(transform: (Int) -> ItemStack): Map<Item, Int> {
    return associateToStacks(HashMap(5), transform)
}

inline fun <M : MutableMap<Item, Int>> IntArray.associateToStacks(destination: M, transform: (Int) -> ItemStack): M {
    for (element in this) {
        val stack = transform(element)
        if (!stack.isEmpty && stack.tag?.isEmpty != false)
            destination.merge(stack.item, stack.count) { old, new -> old + new }
    }
    return destination
}

fun World.setBlockState(pos: BlockPos, state: BlockState, condition: (BlockState) -> Boolean) {
    val blockState = getBlockState(pos)
    if (condition(blockState)) setBlockState(pos, state)
}

fun EnergyIo.use(amount: Double): Boolean {
    if (extract(amount, Simulation.SIMULATE) == amount) {
        extract(amount, Simulation.ACT)
        return true
    }
    return false
}

fun World.isLoaded(pos: BlockPos): Boolean {
    return chunkManager.isChunkLoaded(pos.x shr 4, pos.z shr 4)
}