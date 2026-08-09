import java.util.*

/** Read versions/active.txt once, share as Gradle extension for all build scripts. */
class StonecutterVersions internal constructor(val active: String, private val properties: Properties) {
    /** Looks up a required property for the active `Stonecutter` version. */
    fun get(name: String): String = properties.getProperty(name)
        ?: error("Missing Stonecutter version property '$name' for $active.")

    /** Looks up an optional property for the active `Stonecutter` version. */
    fun getOrNull(name: String): String? = properties.getProperty(name)
}
