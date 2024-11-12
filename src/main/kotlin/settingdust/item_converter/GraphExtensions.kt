package settingdust.item_converter

import org.apache.commons.lang3.math.Fraction
import org.jgrapht.graph.DefaultWeightedEdge

data class FractionUnweightedEdge(
    var fraction: Fraction
) : DefaultWeightedEdge() {
    override fun toString() = fraction.toString()
}