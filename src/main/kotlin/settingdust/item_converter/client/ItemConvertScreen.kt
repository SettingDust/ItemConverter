package settingdust.item_converter.client

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Button.OnPress
import net.minecraft.client.gui.components.Button.OnTooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraftforge.client.ForgeHooksClient
import net.minecraftforge.client.event.ScreenEvent
import net.minecraftforge.common.MinecraftForge
import org.apache.commons.lang3.math.Fraction
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.traverse.BreadthFirstIterator
import settingdust.item_converter.C2SConvertItemPacket
import settingdust.item_converter.C2SConvertItemPacket.Mode
import settingdust.item_converter.ConvertRules
import settingdust.item_converter.DrawableNineSliceTexture
import settingdust.item_converter.ItemConverter
import settingdust.item_converter.Networking
import settingdust.item_converter.SimpleItemPredicate

data class ItemConvertScreen(
    val parent: Screen?, val slot: Slot
) : Screen(Component.translatable("gui.${ItemConverter.ID}.item_convert")) {
    companion object {
        private val TEXTURE = ItemConverter.id("textures/gui/window.png")
        private const val TEXTURE_WIDTH = 128
        private const val TEXTURE_HEIGHT = 128

        private const val WIDTH = 102
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

    private var x = 0
    private var y = 0
    private var width = 0
    private var height = 0
    private var slotInRow = 5
    private var slotInColumn = 1
    private val input = getFrom()

    private fun getFrom() = slot.item

    override fun init() {
        val from = SimpleItemPredicate(getFrom())
        if (ConvertRules.graph.vertexSet().isEmpty() || input.isEmpty || from !in ConvertRules.graph.vertexSet()) {
            onClose()
            return
        }
        val targets =
            BreadthFirstIterator(ConvertRules.graph, from).asSequence().mapNotNull { to ->
                val path = DijkstraShortestPath.findPathBetween(ConvertRules.graph, from, to) ?: return@mapNotNull null
                if (path.vertexList.size == 1) return@mapNotNull null
                val ratio = path.edgeList.fold(Fraction.ONE) { acc, edge -> edge.fraction.multiplyBy(acc) }
                if (ratio.denominator > input.count) return@mapNotNull null
                return@mapNotNull Triple(to, path, ratio)
            }.toList()

        if (targets.isEmpty()) onClose()

        slotInRow = if (targets.size > 30) 11 else 5
        slotInColumn = targets.size / slotInRow + 1
        width = SLOT_SIZE * slotInRow + BORDER * 2
        height = SLOT_SIZE * slotInColumn + BORDER * 2

        x = (super.width - width) / 2
        y = (super.height - height) / 2

        for ((index, pair) in targets.withIndex()) {
            val (to, path, ratio) = pair
            val x = x + BORDER + SLOT_SIZE * (index % slotInRow)
            val y = y + BORDER + SLOT_SIZE * (index / slotInRow)
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

                        for ((i, it) in player.inventoryMenu.slots.withIndex()) {
                            if (!ItemStack.isSameItemSameTags(it.item, button.item)) continue
                            player.level.playSound(
                                player,
                                player.blockPosition(),
                                SoundEvents.ITEM_PICKUP,
                                SoundSource.PLAYERS,
                                0.2F,
                                (player.random.nextFloat() * 0.7F + 1.0F) * 2.0F
                            )
                            minecraft!!.gameMode!!.handleCreativeModeItemAdd(it.item, i)
                        }
                    } else {
                        Networking.channel.sendToServer(
                            C2SConvertItemPacket(
                                slot.index,
                                stack,
                                mode
                            )
                        )
                    }
                },
                OnTooltip { button, pose, mouseX, mouseY ->
                    val button = button as ItemButton
                    renderTooltip(pose, buildList {
                        add(Component.literal("${ratio.denominator}:${ratio.numerator}"))
                        addAll(getTooltipFromItem(button.item))
                        if (Minecraft.getInstance().options.advancedItemTooltips) {
                            add(Component.literal("Path:"))
                            if (path.edgeList.isNotEmpty()) {
                                val edge = path.edgeList[0]
                                val fraction = edge.fraction
                                val sourceVertex = ConvertRules.graph.getEdgeSource(edge)
                                add(sourceVertex.predicate.displayName.copy().append(" x${fraction.denominator}"))
                            }
                            for (edges in path.edgeList.windowed(2, partialWindows = true)) {
                                val firstEdge = edges[0]
                                val firstFraction = firstEdge.fraction
                                val targetVertex = ConvertRules.graph.getEdgeTarget(firstEdge)
                                val secondComponent = Component.literal(">${firstFraction.numerator}x ")
                                    .append(targetVertex.predicate.displayName)
                                if (edges.size == 2) {
                                    val secondEdge = edges[1]
                                    val secondFraction = secondEdge.fraction
                                    secondComponent.append(" x${secondFraction.denominator}")
                                }
                                add(secondComponent)
                            }
                        }
                    }, button.item.tooltipImage, mouseX, mouseY)
                }
            )
            addRenderableWidget(button)
        }
    }

    override fun render(poseStack: PoseStack, mouseX: Int, mouseY: Int, partialTick: Float) {
        if (!SlotInteractManager.converting || getFrom().isEmpty) onClose()
        renderBackground(poseStack)
        super.render(poseStack, mouseX, mouseY, partialTick)
    }

    override fun renderBackground(poseStack: PoseStack) {
        texture.draw(poseStack, x, y, width, height)
        MinecraftForge.EVENT_BUS.post(ScreenEvent.BackgroundRendered(this, poseStack));
    }

    override fun onClose() {
        super.onClose()
        SlotInteractManager.pressedTicks = 0
        SlotInteractManager.converting = false
    }

    override fun isPauseScreen(): Boolean {
        return false
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
    @Suppress("UnstableApiUsage")
    override fun renderButton(pose: PoseStack, mouseX: Int, mouseY: Int, partialTick: Float) {
        val minecraft = Minecraft.getInstance()
        minecraft.itemRenderer.blitOffset = ForgeHooksClient.getGuiFarPlane() - 3000
        minecraft.itemRenderer.renderAndDecorateItem(minecraft.player!!, item, x + 1, y + 1, 0)
        if (isHoveredOrFocused) {
            fill(pose, x + 1, y + 1, x + width - 1, y + height - 1, 0x80FFFFFF.toInt())
            this.renderToolTip(pose, mouseX, mouseY);
        }
        minecraft.itemRenderer.blitOffset = 0f
    }
}