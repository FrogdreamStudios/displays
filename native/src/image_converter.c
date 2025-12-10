#include <jni.h>
#include <string.h>
#include <stdint.h>
#include <math.h>

// Image format conversion (30-60 FPS)
// Optimizations: SIMD, loop unrolling, prefetching, compiler hints

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

// Class:       com_dreamdisplays_screen_NativeImageConverter
// Method:      convertRGBAtoARGB
// Signature:   ([B[BI)V
JNIEXPORT void JNICALL Java_com_dreamdisplays_screen_NativeImageConverter_convertRGBAtoARGB
  (JNIEnv *env, jclass cls, jbyteArray src, jbyteArray dst, jint length) {

    jbyte *src_ptr = (*env)->GetByteArrayElements(env, src, NULL);
    jbyte *dst_ptr = (*env)->GetByteArrayElements(env, dst, NULL);

    if (UNLIKELY(src_ptr == NULL || dst_ptr == NULL)) {
        if (src_ptr) (*env)->ReleaseByteArrayElements(env, src, src_ptr, JNI_ABORT);
        if (dst_ptr) (*env)->ReleaseByteArrayElements(env, dst, dst_ptr, JNI_ABORT);
        return;
    }

    jint i = 0;

#if USE_SIMD && defined(__SSE2__)
    // SSE2 optimized path with loop unrolling (4x) and prefetching
    const __m128i shuffle_mask = _mm_set_epi8(12,15,14,13, 8,11,10,9, 4,7,6,5, 0,3,2,1);

    // Process 64 bytes (16 pixels) per iteration - 4x unroll
    for (; i <= length - 64; i += 64) {
        // Prefetch next cache line
        PREFETCH(src_ptr + i + 64);
        PREFETCH_WRITE(dst_ptr + i + 64);

        // Load and shuffle 4 blocks in parallel
        __m128i p0 = _mm_loadu_si128((__m128i*)(src_ptr + i));
        __m128i p1 = _mm_loadu_si128((__m128i*)(src_ptr + i + 16));
        __m128i p2 = _mm_loadu_si128((__m128i*)(src_ptr + i + 32));
        __m128i p3 = _mm_loadu_si128((__m128i*)(src_ptr + i + 48));

        __m128i s0 = _mm_shuffle_epi8(p0, shuffle_mask);
        __m128i s1 = _mm_shuffle_epi8(p1, shuffle_mask);
        __m128i s2 = _mm_shuffle_epi8(p2, shuffle_mask);
        __m128i s3 = _mm_shuffle_epi8(p3, shuffle_mask);

        _mm_storeu_si128((__m128i*)(dst_ptr + i), s0);
        _mm_storeu_si128((__m128i*)(dst_ptr + i + 16), s1);
        _mm_storeu_si128((__m128i*)(dst_ptr + i + 32), s2);
        _mm_storeu_si128((__m128i*)(dst_ptr + i + 48), s3);
    }

    // Process remaining 16-byte blocks
    for (; i <= length - 16; i += 16) {
        __m128i pixels = _mm_loadu_si128((__m128i*)(src_ptr + i));
        __m128i shuffled = _mm_shuffle_epi8(pixels, shuffle_mask);
        _mm_storeu_si128((__m128i*)(dst_ptr + i), shuffled);
    }
#elif USE_SIMD && defined(__ARM_NEON)
    // NEON optimized path with loop unrolling (4x) and prefetching
    const uint8x16_t shuffle_mask = {3,2,1,0, 7,6,5,4, 11,10,9,8, 15,14,13,12};

    // Process 64 bytes (16 pixels) per iteration - 4x unroll
    for (; i <= length - 64; i += 64) {
        // Prefetch next cache line (NEON)
        __builtin_prefetch(src_ptr + i + 64, 0, 3);
        __builtin_prefetch(dst_ptr + i + 64, 1, 3);

        // Load 4 blocks
        uint8x16_t p0 = vld1q_u8((uint8_t*)(src_ptr + i));
        uint8x16_t p1 = vld1q_u8((uint8_t*)(src_ptr + i + 16));
        uint8x16_t p2 = vld1q_u8((uint8_t*)(src_ptr + i + 32));
        uint8x16_t p3 = vld1q_u8((uint8_t*)(src_ptr + i + 48));

        // Shuffle in parallel
        uint8x16_t s0 = vqtbl1q_u8(p0, shuffle_mask);
        uint8x16_t s1 = vqtbl1q_u8(p1, shuffle_mask);
        uint8x16_t s2 = vqtbl1q_u8(p2, shuffle_mask);
        uint8x16_t s3 = vqtbl1q_u8(p3, shuffle_mask);

        // Store all 4 blocks
        vst1q_u8((uint8_t*)(dst_ptr + i), s0);
        vst1q_u8((uint8_t*)(dst_ptr + i + 16), s1);
        vst1q_u8((uint8_t*)(dst_ptr + i + 32), s2);
        vst1q_u8((uint8_t*)(dst_ptr + i + 48), s3);
    }

    // Process remaining 16-byte blocks
    for (; i <= length - 16; i += 16) {
        uint8x16_t pixels = vld1q_u8((uint8_t*)(src_ptr + i));
        uint8x16_t shuffled = vqtbl1q_u8(pixels, shuffle_mask);
        vst1q_u8((uint8_t*)(dst_ptr + i), shuffled);
    }
#endif

    // Scalar fallback for remaining bytes
    for (; i < length; i += 4) {
        uint8_t r = src_ptr[i];
        uint8_t g = src_ptr[i + 1];
        uint8_t b = src_ptr[i + 2];
        uint8_t a = src_ptr[i + 3];

        dst_ptr[i]     = a;
        dst_ptr[i + 1] = b;
        dst_ptr[i + 2] = g;
        dst_ptr[i + 3] = r;
    }

    (*env)->ReleaseByteArrayElements(env, src, src_ptr, JNI_ABORT);
}

