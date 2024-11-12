package settingdust.item_converter.client

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Button.OnPress
import net.minecraft.client.gui.components.Button.OnTooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import org.apache.commons.lang3.math.Fraction
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.traverse.BreadthFirstIterator
import org.lwjgl.glfw.GLFW
import settingdust.item_converter.C2SConvertItemPacket
import settingdust.item_converter.C2SConvertItemPacket.Mode
import settingdust.item_converter.ConvertRules
import settingdust.item_converter.DrawableNineSliceTexture
import settingdust.item_converter.ItemConverter
import settingdust.item_converter.Networking
import settingdust.item_converter.SimpleItemPredicate

data class ItemConvertScreen(
    val parent: Screen?, val slot: Slot?, val slotIndex: Int
) : Screen(Component.translatable("gui.${ItemConverter.ID}.item_convert")) {
    companion object {
        private val TEXTURE = ItemConverter.id("textures/gui/window.png")
        private const val TEXTURE_WIDTH = 128
        private const val TEXTURE_HEIGHT = 128

        private const val WIDTH = 108
        private const val HEIGHT = 30

        private const val BORDER = 6

        private const val SLOT_SIZE = 18

        val texture = DrawableNineSliceTexture(
            TEXTURE,
            TEXTURE_WIDTH,
            TEXTURE_HEIGHT,
            0,
            0,
            WIDTH,
            HEIGHT,
            BORDER,
            BORDER,
            BORDER,
            BORDER
        )
    }

    private val isContainer = parent is AbstractContainerScreen<*>

    private var x = 0
    private var y = 0
    private var slotInRow = 5
    private var slotInColumn = 1

    private fun getFrom() = slot?.item ?: Minecraft.getInstance().player!!.inventory.getItem(slotIndex)

    override fun init() {
        val input = getFrom()
        val from = SimpleItemPredicate(getFrom())
        val targets =
            BreadthFirstIterator(ConvertRules.graph, from).asSequence().mapNotNull { to ->
                val path = DijkstraShortestPath.findPathBetween(ConvertRules.graph, from, to) ?: return@mapNotNull null
                val ratio = path.edgeList.fold(Fraction.ONE) { acc, (fraction) -> fraction.multiplyBy(acc) }
                if (ratio.denominator > input.count) return@mapNotNull null
                return@mapNotNull Triple(to, path, ratio)
            }.toList()

        slotInRow = if (targets.size > 30) 11 else 5
        slotInColumn = targets.size / slotInRow + 1
        x = (width - SLOT_SIZE * slotInRow) / 2 + BORDER
        y = (height - SLOT_SIZE * slotInColumn) / 2 + BORDER
        width = SLOT_SIZE * slotInRow + BORDER * 2
        height = SLOT_SIZE * slotInColumn + BORDER * 2

        for ((index, pair) in targets.withIndex()) {
            val (to, path, ratio) = pair
            val x = x + SLOT_SIZE * (index % slotInRow)
            val y = y + SLOT_SIZE * (index / slotInRow)
            val button = ItemButton(
                to.predicate.copy().apply { count = ratio.numerator }, x, y, SLOT_SIZE, SLOT_SIZE,
                OnPress {
                    val mode = if (!hasShiftDown()) Mode.ONE else Mode.ALL
                    val button = it as ItemButton
                    val stack = button.item.copy()
                    if (parent is CreativeModeInventoryScreen) {
                        stack.popTime = 5
                        val player = minecraft!!.player!!
                        when (mode) {
                            Mode.ONE -> {
                                stack.count = ratio.numerator
                                player.inventory.add(stack)
                            }

                            Mode.ALL -> {
                                val times = input.count / ratio.denominator
                                val amount = ratio.denominator * times
                                stack.count = amount
                                player.inventory.add(stack)
                            }
                        }
                        val slotIndex = player.inventory.findSlotMatchingItem(stack)
                        if (slotIndex != -1) {
                            player.level.playSound(
                                player,
                                player.blockPosition(),
                                SoundEvents.ITEM_PICKUP,
                                SoundSource.PLAYERS,
                                0.2F,
                                (player.random.nextFloat() * 0.7F + 1.0F) * 2.0F
                            )
                            minecraft!!.gameMode!!.handleCreativeModeItemAdd(stack, slotIndex)
                        }
                    } else {
                        Networking.channel.sendToServer(
                            C2SConvertItemPacket(
                                slotIndex,
                                stack,
                                mode
                            )
                        )
                    }
                },
                OnTooltip { button, pose, mouseX, mouseY ->
                    val button = button as ItemButton
                    renderTooltip(pose, buildList {
                        add(Component.literal("${ratio.numerator}:${ratio.denominator}"))
                        addAll(getTooltipFromItem(button.item))
                        if (Minecraft.getInstance().options.advancedItemTooltips) {
                            add(Component.literal("Path:"))
                            path.edgeList.dropLast(1).fold(Component.empty()) { acc, edge ->
                                val (fraction) = edge
                                val sourceVertex = ConvertRules.graph.getEdgeSource(edge)
                                acc.append(sourceVertex.predicate.displayName)
                                    .append(" x${fraction.numerator}\n>${fraction.denominator}x ")
                            }
                            add(path.vertexList.last().predicate.displayName)
                        }
                    }, button.item.tooltipImage, mouseX, mouseY)
                }
            )
            addRenderableWidget(button)
        }

        if (!isContainer) {
            GLFW.glfwSetCursorPos(minecraft!!.window.window, x.toDouble(), y.toDouble())
        }
    }

    override fun renderBackground(poseStack: PoseStack) {
        texture.draw(poseStack, x, y, width, height)
    }
}

open class ItemButton(
    val item: ItemStack,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    onPress: OnPress,
    onTooltip: OnTooltip
) :
    Button(x, y, width, height, Component.empty(), onPress, onTooltip) {
    override fun renderButton(pose: PoseStack, mouseX: Int, mouseY: Int, partialTick: Float) {
        val minecraft = Minecraft.getInstance()
        minecraft.itemRenderer.renderAndDecorateItem(minecraft.player!!, item, x, y, 0)
        if (isHoveredOrFocused)
            fill(pose, x, y, x + width, y + height, (0x888888 or ((alpha * 0xFF).toInt() shl 24)).toInt())
    }
}