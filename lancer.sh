#!/bin/bash
# Compile le jeu, puis le lance.
cd "$(dirname "$0")" || exit 1
rm -rf build && mkdir -p build && javac -encoding UTF-8 -d build src/getajob/*.java && java -cp build getajob.Main
