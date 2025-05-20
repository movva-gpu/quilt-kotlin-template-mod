package com.example.example_mod

import net.minecraft.util.Identifier
import org.quiltmc.loader.api.ModContainer
import org.quiltmc.loader.api.ModMetadata
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ExampleMod : ModInitializer {
    override fun onInitialize(mod: ModContainer) {
        modMetadata = mod.metadata()

        LOGGER.info("Hello Quilt world from ${mod.metadata().name()}! Remember to stay fresh!")
        LOGGER.info("This is Pineapple: ${"pineapple".id()}")
    }

    companion object {
        // This logger is used to write text to the console and the log file.
        // It is considered best practice to use your mod name as the logger's name.
        // That way, it's clear which mod wrote info, warnings, and errors.
        val LOGGER: Logger = LoggerFactory.getLogger("Example Mod")

        private var modMetadata: ModMetadata? = null
        val METADATA: ModMetadata
            get() = modMetadata!!

        fun String.id(): Identifier = Identifier(METADATA.id(), this)
    }
}
