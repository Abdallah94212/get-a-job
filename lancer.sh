#!/bin/bash
# Compile le jeu dans build/, puis le lance.
cd "$(dirname "$0")" || exit 1
rm -rf build && mkdir -p build \
  && javac -encoding UTF-8 -d build $(find src -name "*.java") \
  && java -cp build getajob.Main
