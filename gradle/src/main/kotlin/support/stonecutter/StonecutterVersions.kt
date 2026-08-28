package support.stonecutter

import java.util.*

class StonecutterVersions internal constructor(val active: String, private val properties: Properties) {
    fun get(name: String): String = properties.getProperty(name)
        ?: error("Missing Stonecutter version property '$name' for $active.")

    fun getOrNull(name: String): String? = properties.getProperty(name)
}
