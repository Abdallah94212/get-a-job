#!/bin/bash
# Compile le jeu et les tests dans build-tests/, puis lance les tests JUnit 5 sans ouvrir de fenêtre.
cd "$(dirname "$0")" || exit 1
JUNIT=lib/junit-platform-console-standalone-1.10.2.jar
rm -rf build-tests && mkdir -p build-tests \
  && javac -encoding UTF-8 -d build-tests -cp "$JUNIT" $(find src tests -name "*.java") \
  && java -Djava.awt.headless=true -jar "$JUNIT" execute --class-path build-tests --scan-class-path \
       --disable-banner --details=tree --details-theme=ascii
