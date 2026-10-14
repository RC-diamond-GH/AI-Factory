package com.rcdiamond.aifactory.addon.jei.client.integration

import com.mojang.logging.LogUtils
import com.rcdiamond.aifactory.addon.jei.client.JeiQueryGateway
import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.runtime.IJeiRuntime
import net.minecraft.resources.ResourceLocation

/**
 * JEI entry point of the optional recipe-query addon.
 *
 * JEI discovers this class itself: it scans the mod's classes for the [JeiPlugin] annotation during
 * mod construction, so the base mod never references it (no Forge event subscriber and no mod event
 * bus registration is needed, and nothing JEI-typed is loaded when JEI is absent). JEI instantiates
 * it with its no-argument constructor.
 *
 * ### What it owns
 * Only the lifetime of the backend. `onRuntimeAvailable` builds one [JeiQueryBackend] per JEI runtime
 * generation and publishes it on [JeiQueryGateway]; `onRuntimeUnavailable` (world leave, or JEI being
 * reloaded) uninstalls exactly that instance and releases the reference. The gateway delegates the
 * pure [com.rcdiamond.aifactory.addon.jei.client.JeiQueryService] calls to the installed backend and
 * answers `{ok:false, error:"jei_not_ready"}` whenever none is installed, so a client without JEI, or
 * a query while JEI reloads, fails closed instead of reaching JEI types.
 *
 * The generation stamp increases with every runtime, which is what makes a key from a previous
 * session unresolvable rather than reusable (see [JeiQueryBackend]).
 */
@JeiPlugin
class AiFactoryJeiPlugin : IModPlugin {

    private var backend: JeiQueryBackend? = null
    private var generation = 0L

    override fun getPluginUid(): ResourceLocation = ResourceLocation.fromNamespaceAndPath("ai_factory", "recipe_query")

    override fun onRuntimeAvailable(jeiRuntime: IJeiRuntime) {
        // JEI can call this again after a world change; the previous generation must not stay
        // installed (its key bindings belong to the previous runtime).
        val previous = backend
        if (previous != null) {
            JeiQueryGateway.uninstall(previous)
        }
        val next = JeiQueryBackend(jeiRuntime, ++generation)
        backend = next
        JeiQueryGateway.install(next)
        LOG.info("AI Factory JEI recipe queries are available (runtime generation $generation).")
    }

    override fun onRuntimeUnavailable() {
        val current = backend ?: return
        backend = null
        JeiQueryGateway.uninstall(current)
        LOG.info("AI Factory JEI recipe queries are unavailable until JEI's runtime is back.")
    }

    private companion object {
        private val LOG = LogUtils.getLogger()
    }
}
