# GET A JOB : contexte pour Claude

Jeu 2D en Java pur (Swing/AWT, sans moteur), vue de côté, pixel art 480 × 270, au Kremlin-Bicêtre, la nuit.
Projet de jeu Epitech. L'auteur écrit en français : réponds en français.

## Commandes

- Lancer le jeu : `bash jeu/lancer.sh` (compile dans `jeu/build`, lance `jeu.Fenetre ressources` depuis `jeu/`).
- Avec le menu M du développeur (M ou F2 : aller au début d'une mission, valider l'objectif en cours) :
  `bash jeu/dev/lancer.sh`, ou F5 dans VS Code. Partie à part : `~/.get-a-job/partie_test.properties`.
  Après un changement de `campagne.txt`, refaire ses raccourcis : `bash jeu/dev/raccourcis.sh`.
- Tous les tests : `bash jeu/tester.sh` (JUnit 5, sans fenêtre). État au 19/11/2026 : 211 tests, 0 échec.
- Un seul test, depuis `jeu/` :
  `javac -d build-essais -cp lib/junit-platform-console-standalone-1.10.2.jar src/*/*.java tests/*/*.java`
  puis `java -Djava.awt.headless=true -jar lib/junit-platform-console-standalone-1.10.2.jar execute --class-path build-essais --select-class jeu.CombatArsenalTest`
  (nom de classe avec son paquet ; `--select-method jeu.Classe#methode` pour une méthode).
- VS Code : `.vscode/launch.json` (« GET A JOB (menu M, développeur) » par défaut, « GET A JOB »), `.vscode/tasks.json`
  (« Lancer le jeu avec le menu M », « Lancer le jeu », « Lancer les tests », « Refaire les raccourcis du menu M »).

## Architecture

- `jeu/src/decor` : le moteur de rendu. `Rendu` (pixels CPU dans un int[] 480 × 270, couches LOIN, FOND, OBJETS triés
  par y avec les acteurs, DEVANT, LUMIERES), `Scene` (lit les `.niveau`, collisions `bloque()` au sol), `Joueur`, `Poses`.
- `jeu/src/histoire` : le modèle, **sans aucun affichage**. `LecteurCampagne` lit `ressources/histoire/campagne.txt`
  (missions, objectifs, cinématiques, figurants), `Progression` (mission, étape, drapeaux), `Etat` (argent, santé,
  armes, loyer), `Inventaire`, `Organisation` et `Mois` (l'équipe et le mois d'affaires), `Sauvegarde` (Properties).
- `jeu/src/jeu` : le jeu. `Fenetre` (fil à 60 Hz, dt mesuré plafonné à 0,05 s), `Jeu` (maj et dessiner protégés,
  erreurs dans `~/.get-a-job/incidents.log`), `EcranPartie` (modes JEU, SCENE, PAUSE, BILAN, TELEPHONE, TEMPS, POCHES,
  CONDUITE), `Monde`, `Combat`, `Scenographe`, `Vie` (le quartier vivant), `Conduite`, `Telephone`, `Poches`, `Audio`.
- `jeu/tests` : `Robot` joue la vraie partie sans fenêtre ; `ParcoursTest` lui fait jouer toute la campagne.
- `jeu/ressources` : images, sons, cartes `.niveau`, `histoire/campagne.txt` (l'histoire est des données, pas du code).
- `designs/` : scripts Python qui dessinent les sprites, décors et cartes, et synthétisent sons et voix. Les `.niveau`
  sont générés par `designs/decor/avenue.py` : ne pas les modifier à la main.
- Documentation : `jeu/LISEZMOI.md` (commandes, missions, architecture), `jeu/NARRATION.md`, `jeu/SON.md`,
  `histoire-du-jeu.md` (la bible de l'histoire, actes 1 à 7).

## Conventions

- Noms et commentaires en français. Les commentaires du code sont écrits sans accents ; les textes affichés en ont.
- Indentation de 4 espaces, lignes jusqu'à environ 150 caractères.
- Les touches sont lues dans `jeu/src/jeu/Touches.java` (clavier pensé AZERTY : ZQSD, A attaque).
- Vérifier dans le code avant d'affirmer qu'une mécanique existe. Ne jamais présenter une idée comme implémentée.

## État du projet

- Jouable : prologue et actes 1 et 2, 12 missions. Les actes 3 à 7 sont écrits (`histoire-du-jeu.md`), pas développés ;
  les cartes `bureaux`, `hall_tour`, `etage_anges`, `bureau_baba` sont dessinées mais fermées.
- Le combat (touche A hybride, 11 armes, 4 styles d'ennemis plus les aliens) est testé mais pas équilibré à la main.
- Faiblesses connues : `Combat.java` (~2 600 lignes) et `EcranPartie.java` (~2 150) à découper ; pas de touches réglables ;
  une seule sauvegarde ; les nouveaux ennemis sont des recolorations.

## À ne pas oublier

- **Aide au test à retirer avant la sortie** : `dev/`, `jeu/dev/`, `jeu/ressources/dev/`, `jeu/src/jeu/AideDev.java` et
  les lignes marquées « AideDev » (voir `jeu/dev/LISEZMOI.md`).
- **Affiche de la chambre non libre** (`designs/decor/packs/affiche_chambre/`, fan-art) : à remplacer avant toute
  diffusion publique. Le dépôt GitHub est public.
- Ne pas committer ni pousser sans demande. Jamais de `push --force`.
- Toute génération d'image, de vidéo ou de son payante : annoncer le coût et demander l'accord d'abord.
