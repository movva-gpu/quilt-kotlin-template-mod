package com.example.example_mod

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator

class ExampleModDataGenerator : DataGeneratorEntrypoint {
    override fun onInitializeDataGenerator(generator: FabricDataGenerator) {
        val pack = generator.createPack()

        // Add providers to `pack` here. e.g.:
        // pack.addProvider(::ExampleModBlockTagProvider)
    }
}
