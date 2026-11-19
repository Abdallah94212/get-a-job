# GET A JOB — version lite

> 23 h, Bobigny. Le loyer tombe demain : **120 €**. Ton pote Youngssef t'a trouvé un plan :
> livrer pour le Maître Poulet. Trois commandes, quarante euros chacune.
> Mais l'avenue n'est pas vide la nuit : des racketteurs font leur ronde.
> Rentre chez toi avec l'argent **avant 6 h**.

Un petit beat'em up / jeu de livraison en 2D, en Java Swing, sans aucune bibliothèque externe.
17 classes, environ 1 270 lignes, deux hiérarchies d'héritage, 15 tests.

---

## Lancer et tester

```bash
bash lancer.sh      # compile dans build/ puis ouvre la fenêtre
bash tester.sh      # compile src + tests et lance JUnit 5 (jar fourni dans lib/)
```

Il faut seulement un JDK 17 ou plus récent (testé avec OpenJDK 27).

| Touche | Action |
|---|---|
| Flèches / ZQSD / WASD | marcher (gauche-droite, et en profondeur vers les façades ou la chaussée) |
| Espace | coup de poing |
| Entrée | valider (titre, récit) |
| Échap | pause (puis Q pour abandonner) |

---

## L'expérience de jeu (ce qu'on raconte au jury)

On ne présente pas une liste de fonctionnalités : on raconte **une nuit**.

1. **Le contexte** : un jeune du quartier doit payer son loyer demain matin. Il lui manque 120 €.
2. **L'objectif** : prendre une commande au Maître Poulet, la livrer, être payé. Trois fois. Puis rentrer.
3. **Les obstacles** :
   - **l'heure** : 2 minutes de jeu par seconde réelle, la nuit dure 3 min 30 ;
   - **les racketteurs** : s'ils t'attrapent, tu perds une vie **et** le sac. Il faut retourner au comptoir ;
   - **la rumeur** : à chaque livraison, un racketteur de plus se poste dans l'avenue.
4. **Les choix du joueur** :
   - frapper : le racketteur est étourdi 2 s, mais il faut être bien placé ;
   - ou contourner : changer de trottoir en profondeur et passer sous son champ de vision (110 px, même hauteur).
5. **La fin** :
   - rentré au 12 avec 120 € → **victoire** ;
   - plus de vies → **défaite** ;
   - 6 h sonne → **défaite**.

---

## Architecture

```
Main ── crée la fenêtre et le Jeu

Jeu (JPanel) ── la boucle : Timer 16 ms → dt → ecran.mettreAJour → repaint
 ├─ Clavier (KeyAdapter) ── touches tenues / touches qui viennent d'être appuyées
 └─ Ecran (abstraite) ─────────── un état du jeu
     ├─ EcranTitre
     ├─ EcranRecit      (texte qui apparaît ligne à ligne : intro, fins)
     └─ EcranPartie ── possède Niveau + Tournee + Camera, dessine le HUD

Niveau ── le décor, les obstacles, et toutes les entités
Tournee ── les règles : étapes, argent, heure, victoire / défaite
Camera ── suit le joueur, bornée aux bords du décor

Entite (abstraite) ─────────── position (x, y) = les pieds ; contact() ; dessiner()
 └─ Personnage (abstraite) ── animation de marche, deplacer() avec collisions, miroir, ombre
     ├─ Joueur   ── piloté par le Clavier ; coup de poing, vies, argent, sac
     ├─ Ennemi   ── machine à états : PATROUILLE → POURSUITE → ETOURDI
     └─ Client   ── attend sa commande, se tourne vers le joueur

Animation ── une suite d'images et une durée par image
Ressources ── chargement des images (avec cache), de la police, des obstacles, des sons
```

### Pourquoi cet héritage (et pas un autre)

- **`Entite`** contient ce que **toute chose du monde** partage :
  - une position ;
  - une zone de contact au sol ;
  - une façon de se dessiner debout sur ses pieds.

  Elle déclare deux méthodes abstraites : `mettreAJour(dt, niveau)` et `image()`.
- **`Personnage`** ajoute ce que **tout être qui marche** partage : l'animation de marche, le déplacement avec collisions (`deplacer`) et le regard à gauche ou à droite. Le code de collision n'existe qu'**une seule fois**.
- **`Joueur`, `Ennemi`, `Client`** ne diffèrent que par **leur décision** (le clavier, une IA à états, rien) et par leur image du moment.
- **Le polymorphisme est utilisé, pas décoratif** :
  - `Niveau.mettreAJour` parcourt une `List<Entite>` et appelle `entite.mettreAJour(dt, this)` sans savoir qui est qui ;
  - `EcranPartie` dessine la même liste triée par `y`, avec la même méthode `dessiner` pour tout le monde.
- **`Ecran`** : chaque état du jeu (titre, récit, partie) a ses propres `mettreAJour` et `dessiner`. `Jeu` ne connaît que la classe abstraite. Changer d'état, c'est `jeu.changerEcran(...)`.

---

## Où trouver chaque fondamental du jeu 2D

