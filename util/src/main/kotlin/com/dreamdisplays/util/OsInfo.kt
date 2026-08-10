package com.dreamdisplays.util

import java.util.*

/**
 * Single source of truth for OS / architecture detection.
 */
object OsInfo {
    private val os: String = System.getProperty("os.name", "").lowercase(Locale.ENGLISH)
    private val arch: String = System.getProperty("os.arch", "").lowercase(Locale.ENGLISH)

    val isWindows: Boolean = "win" in os
    val isMac: Boolean = "mac" in os

    /** True on Linux and Linux-derived kernels (`"nux"`/`"nix"` in `os.name`); false on BSD/Solaris/other Unix. */
    val isLinux: Boolean = "nux" in os || "nix" in os

    /** True on any 64-bit or 32-bit ARM architecture (aarch64, arm64, armv7, ...). */
    val isArm: Boolean = "aarch64" in arch || "arm64" in arch || "arm" in arch

    /** True specifically on 64-bit ARM. */
    val isArm64: Boolean = "aarch64" in arch || "arm64" in arch
}
