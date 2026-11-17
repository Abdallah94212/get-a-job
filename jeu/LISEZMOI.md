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
| E, Entrée | Parler, agir, entrer, passer à la réplique suivante, choisir une réponse (Espace aussi, dans un dialogue) |
| A | Attaquer : un coup, un élan vers l'adversaire à quelques pas, ou un tir si l'arme en main tire et qu'il est loin |
| Espace | Sauter ; en l'air, A : coup de pied sauté (bas + A : attaque plongeante) |
| C / X | Esquive / garde (X au moment où le signe d'un ennemi passe au blanc : contre) |
| R / V | Recharger / arme suivante (les poings, puis chaque arme du sac) |
| J, K, L, H, G | Poing, pied (maintenu : puissant), balayage, lancer, lâcher l'arme |
| I / P | Les poches / le téléphone |
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
| 2.1 Le chimiste | Labo, banc | Savoir ce que veut Gero, demander le boîtier du mécano à Youngssef | |
| 2.2 Le boîtier du mécano | Banc, cour du 29 | Récupérer le boîtier, attendre vendredi | La cour du 29 |
| 2.3 La RS6 | Cour du 29, garage | Passer par la cour, battre le guetteur, la petite porte, les deux boîtiers, les aliens | |
| 2.4 Cap sur Namek | Namek | Récolter six plants au chrono, la douane | Namek |
| 2.5 Notre épicerie | Notre rue | Recruter, reprendre l'épicerie de Papy Bocal | L'épicerie |
| 2.6 L'affaire | L'épicerie, le téléphone | Confier les premières opérations, lancer Namek, laisser passer un mois | |
| 2.7 La visite | Notre rue | Les gars de Geto devant l'épicerie | |

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
- `Audio` est le mixeur central : six bus (musique, ambiances, monde extérieur, effets, voix, interface), chacun avec
  son volume, son filtre et sa réverbération ; des états de mixage pour les cinématiques (un choc étouffe le monde) ;
  des priorités, un anti-répétition, un limiteur. `Sonorisation` décide de ce qu'on entend selon le lieu, la météo,
  la position du joueur et le sol sous ses pieds. `Parole` fait parler les personnages en syllabes, au rythme du texte.
  La direction sonore complète (identité, carte des lieux, cinématiques, voix) est décrite dans `SON.md`.
- `EcranPartie` relie le tout : il transmet à `Progression` ce que fait le joueur et affiche ce qui en résulte.
- `Jeu` tourne sans fenêtre, ce qui permet aux tests de jouer la vraie campagne.

Les tests (`tests/`, JUnit 5, 204 tests) couvrent la stabilité (chaque scène passée avec Échap et jouée avec E,
une partie tapée au hasard qui rend toujours la main, aucune erreur rattrapée), le temps et la continuité entre les
missions, le quartier vivant, le combat, la progression, les drapeaux, la sauvegarde, la lecture de la
campagne, l'organisation et le mois d'affaires, et le parcours complet : un joueur automatique termine les douze
missions en tenant les touches (combats et délégations compris), vérifie
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

## La santé, le souffle, l'argent, le scooter

Quatre mécaniques qui se tiennent (`histoire/Etat.java`, sauvegardées avec la partie) :

| Mécanique | Ce qui la fait baisser | Ce qui la remonte | Conséquence |
|---|---|---|---|
| Souffle | Courir (18 par seconde), porter des caisses | Marcher, s'arrêter, manger, dormir | À zéro, on ne court plus avant d'avoir repris 30 ; chargé, on se traîne |
| Santé | Se faire repérer en mission furtive (30), heurter un obstacle en scooter (6, jamais mortel) | Un menu chez Old Geto (45), dormir chez soi (tout) | Sous 35 : on marche moins vite et le souffle est plafonné à 60. À zéro : réveil chez soi, un tiers de l'argent perdu |
| Argent | Louer le scooter (5), manger (8, puis 12 quand Geto est devenu l'ennemi), tomber K.-O. | Les livraisons et les paies de l'histoire | Sans argent : ni scooter ni repas |
| Scooter | La location dure 3 minutes | Louer à Youngssef, au banc | Il ne coûte pas de souffle et va plus vite ; F pour en descendre, E pour y remonter là où on l'a laissé |

Dans la campagne : `service <scène>` sous un figurant (ce qu'il propose quand on lui parle sans que l'histoire
l'attende), et dans une scène `argent +12`, `paie 5 <étiquette si l'argent manque>`, `soin 45`, `loue 180`.

## Le téléphone (touche P)

Tout ce qu'on y fait a une suite dans le monde (`jeu/Telephone.java` pour l'écran, `histoire/Etat.java` pour les données).
L'accueil est celui d'un vrai téléphone : l'heure et le jour du quartier, le solde, une grille d'icônes (flèches pour
se déplacer, E pour ouvrir) avec leurs pastilles (messages non lus, loyer dû, colis, opérations en cours), et les deux
dernières notifications. Chaque appli a sa couleur et glisse en place ; les listes montrent les visages (vignettes
des portraits), les montants en couleur, les jauges en barres. L'équipe : une carte par petit, E mène à ses opérations.

| Écran | Ce qu'on y fait | La suite dans le jeu |
|---|---|---|
| Messages | Lire, répondre par une des réponses proposées | Une réponse lève un drapeau de l'histoire : un objectif secondaire s'ouvre (les courses de Tata Lili) |
| Banque | Solde, derniers mouvements, payer un mois de loyer (40) | Un mois de loyer tombe à chaque mission terminée ; à deux mois de retard, plus de courant : on ne dort plus chez soi |
| Annonces | Acheter un commerce (kiosque 120, laverie 260 après avoir quitté Geto) | Sa recette tombe à chaque mission terminée |
| Commandes | Casque 35, baskets 45, gilet 80, bombe lacrymogène 60 | On paie, le colis arrive chez soi après une minute de jeu, il faut rentrer l'ouvrir pour avoir l'objet |
| Contacts | Appeler Youngssef | Il fait porter le scooter là où l'on est : 8 euros au lieu de 5 au banc |
| Opérations (dès 2.6) | Choisir une opération, le petit qui s'en charge et sa paie (radin, juste, avec prime) ; voir ses chances, les frais, la recette espérée | Frais et paie sortent tout de suite ; à la fin, la recette entre et le petit écrit |
| Équipe (dès 2.6) | Les jauges de fiabilité et de confiance, ce que chacun fait, ses réussites et ses échecs | Mal payé ou sans confiance, un petit refuse ou se sert sur la recette |

Dans la campagne : `sms` (affiché dans la scène, gardé par le téléphone) et
`message <qui> | <texte> | <réponse> -> +drapeau | <réponse> -> -` (arrive sans arrêter la scène).

## La typographie

Le décor reste en pixels ; le texte, lui, est net. Il est dessiné quatre fois plus fin que le décor sur un calque
transparent (`Rendu.texte`), posé par-dessus l'image agrandie : à l'écran comme dans les captures (`Capture`).
Deux polices libres (SIL Open Font License, dans `ressources/polices`) :
- **Atkinson Hyperlegible** (Braille Institute) pour tout ce qu'on lit : dialogues, objectifs, téléphone,
  notifications. Elle a été dessinée pour être lue sans fatigue ; ses accents français sont complets.
- **Barlow Condensed** (Jeremy Tribby) pour ce qu'on repère d'un coup d'œil : titres, noms des personnages, menus,
  étiquettes, chiffres.

Les textes écrits en capitales dans la campagne (objectifs, récits, notifications) sont affichés en casse de phrase,
noms propres compris (`jeu/Casse.java`) ; les titres et les noms gardent leurs capitales.

## L'ambiance : les effets et les sons

`jeu/Effets.java` fait vivre l'image et le son autour du joueur, sans rien changer aux règles :
- **Dehors :** la pluie gicle sous les pas (des flaques quand on court), la buée du souffle dans le froid, la fumée
  du scooter, la vapeur des bouches d'égout, des voitures qui passent (phares, feux arrière et leur reflet sur la
  chaussée mouillée), des étincelles sur l'enseigne du Maître Poulet, l'orage (l'éclair, puis le tonnerre).
- **Dedans :** la poussière dans la lumière de la télé ; l'éclair vu par la fenêtre, le tonnerre étouffé.
- **Le corps :** un coup fait trembler l'image et la rougit ; blessé, le bord de l'écran bat au rythme du cœur qu'on
  entend ; à bout de souffle, on l'entend respirer et l'image s'assombrit sur les bords.
- **Les bruits au loin (`Sonorisation`) :** le périph en fond, et au hasard une sirène, un chien, une moto, une
  alarme, une bouteille ; au Maître Poulet la vaisselle et le micro-ondes ; chez soi, la ville étouffée par la vitre.
- **L'interface :** l'argent qui rentre (pièces et éclats dorés), le téléphone qu'on sort et qu'on range, la
  notification d'un message, le colis qu'on ouvre, le repas.

Les sons viennent de packs libres d'OpenGameArt (détail : `ressources/sons/CREDITS.md`, produit par
`designs/sons/preparer.py`). Un test (`SonsTest`) vérifie que chaque son appelé par le code existe.

## La rue qui s'éteint

Le décor est enregistré dans la palette Endesga 32 (par ENDESGA, lospec.com/palette-list/endesga-32). Au début du jeu,
le quartier est en couleurs vives. Plus le héros monte, plus la rue devient grise, en quatre étapes :

| Étape | Moment de l'histoire | Couleurs |
|---|---|---|
| 1 | Missions 0.1 et 1.1 | Endesga pure : le quartier vivant |
| 2 | Missions 1.2, 1.3 et le début de 1.4 | Les mêmes teintes, ternies |
| 3 | Dès que le héros quitte Old Geto (drapeau `parti_de_chez_geto`), jusqu'à 2.3 | Presque gris, tiré vers le gris-bleu |
| 4 | Missions 2.4 et 2.5 | Gris ; seules les fenêtres et les enseignes gardent un peu de lumière |

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

## Fluidité

Le jeu tourne dans son propre fil, à 60 images par seconde d'un pas régulier, et pose l'image à l'écran par double
tampon. Le décor (480 × 270) et le calque du texte sont confiés à la carte graphique, qui les agrandit elle-même :
le processeur ne recalcule plus chaque pixel de l'écran.

| Mesure, sur un MacBook | Avant | Après |
|---|---|---|
| Fenêtre ×2 | 53 images/s, saccades (écarts de 20 ms, pics à 144 ms) | 60 images/s |
| Plein écran | 13 images/s (72 ms de peinture par image) | 60 images/s (0,3 ms de peinture) |

Pour mesurer soi-même : `java -Dgaj.mesure=true -cp build jeu.Fenetre ressources` écrit toutes les deux secondes le
nombre d'images par seconde et le temps de chaque étape. Avec `-Dgaj.demo=true` (et `-Dgaj.taille=3` pour le plein
écran), un pilote automatique lance une partie et court dans la rue, sur une sauvegarde temporaire.

## Le scooter

E le fait monter, et, à l'arrêt, le fait descendre : le scooter reste garé là où on l'a laissé, le héros pose le pied
à côté (jamais dans un mur). Arrivé devant un commerce, E fait entrer : le scooter attend devant la porte. F le gare
tout de suite, où qu'on soit. La location ne s'écoule que pendant qu'on roule ; quand elle finit, le scooter reste
garé sur place, et on peut le reprendre pour 5 euros.

## Le combat, les poubelles, les encarts

- **Le combat** : J donne un coup de poing (rapide, courte portée), K un coup de pied (lent, porte plus loin,
  repousse). Un ennemi qui prépare son coup affiche un signe rouge ; le toucher avant l'interrompt. Deux ennemis au
  plus attaquent à la fois. Perdre un combat ne tue pas : l'histoire continue avec sa scène de défaite.
- **Les poubelles** : deux grands conteneurs par carte de l'avenue, trois dans la cour du 29. E pour fouiller, une
  fois par mission : souvent rien, parfois quelques pièces, parfois une arme (bouteille, planche, tuyau, barre).
- **Les armes** : une seule en main (J frappe avec), G pour la lâcher. Elles s'usent, sauf la barre de fer.
  Pause, « Tes poches » : les objets de l'histoire et l'arme en main.
- **Les encarts** expliquent un objet ou une mécanique au moment où on la découvre, une seule fois. Ils se déclarent
  dans `campagne.txt` (`encart id GENRE | TITRE | image | ligne | ligne`) et se montrent depuis une scène (`encart id`)
  ou depuis le jeu. Une scène peut lancer un combat (`combat alien 4 scene_victoire scene_defaite`) et une porte peut
  être fermée à un moment de l'histoire (`ferme depuis vers +drapeau | phrase`).

- **Le combat, refait** (`jeu/Combat.java`) : J enchaîne trois coups si l'on rappuie pendant le geste (direct, second
  direct, crochet qui fait tomber) ; chaque coup coûte du souffle, à bout on frappe plus lentement. L'image se fige un
  instant à l'impact. Touché, le héros recule ; trois coups de suite ou un coup lourd quand il est faible, il tombe, reste
  au sol, se relève (intouchable le temps de se remettre). Les ennemis enchaînent deux coups, et une fois touchés ils
  apprennent : garde (un trait bleu devant eux, le pied la casse) ou esquive en arrière. Interrompus une fois, ils
  arment le coup suivant sans se laisser couper : on ne les enferme plus dans une rafale. Trois coups de suite, ils
  tombent et se relèvent en colère. Les aliens bondissent de loin. Les poses (armer, frapper, encaisser, au sol, se
  relever, à bout de souffle) sont tirées des dessins existants (`decor/Poses.java`), sans rien redessiner.

## Le quartier vivant (`jeu/Vie.java`)

- Chaque personnage a sa routine (`routine poste|flane|assis|comptoir [rayon]` dans la campagne) : Savon va et vient
  devant la laverie, Ticket reste au parking et regarde autour de lui, Youngssef reste assis sur son banc, Geto tient
  son comptoir, Gero tourne entre ses cuves. Tous suivent des yeux le héros qui passe. Celui que l'histoire attend
  reste à sa place.
- Dans l'avenue, des passants sortent des immeubles ou arrivent par le bord de l'écran, traversent, s'arrêtent à deux
  pour discuter, repartent : plus nombreux l'après-midi que la nuit, moins sous la pluie. E près d'eux : une phrase.
- Bousculé, on s'écarte et on le dit. Frappé, on réagit selon son caractère (`caractere fragile|costaud|police|proche`,
  déduit du dessin sinon) : fuir en criant, riposter (une bagarre de rue), intervenir (la police : une amende si elle
  gagne), ou s'en souvenir (un petit de l'équipe perd de la confiance). Les témoins crient, s'éloignent, s'en mêlent.
- Les petits de l'équipe partis sur une opération ne sont pas à la boutique, et le scooter de Youngssef n'est pas
  devant l'épicerie quand il roule (`membre <id>`).
- Les personnages importants se présentent la première fois qu'ils parlent : une fiche avec leur portrait manga,
  leur rôle, ce que le héros sait d'eux à ce moment-là, et où ils en sont tous les deux (`encart presente_<id>
  PERSONNAGE | ...`).

## La route de Namek (mission 2.4, `jeu/Conduite.java`)

Une séquence de conduite vue du dessus, jouée dans la boucle du jeu (un mode de plus, comme le téléphone) : la scène
de départ dit `conduite namek namek_arrivee`, la route se joue, puis la scène d'arrivée. Trois tronçons de plus en
plus durs : l'autoroute de nuit (des camions à doubler), la départementale (virages, nids-de-poule, chèvres), la piste
de Namek à l'aube (étroite, des rochers, le vide au bord). Haut accélère, bas freine, gauche et droite tournent, Maj
pour le turbo (il boit). L'essence baisse (bidons rouges sur la route), chaque choc abîme la RS6, le gravier freine.
Tomber de la piste, la panne sèche ou la voiture qui fume font repartir au début du tronçon (et, deux fois au même
endroit, juste avant ce passage). Arriver après le lever du soleil raccourcit la récolte à pied de quinze secondes
(`soleil_leve`) ; une RS6 très abîmée se remarque (`rs6_abimee`). Échap met en pause. Une partie quittée au volant
reprend la route au chargement.

Les images de la route (voitures, plots, fûts, pneus, rochers, flaques d'huile, arbres, asphalte, herbe, sable) viennent
du « Racing Pack » de Kenney (www.kenney.nl), sous licence CC0 : le pack est dans `designs/assets/kenney_racing/`, et
`designs/decor/conduite.py` les réduit à l'échelle du jeu, les assombrit pour la nuit et réchauffe l'aube de Namek.

## Le combat, deuxième passe

- J : trois coups enchaînés si l'on rappuie (le dernier fait tomber). K : coup de pied ; K maintenu puis lâché : coup
  de pied chargé, qui casse une garde et envoie au sol. L : balayage, des deux côtés, qui fait tomber garde ou pas.
  H : lancer une canette, une pierre (trouvées dans les poubelles) ou l'arme en main : l'objet vole, a une ombre,
  touche ce qu'il rencontre (ennemi ou passant), et une arme retombe par terre, où on la ramasse.
- Chaque coup a son anticipation (l'épaule ou la hanche recule), puis sa phase d'impact : rien ne touche avant. Le
  héros se tourne vers l'ennemi qu'il vise et se cale à sa hauteur.
- À plusieurs : deux attaques au plus à la fois, les autres se postent de part et d'autre (ils encerclent), les corps
  ne passent pas les uns dans les autres. Une bagarre de rue se fuit : assez loin, assez longtemps, elle s'arrête.
- Les astuces apparaissent une fois, au moment où elles servent : les touches au premier combat, K maintenu quand un
  ennemi se garde, L quand on est entouré, H quand on a de quoi lancer, la fuite dans une bagarre de rue.

## Le loyer, la banque, les récapitulatifs

- Le loyer du 12 suit le calendrier : trois mois de retard au début (120 euros, la lettre de la régie), un mois de plus
  le 5 de chaque mois, 40 euros. Ce qui est dû, c'est ce qui est tombé moins ce qui est payé : la banque, le téléphone,
  les récapitulatifs et la sauvegarde lisent ce seul chiffre. Quatre mois de retard : plus de courant.
- Une mission peut avoir une ellipse (`ellipse <scene>`) : ce qui s'est passé hors de l'écran, appliqué pour de vrai
  au moment du saut dans le temps. Avant « Le Plafond » : trois semaines de livraisons payées par Geto, et le retard de
  loyer réglé. L'écran du temps qui passe montre alors « LE COMPTE » : les lignes de la banque, et le solde avant, après.
- Le récapitulatif de fin de mission montre ce qui s'est passé (les objectifs) et ce que la mission a fait au compte,
  par poste (ventes de Cell, paies de l'équipe, cuves de Gero, loyers, frais, ce qu'a payé Geto), depuis son début.
- Au téléphone, une réponse entre parenthèses (« (Ne rien répondre.) ») est un geste : rien n'est envoyé.
- Une scène réserve sa place : ceux qui n'y jouent pas et sont trop près s'écartent à pied, puis reviennent.

## L'inventaire (touche I)

- `histoire/Inventaire.java` est la seule base des objets : livraisons (sacs de Geto, enveloppe, caisses, sachets,
  plants, Cell), armes (trois au plus dans le sac, une en main, chacune avec son usure), objets (beeper, boîtier,
  éclat, ce qu'on achète). Les objets empilables partagent une ligne et une quantité ; les autres gardent la leur.
  Tout ce qui touche aux objets passe par lui : les scènes (`donne <objet> [n] [qui]`, `remet ...`), les objectifs
  (`avec:<objet>` : pas de livraison sans le sac, le jeu le dit), le combat (l'arme en main), les poubelles, les colis
  du téléphone (ouverts, ils entrent dans l'inventaire), la sauvegarde. Une partie d'avant l'inventaire retrouve ce
  qu'elle avait (les objets achetés, l'arme, le beeper, et les sacs déjà remis par une scène mais pas encore livrés).
- La remise se joue : celui qui donne se penche, le héros tend la main, l'objet passe de l'un à l'autre sur un petit
  arc, avec le son de sa matière (papier kraft, enveloppe, caisse, sachet, feuillage) ; à l'arrivée, « +1 SAC BLANC »
  glisse à droite de l'écran. Pendant une livraison, le héros tient le sac à la main, et « 2 À LIVRER · I » s'affiche.
- L'écran (I, ou « Tes poches » dans la pause) : trois onglets, une grille d'illustrations (`obj_*.png`, dessinées par
  `designs/decor/inventaire.py`) avec les quantités, et la fiche de l'objet choisi (à qui il va, ce qu'il vaut, l'usure
  d'une arme). E prend une arme en main ou la range, mange le sandwich ; G pose une arme par terre. Ce dont l'histoire a
  besoin ne se jette pas.

## L'appel de Geto (mission 1.2)

`appel <qui> sonne <voix>` : son téléphone sonne et vibre, l'appelant s'affiche au-dessus de lui, il s'interrompt.
`appel <qui> decroche` : il porte le téléphone à l'oreille et se détourne du héros. Tant qu'il est en ligne, ses
répliques ont un onglet bleu « AU TÉLÉPHONE », son portrait tient le combiné, et la voix au bout du fil n'a pas de
visage : l'écran d'un appel en cours (durée, onde de la voix), ses répliques en bleu pâle, entre guillemets.
`appel <qui> raccroche` : il raccroche et revient vers le héros. Sons : `tel_sonnerie`, `tel_decroche`,
`tel_raccroche` (`designs/sons/acte1.py`).

Les fiches de présentation ne s'ouvrent qu'à une vraie rencontre : quand le joueur adresse la parole au personnage
(E), ou quand celui-ci est juste à côté du héros dans la scène. Youngssef qui appelle de loin en sortant de chez soi
n'est présenté qu'au moment où on va lui parler.

## Le temps, les journées, la continuité

- Chaque mission dit quand elle se passe (`quand <jour> <hh:mm>`, le jour 1 étant le premier vendredi). L'horloge
  avance d'une minute par seconde de jeu ; une scène peut faire une ellipse (`temps <jour> <hh:mm>`).
- Plus de six heures entre deux missions : l'écran du temps qui passe montre les jours qui défilent et ce qui est
  vraiment arrivé entre-temps (opérations terminées et payées, colis livrés, scooter rendu, loyer dû), et le héros se
  réveille chez lui. Une nuit de sommeil fait de même, en plus court.
- Le scooter emprunté retourne chez Youngssef après une nuit ; tombé à scooter, on le retrouve là où on est tombé.
  Une arme posée (G) reste par terre, même après une sauvegarde, et se ramasse avec E.
- La stabilité : une erreur ne fige plus la fenêtre (elle est écrite dans `~/.get-a-job/incidents.log`, la partie
  ferme ce qui était ouvert et rend la main ; trois de suite, retour au menu, partie enregistrée). Un encart ouvert
  par une scène se voit (c'était la cause des images figées pendant les dialogues). Les touches tenues sont oubliées
  quand la fenêtre perd le clavier. Une scène qui tourne en rond est abandonnée au lieu de bloquer le jeu.

## L'organisation et le mois d'affaires

- `histoire/Organisation.java` : les petits, leurs jauges, les opérations (livraisons, comptoir, ardoise d'Omar, course
  de Namek), le lancement, l'avancement, le règlement. Tout l'argent passe par la banque du héros (`Etat.payer` et
  `Etat.gagner`, avec un libellé) : une opération n'est payée qu'une fois, au départ. Les opérations en cours sont
  sauvegardées et continuent sur toutes les cartes ; une nuit de sommeil les fait avancer.
- `histoire/Mois.java` : trente jours d'activité calculés d'un coup, avec les vraies opérations, les loyers et les
  frais, puis sauvegardés. `jeu/Timelapse.java` les rejoue jour après jour : calendrier, solde, courbe, totaux par
  poste, notifications et messages des petits, puis le bilan. E accélère, Échap passe (le mois est déjà compté).
- `tests/histoire/OrganisationTest.java` vérifie les réussites, les ventes décevantes et les échecs, les jauges, les
  refus, l'absence de double paiement, la sauvegarde des opérations en cours, et qu'un mois se relit euro par euro.

## Le combat hybride et l'arsenal

- **Une touche, A, qui choisit.** `jeu/Combat.java` choisit une cible (celle qui est dans la direction tenue, sinon
  celle de devant ; un mur entre les deux l'écarte) et décide : arme à feu et cible loin, un tir ; cible proche, ou
  arme de contact, un coup ; cible à quelques pas et pas d'arme à feu, un élan vers elle puis le coup. Le passage du tir
  au corps à corps a une marge (on entre au contact sous 38 pixels, on revient au tir au-delà de 58) : pas de bascule
  sans fin à la limite. Rappuyer enchaîne (poing, direct, pied ou crochet selon sa garde, circulaire) ; tenir la
  direction de l'adversaire donne l'uppercut, qui le soulève. Un compteur d'enchaînement s'affiche dès trois coups ; au
  cinquième, les coups s'accélèrent.
- **Le saut au combat.** En l'air, A est un coup de pied sauté qui avance et projette ; bas + A, une attaque plongeante
  qui couche tout autour à la réception ; juste après la réception, A reprend l'enchaînement au deuxième coup. Les coups
  sautés passent les gardes. Un ennemi qui voit le héros sauter peut viser haut et le cueillir en l'air (il est alors
  projeté). La position au sol (x, y) et la hauteur (z) sont séparées, pour le héros comme pour les ennemis projetés.
- **Défense.** C : esquive (un instant intouchable, dans la direction tenue). X maintenu : la garde prend l'essentiel
  d'un coup, mais coûte du souffle et cède à bout. X quand le signe rouge d'un ennemi passe au blanc : contre, il
  vacille, le héros riposte.
- **Quatre temps par coup** (élan, déclenchement, impact avec un gel d'image, retour en garde) et des réactions selon la
  force : la tête qui part, le déséquilibre (il voit des étoiles), la chute, la projection, le soulèvement (on peut le
  reprendre en l'air), un temps au sol avant de se relever. Poussière, éclats, secousse légère.
- **L'arsenal** (`histoire/Etat.java`, `Etat.Arme`, et l'inventaire) : bouteille, planche, tuyau, barre ; batte (deux
  élans, le second envoie voler) ; katana (trois coupes, la dernière soulève) ; katana relique (quatre coupes, la
  dernière libère une onde ; unique, il ne casse pas, ne se lance pas, ne se pose pas) ; pistolet (précis, coup par
  coup, 8 balles) ; pistolet lourd (lent, la balle traverse un corps et couche, 6 balles) ; mitraillette (rafale, se
  disperse vite, 25 balles) ; fusil d'assaut (loin, fort, 30 cartouches rares). Chaque arme à feu a ses dégâts, sa
  cadence, sa portée, sa dispersion et son échauffement, son chargeur, son temps de rechargement et ses munitions
  (balles de 9 mm, de .45, cartouches de fusil, rangées dans les poches). Le chargeur est sauvegardé avec l'arme, posée
  par terre comme dans le sac. Les balles s'arrêtent aux murs et aux meubles. Un appui pendant l'intervalle entre deux
  tirs n'est pas perdu : le tir part dès que l'arme le permet.
- **Les ennemis** : le bagarreur ; le sabreur (pare, riposte aussitôt, trois coupes) ; le tireur et le mitrailleur
  (gardent leurs distances, cherchent une ligne de tir, visent avec un trait rouge, tirent, rechargent ; de près, la
  crosse) ; la brute (lente, les petits coups ne l'arrêtent pas, elle charge : on saute par-dessus ou on esquive ;
  contre un mur, elle reste sonnée) ; l'alien (bondit). Deux au plus attaquent au contact, un seul tireur fait feu à la
  fois. La difficulté monte avec l'histoire (`EcranPartie.niveauDuCombat`). Un ennemi armé lâche son arme en tombant.
- **La progression** : à l'acte 1, les poings et ce que cachent les conteneurs ; à l'acte 2, la batte dans le conteneur
  de la cour du 29 ; le pistolet de Papy Bocal quand il cède l'épicerie (2.5) ; à Namek (2.4), la lame plantée dans la
  roche, qui ne sort que pour celui qui porte l'éclat irisé ; à la fin de l'acte 2 (2.7), la visite des gars de Geto
  tourne au combat contre une bande armée (bagarreur, sabreur, mitrailleur, brute) : leurs armes restent par terre, et
  le sac tombé de leur coffre contient le pistolet lourd et le fusil d'assaut, pour l'acte 3.
- **Les dessins et les sons** : `designs/decor/arsenal.py` (douze poses de combat du héros au même trait que
  `heros.py`, les armes en main, les icônes, le sabreur, le tireur, la brute, la lame dans la roche, le sac) et
  `designs/sons/arsenal.py` (quatre détonations, la mécanique des armes, la lame, la batte, les impacts, la garde, le
  contre, l'esquive, la plongée).
- **Les tests** : `tests/jeu/CombatArsenalTest.java` (les seize situations : enchaîner, pieds et balayette, attaques
  aériennes, esquive, plusieurs adversaires, tir de loin et coup de près, la marge, changer de cible, les murs qui
  arrêtent les balles, ramasser une arme, tirer et recharger, la rafale, le chargeur vide, changer d'arme en combat, la
  sauvegarde ; et le tireur, la brute, les armes lâchées, la relique) et `tests/jeu/ArsenalHistoireTest.java` (où
  l'histoire place les armes).

