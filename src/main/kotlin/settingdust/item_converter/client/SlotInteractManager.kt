package settingdust.item_converter.client

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiComponent
import net.minecraft.client.gui.components.Widget
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent
import net.minecraftforge.client.event.RegisterKeyMappingsEvent
import net.minecraftforge.client.event.RenderGuiEvent
import net.minecraftforge.client.event.ScreenEvent
import net.minecraftforge.client.gui.overlay.ForgeGui
import net.minecraftforge.client.gui.overlay.GuiOverlayManager
import net.minecraftforge.client.gui.overlay.IGuiOverlay
import net.minecraftforge.event.TickEvent
import settingdust.item_converter.ItemConverter
import thedarkcolour.kotlinforforge.forge.FORGE_BUS

object SlotInteractManager {
    const val PRESS_TICKS = 20
    var pressedTicks = 0
    var converting = false
    val KEY = KeyMapping("key.item_converter.slot_interact", InputConstants.KEY_LALT, "key.categories.inventory")

    init {
        FORGE_BUS.register { event: RegisterKeyMappingsEvent ->
            event.register(KEY)
        }
        FORGE_BUS.register { event: ScreenEvent.KeyPressed.Pre ->
            pressedTicks = 0
            KEY.isDown = true
        }
        FORGE_BUS.register { event: ScreenEvent.KeyReleased.Pre ->
            pressedTicks = 0
            KEY.isDown = false
            converting = false
        }
        FORGE_BUS.register { event: TickEvent.ClientTickEvent ->
            if (event.phase != TickEvent.Phase.END) return@register
            if (KEY.isDown) {
                pressedTicks++
            }
        }
        FORGE_BUS.register { event: ScreenEvent.Opening ->
            pressedTicks = 0
        }

        FORGE_BUS.register { event: RegisterGuiOverlaysEvent ->
            event.registerAboveAll("${ItemConverter.ID}:slot_interact_progress", SlotInteractProgress())
        }

        val progress = SlotInteractProgress()
        FORGE_BUS.register { event: RenderGuiEvent.Post ->
            val minecraft = Minecraft.getInstance()
            val screen = minecraft.screen
            if (pressedTicks <= PRESS_TICKS) {
                if (screen is AbstractContainerScreen<*>) {
                    if (screen.slotUnderMouse != null && screen.menu.carried.isEmpty) {
                        val hoveredSlot = screen.slotUnderMouse!!
                        progress.x = screen.guiLeft + hoveredSlot.x
                        progress.y = screen.guiTop + hoveredSlot.y
                        progress.render(event.poseStack)
                    } else {
                        pressedTicks = 0
                    }
                }
            } else {
                if (screen is AbstractContainerScreen<*> && screen.slotUnderMouse != null && screen.menu.carried.isEmpty) {
                    if (!converting) {
                        minecraft.pushGuiLayer(
                            ItemConvertScreen(
                                screen,
                                screen.slotUnderMouse,
                                screen.slotUnderMouse!!.index
                            )
                        )
                    }
                    converting = true
                } else if (screen == null) {
                    minecraft.pushGuiLayer(ItemConvertScreen(screen, null, minecraft.player!!.inventory.selected))
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
        val progress = (SlotInteractManager.pressedTicks / SlotInteractManager.PRESS_TICKS.toFloat())
        val width = (WIDTH * progress).toInt().coerceIn(0, 1)
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
        x = screenWidth / 2 - 91 + gui.minecraft.player!!.inventory.selected * 20
        y = screenHeight - 22
        render(poseStack)
    }
}