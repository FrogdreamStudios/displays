package com.dreamdisplays.platform.proxy

import io.github.arnodoelinger.platformweaver.PlatformOnly

/**
 * Marks a declaration as `Velocity`-specific.
 *
 * @see BungeecordOnly
 */
@PlatformOnly("velocity")
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
)
@Retention(AnnotationRetention.SOURCE)
annotation class VelocityOnly
