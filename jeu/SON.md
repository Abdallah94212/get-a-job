# GET A JOB — direction sonore

## L'identité

Une nuit de pluie au Kremlin-Bicêtre, entendue de très près. Le son du jeu est **humide, proche et un peu étouffé** :
la pluie est partout, la ville ne s'arrête jamais (le périph au loin, un klaxon, un bus), mais on l'entend souvent
à travers quelque chose — une vitre, un mur, un rideau de fer. Les intérieurs sont petits et vivants (un frigo, un
néon, une radio de cuisine), jamais vides. La musique est rare : elle accompagne la rue, puis se tait et laisse la
ville respirer.

Trois règles guident chaque choix :

1. **On entend ce que le héros entend.** Dans sa chambre, la rue passe par la vitre (le grave seulement). Quand un
   choc le coupe du monde, le monde s'étouffe ; seuls restent son cœur, son souffle, un sifflement.
2. **Le silence est un son.** Après un choc, on laisse le vide. Chez soi, la nuit, pas de musique : la pièce seule.
3. **Rien ne tourne en boucle visible.** Chaque bruit de fond est fait de couches indépendantes, chacune à son
   rythme ; chaque bruit répété a plusieurs prises, jamais deux fois la même de suite.

## L'architecture

| Pièce | Rôle |
|---|---|
| `Audio` | Le mixeur central. Six bus, un volume par bus, un filtre et une part de réverbération par bus, les états de mixage, la conversation (la musique se range sous les voix), la salle (une réverbération qui suit le lieu), les priorités (48 sons au plus, les plus faibles cèdent), l'anti-répétition, le limiteur, le chargement à la demande des musiques et leur libération. |
| `Sonorisation` | La carte sonore : ce qu'on entend dans chaque lieu, les pas selon le sol, le scooter, les portes, les gestes. |
| `Parole` | Les voix des personnages : des syllabes qui suivent le texte, avec intonation, émotions et réactions. |
| `Effets` | Les sons liés à ce qu'on voit : voitures qui passent, orage, cœur et souffle quand on est blessé. |
| `Scenographe` | Les cinématiques : `son`, `ambiance`, `musique`, `mixage`, le ton des répliques. |

### Les bus

| Bus | Ce qui y passe | Réglage | Particularité |
|---|---|---|---|
| Musique | les thèmes | Musique | baisse de 45 % quand quelqu'un parle |
| Ambiances | boucles des lieux, télé, radio | Ambiances | légère réverbération |
| Monde extérieur | pluie, rue, périph, klaxons, sirènes, tonnerre | Ambiances | filtré selon le lieu : ouvert dehors, 700 Hz derrière une vitre |
| Effets | pas, portes, gestes, scooter | Effets | toute la réverbération de la salle |
| Voix | syllabes, réactions, et le corps (cœur, souffle, acouphène) | Voix | jamais étouffé par un état de mixage |
| Interface | menus, téléphone, objectifs, missions | Interface | sec, toujours net |

Un volume général règle le tout. Les six volumes sont dans les options, et chaque curseur fait entendre un son de
son propre bus quand on le déplace.

### Les états de mixage

`mixage <état> [durée]` dans une scène :

- **normal** : tout est ouvert.
- **choc** : le monde (ambiances, extérieur, effets, musique) passe sous 260 Hz et recule ; la voix et le corps restent nets.
- **sourdine** : le monde passe sous 750 Hz. C'est le souvenir, la sidération, le temps suspendu.
- **silence** : tout se tait sauf la voix et l'interface.

Une scène qui se termine ramène toujours le mixage à normal. Changer d'écran coupe les effets et les voix en cours.

## La carte des lieux

| Lieu | Fond (boucles) | Sources placées | Événements au hasard | Sol | Salle | Dehors |
|---|---|---|---|---|---|---|
| L'avenue | rue sèche ou mouillée, périph, pluie, gouttière, ruissellement | foule devant le Poulet, la brasserie, le café ; néon ; laverie ; pluie sur l'abribus et les vitrines | klaxons, bus, scooters, motos, sirène, chien, alarme, bouteille, porte, rideau de fer, clochette, passants qui parlent, rafales avec une enseigne qui grince, plaque d'égout | bitume (mouillé : flaques) | légère | — |
| L'appartement | frigo, courant, horloge | la télé restée sur la neige | voisins à travers le mur, tuyaux, parquet, chien et sirène derrière la vitre | parquet qui grince | petite | vitre (700 Hz) |
| Le Maître Poulet | frigo, néon, hotte, murmure | radio sur le comptoir, friteuse au fond | vaisselle, casserole, micro-ondes, caisse, hachoir, clochette, clients | carrelage | moyenne | porte vitrée |
| Le labo | machine, courant, ventilation | serveurs au poste, cuves qui bouillonnent | bips, ferraille, interrupteurs, bulles, clavier | béton | moyenne | presque rien |
| Le garage | courant, compresseur | radio sur l'établi | outils, ferraille, déclics ; pluie sur le toit de tôle | béton | grande, métallique | rideau à moitié ouvert |
| L'épicerie, la boucherie | frigo, néon | compresseur des frigos | bouteilles, caisse, hachoir, couteau | carrelage | moyenne | vitrine |
| La rame de métro | roulement, murmure | — | voyageurs qui parlent, vêtements, journal | métal | moyenne | aucun |
| Namek | vent sec, cigales | — | rafales, cailloux, feuillage | gravier | dehors | — |
| Les bureaux, la tour | ventilation | — | téléphones, clavier, ascenseur, pas dans le hall | carrelage | grande | fenêtres fermées |