// Class:       com_dreamdisplays_screen_NativeImageConverter
// Method:      convertABGRtoRGBA
// Signature:   ([BLjava/nio/ByteBuffer;I)V
JNIEXPORT void JNICALL Java_com_dreamdisplays_screen_NativeImageConverter_convertABGRtoRGBA
  (JNIEnv *env, jclass cls, jbyteArray src, jobject dst, jint length) {

    jbyte *src_ptr = (*env)->GetByteArrayElements(env, src, NULL);
    jbyte *dst_ptr = (*env)->GetDirectBufferAddress(env, dst);

    if (UNLIKELY(src_ptr == NULL || dst_ptr == NULL)) {
        if (src_ptr) (*env)->ReleaseByteArrayElements(env, src, src_ptr, JNI_ABORT);
        return;
    }

    jint i = 0;

#if USE_SIMD && defined(__SSE2__)
    // SSE2 optimized path with loop unrolling (4x) and prefetching
    const __m128i shuffle_mask = _mm_set_epi8(12,13,14,15, 8,9,10,11, 4,5,6,7, 0,1,2,3);

    // Process 64 bytes (16 pixels) per iteration - 4x unroll
    for (; i <= length - 64; i += 64) {
        // Prefetch next cache line
        PREFETCH(src_ptr + i + 64);
        PREFETCH_WRITE(dst_ptr + i + 64);

        // Load and shuffle 4 blocks in parallel
        __m128i p0 = _mm_loadu_si128((__m128i*)(src_ptr + i));
        __m128i p1 = _mm_loadu_si128((__m128i*)(src_ptr + i + 16));
        __m128i p2 = _mm_loadu_si128((__m128i*)(src_ptr + i + 32));
        __m128i p3 = _mm_loadu_si128((__m128i*)(src_ptr + i + 48));

        __m128i s0 = _mm_shuffle_epi8(p0, shuffle_mask);
        __m128i s1 = _mm_shuffle_epi8(p1, shuffle_mask);
        __m128i s2 = _mm_shuffle_epi8(p2, shuffle_mask);
        __m128i s3 = _mm_shuffle_epi8(p3, shuffle_mask);

        _mm_storeu_si128((__m128i*)(dst_ptr + i), s0);
        _mm_storeu_si128((__m128i*)(dst_ptr + i + 16), s1);
        _mm_storeu_si128((__m128i*)(dst_ptr + i + 32), s2);
        _mm_storeu_si128((__m128i*)(dst_ptr + i + 48), s3);
    }

    // Process remaining 16-byte blocks
    for (; i <= length - 16; i += 16) {
        __m128i pixels = _mm_loadu_si128((__m128i*)(src_ptr + i));
        __m128i shuffled = _mm_shuffle_epi8(pixels, shuffle_mask);
        _mm_storeu_si128((__m128i*)(dst_ptr + i), shuffled);
    }
#elif USE_SIMD && defined(__ARM_NEON)
    // NEON optimized path with loop unrolling (4x) and prefetching
    const uint8x16_t shuffle_mask = {3,2,1,0, 7,6,5,4, 11,10,9,8, 15,14,13,12};

    // Process 64 bytes (16 pixels) per iteration - 4x unroll
    for (; i <= length - 64; i += 64) {
        // Prefetch next cache line
        __builtin_prefetch(src_ptr + i + 64, 0, 3);
        __builtin_prefetch(dst_ptr + i + 64, 1, 3);

        // Load 4 blocks
        uint8x16_t p0 = vld1q_u8((uint8_t*)(src_ptr + i));
        uint8x16_t p1 = vld1q_u8((uint8_t*)(src_ptr + i + 16));
        uint8x16_t p2 = vld1q_u8((uint8_t*)(src_ptr + i + 32));
        uint8x16_t p3 = vld1q_u8((uint8_t*)(src_ptr + i + 48));

        // Shuffle in parallel
        uint8x16_t s0 = vqtbl1q_u8(p0, shuffle_mask);
        uint8x16_t s1 = vqtbl1q_u8(p1, shuffle_mask);
        uint8x16_t s2 = vqtbl1q_u8(p2, shuffle_mask);
        uint8x16_t s3 = vqtbl1q_u8(p3, shuffle_mask);

        // Store all 4 blocks
        vst1q_u8((uint8_t*)(dst_ptr + i), s0);
        vst1q_u8((uint8_t*)(dst_ptr + i + 16), s1);
        vst1q_u8((uint8_t*)(dst_ptr + i + 32), s2);
        vst1q_u8((uint8_t*)(dst_ptr + i + 48), s3);
    }

    // Process remaining 16-byte blocks
    for (; i <= length - 16; i += 16) {
        uint8x16_t pixels = vld1q_u8((uint8_t*)(src_ptr + i));
        uint8x16_t shuffled = vqtbl1q_u8(pixels, shuffle_mask);
        vst1q_u8((uint8_t*)(dst_ptr + i), shuffled);
    }
#endif

    // Scalar fallback for remaining bytes
    for (; i < length; i += 4) {
        uint8_t a = src_ptr[i];
        uint8_t b = src_ptr[i + 1];
        uint8_t g = src_ptr[i + 2];
        uint8_t r = src_ptr[i + 3];

        dst_ptr[i]     = r;
        dst_ptr[i + 1] = g;
        dst_ptr[i + 2] = b;
        dst_ptr[i + 3] = a;
    }

    (*env)->ReleaseByteArrayElements(env, src, src_ptr, JNI_ABORT);
}

