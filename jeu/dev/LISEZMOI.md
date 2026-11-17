# Aide au test (à supprimer à la fin du projet)

- `./dev/lancer.sh` : le jeu avec l'aide. M (ou F2) ouvre le menu : aller au début de n'importe quelle mission, valider
  l'objectif en cours, +500 euros, se soigner, des pierres et une barre, un combat ici, la route de Namek, +6 heures.
- La partie de test est à part (`~/.get-a-job/partie_test.properties`) : la vraie sauvegarde n'est pas touchée.
- `./dev/raccourcis.sh` refait les parties de début de mission (`ressources/dev/`) après un changement de la campagne :
  le robot des essais joue toute la campagne et enregistre le début de chaque mission.

Pour supprimer l'aide : effacer `dev/` (et `dev/` à la racine du projet), `ressources/dev/`, `src/jeu/AideDev.java`, et les lignes marquées « AideDev »
dans `Jeu.java`, `Fenetre.java` et `EcranPartie.java`.
