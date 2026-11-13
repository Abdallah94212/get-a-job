# GET A JOB : le jeu

Un jeu 2D en vue de côté, dans l'avenue de Fontainebleau au Kremlin-Bicêtre. On y joue le prologue et les actes 1 et 2
de l'histoire (`histoire-du-jeu.md`) : neuf missions reliées entre elles, des scènes jouées avec les décors du jeu,
des dialogues à choix, des lieux qui s'ouvrent au fil de l'histoire, une ambiance sonore, un menu complet et une
sauvegarde. Java pur, sans bibliothèque à installer (JDK 17 ou plus). La logique de l'histoire est expliquée dans
[NARRATION.md](NARRATION.md).

## Lancer et tester

```bash
bash jeu/lancer.sh
```

```bash
bash jeu/tester.sh
```

| Touche | Effet |
|---|---|
| Flèches ou ZQSD | Marcher : gauche et droite le long de l'avenue, haut et bas entre le trottoir et la chaussée |
| Maj | Courir |
| E, Entrée ou Espace | Parler, agir, entrer, passer à la réplique suivante, choisir une réponse |
| Haut, bas | Changer de réponse dans un dialogue |
| Échap | Pause en jeu, retour dans les menus, passer une cinématique |
| T | Trame à l'ancienne, oui ou non |

La partie s'enregistre toute seule à chaque objectif, à chaque porte et quand on revient au menu, dans
`~/.get-a-job/partie.properties`. Les options sont dans `~/.get-a-job/reglages.properties`.

## La campagne

| Mission | Lieu | Ce qu'on y fait | Ce qu'elle ouvre |
|---|---|---|---|
| 0.1 Ce soir-là | Chez toi | L'introduction, le courrier de la régie, descendre dans la rue | |
| 1.1 Le scooter de Youngssef | Bas et milieu de l'avenue | Faire les deux livraisons de Youngssef sur son scooter | Le garage |
| 1.2 La viande de Bidoche | Maître Poulet, ligne 7, Crimée | La course de Geto, la scène de la rame, Bidoche, Soul, la Cell Perfect | Le métro |
| 1.3 Livreur de Verte | Labo, milieu de l'avenue | La marchandise chez Gero, trois livraisons sans se faire voir | Le labo |
| 1.4 Le plafond | Maître Poulet | Réclamer sa part, quitter Geto, le beeper du SDF | |
| 2.1 Le chimiste | Labo, banc | Proposer un marché à Gero, revoir Youngssef | |
| 2.2 La RS6 | Garage d'Omar | Voler la voiture par la petite porte | |
| 2.3 Cap sur Namek | Namek | Récolter six plants au chrono, la douane | Namek |
| 2.4 Notre épicerie | Notre rue | Recruter, reprendre l'épicerie de Papy Bocal | L'épicerie |

Une mission n'est disponible que quand la précédente est terminée. Un lieu fermé refuse réellement l'entrée et dit
pourquoi ; les bureaux et la tour restent fermés, ils appartiennent aux actes 3 à 7, qui ne sont pas encore écrits.
Toute la campagne est décrite dans un fichier texte, `ressources/histoire/campagne.txt` : lieux fermés, personnages,
missions, objectifs, scènes, réponses à choisir et drapeaux (ce dont l'histoire se souvient : un choix de dialogue,
un objet donné). On la modifie sans toucher au code ; son en-tête explique chaque instruction, et le jeu refuse au
lancement une campagne qu'on ne pourrait pas finir (cible inconnue, lieu jamais ouvert, saut vers une étiquette absente).

## Le code

| Paquet | Rôle | Classes principales |
|---|---|---|
| `decor` | Le moteur d'affichage : cartes en couches, tri par profondeur, police en pixels, effets | `Scene`, `Rendu`, `Acteur`, `Joueur`, `Police` |
| `histoire` | Le modèle de l'histoire, sans aucun affichage | `Campagne`, `Chapitre`, `Mission`, `Objectif`, `Progression`, `Sauvegarde`, `Cinematique`, `LecteurCampagne` |
| `jeu` | Le jeu lui-même : écrans, monde, interface, son | `Jeu`, `Fenetre`, `EcranMenu`, `EcranPartie`, `Monde`, `Scenographe`, `Affichage`, `Audio`, `Sonorisation`, `Reglages` |

- `Progression` décide de tout ce qui touche à l'avancée : mission en cours, objectif en cours, lieux ouverts, échec
  d'une séquence chronométrée ou furtive. Elle ne connaît ni l'écran ni le clavier.
- `Monde` met l'histoire dans le décor : personnages présents, objets sur lesquels agir, patrouilles, camera.
- `Scenographe` joue les scènes avec les éléments du jeu : décor, personnages qui marchent, caméra, gros plans, SMS,
  annonces du métro, dialogues avec bustes et réponses à choisir.