// Class:       com_dreamdisplays_screen_Converter
// Method:      scaleRGBAImage
// Signature:   (Ljava/nio/ByteBuffer;IILjava/nio/ByteBuffer;II)V
JNIEXPORT void JNICALL Java_com_dreamdisplays_screen_Converter_scaleRGBAImage
  (JNIEnv *env, jclass cls, jobject src, jint srcW, jint srcH, jobject dst, jint dstW, jint dstH) {

    uint8_t *src_ptr = (uint8_t*)(*env)->GetDirectBufferAddress(env, src);
    uint8_t *dst_ptr = (uint8_t*)(*env)->GetDirectBufferAddress(env, dst);

    if (UNLIKELY(src_ptr == NULL || dst_ptr == NULL)) {
        return;
    }

    // Calculate scaling to maintain aspect ratio (cover mode)
    double scaleW = (double)dstW / srcW;
    double scaleH = (double)dstH / srcH;
    double scale = (scaleW > scaleH) ? scaleW : scaleH;
    int scaledW = (int)(srcW * scale + 0.5);
    int scaledH = (int)(srcH * scale + 0.5);

    // Calculate offsets to center the image
    int offsetX = (dstW - scaledW) / 2;
    int offsetY = (dstH - scaledH) / 2;

    // Fill destination with black (transparent)
    memset(dst_ptr, 0, dstW * dstH * 4);

    // Nearest neighbor scaling with SIMD optimization
    int y;
    for (y = 0; y < dstH; y++) {
        int srcY = (int)((y - offsetY) * srcH / (double)scaledH);

        if (srcY < 0 || srcY >= srcH) continue;

        int x;
        for (x = 0; x < dstW; x++) {
            int srcX = (int)((x - offsetX) * srcW / (double)scaledW);

            if (srcX >= 0 && srcX < srcW) {
                // Copy 4 bytes (RGBA) from source to destination
                int srcIdx = (srcY * srcW + srcX) * 4;
                int dstIdx = (y * dstW + x) * 4;

                memcpy(dst_ptr + dstIdx, src_ptr + srcIdx, 4);
            }
        }
    }
}

// Class:       com_dreamdisplays_screen_Converter
// Method:      calculateScreenDistance
// Signature:   (IIIILjava/lang/String;III)D
JNIEXPORT jdouble JNICALL Java_com_dreamdisplays_screen_Converter_calculateScreenDistance
  (JNIEnv *env, jclass cls, jint screenX, jint screenY, jint screenZ, jstring facing, jint width, jint height, jint posX, jint posY, jint posZ) {

    const char *facing_str = (*env)->GetStringUTFChars(env, facing, NULL);
    if (facing_str == NULL) return 0.0;

    int maxX = screenX;
    int maxY = screenY + height - 1;
    int maxZ = screenZ;

    // Determine facing direction
    if (strcmp(facing_str, "NORTH") == 0 || strcmp(facing_str, "SOUTH") == 0) {
        maxX += width - 1;
    } else if (strcmp(facing_str, "EAST") == 0 || strcmp(facing_str, "WEST") == 0) {
        maxZ += width - 1;
    }

    (*env)->ReleaseStringUTFChars(env, facing, facing_str);

    // Clamp position to screen bounds
    int clampedX = (posX < screenX) ? screenX : ((posX > maxX) ? maxX : posX);
    int clampedY = (posY < screenY) ? screenY : ((posY > maxY) ? maxY : posY);
    int clampedZ = (posZ < screenZ) ? screenZ : ((posZ > maxZ) ? maxZ : posZ);

    // Calculate Euclidean distance
    int dx = posX - clampedX;
    int dy = posY - clampedY;
    int dz = posZ - clampedZ;

    return sqrt(dx * dx + dy * dy + dz * dz);
}
