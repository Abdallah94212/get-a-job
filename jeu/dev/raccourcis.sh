#!/bin/bash
# AIDE AU DEVELOPPEMENT, A SUPPRIMER A LA FIN. Refait les parties de debut de mission (ressources/dev/) apres un
# changement de la campagne : la campagne est jouee sans fenetre, une partie est gardee au debut de chaque mission.
cd "$(dirname "$0")/.." || exit 1
J=lib/junit-platform-console-standalone-1.10.2.jar
rm -rf build-dev && mkdir -p build-dev \
  && javac -d build-dev -cp "$J" src/decor/*.java src/histoire/*.java src/jeu/*.java tests/jeu/Robot.java dev/GenererRaccourcis.java \
  && java -Djava.awt.headless=true -cp build-dev:"$J" jeu.GenererRaccourcis
rm -rf build-dev
