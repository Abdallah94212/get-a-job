#!/bin/bash
# AIDE AU DEVELOPPEMENT, A SUPPRIMER A LA FIN. Lance le jeu avec l'aide au test : M (ou F2) ouvre le menu (aller au
# debut d'une mission, outils). La partie de test est enregistree a part (~/.get-a-job/partie_test.properties).
# Chaque lancement compile dans son propre dossier temporaire : relancer ne casse pas un jeu deja ouvert.
cd "$(dirname "$0")/.." || exit 1
classes=$(mktemp -d /tmp/getajob.XXXXXX)
trap 'rm -rf "$classes"' EXIT
javac -d "$classes" src/decor/*.java src/histoire/*.java src/jeu/*.java || exit 1
echo "Le jeu s'ouvre. M : les missions de test."
java -Dgaj.dev=true -cp "$classes" jeu.Fenetre ressources
