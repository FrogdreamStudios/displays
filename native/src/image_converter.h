#ifndef IMAGE_CONVERTER_H
#define IMAGE_CONVERTER_H

#include <jni.h>

// Image converter

// Compiler hints for optimization
#if defined(__GNUC__) || defined(__clang__)
    #define LIKELY(x)   __builtin_expect(!!(x), 1)
    #define UNLIKELY(x) __builtin_expect(!!(x), 0)
    #define PREFETCH(addr) __builtin_prefetch(addr, 0, 3)
    #define PREFETCH_WRITE(addr) __builtin_prefetch(addr, 1, 3)
#else
    #define LIKELY(x)   (x)
    #define UNLIKELY(x) (x)
    #define PREFETCH(addr)
    #define PREFETCH_WRITE(addr)
#endif

// Check for SIMD support
#if defined(__SSE2__) || defined(__ARM_NEON)
    #define USE_SIMD 1
    #if defined(__SSE2__)
        #include <emmintrin.h>
        #include <smmintrin.h> // SSE4.1 for stream stores
        #define SIMD_WIDTH 16
    #elif defined(__ARM_NEON)
        #include <arm_neon.h>
        #define SIMD_WIDTH 16
    #endif
#else
    #define USE_SIMD 0
#endif

#endif // IMAGE_CONVERTER_H
