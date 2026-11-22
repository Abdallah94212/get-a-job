# GET A JOB — conception (UML)

Les diagrammes sont écrits en [Mermaid](https://mermaid.js.org/) : GitHub les affiche directement.
Ils décrivent le code de `src/getajob/`, réparti en quatre packages : `moteur`, `entites`, `monde` et `ecrans`.

1. [Diagramme de classes : les entités](#1-diagramme-de-classes--les-entités)
2. [Diagramme de classes : le jeu, les écrans et les règles](#2-diagramme-de-classes--le-jeu-les-écrans-et-les-règles)
3. [Diagrammes d'états](#3-diagrammes-détats) (le jeu, un racketteur, la tournée)
4. [Diagramme de séquence : un tour de boucle](#4-diagramme-de-séquence--un-tour-de-boucle)
5. [Les design patterns utilisés](#5-les-design-patterns-utilisés)

---

## 1. Diagramme de classes : les entités

Tout ce qui vit dans l'avenue est une `Entite`. Tout ce qui marche est un `Personnage`.
Les trois sous-classes ne diffèrent que par **leur façon de décider** (`mettreAJour`) et **de se montrer** (`image`, `dessiner`).

```mermaid
classDiagram
    direction TB

    class Entite {
        <<abstract>>
        #double x
        #double y
        +mettreAJour(double dt, Niveau niveau) void*
        #image() BufferedImage*
        +contact() Rectangle
        +touche(Entite autre) boolean
        +dessiner(Graphics2D g, Camera camera) void
        #ecranX(BufferedImage img, Camera camera) int
        #ecranY(BufferedImage img) int
        +placer(double x, double y) void
    }

    class Personnage {
        <<abstract>>
        -Animation marche
        #boolean versDroite
        #deplacer(double dx, double dy, Niveau niveau) void
        #animer(double dt, boolean bouge) void
        #image() BufferedImage
        +dessiner(Graphics2D g, Camera camera) void
    }

    class Joueur {
        +double VITESSE$
        +int VIES_AU_DEPART$
        -int vies
        -int argent
        -boolean commande
        -double pose
        -double recharge
        -double intouchable
        +mettreAJour(double dt, Niveau niveau) void
        -frapper(Niveau niveau) void
        +zoneDeCoup() Rectangle
        +etreTouche(boolean coupVenantDeDroite, Niveau niveau) void
        +prendreCommande() void
        +livrer(int prix) void
        #image() BufferedImage
        +dessiner(Graphics2D g, Camera camera) void
    }

    class Ennemi {
        -Etat etat
        -double debutRonde
        -double finRonde
        -double etourdi
        +mettreAJour(double dt, Niveau niveau) void
        -faireSaRonde(double dt, Niveau niveau) void
        -poursuivre(Joueur joueur, double dt, Niveau niveau) void
        +recevoirCoup(boolean venantDeLaGauche, Niveau niveau) void
        +apresAvoirAttrape() void
        +peutAttraper() boolean
        +dessiner(Graphics2D g, Camera camera) void
    }

    class Etat {
        <<enumeration>>
        PATROUILLE
        POURSUITE
        ETOURDI
    }

    class Client {
        -String nom
        -String phrase
        -boolean servi
        +mettreAJour(double dt, Niveau niveau) void
        +servir() void
    }

    class Animation {
        -BufferedImage[] images
        -double dureeParImage
        +avancer(double dt) void
        +imageCourante() BufferedImage
    }

    Entite <|-- Personnage
    Personnage <|-- Joueur
    Personnage <|-- Client
    Personnage <|-- Ennemi
    Personnage *-- "1" Animation : marche
    Ennemi --> Etat : état courant
    Joueur --> Clavier : lit les touches
```

**Points clés :**
- `deplacer` (collisions et glissement) n'est écrit **qu'une fois**, dans `Personnage`. Le joueur, les racketteurs et les clients s'en servent tous.
- `mettreAJour` est **abstraite** : chaque sous-classe doit dire comment elle se comporte.
  - Le `Joueur` obéit au clavier.
  - L'`Ennemi` suit sa machine à états.
  - Le `Client` se tourne vers le héros.
- `Joueur` et `Ennemi` **redéfinissent** `dessiner` en appelant d'abord `super.dessiner`, puis ajoutent leur détail : le sac pour le joueur, les étoiles pour le racketteur étourdi.

---

## 2. Diagramme de classes : le jeu, les écrans et les règles

`Jeu` ne connaît qu'un `Ecran` abstrait : c'est l'écran courant qui décide quoi faire et quoi dessiner.
`EcranPartie` assemble le monde (`Niveau`), les règles (`Tournee`) et la `Camera`.

```mermaid
classDiagram
    direction TB

    class Main {
        +main(String[] args)$
    }

    class Jeu {
        +int LARGEUR$
        +int HAUTEUR$
        +int ZOOM$
        -Timer boucle
        -Ecran ecran
        -long dernierTour
        +demarrer() void
        -tour() void
        +changerEcran(Ecran nouveau) void
        #paintComponent(Graphics g) void
    }

    class Clavier {
        -Set~Integer~ tenues
        -Set~Integer~ justeAppuyees
        +keyPressed(KeyEvent e) void
        +keyReleased(KeyEvent e) void
        +tenue(int... touches) boolean
        +vientDAppuyer(int... touches) boolean
        +directionX() int
        +directionY() int
        +finDuTour() void
    }

    class Ecran {
        <<abstract>>
        #Jeu jeu
        +mettreAJour(double dt, Clavier clavier) void*
        +dessiner(Graphics2D g) void*
        #ecrireAuCentre(Graphics2D g, String texte, int y, float taille, Color couleur)$
    }

    class EcranTitre {
        -double defilement
        +mettreAJour(double dt, Clavier clavier) void
        +dessiner(Graphics2D g) void
    }

    class EcranRecit {
        -String titre
        -List~String~ lignes
        -Ecran suivant
        +mettreAJour(double dt, Clavier clavier) void
        +dessiner(Graphics2D g) void
    }

    class EcranPartie {
        -boolean enPause
        +mettreAJour(double dt, Clavier clavier) void
        +dessiner(Graphics2D g) void
        -finir(Issue issue) void
    }

    class Niveau {
        +int LARGEUR$
        +Rectangle COMPTOIR$
        +Rectangle CHEZ_TOI$
        -List~Rectangle~ obstacles
        +bloque(Rectangle pieds) boolean
        +mettreAJour(double dt) void
        +entites() List~Entite~
        +entitesDansLOrdreDeDessin() List~Entite~
        +ajouterEnnemi(double debut, double fin, double y) void
    }

    class Tournee {
        +int LOYER$
        +int PRIX_D_UNE_LIVRAISON$
        -double minutes
        -int livraisons
        +mettreAJour(double dt) void
        +etape() Etape
        +issue() Issue
        +clientActuel() Client
        +objectif() String
        +heure() String
    }

    class Etape {
        <<enumeration>>
        CHERCHER_LA_COMMANDE
        LIVRER
        RENTRER
    }

    class Issue {
        <<enumeration>>
        EN_COURS
        VICTOIRE
        PLUS_DE_VIES
        TROP_TARD
    }

    class Camera {
        -double x
        +suivre(Entite cible, int largeurDuMonde) void
        +x() int
    }

    class Ressources {
        <<utility>>
        -Map~String,BufferedImage~ IMAGES$
        +image(String nom) BufferedImage$
        +police(float taille) Font$
        +obstacles() List~Rectangle~$
        +jouerSon(String nom) void$
    }

    JPanel <|-- Jeu
    KeyAdapter <|-- Clavier
    Ecran <|-- EcranTitre
    Ecran <|-- EcranRecit
    Ecran <|-- EcranPartie

    Main ..> Jeu : crée
    Jeu *-- "1" Clavier
    Jeu o-- "1" Ecran : écran courant
    EcranRecit o-- "1" Ecran : suivant
    EcranPartie *-- "1" Niveau
    EcranPartie *-- "1" Tournee
    EcranPartie *-- "1" Camera
    Tournee --> Niveau : applique les règles
    Tournee ..> Etape
    Tournee ..> Issue
    Niveau *-- "1" Joueur
    Niveau *-- "*" Ennemi
    Niveau *-- "3" Client
    Niveau ..> Ressources : décor, obstacles
```

**Points clés :**
- **Séparation des responsabilités** :
  - le `Niveau` sait **où** sont les choses et si un endroit est libre ;
  - la `Tournee` sait **ce qui compte** : l'étape, l'argent, l'heure, la victoire ;
  - l'`EcranPartie` **affiche**.
- Les règles se testent donc **sans fenêtre** : c'est ce que font les tests JUnit.
- `EcranRecit` contient l'écran `suivant`. C'est ce qui permet d'enchaîner intro → partie, ou fin → titre, sans que `Jeu` le sache.

---

## 3. Diagrammes d'états

### Le jeu (les écrans)

```mermaid
stateDiagram-v2
    [*] --> Titre
    Titre --> Intro : Entrée
    Titre --> [*] : Échap (quitter)
    Intro --> Partie : Entrée

    state "Partie (EcranPartie)" as Partie {
        [*] --> EnJeu
        EnJeu --> Pause : Échap
        Pause --> EnJeu : Échap
    }

    Partie --> Titre : Q pendant la pause (abandonner)
    Partie --> FinVictoire : issue() = VICTOIRE
    Partie --> FinPlusDeVies : issue() = PLUS_DE_VIES
    Partie --> FinTropTard : issue() = TROP_TARD
    FinVictoire --> Titre : Entrée
    FinPlusDeVies --> Titre : Entrée
    FinTropTard --> Titre : Entrée

    Titre : EcranTitre
    Intro : EcranRecit (la nuit commence)
    FinVictoire : EcranRecit (le loyer est payé)
    FinPlusDeVies : EcranRecit (sur le trottoir)
    FinTropTard : EcranRecit (le compteur saute)
```

### Un racketteur (`Ennemi.Etat`)

```mermaid
stateDiagram-v2
    state "PATROUILLE : va et vient sur sa ronde, 40 px/s" as PATROUILLE
    state "POURSUITE : fonce sur le héros, 68 px/s (moins vite que lui)" as POURSUITE
    state "ETOURDI : ne bouge plus, n'attrape personne" as ETOURDI
    [*] --> PATROUILLE
    PATROUILLE --> POURSUITE : voit le héros (moins de 110 px, même trottoir)
    POURSUITE --> PATROUILLE : le perd de vue
    PATROUILLE --> ETOURDI : coup de poing ou héros attrapé
    POURSUITE --> ETOURDI : coup de poing ou héros attrapé
    ETOURDI --> PATROUILLE : 2 s après un coup, 1 s après avoir attrapé

```

### La tournée (`Tournee.Etape`)

```mermaid
stateDiagram-v2
    [*] --> CHERCHER_LA_COMMANDE
    CHERCHER_LA_COMMANDE --> LIVRER : touche le comptoir du Maître Poulet
    LIVRER --> CHERCHER_LA_COMMANDE : client livré (+40 €), moins de 3 livraisons
    LIVRER --> CHERCHER_LA_COMMANDE : attrapé par un racketteur (sac perdu, -1 vie)
    LIVRER --> RENTRER : 3e client livré
    RENTRER --> [*] : touche la porte de chez soi avec 120 € (VICTOIRE)

    note right of CHERCHER_LA_COMMANDE
        À tout moment, la partie est perdue
        si les vies tombent à 0 (PLUS_DE_VIES)
        ou si l'horloge atteint 6 h (TROP_TARD).
    end note
```

---

## 4. Diagramme de séquence : un tour de boucle

Environ 60 fois par seconde, pendant une partie.

```mermaid
sequenceDiagram
    participant T as Timer Swing (16 ms)
    participant J as Jeu
    participant E as EcranPartie
    participant N as Niveau
    participant X as chaque Entite
    participant R as Tournee
    participant C as Camera
    participant K as Clavier

    T->>J: tour()
    J->>J: dt = temps écoulé (au plus 0,05 s)
    J->>E: mettreAJour(dt, clavier)
    E->>N: mettreAJour(dt)
    loop pour chaque entité (polymorphisme)
        N->>X: mettreAJour(dt, niveau)
        Note over X: Joueur lit le clavier,<br/>Ennemi suit sa machine à états,<br/>Client se tourne vers le héros
    end
    E->>R: mettreAJour(dt)
    Note over R: horloge, racketteurs,<br/>comptoir, livraison
    E->>C: suivre(joueur, LARGEUR)
    E->>R: issue()
    alt la nuit est finie
        E->>J: changerEcran(EcranRecit de fin)
    end
    J->>K: finDuTour()
    J->>J: repaint()

    Note over J: Swing appelle ensuite paintComponent
    J->>E: dessiner(g)
    E->>N: entitesDansLOrdreDeDessin()
    loop de la plus haute à la plus basse (profondeur)
        E->>X: dessiner(g, camera)
    end
    J->>J: agrandit l'image 480×270 ×3
```

---

## 5. Les design patterns utilisés

| Pattern | Où | Pourquoi |
|---|---|---|
| **Game Loop** | `Jeu.tour()` | Une boucle à pas de temps mesuré (`dt`) : le jeu va à la même vitesse sur toutes les machines. |
| **Update Method** | `Entite.mettreAJour`, appelée par `Niveau.mettreAJour` | Chaque entité sait se mettre à jour elle-même. Le niveau les parcourt sans savoir qui est qui. |
| **State** | `Ecran` et ses trois sous-classes, `Jeu.changerEcran` | `Jeu` délègue à l'écran courant. Ajouter un écran, c'est ajouter une classe, sans toucher à `Jeu`. |
| **Machine à états (enum)** | `Ennemi.Etat`, `Tournee.Etape` | La version simple du State : un `enum` et un `switch`, suffisante tant que chaque état reste court. |
| **Template Method** | `Entite.dessiner` appelle `image()`, abstraite | Le dessin est le même pour tous. Seule l'image change : la marche, le coup de poing, le choc. |
| **Observer** | `Clavier extends KeyAdapter`, branché par `addKeyListener` | Swing prévient le `Clavier` à chaque touche. Le jeu lit ensuite l'état du clavier une fois par tour. |
| **Cache (proche du Flyweight)** | `Ressources.image` | Chaque image n'est chargée qu'une fois et partagée par toutes les entités qui l'utilisent. |
