# GET A JOB

Jeu 2D en Java, vu de côté, au Kremlin-Bicêtre, la nuit.

- `jeu/` : le jeu (code Java, ressources, tests). Voir `jeu/LISEZMOI.md`.
- `designs/` : les scripts Python qui dessinent les décors, les personnages et les fiches.
- `histoire-du-jeu.md` : la bible de l'histoire.

Lancer le jeu : `bash jeu/lancer.sh`. Lancer les tests : `bash jeu/tester.sh`.

## Dans VS Code

Ouvrir le dossier racine du dépôt (celui de ce fichier). Il faut un JDK (17 ou plus) et l'extension
« Extension Pack for Java », que VS Code propose à l'ouverture (`.vscode/extensions.json`).

- Lancer le jeu : F5, ou le panneau « Exécuter et déboguer ». Par défaut, **GET A JOB (menu M, développeur)** :
  M (ou F2) ouvre le menu pour aller au début de n'importe quelle mission ou valider l'objectif en cours, sur une
  partie de test à part. **GET A JOB** lance le jeu normal. Le jeu démarre depuis `jeu/`, où il trouve `ressources`.
- Sans l'extension : `Terminal › Exécuter la tâche…` propose « Lancer le jeu avec le menu M (développeur) »,
  « Lancer le jeu », « Lancer les tests » et « Refaire les raccourcis du menu M ».
- Les sources sont dans `jeu/src`, les essais dans `jeu/tests`, JUnit dans `jeu/lib` (`.vscode/settings.json`).
