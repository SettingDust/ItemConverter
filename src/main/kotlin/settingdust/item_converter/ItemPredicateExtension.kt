package settingdust.item_converter

import net.minecraft.advancements.critereon.ItemPredicate
import net.minecraft.advancements.critereon.MinMaxBounds
import net.minecraft.world.item.ItemStack

fun ItemStack.toItemPredicate() =
    ItemPredicate.Builder.item().of(item).apply {
        withCount(MinMaxBounds.Ints.atLeast(count))
        if (isDamageableItem)
            hasDurability(MinMaxBounds.Ints.atLeast(maxDamage - damageValue))
        if (tag != null) {
            hasNbt(tag!!)
        }
    }.build()