| Fondamental | Où | Ce qu'il faut savoir expliquer |
|---|---|---|
| **Boucle de jeu** | `Jeu.tour()` | Un `javax.swing.Timer` à 16 ms (≈ 60 images/s). Il tourne sur le thread de Swing, donc aucun problème de concurrence. |
| **Temps / dt** | `Jeu.tour()` | `dt` est mesuré avec `System.nanoTime()` et plafonné à 0,05 s. Toutes les vitesses sont en pixels **par seconde** et multipliées par `dt`. |
| **Rendu** | `Jeu.paintComponent`, `EcranPartie.dessiner` | On dessine dans une image 480×270, agrandie ×3 sans lissage (pixel art). Ordre : décor, puis entités triées par `y`, puis HUD. |
| **Coordonnées et caméra** | `Entite`, `Camera` | Coordonnées monde (avenue de 4 260 px) contre coordonnées écran : `xEcran = x − camera.x()`. La caméra est bornée aux bords. |
| **Clavier** | `Clavier` | `tenue()` sert à marcher (tant que la touche est enfoncée). `vientDAppuyer()` sert à frapper ou valider (une seule fois par appui), remis à zéro par `finDuTour()`. |
| **Entités** | `Entite` → `Personnage` → `Joueur` / `Ennemi` / `Client` | Voir l'architecture ci-dessus. |
| **Collisions** | `Personnage.deplacer`, `Niveau.bloque`, `Entite.contact` / `touche` | Rectangles **au sol**, pas sur le sprite entier (vue de trois quarts). On teste x puis y séparément : bloqué sur un axe, on **glisse** sur l'autre. |
| **Interactions** | `Tournee.mettreAJour` | Contact avec le comptoir : prendre la commande. Contact avec le client : livrer. Contact avec un racketteur : `etreTouche`. Coup de poing : `Joueur.zoneDeCoup()` contre l'ennemi. |
| **Animations** | `Animation`, `Personnage.animer` | Deux images de marche alternées toutes les 0,17 s. Des poses fixes pour le coup et le choc. Le joueur clignote quand il est intouchable. |
| **États** | `Ecran` (états du jeu), `Ennemi.Etat` (IA), `Tournee.Etape` (progression) | Trois niveaux d'états, chacun avec un `enum` ou une classe abstraite. |
| **Victoire / défaite** | `Tournee.issue()` | `EN_COURS`, `VICTOIRE`, `PLUS_DE_VIES` ou `TROP_TARD`. `EcranPartie` la lit à chaque tour et passe à l'écran de fin. |

---

## Ce qui a été coupé, et pourquoi

Le projet d'origine faisait 60 classes et environ 18 450 lignes, avec un seul `extends` (`Fenetre extends Canvas`). Certaines classes dépassaient 2 000 lignes (`Combat`, `EcranPartie`). Le jury a eu raison : c'était trop pour un premier jeu, et l'objet n'y était pas.

| Coupé | Raison |
|---|---|
| Langage de script de missions, 10+ missions | La progression tient en un `enum Etape` de trois valeurs. |
| Inventaire, téléphone, économie, boutiques | Systèmes entiers sans rapport avec les fondamentaux. |
| Scooter, conduite, métro, plusieurs quartiers | Un seul niveau suffit pour montrer le défilement et la caméra. |
| Arsenal, combos, armes, barres de vie | Un coup, une pose, un ennemi étourdi : la même notion, mais lisible. |
| Vie des PNJ, dialogues à choix, cinématiques manga | Un écran de récit ligne à ligne porte l'histoire. |
| Parallaxe à plusieurs couches | Le décor est **une seule image**, exportée une fois depuis l'ancien moteur. |
| Sauvegarde, menus d'options, outils développeur (menu M) | Une partie dure 3 min 30, il n'y a rien à sauvegarder. |

L'ancienne version n'est pas perdue : elle reste dans l'historique Git (commits `f808fd4` à `0c04462`, avant le commit de retrait). Les sprites et les sons de la version lite viennent d'elle : ils ont été dessinés et synthétisés par nos propres scripts.

Ce qui reste du projet d'origine :

- l'identité : l'avenue, les personnages, la police ;
- les sons, générés par nos propres scripts ;
- le ton de l'histoire.

---

## Tests

```
DeplacementTest  (5) : vitesse, immobile, obstacle, glissement, bords de la rue
CombatTest       (4) : ronde, poursuite, coup de poing qui étourdit, se faire attraper
TourneeTest      (6) : début, une livraison = 40 €, pas de sac = pas payé,
                       3 livraisons + rentrer = victoire, 6 h = défaite, plus de vies = défaite
```

Les tests n'ouvrent aucune fenêtre : `Niveau` et `Tournee` ne dépendent pas de Swing pour leurs règles, et les sons sont ignorés en mode headless.

---

## Conseils pour la soutenance

1. **Ouvrir sur l'histoire**, pas sur le code : 23 h, 120 €, trois livraisons, des racketteurs.
2. **Jouer une partie en direct** : environ 2 minutes. Se faire attraper une fois exprès, pour montrer la perte du sac.
3. **Montrer le schéma d'héritage**, puis ouvrir `Niveau.mettreAJour` : 3 lignes qui font vivre tout le monde. C'est le polymorphisme.
4. **Ouvrir `Personnage.deplacer`** : les collisions et le glissement en 10 lignes.
5. **Ouvrir `Ennemi`** : la machine à états, le `switch` sur `Etat`.
6. **Dire ce qu'on a coupé** et pourquoi : c'est une preuve de maturité, pas un aveu.
7. **Lancer `tester.sh`** devant le jury.
