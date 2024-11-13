package settingdust.item_converter.client

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiComponent
import net.minecraft.client.gui.components.Widget
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.client.event.InputEvent
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent
import net.minecraftforge.client.event.RegisterKeyMappingsEvent
import net.minecraftforge.client.event.RenderGuiEvent
import net.minecraftforge.client.event.ScreenEvent
import net.minecraftforge.client.gui.overlay.ForgeGui
import net.minecraftforge.client.gui.overlay.GuiOverlayManager
import net.minecraftforge.client.gui.overlay.IGuiOverlay
import net.minecraftforge.event.TickEvent
import thedarkcolour.kotlinforforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.forge.MOD_BUS

object SlotInteractManager {
    const val PRESS_TICKS = 20
    var pressedTicks = 0
    var converting = false
    val KEY = KeyMapping("key.item_converter.slot_interact", InputConstants.KEY_LALT, "key.categories.inventory")

    init {
        MOD_BUS.addListener { event: RegisterKeyMappingsEvent ->
            event.register(KEY)
        }
        FORGE_BUS.addListener { event: InputEvent.Key ->
            if (event.key == KEY.key.value) {
                when (event.action) {
                    InputConstants.PRESS -> {
                        KEY.isDown = true
                        pressedTicks = 0
                    }

                    InputConstants.RELEASE -> {
                        KEY.isDown = false
                        pressedTicks = 0
                        converting = false
                    }
                }
            }
        }

        FORGE_BUS.addListener { event: TickEvent.ClientTickEvent ->
            if (event.phase != TickEvent.Phase.END) return@addListener
            val minecraft = Minecraft.getInstance()
            val screen by lazy { minecraft.screen }
            val inventory by lazy { minecraft.player!!.inventory }
            if (KEY.isDown &&
                ((screen is AbstractContainerScreen<*>
                        && (screen as AbstractContainerScreen<*>).slotUnderMouse != null
                        && (screen as AbstractContainerScreen<*>).menu.carried.isEmpty)
                        || (!inventory.getItem(inventory.selected).isEmpty && screen == null))
            ) {
                pressedTicks++
            }
        }

        FORGE_BUS.addListener { event: ScreenEvent.Opening ->
            pressedTicks = 0
        }

        FORGE_BUS.addListener { event: ScreenEvent.Closing ->
            pressedTicks = 0
        }

        MOD_BUS.addListener { event: RegisterGuiOverlaysEvent ->
            event.registerAboveAll("slot_interact_progress", SlotInteractProgress())
        }

        val progress = SlotInteractProgress()
        FORGE_BUS.addListener { event: RenderGuiEvent.Post ->
            val minecraft = Minecraft.getInstance()
            val screen = minecraft.screen
            if (pressedTicks <= PRESS_TICKS) {
                if (screen is AbstractContainerScreen<*>) {
                    val hoveredSlot = screen.slotUnderMouse!!
                    progress.x = screen.guiLeft + hoveredSlot.x
                    progress.y = screen.guiTop + hoveredSlot.y
                    progress.render(event.poseStack)
                }
            } else {
                if (!converting) {
                    if (screen is AbstractContainerScreen<*>) {
                        minecraft.pushGuiLayer(
                            ItemConvertScreen(
                                screen,
                                screen.slotUnderMouse,
                                screen.slotUnderMouse!!.index
                            )
                        )
                        converting = true
                    } else if (screen == null) {
                        val inventory = minecraft.player!!.inventory
                        val selected = inventory.selected
                        if (inventory.getItem(selected).isEmpty) return@addListener
                        minecraft.pushGuiLayer(ItemConvertScreen(screen, null, selected))
                        converting = true
                        pressedTicks = 0
                    }
                }
            }
        }
    }
}

data class SlotInteractProgress(
    var x: Int = 0,
    var y: Int = 0
) : GuiComponent(), Widget, IGuiOverlay {
    companion object {
        const val WIDTH = 16
        const val HEIGHT = 2
        const val COLOR = 0xFFF0F0F0.toInt()
    }

    fun render(poseStack: PoseStack) {
        if (SlotInteractManager.converting) return
        val progress =
            (SlotInteractManager.pressedTicks / SlotInteractManager.PRESS_TICKS.toFloat()).coerceIn(0f, 1f)
        val width = (WIDTH * progress).toInt()
        if (width > 0) {
            fill(poseStack, x, y, x + width, y + HEIGHT, COLOR)
        }
    }

    override fun render(
        poseStack: PoseStack,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float
    ) = render(poseStack)

    override fun render(
        gui: ForgeGui,
        poseStack: PoseStack,
        partialTick: Float,
        screenWidth: Int,
        screenHeight: Int
    ) {
        if (gui.minecraft.options.hideGui) return
        if (GuiOverlayManager.findOverlay(ResourceLocation("minecraft:hotbar")) == null) return
        val inventory = gui.minecraft.player!!.inventory
        x = screenWidth / 2 - 91 + inventory.selected * 20 + 3
        y = screenHeight - 22 + 3
        render(poseStack)
    }
}