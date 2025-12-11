cmake_minimum_required(VERSION 3.15)
project(dreamdisplays_native C)

set(CMAKE_C_STANDARD 11)

# ENABLE SIMD OPTION
option(USE_SIMD "Enable SIMD optimizations (SSE2/NEON)" ON)

# SET JAVA_HOME
if(DEFINED ENV{JAVA_HOME})
    set(JAVA_HOME $ENV{JAVA_HOME})
    message(STATUS "Using JAVA_HOME from environment: ${JAVA_HOME}")
    set(JAVA_INCLUDE_PATH "${JAVA_HOME}/include")
    if(APPLE)
        set(JAVA_INCLUDE_PATH2 "${JAVA_HOME}/include/darwin")
    elseif(UNIX)
        set(JAVA_INCLUDE_PATH2 "${JAVA_HOME}/include/linux")
    endif()
    set(JAVA_AWT_INCLUDE_PATH "${JAVA_HOME}/include")
endif()

# FIND JNI
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

message(STATUS "JNI_INCLUDE_DIRS: ${JNI_INCLUDE_DIRS}")
message(STATUS "JNI_LIBRARIES: ${JNI_LIBRARIES}")

# SOURCE_FILES
set(SOURCE_FILES
    src/color_conversion.c
    src/image_scaling.c
    src/math_utils.c
)

# SHARED LIBRARY
add_library(dreamdisplays_native SHARED
    ${SOURCE_FILES}
)

# SIMD COMPILATION FLAGS
if(USE_SIMD)
    if(CMAKE_SYSTEM_PROCESSOR MATCHES "x86_64|AMD64|i686|i386")
        target_compile_options(dreamdisplays_native PRIVATE -msse2)
    elseif(CMAKE_SYSTEM_PROCESSOR MATCHES "^arm$|^armv")
        target_compile_options(dreamdisplays_native PRIVATE -mfpu=neon)
    endif()
endif()

# LINK JNI LIBRARIES
if(NOT APPLE)
    target_link_libraries(dreamdisplays_native ${JNI_LIBRARIES})
endif()

# SET OUTPUT NAME
set_target_properties(dreamdisplays_native PROPERTIES
    OUTPUT_NAME "dreamdisplays_native"
)

# SET PLATFORM-SPECIFIC EXTENSIONS
if(WIN32)
    set_target_properties(dreamdisplays_native PROPERTIES
        PREFIX ""
        SUFFIX ".dll"
    )
elseif(APPLE)
    set_target_properties(dreamdisplays_native PROPERTIES
        PREFIX "lib"
        SUFFIX ".dylib"
    )
else()
    set_target_properties(dreamdisplays_native PROPERTIES
        PREFIX "lib"
        SUFFIX ".so"
    )
endif()

# INSTALLATION
install(TARGETS dreamdisplays_native
    LIBRARY DESTINATION ${CMAKE_SOURCE_DIR}/../common/src/main/resources/natives
    RUNTIME DESTINATION ${CMAKE_SOURCE_DIR}/../common/src/main/resources/natives
)