Quand la régie coupe le courant chez soi, le frigo, le courant et la télé se taisent : il ne reste que l'horloge et
la pluie.

## Le corps et les gestes

- **Les pas** changent avec le sol. Courir les rend plus forts et plus espacés, et fait froisser les vêtements.
  S'arrêter net après une course, ou faire demi-tour en courant, fait crisser la semelle.
- **Le scooter** : une béquille et une selle quand on monte, puis le démarrage. Un ralenti qui hoquette. Un régime
  qui suit la vitesse, monte un peu plus quand on accélère et retombe quand on lâche. Les pneus chuintent sur la
  chaussée mouillée. Quand on le gare : le moteur qu'on coupe, le pot qui refroidit, la béquille, les clés.
- **Les portes** : la poignée, la porte qui s'ouvre, puis qui se referme derrière soi. Chaque lieu a la sienne
  (clochette de boutique, rideau de fer du garage, porte blindée du labo).
- **Les gestes** : chaque point d'action a son bruit. On ouvre le livre, on froisse les photos, on soulève les
  caisses, on fouille un tiroir, on récolte, on pose le sac sur le comptoir.

## Les voix

Pas de doublage : des syllabes sans langue, propres à chacun, qui suivent le texte. 26 voix sont calculées par
`designs/sons/voix.py` : une corde vocale, trois formants, des consonnes.

| Voix | Identité |
|---|---|
| Le héros | grave sans être lourd, un peu voilé, il pèse ses mots |
| Old Geto | très grave, rauque, posé : chaque syllabe prend son temps |
| Youngssef | clair, rapide, des syllabes courtes qui sautent |
| Tata Lili | chaude, chantante (la plus grande amplitude mélodique) |
| Omar | sec, coupé |
| Bidoche | grave et rond, il rit facilement |
| La femme à l'écharpe rouge | douce, beaucoup d'air |
| Le type du métro | bas, nasal, trop lent |
| Gero | précis, nasal, clinique, toujours le même débit (presque aucune mélodie) |
| Soul | calme, lié, sans aspérité |
| Le SDF | très bas, cassé, plus de souffle que de voix |
| Savon, Ticket | ados : rapide et frondeur pour l'un, nonchalant pour l'autre |
| La régie, les annonces du métro | une voix de téléphone, filtrée |
| Passants, clients, policiers | trois voix communes, à des hauteurs différentes |

Comment elles parlent :

- **Une syllabe par groupe de voyelles**, pas une par lettre, jamais plus vite que le débit propre au personnage.
- **Le texte respire** : un temps après une virgule, plus long après un point, plus long encore après des points
  de suspension. La voix s'arrête pendant ces temps.
- **La phrase a une mélodie** : elle part un peu haut et descend. Elle remonte sur la fin d'une question, monte et
  force sur une exclamation, s'éteint sur des points de suspension. Un mot en capitales est dit plus fort.
- **Le ton d'une réplique** (`dit geto:menace`) change la hauteur, l'écart, la force et le débit. Il peut s'annoncer
  par une réaction avant le texte : un soupir pour la tristesse ou la fatigue, un « hm » pour la gêne, une
  inspiration pour la peur, un « oh » pour la surprise, un grognement pour la colère.
- **Les interjections** (« Hm. », « Pff », « Haha », « Hein ? ») sont dites par une vraie réaction.
- **La voix vient du côté de son buste** : le héros à gauche, l'autre à droite.
- **Elle s'arrête net** quand on termine la réplique d'une touche ou qu'on ferme la boîte. La musique et le monde
  se rangent sous les voix tant que la boîte est ouverte.

## Les cinématiques

**L'intro** se joue en quatre temps.

- **Avant** : la ville sous la pluie, ouverte. Puis dedans : la pluie derrière la vitre, la télé qui parle toute
  seule (un présentateur sans langue, par un petit haut-parleur), l'horloge. Une montée grave, presque inaudible,
  commence sur ses yeux.
- **Pendant** : la télé crache, le présentateur bute sur un mot, un coup sourd, et il inspire d'un coup.
- **Réaction** : le monde s'étouffe d'un seul coup et un sifflement prend sa place. On n'entend plus que son cœur.
  La cigarette qui tombe est le seul son net.
