cmake_minimum_required(VERSION 3.15)
project(dreamdisplays_native C)

set(CMAKE_C_STANDARD 11)

# ENABLE SIMD OPTION
option(USE_SIMD "Enable SIMD optimizations (SSE2/NEON)" ON)

# SET JAVA_HOME
if(NOT DEFINED ENV{JAVA_HOME})
    message(STATUS "JAVA_HOME not set, attempting to detect...")

    # Try to find Java using find_package
    find_package(Java COMPONENTS Development REQUIRED)
    if(Java_Development_FOUND)
        message(STATUS "Found Java Development Kit.")
    endif()
else()
    set(JAVA_HOME $ENV{JAVA_HOME})
    message(STATUS "Using JAVA_HOME from environment: ${JAVA_HOME}.")
endif()

# Set Java include paths
if(DEFINED ENV{JAVA_HOME})
    set(JAVA_HOME $ENV{JAVA_HOME})
    set(JAVA_INCLUDE_PATH "${JAVA_HOME}/include")

    if(APPLE)
        set(JAVA_INCLUDE_PATH2 "${JAVA_HOME}/include/darwin")
    elseif(WIN32)
        set(JAVA_INCLUDE_PATH2 "${JAVA_HOME}/include/win32")
    elseif(UNIX)
        set(JAVA_INCLUDE_PATH2 "${JAVA_HOME}/include/linux")
    endif()
endif()

# FIND JNI
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

# SOURCE_FILES
set(SOURCE_FILES
    src/color_conversion.c
    src/image_scaling.c
    src/math_utils.c
)

# SHARED LIBRARY
add_library(dreamdisplays_native SHARED ${SOURCE_FILES})

# APPLY SIMD OPTIMIZATIONS
if(USE_SIMD)
    # x86/x64 - SSE2
    if(CMAKE_SYSTEM_PROCESSOR MATCHES "x86_64|AMD64|i686|i386")
        message(STATUS "Enabling SSE2 SIMD optimizations...")
        if(MSVC)
            target_compile_options(dreamdisplays_native PRIVATE /arch:SSE2)
        else()
            target_compile_options(dreamdisplays_native PRIVATE -msse2)
        endif()
    # ARM - NEON
    elseif(CMAKE_SYSTEM_PROCESSOR MATCHES "^arm$|^armv[7-9]|aarch64")
        message(STATUS "Enabling NEON SIMD optimizations...")
        target_compile_options(dreamdisplays_native PRIVATE -mfpu=neon)
    endif()
endif()

# PLATFORM-SPECIFIC LINKING
if(NOT APPLE)
    target_link_libraries(dreamdisplays_native PRIVATE ${JNI_LIBRARIES})
endif()

# SET OUTPUT PROPERTIES
set_target_properties(dreamdisplays_native PROPERTIES
    OUTPUT_NAME "dreamdisplays_native"
    VERSION 1.1.0
    SOVERSION 1
)

# SET PLATFORM-SPECIFIC LIBRARY NAMING
if(WIN32)
    set_target_properties(dreamdisplays_native PROPERTIES
        PREFIX ""
        SUFFIX ".dll"
    )
    message(STATUS "Building for Windows (.dll)...")
elseif(APPLE)
    set_target_properties(dreamdisplays_native PROPERTIES
        PREFIX "lib"
        SUFFIX ".dylib"
        MACOSX_RPATH ON
    )
    message(STATUS "Building for macOS (.dylib)...")
else()
    set_target_properties(dreamdisplays_native PROPERTIES
        PREFIX "lib"
        SUFFIX ".so"
    )
    message(STATUS "Building for Linux (.so)...")
endif()

# INSTALLATION
install(TARGETS dreamdisplays_native
    LIBRARY DESTINATION ${CMAKE_SOURCE_DIR}/../common/src/main/resources/natives
    RUNTIME DESTINATION ${CMAKE_SOURCE_DIR}/../common/src/main/resources/natives
)
