#!/bin/bash
# Compile le jeu et ses essais, puis lance tous les tests unitaires (JUnit 5), sans ouvrir de fenetre.
cd "$(dirname "$0")" || exit 1
JUNIT=lib/junit-platform-console-standalone-1.10.2.jar
rm -rf build-essais && mkdir -p build-essais \
  && javac -d build-essais -cp "$JUNIT" src/decor/*.java src/histoire/*.java src/jeu/*.java tests/histoire/*.java tests/jeu/*.java \
  && java -Djava.awt.headless=true -jar "$JUNIT" execute --class-path build-essais --scan-class-path --disable-banner \
       --details=tree --details-theme=ascii "$@"