- **Après** : la télé n'est plus que de la neige. Le monde revient lentement pendant que la caméra recule. Puis le
  silence, un souffle, et il se tourne vers nous : sa voix, la pièce, rien d'autre. Un déclic, et la musique de la
  rue démarre sur « Alors, pour commencer… ». Le carillon de la mission attend la fin de la scène.

**Les autres moments forts** :

- **Le métro** : les deux rames se croisent, il reconnaît Soul. Le monde passe en sourdine et son cœur bat une fois.
- **Le souvenir du banc** : la rue de jour, comme à travers du coton (sourdine), puis le présent revient.
- **Le sac de Geto** : un coup sourd, la sourdine, la menace dans la voix de Geto.
- **Le plafond** : les clés posées sur le comptoir, la musique se tait, il ne reste que Geto.
- **Le beeper vert** : la tête qui part (sourdine, montée), puis le choc ; le réveil par terre, le monde revient lentement.
- **La douane** : son cœur pendant qu'on ouvre le coffre.
- **L'étincelle** : le premier coup étouffe tout et laisse un sifflement, jusqu'au carton de fin d'acte.

Les cartons de chapitre s'ouvrent sur un accord grave et une cloche lointaine : une porte, pas une fanfare.

## La musique

| Contexte | Thème | Comportement |
|---|---|---|
| Menu | `mus_menu` | avec la pluie, de loin |
| La rue | `mus_rue` | joue un passage complet, se tait de 35 à 80 s, revient en fondu lent |
| La nuit (RS6) | `mus_nuit` | respire comme la rue |
| Chez soi, les boutiques | aucun | la pièce seule |
| Le labo | `mus_labo` | |
| Filature | `mus_filature` | mission furtive |
| Tension | `mus_tension` | métro, récolte chronométrée, l'étincelle |
| Récit, souvenir | `mus_recit`, `mus_souvenir` | imposés par les scènes |

Les changements se font en fondu enchaîné (2,2 s). Une musique qu'on n'entend plus est libérée de la mémoire.

## L'interface

- **Les menus** : un clic feutré pour se déplacer, une note douce pour valider, un retour plus grave.
- **Le téléphone** a sa propre famille de sons, vitrée. Il s'ouvre et se ferme avec le froissement de la poche.
  Chaque appli répond à sa façon : un tic pour naviguer, deux notes de marimba pour un message, un tampon et une
  note pour le loyer payé, une confirmation pour un achat, une erreur douce. Une recette qui tombe fait tinter la
  banque.
- **Les objectifs et les missions** ont de petites suites de notes ; la nouvelle mission joue deux notes.

## Les ressources

Tout est produit par des scripts, rien n'est posé à la main :

- `designs/sons/preparer.py` télécharge les enregistrements libres (CC0 surtout, CC-BY et CC-BY-SA cités), les
  découpe, les met en boucle, les filtre, les ramène à un niveau commun et les range par dossier.
- `designs/sons/bruitages.py` fabrique ce qu'aucun enregistrement ne donnait juste : la télé de l'intro,
  l'acouphène, la montée grave, les klaxons, le bus, le scooter qui passe, le ralenti, la friteuse, les serveurs,
  la radio, les voisins, les sons du téléphone, le carton de chapitre.
- `designs/sons/voix.py` calcule les 26 voix.

Les auteurs et les licences sont dans `ressources/sons/CREDITS.md` et dans les crédits du jeu.

## Les tests

`tests/jeu/MixageTest.java` fait tourner le mixeur pour de vrai (hors ligne) et mesure ce qu'il produit :

- pas de saturation, même avec des dizaines de sons ;
- chaque bus obéit à son réglage ;
- une voix coupée se tait en 20 ms ;
- un choc coupe les aigus du monde mais pas le cœur ;
- la vitre ne laisse passer que le grave de la pluie ;
- pas de doublon dans la même image ;
- la musique se fond et s'éteint ;
- une syllabe par groupe de voyelles, Geto plus lent que Youngssef, les réactions des émotions et des
  interjections, la voix qui s'arrête quand on coupe la parole, le texte qui respire à la ponctuation ;
- dans le vrai jeu : l'intro raconte le choc dans le bon ordre sonore, et le son reprend juste après le
  chargement d'une sauvegarde.

`SonsTest` et `ParcoursTest` vérifient que chaque son demandé existe et que l'ambiance suit le lieu.

## Écouter

`designs/sons/ecoute/` contient le jeu enregistré tel qu'il sonne, lieu par lieu : le vrai jeu tourne sans fenêtre,
le mixeur calcule le son image par image, et chaque scène est exportée en MP3. Le menu, l'intro, l'appartement, la
rue sous la pluie, un dialogue avec Youngssef, le scooter, le Maître Poulet, le labo, le garage, le métro,
l'épicerie, Namek, le téléphone.
