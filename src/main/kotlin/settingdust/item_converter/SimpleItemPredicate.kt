package settingdust.item_converter

import net.minecraft.advancements.critereon.NbtPredicate
import net.minecraft.world.item.ItemStack
import org.antlr.v4.runtime.misc.Predicate

data class SimpleItemPredicate(
    val predicate: ItemStack
) : Predicate<ItemStack> {
    private val nbt = predicate.tag?.let { NbtPredicate(it) }

    override fun test(item: ItemStack): Boolean {
        if (!item.`is`(predicate.item)) return false
        if (nbt?.matches(item) == false) return false
        return true
    }

    override fun equals(other: Any?): Boolean {
        return predicate.serializeNBT()
            .also { it.remove("Count") } == (other as? SimpleItemPredicate)?.predicate?.serializeNBT()
            ?.also { it.remove("Count") }
    }

    override fun hashCode(): Int {
        return predicate.serializeNBT().also { it.remove("Count") }.hashCode()
    }
}
