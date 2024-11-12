package settingdust.item_converter.client

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import settingdust.item_converter.ItemConverter

data class ItemConvertScreen(
    val parent: Screen?,
    val slot: Slot?,
    val slotIndex: Int,
    val input: ItemStack
) : Screen(Component.translatable("gui.${ItemConverter.ID}.item_convert")) {
    companion object {
        fun getMousePosition(): Pair<Double, Double> {
            val mouseHandler = Minecraft.getInstance().mouseHandler
            return mouseHandler.xpos() to mouseHandler.ypos()
        }
    }

    private val originalMousePos = getMousePosition()
}