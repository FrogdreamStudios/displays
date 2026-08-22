package com.dreamdisplays.platform.client.render

import com.dreamdisplays.api.render.backend.model.ShaderBackend

/** Shader-pack detector: checks for `Iris`, `OptiFine`, or `Canvas` shader packs without adding hard dependencies. */
internal object ShaderPackCompat {
    /** True when any supported shader pack is currently in use. */
    val isShaderPackActive: Boolean get() = shaderBackend() != ShaderBackend.NONE

    /**
     * Identity of the current shader state. Changes whenever a pack is enabled, disabled, swapped, or reloaded,
     * so callers can drop pipeline-bound GPU resources; the backend alone would miss a pack-to-pack switch.
     */
    fun shaderStateToken(): Any = shaderBackend() to irisPipelineId()

    /** Identity hash of Iris's live rendering pipeline, or 0 without Iris. A reload replaces that object. */
    private fun irisPipelineId(): Int = runCatching {
        val manager = Class.forName("net.irisshaders.iris.Iris").getMethod("getPipelineManager").invoke(null)
        val pipeline = manager.javaClass.getMethod("getPipelineNullable").invoke(manager)
        System.identityHashCode(pipeline)
    }.getOrDefault(0)

    /** Active shader backend, or [ShaderBackend.NONE]. */
    fun shaderBackend(): ShaderBackend = when {
        irisShaderPackActive() -> ShaderBackend.IRIS
        optifineShaderPackActive() -> ShaderBackend.OPTIFINE
        canvasRendererActive() -> ShaderBackend.CANVAS
        else -> ShaderBackend.NONE
    }

    /** `Iris` shaders. */
    private fun irisShaderPackActive(): Boolean = runCatching {
        val apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi")
        val api = apiClass.getMethod("getInstance").invoke(null)
        api.javaClass.getMethod("isShaderPackInUse").invoke(api) as? Boolean == true
    }.getOrDefault(false)

    /** `Optifine` shaders. */
    private fun optifineShaderPackActive(): Boolean = runCatching {
        Class.forName("net.optifine.Config").getMethod("isShaders").invoke(null) as? Boolean == true
    }.getOrDefault(false)

    /** `Canvas` shaders (it's an old project, but it's still in use by some people). */
    private fun canvasRendererActive(): Boolean =
        classPresent("grondag.canvas.CanvasMod") || classPresent("io.vram.canvas.CanvasFabricMod")

    /** True if the given class is present. */
    private fun classPresent(name: String): Boolean = runCatching {
        Class.forName(name, false, ShaderPackCompat::class.java.classLoader)
        true
    }.getOrDefault(false)
}
