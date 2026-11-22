# GET A JOB

Jeu 2D en Java (Swing), réalisé dans le cadre du projet *2D Game*.
Un beat'em up de rue vu de trois quarts, mêlé à un jeu de livraison contre la montre.

> Vendredi, 23 h, Le Kremlin-Bicêtre. Trois mois de loyer en retard : **120 €**.
> À 6 h, la régie coupe le courant. Youngssef, le livreur du Maître Poulet, s'est tordu la cheville :
> ce soir, ses livraisons sont pour toi. Quarante euros la course.
> Mais des racketteurs traînent dans l'avenue, et ils visent les livreurs.

---

## Le jeu

**Objectif** : prendre une commande au Maître Poulet, la livrer au client, être payé. Trois fois.
Puis rentrer chez soi avec les 120 € avant 6 h.

**Ce qui complique la nuit :**
- l'heure tourne : 2 minutes de jeu par seconde réelle, la nuit dure 3 min 30 ;
- un racketteur qui attrape le héros lui prend une vie **et** le sac : il faut retourner au comptoir ;
- après chaque livraison, un racketteur de plus se poste dans l'avenue.

Face à un racketteur, deux possibilités :
- le frapper : il reste étourdi deux secondes ;
- le contourner en changeant de trottoir : il ne voit le héros que s'il est à moins de 110 pixels et à peu près à la même hauteur.

**Fins de partie :**

| Issue | Condition |
|---|---|
| Victoire | Rentré chez soi avec les 120 € |
| Défaite | Plus de vies |
| Défaite | 6 h sonne |

**Commandes :**