- `Audio` est un petit mixeur logiciel : musique, ambiances en boucle et effets, chacun avec son volume, en fondu.
  `Sonorisation` décide de ce qu'on entend selon le lieu, la météo, la position du joueur et le sol sous ses pieds.
- `EcranPartie` relie le tout : il transmet à `Progression` ce que fait le joueur et affiche ce qui en résulte.
- `Jeu` tourne sans fenêtre, ce qui permet aux tests de jouer la vraie campagne.

Les tests (`tests/`, JUnit 5, 42 tests) couvrent la progression, les drapeaux, la sauvegarde, la lecture de la
campagne, et le parcours complet : un joueur automatique termine les neuf missions en tenant les touches, vérifie
que les lieux fermés le restent, que la partie se reprend au même endroit après chaque objectif, que les réponses
changent la suite, que chaque scène va jusqu'au bout, que chaque son demandé existe et que l'ambiance suit le lieu.

## Les cartes

| Carte | Contenu | Longueur |
|---|---|---|
| `avenue_bas` | Le Maître Poulet, chez toi, la fourche et le banc, le panneau « GET A JOB » | 4 260 px |
| `avenue_milieu` | Le mur de Sisyphe, le labo, la laverie, notre épicerie, le garage, le café, le parking, la salle | 5 250 px |
| `avenue_haut` | Chez les Fatou, le métro, les bureaux de la Krousty, la tour | 3 880 px |
| `maitre_poulet` | La salle, le comptoir d'Old Geto, la cuisine, l'arrière-boutique | 760 px |
| `appartement` | Chez toi : canapé, télé, poster, mur du plan | 480 px |
| `labo` | Le labo de Gero : vestiaire, paillasse, cuves, poste de contrôle, stock | 960 px |
| `epicerie` | Notre épicerie : rayons, frigos, comptoir, les petits | 600 px |
| `garage` | L'atelier d'Omar : l'établi et le Flipper, la RS6, le pont, le bureau | 760 px |
| `bureaux` | L'open space de la Krousty, la salle de réunion vitrée, l'ascenseur | 760 px |
| `hall_tour` | Le hall : l'écran « Bienvenue sur Titan », les portiques, l'ascenseur | 640 px |
| `metro` | Carte parallèle : la rame de l'intermission | 960 px |
| `namek` | Carte parallèle : la récolte, au lever du soleil (on y va par la RS6 du garage) | 960 px |
| `etage_anges` | Carte parallèle : l'étage sans numéro, dans l'autre dimension | 960 px |
| `bureau_baba` | Carte parallèle : le dernier étage | 600 px |

Les trois cartes de l'avenue s'enchaînent quand on arrive au bout. On entre dans les intérieurs par leur porte.

## Le décor et les objets ajoutés

Le décor est celui de la première version : immeubles, façades, trottoirs, chaussée et intérieurs sont dessinés par
les scripts de `designs/decor/`. Rien n'y a été remplacé. Deux choses s'y ajoutent.

**Les meubles et les plantes** viennent du pack pH64 de Stacy Kendra Love (Narcissist Interactive), sous licence
CC-BY 4.0. Détail et source dans `jeu/ressources/packs/ph64/CREDITS.md`. Ils sont reteintés pour chaque lieu.

| Lieu | Ajouté |
|---|---|
| Chez toi | Coin bureau (bureau, écran, lampe, chaise), lit, lampadaire allumé, plante, horloge, radio |
| Maître Poulet | Trois suspensions, deux tables avec leurs chaises, une plante, un plan de travail en cuisine |
| Notre épicerie | Deux présentoirs, une plante, une pendule, un téléphone au comptoir |
| Garage d'Omar | Radio et ventilateur sur l'établi, tabouret, pendule, le coin d'Omar (classeur, bureau, écran, siège) |
| Labo de Gero | Poste d'analyse (bureau, écran, tour, siège), tabouret, classeur, caméra |
| Bureaux Krousty | Sièges devant les bureaux, lampe, téléphone, classeur, horloge, chaises de réunion, plante, caméra, deux postes |
| Hall de la tour | Coin d'attente (canapé, table basse, fauteuil), plantes, écran et téléphone à l'accueil, caméra |
| Bureau de Baba | Deux fauteuils de visiteur, lampadaire, buffet, vieux téléphone, lampe, plante |
| Avenue | Plantes en pot devant le Maître Poulet, les bureaux, la tour et chez les Fatou, jardinières des terrasses et du parvis, applique de ta porte |

Un meuble posé contre le mur ne gêne pas. Un meuble posé dans la pièce arrête le joueur, qui peut passer devant ou
derrière lui.

**Le petit mobilier de rue** (vélo, boîte aux lettres, barrière, buisson) est dessiné dans `objets.py`, comme le reste
du décor : aucun pack trouvé n'allait avec le style de l'avenue.

