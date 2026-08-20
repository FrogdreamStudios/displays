package com.dreamdisplays.platform.server.registrar

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.github.arnodoelinger.platformweaver.FabricOnly
import io.github.arnodoelinger.platformweaver.NeoForgeOnly
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry.registerArgumentType
import net.minecraft.commands.synchronization.ArgumentTypeInfos
import net.minecraft.commands.synchronization.SingletonArgumentInfo
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import java.util.concurrent.CompletableFuture
//? if >=1.21.11 {
import net.minecraft.resources.Identifier

//?} else
/*import net.minecraft.resources.ResourceLocation as Identifier*/

/**
 * Space-delimited unquoted token for bare selectors, URLs in `/display fullscreen` commands.
 */
object BareTokenArgumentType : ArgumentType<String> {
    private val MISSING = SimpleCommandExceptionType(LiteralMessage("Expected a value."))
    val ID: Identifier = Identifier.fromNamespaceAndPath("dreamdisplays", "bare_token")

    override fun parse(reader: StringReader): String {
        val start = reader.cursor
        while (reader.canRead() && reader.peek() != ' ') reader.skip()
        if (reader.cursor == start) throw MISSING.create()
        return reader.string.substring(start, reader.cursor)
    }
}

/**
 * Registers [BareTokenArgumentType]'s sync info via `Fabric` API's public `ArgumentTypeRegistry`.
 */
@FabricOnly
object FabricBareTokenArgumentType {
    private var registered = false

    /** Idempotent; call once, early, from mod init. */
    fun register() {
        if (registered) return
        registered = true
        val info = SingletonArgumentInfo.contextFree { BareTokenArgumentType }
        registerArgumentType(
            BareTokenArgumentType.ID, BareTokenArgumentType::class.java, info,
        )
    }
}

/**
 * Registers [BareTokenArgumentType]'s sync via `NeoForge` reflection.
 */
@NeoForgeOnly
object NeoForgeBareTokenArgumentType {
    private var registered = false

    /** Idempotent; call once, early, from mod init. */
    fun register() {
        if (registered) return
        registered = true
        val field = ArgumentTypeInfos::class.java.getDeclaredField("BY_CLASS")
        field.isAccessible = true
        val byClass = field.get(null)!!
        val info = SingletonArgumentInfo.contextFree { BareTokenArgumentType }
        byClass.javaClass.getMethod("putIfAbsent", Any::class.java, Any::class.java)
            .invoke(byClass, BareTokenArgumentType::class.java, info)
        Registry.register(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, BareTokenArgumentType.ID, info)
    }
}

object GreedyStringAsAny : ArgumentType<Any> {
    private val delegate = StringArgumentType.greedyString()
    override fun parse(reader: StringReader): Any = delegate.parse(reader)
    override fun <S> listSuggestions(context: CommandContext<S>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> =
        delegate.listSuggestions(context, builder)
    override fun getExamples(): Collection<String> = delegate.examples
}