| Touche | Action |
|---|---|
| Flèches, ZQSD ou WASD | Marcher (le long de l'avenue, et en profondeur vers les façades ou la chaussée) |
| Espace | Coup de poing |
| Entrée | Valider (titre, récits) |
| Échap | Pause ; pendant la pause, Q pour abandonner |

---

## Installation et lancement

Le projet n'a besoin que d'un **JDK 17 ou plus récent**. Il n'utilise aucune bibliothèque externe ; JUnit sert uniquement aux tests.

```bash
bash lancer.sh      # compile dans build/ puis ouvre le jeu
bash tester.sh      # compile le jeu et les tests, puis lance les 15 tests JUnit
```

Dans VS Code, avec l'extension *Extension Pack for Java*, les tâches « Lancer le jeu » et « Lancer les tests » sont configurées, et F5 lance le jeu.

---

## Organisation du projet

```
.
├── src/getajob/
│   ├── Main.java          point d'entrée : crée la fenêtre et démarre le jeu
│   ├── moteur/            boucle de jeu, clavier, caméra, animation, chargement des ressources
│   ├── entites/           tout ce qui vit dans la rue : le joueur, les racketteurs, les clients
│   ├── monde/             le niveau (décor, obstacles, entités) et les règles de la nuit
│   └── ecrans/            les états du jeu : titre, récit, partie
├── tests/getajob/         tests JUnit, rangés dans les mêmes packages que les classes testées
├── ressources/
│   ├── images/            décor et sprites
│   ├── sons/              effets sonores
│   ├── polices/           police Barlow Condensed (licence OFL fournie)
│   └── obstacles.txt      rectangles au sol que les personnages ne peuvent pas traverser
├── lib/                   JUnit 5 (console standalone), pour les tests uniquement
├── lancer.sh, tester.sh   compilation, lancement et tests
├── presentation/          diaporama du reveal, version PDF et discours
└── UML.md                 diagrammes UML du projet
```

| Package | Classes |
|---|---|
| `moteur` | `Jeu`, `Clavier`, `Camera`, `Animation`, `Ressources` |
| `entites` | `Entite`, `Personnage`, `Joueur`, `Ennemi`, `Client` |
| `monde` | `Niveau`, `Tournee` |
| `ecrans` | `Ecran`, `EcranTitre`, `EcranRecit`, `EcranPartie` |

---

## Architecture

Deux hiérarchies d'héritage structurent le jeu :

```
Entite (abstraite)               position au sol, zone de contact, dessin
 └─ Personnage (abstraite)       animation de marche, déplacement avec collisions, ombre, miroir
     ├─ Joueur                   piloté au clavier : coup de poing, vies, argent, commande
     ├─ Ennemi                   machine à états : PATROUILLE, POURSUITE, ETOURDI
     └─ Client                   attend sa commande et se tourne vers le joueur

Ecran (abstraite)                un état du jeu : mettreAJour + dessiner
 ├─ EcranTitre
 ├─ EcranRecit                   texte ligne par ligne : introduction et fins
 └─ EcranPartie                  possède le Niveau, la Tournee et la Camera
```

Les responsabilités sont séparées :
- **`Niveau`** sait où sont les choses et si un endroit est libre ;
- **`Tournee`** applique les règles : étapes, argent, heure, victoire et défaites ;
- **`EcranPartie`** relie les deux et affiche la partie.

Les règles ne dépendent pas de l'affichage : elles sont testées sans ouvrir de fenêtre.

Les diagrammes de classes, d'états et de séquence sont dans **[UML.md](UML.md)**.

---

## Les fondamentaux du jeu 2D dans le code

| Fondamental | Où | Principe |
|---|---|---|
| **Boucle de jeu** | `Jeu.tour()` | Un `javax.swing.Timer` toutes les 16 ms (environ 60 tours par seconde). Mise à jour, puis rendu. |
| **Temps** | `Jeu.tour()` | `dt` est mesuré avec `System.nanoTime()` et plafonné à 0,05 s. Les vitesses sont en pixels par seconde, multipliées par `dt`. |
| **Rendu** | `Jeu.paintComponent`, `EcranPartie.dessiner` | Le jeu dessine dans une image de 480×270, agrandie trois fois sans lissage. Ordre : décor, entités triées par profondeur, interface. |
| **Coordonnées et caméra** | `Entite`, `Camera` | Avenue de 4 260 pixels. Position à l'écran = position dans le monde − `camera.x()`. La caméra reste dans les limites du décor. |
| **Clavier** | `Clavier` | `tenue()` pour les actions continues (marcher), `vientDAppuyer()` pour les actions ponctuelles (frapper, valider). |
| **Entités** | `entites/` | Héritage `Entite` → `Personnage` → `Joueur` / `Ennemi` / `Client`. `Niveau.mettreAJour` les fait toutes vivre par polymorphisme. |
| **Collisions** | `Personnage.deplacer`, `Niveau.bloque`, `Entite.touche` | Rectangles au sol. Chaque axe est testé séparément : bloqué sur un axe, le personnage glisse sur l'autre. |
| **Interactions** | `Tournee.mettreAJour`, `Joueur.frapper` | Comptoir : prendre la commande. Client : livrer. Racketteur : perdre une vie. Coup de poing : étourdir. |
| **Animations** | `Animation`, `Personnage.animer` | Deux images de marche alternées toutes les 0,17 s, des poses pour le coup et le choc, un clignotement d'invincibilité. |
| **États** | `Ecran`, `Ennemi.Etat`, `Tournee.Etape` | États du jeu (écrans), du racketteur (machine à états) et de la progression de la nuit. |
| **Victoire et défaite** | `Tournee.issue()` | `EN_COURS`, `VICTOIRE`, `PLUS_DE_VIES` ou `TROP_TARD`. L'écran de partie passe au récit de fin correspondant. |

---

## Tests

| Classe de test | Tests | Ce qui est vérifié |
|---|---|---|
| `DeplacementTest` | 5 | Vitesse, immobilité, obstacle, glissement le long d'un obstacle, bords de la rue |
| `CombatTest` | 4 | Ronde, poursuite, coup de poing qui étourdit, héros attrapé |
| `TourneeTest` | 6 | Début de la nuit, paiement d'une livraison, client non payé sans commande, victoire, défaite à 6 h, défaite sans vies |

Les tests utilisent les vraies classes du jeu. Ils simulent le clavier et font avancer le temps par pas de 1/60 s, comme la boucle de jeu.

---

## Présentation

Le diaporama du reveal se trouve dans [presentation/](presentation/) : [get-a-job-reveal.pptx](presentation/get-a-job-reveal.pptx), avec les notes d'oral, sa version [PDF](presentation/get-a-job-reveal.pdf) et le [discours](presentation/DISCOURS.md).
Les diapositives 1 à 14 présentent l'univers et les mécaniques du jeu complet. La diapositive 15 introduit cette version, qui sert à la démonstration.