Packs écartés après essai : « Street Sprites » (CC0, trait de dessin animé), « City Pixel Tileset » (CC0, palmiers et
bouche d'incendie américaine, trop petit), et le « RPG Urban Pack » de Kenney (vu de dessus, trop rond et coloré).

## La rue qui s'éteint

Le décor est enregistré dans la palette Endesga 32 (par ENDESGA, lospec.com/palette-list/endesga-32). Au début du jeu,
le quartier est en couleurs vives. Plus le héros monte, plus la rue devient grise, en quatre étapes :

| Étape | Moment de l'histoire | Couleurs |
|---|---|---|
| 1 | Missions 0.1 et 1.1 | Endesga pure : le quartier vivant |
| 2 | Missions 1.2, 1.3 et le début de 1.4 | Les mêmes teintes, ternies |
| 3 | Dès que le héros quitte Old Geto (drapeau `parti_de_chez_geto`), jusqu'à 2.2 | Presque gris, tiré vers le gris-bleu |
| 4 | Missions 2.3 et 2.4 | Gris ; seules les fenêtres et les enseignes gardent un peu de lumière |

Rien n'est redessiné : `Scene.teinter` remplace les 32 couleurs par celles de l'étape (`decor/Palette.java`, fichier
généré par `designs/decor/palette.py`). L'étape vient de `Progression.etapeDeLaRue`. Les personnages gardent leurs
couleurs. Old Geto fait exception : il est en couleurs tant qu'il est le patron (`geto.png`), puis gris quand il
devient l'ennemi (`geto_gris.png`), au même moment que l'étape 3.

Les arbres de l'avenue viennent du FREE Pixel Art Tree Pack de karsiori (CC0, `designs/decor/packs/arbres_karsiori`).
Deux formes alternent, avec des verts, des hauteurs et des sens différents.

## Comment c'est fait

| Dossier | Rôle |
|---|---|
| `designs/decor/` | Les scripts Python qui dessinent les textures et construisent les cartes (`meubles.py` pour les objets du pack) |
| `jeu/ressources/packs/` | Les planches des packs, telles que téléchargées, avec leur licence |
| `jeu/ressources/decor/` | Le résultat : les images et un fichier `.niveau` par carte (générés, ne pas modifier à la main) |
| `jeu/src/decor/` | Le code Java |

Pour tout reconstruire, par exemple à un autre acte (de 1 à 7) :

```bash
cd designs && ~/Documents/comfy/.venv/bin/python decor/avenue.py 5
```

Pour ajouter une carte : écrire une fonction dans `designs/decor/avenue.py` (et son fond dans `interieurs.py`
si c'est un intérieur), puis y relier une porte avec `porte(...)`.

## Ajouts du mode histoire

- **Le labo** fait maintenant 960 px et se lit en cinq zones : le vestiaire (casiers, blouses, coin café), la
  paillasse (verrerie, microscope, centrifugeuse, étagères de produits, hotte), les cuves (manomètres, vannes,
  armoire électrique, câbles, marquage au sol), le poste de contrôle (tableau blanc, écrans de suivi, baie de
  serveurs, poste à deux écrans) et le stock (rayonnage, sacs de Piccolo, palette de cartons).
- **Viennent du pack pH64** (CC-BY 4.0) : bureau, écrans, tour, sièges, fauteuil, table basse, serveurs, cafetière,
  radio, balance, lampe, caméra, plantes. **Dessinés pour le jeu** dans `designs/decor/` : les murs et tout ce qui y
  est fixé, le clavier, la souris, les dossiers, le chariot, la palette, les bidons, le Flipper Zero.
- **Les personnages** (`designs/decor/personnages.py`) sont dessinés pour le jeu, dans le trait du gabarit du héros,
  avec un second dessin pour la marche. Chacun a un buste dessiné à part pour les dialogues. Old Geto et l'ange
  gardent leurs dessins d'origine. Le héros a aussi ses poses en scooter et chargé.
- **La boucherie de Bidoche** (Crimée) est un nouvel intérieur ; **la rame du métro** reçoit ses passagers.
- **Les gros plans** des scènes (les yeux devant la télé, le courrier, le sac, les vitres du métro, le GPS, le mur
  du plan) sont dessinés dans le même style.
- **La police** du jeu est celle des enseignes de l'avenue, complétée des accents du français.
- **La pluie** tombe dans les rues pendant les missions où il pleut (et dans le menu), avec des éclaboussures sur la
  chaussée ; à l'intérieur, on ne la voit plus mais on l'entend contre les vitres.
- **Les sons** : 45 enregistrements et musiques sous licence libre (CC0 surtout, quatre en CC-BY-SA 4.0), et
  18 sons fabriqués par `designs/sons/preparer.py` quand aucun enregistrement libre ne convenait (le métro, le
  scooter, l'interface, le vent). Sources, auteurs et licences : `ressources/sons/CREDITS.md`.
