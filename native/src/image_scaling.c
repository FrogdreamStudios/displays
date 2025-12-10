#include "image_converter.h"
#include <string.h>

// Image scaling functions

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
