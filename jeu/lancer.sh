#!/bin/bash
# Compile puis lance le jeu. Fleches ou ZQSD : marcher. Maj : courir. E : agir. Echap : pause. T : trame.
cd "$(dirname "$0")" || exit 1
mkdir -p build && javac -d build src/decor/*.java src/histoire/*.java src/jeu/*.java && java -cp build jeu.Fenetre ressources "$@"
