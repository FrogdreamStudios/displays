#include "image_converter.h"
#include <string.h>
#include <math.h>

// Mathematical utility functions

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
