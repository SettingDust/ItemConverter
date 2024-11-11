package settingdust.item_converter

import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.registries.NewRegistryEvent
import net.minecraftforge.registries.RegistryBuilder
import settingdust.item_converter.ConvertRules.KEY
import settingdust.item_converter.ItemConverter.id

object ModEventHandler {
    @SubscribeEvent
    internal fun onNewRegistry(event: NewRegistryEvent) {
        RuleGeneratorTypes.REGISTRY = event.create(
            RegistryBuilder<RuleGeneratorType>().setName(RuleGeneratorTypes.KEY.location()).disableSync()
        ) { it.register(id("recipe"), RuleGeneratorTypes.RECIPE) }
        RuleGenerators.REGISTRY = event.create(
            RegistryBuilder<RuleGenerator>().setName(RuleGenerators.KEY.location())
                .dataPackRegistry(RuleGenerator.CODEC).disableSync()
        )
        ConvertRules.REGISTRY = event.create(
            RegistryBuilder<ConvertRule>().setName(KEY.location())
                .dataPackRegistry(ConvertRule.CODEC, ConvertRule.CODEC)
        )
    }
}