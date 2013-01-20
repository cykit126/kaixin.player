#!/bin/bash
# http://roman10.net/src/build_android_r6.txt

NDK=/Users/kaixin/Work/DevEnv/android/android-ndk-r7
PLATFORM=$NDK/platforms/android-8/arch-arm/
PREBUILT=$NDK/toolchains/arm-linux-androideabi-4.4.3/prebuilt/darwin-x86

#sed -i 's/HAVE_LRINT 0/HAVE_LRINT 1/g' config.h
#sed -i 's/HAVE_LRINTF 0/HAVE_LRINTF 1/g' config.h
#sed -i 's/HAVE_ROUND 0/HAVE_ROUND 1/g' config.h
#sed -i 's/HAVE_ROUNDF 0/HAVE_ROUNDF 1/g' config.h
#sed -i 's/HAVE_TRUNC 0/HAVE_TRUNC 1/g' config.h
#sed -i 's/HAVE_TRUNCF 0/HAVE_TRUNCF 1/g' config.h

make
make install
$PREBUILT/bin/arm-linux-androideabi-ar d /Users/kaixin/Work/RadioPlayer/build-android/lib/libavcodec/libavcodec.a inverse.o
#$PREBUILT/bin/arm-linux-androideabi-ld -rpath-link=$PLATFORM/usr/lib -L$PLATFORM/usr/lib  -soname libffmpeg.so -shared -nostdlib  -z,noexecstack -Bsymbolic --whole-archive --no-undefined -o $PREFIX/libffmpeg.so libavcodec/libavcodec.a libavformat/libavformat.a libavutil/libavutil.a libswscale/libswscale.a -lc -lm -lz -ldl -llog  --warn-once  --dynamic-linker=/system/bin/linker $PREBUILT/lib/gcc/arm-linux-androideabi/4.4.3/libgcc.a
