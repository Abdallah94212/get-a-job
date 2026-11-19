# GET A JOB

Jeu 2D en Java, vu de côté, au Kremlin-Bicêtre, la nuit.

- `jeu/` : le jeu (code Java, ressources, tests). Voir `jeu/LISEZMOI.md`.
- `designs/` : les scripts Python qui dessinent les décors, les personnages et les fiches.
- `histoire-du-jeu.md` : la bible de l'histoire.

Lancer le jeu : `bash jeu/lancer.sh`. Lancer les tests : `bash jeu/tester.sh`.

## Dans VS Code

Ouvrir le dossier racine du dépôt (celui de ce fichier). Il faut un JDK (17 ou plus) et l'extension
« Extension Pack for Java », que VS Code propose à l'ouverture (`.vscode/extensions.json`).

- Lancer le jeu : panneau « Exécuter et déboguer », configuration **GET A JOB** (ou **GET A JOB (aide au test)**).
  Le jeu démarre depuis `jeu/`, où il trouve le dossier `ressources`.
- Sans l'extension : `Terminal › Exécuter la tâche…` propose « Lancer le jeu » et « Lancer les tests ».
- Les sources sont dans `jeu/src`, les essais dans `jeu/tests`, JUnit dans `jeu/lib` (`.vscode/settings.json`).
