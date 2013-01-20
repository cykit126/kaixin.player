#!/bin/sh

if [ ! -n "$1" ]
then
	echo "Usage:\n"
	echo "    configure.sh ffmpeg_path prefix_path\n"
	echo "    configure.sh ffmpeg_path\n"
	exit 1
fi

if [ ! -s $1/configure ]
then
	echo "$1/configure doesn't exist.\n"
	exit 1
fi

if [ -n "$2" ]
then
	PREFIX=$2
else
	PREFIX=/usr/local
fi

$1/configure \
--prefix=$2 \
--enable-debug=3 \
--enable-runtime-cpudetect \
--enable-pic \
--enable-static \
--disable-shared \
--enable-version3 \
--enable-gpl \
--disable-optimizations \
--disable-stripping \
--disable-doc \
--disable-yasm \
--enable-protocol=file,http,tcp,rtsp \
--enable-hwaccels \
--enable-network \
--enable-ffmpeg \
--disable-ffplay \
--enable-ffprobe \
--disable-ffserver \
--disable-filters \
--disable-asm \
--disable-altivec \
--disable-amd3dnow \
--disable-amd3dnowext \
--disable-mmx \
--disable-mmx2 \
--disable-sse \
--disable-ssse3 \
--disable-avx \
--disable-armv5te \
--disable-armv6 \
--disable-armv6t2 \
--disable-armvfp \
--disable-iwmmxt \
--disable-mmi \
--disable-neon \
--disable-vis 

