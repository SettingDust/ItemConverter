package settingdust.item_converter

import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import org.apache.commons.lang3.math.Fraction
import org.jgrapht.graph.DefaultWeightedEdge

class FractionUnweightedEdge(
    var fraction: Fraction,
    val sound: SoundEvent = SoundEvents.ITEM_PICKUP,
    val pitch: Float = 1.0f,
    val volume: Float = 1.0f
) : DefaultWeightedEdge() {
    override fun toString() = fraction.toString()
}