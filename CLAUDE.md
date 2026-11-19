# GET A JOB (version lite) : contexte pour Claude

Petit jeu 2D en Java Swing, sans dépendance (JUnit seulement pour les tests).
Version volontairement réduite après les Mid-Terms : le jury voulait une POO claire
(héritage justifié, polymorphisme) et un code qu'on sait expliquer.

- Lancer : `bash lancer.sh` (depuis la racine, pour trouver `ressources/`).
- Tester : `bash tester.sh` (JUnit 5, sans fenêtre).
- Sources : `src/getajob/` (17 classes) ; tests : `tests/getajob/`.
- Hiérarchies : `Entite` → `Personnage` → `Joueur` / `Ennemi` / `Client`, et `Ecran` → `EcranTitre` / `EcranRecit` / `EcranPartie`.
- Les règles (étapes, argent, heure, victoire/défaite) sont dans `Tournee`, pas dans les écrans.
- Le README explique où se trouve chaque fondamental du jeu 2D.

Règles : garder le code simple et explicable. Toute nouvelle règle de jeu doit avoir un test.
Pas de nouveau système sans en parler : le but est la clarté, pas le nombre de fonctionnalités.
L'ancienne version complète n'est plus dans ce dossier. Elle est dans `~/Documents/get-a-job-complet`
et dans l'historique Git avant le commit de retrait.
