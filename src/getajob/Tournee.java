package getajob;

/**
 * Les règles de la nuit. Le Niveau dit OÙ sont les choses ; la Tournee dit CE QUI COMPTE :
 *   - l'heure tourne, de 23 h à 6 h (2 minutes de jeu par seconde réelle) ;
 *   - on prend une commande au Maître Poulet, on la livre au client suivant : 40 euros ;
 *   - un racketteur qui attrape le héros lui coûte une vie et la commande ;
 *   - après trois livraisons, il faut rentrer chez soi pour payer le loyer (120 euros).
 * Victoire : rentré avec les 120 euros avant 6 h. Défaite : plus de vies, ou 6 h sonne.
 */
public class Tournee {
    public enum Etape { CHERCHER_LA_COMMANDE, LIVRER, RENTRER }
    public enum Issue { EN_COURS, VICTOIRE, PLUS_DE_VIES, TROP_TARD }

    public static final int LOYER = 120;
    public static final int PRIX_D_UNE_LIVRAISON = 40;
    public static final double DEBUT = 23 * 60;            // en minutes : 23 h 00
    public static final double FIN = 30 * 60;              // 6 h 00, le lendemain
    public static final double MINUTES_PAR_SECONDE = 2.0;

    private final Niveau niveau;
    private double minutes = DEBUT;
    private int livraisons;
    private String message = "";
    private double dureeDuMessage;

    public Tournee(Niveau niveau) {
        this.niveau = niveau;
        annoncer("Youngssef : « Le Maître Poulet, c'est juste là, à droite. Dis que tu viens pour moi. »");
    }

    public void mettreAJour(double dt) {
        minutes += dt * MINUTES_PAR_SECONDE;
        dureeDuMessage -= dt;
        Joueur joueur = niveau.joueur();
        verifierLesRacketteurs(joueur);
        if (etape() == Etape.CHERCHER_LA_COMMANDE && joueur.contact().intersects(Niveau.COMPTOIR)) {
            joueur.prendreCommande();
            Ressources.jouerSon("objectif");
            annoncer("Old Geto : « C'est pour " + clientActuel().nom() + ". Et tu traînes pas. »");
        } else if (etape() == Etape.LIVRER && joueur.touche(clientActuel())) {
            livrer(joueur, clientActuel());
        }
    }

    /** Un contact avec un racketteur qui n'est pas étourdi, et le héros n'est pas en train de clignoter : il est attrapé. */
    private void verifierLesRacketteurs(Joueur joueur) {
        for (Ennemi ennemi : niveau.ennemis()) {
            if (ennemi.peutAttraper() && joueur.peutEtreTouche() && ennemi.touche(joueur)) {
                boolean avaitLeSac = joueur.porteUneCommande();
                joueur.etreTouche(ennemi.x() > joueur.x(), niveau);
                ennemi.apresAvoirAttrape();
                annoncer(avaitLeSac ? "Ils t'ont pris le sac. Retourne au Maître Poulet." : "Aïe. Ne reste pas là.");
            }
        }
    }

    private void livrer(Joueur joueur, Client client) {
        joueur.livrer(PRIX_D_UNE_LIVRAISON);
        client.servir();
        livraisons++;
        Ressources.jouerSon("tel_argent");
        annoncer(Character.toUpperCase(client.nom().charAt(0)) + client.nom().substring(1) + " : « " + client.phrase() + " »");
        // la nouvelle a fait le tour : un racketteur de plus se poste plus loin dans l'avenue
        if (livraisons == 1) {
            niveau.ajouterEnnemi(2850, 3300, 230);
        } else if (livraisons == 2) {
            niveau.ajouterEnnemi(1500, 2100, 220);
        }
    }

    /** Où en est la nuit. */
    public Etape etape() {
        if (livraisons >= niveau.clients().size()) {
            return Etape.RENTRER;
        }
        return niveau.joueur().porteUneCommande() ? Etape.LIVRER : Etape.CHERCHER_LA_COMMANDE;
    }

    /** La partie continue, ou elle est finie : gagnée ou perdue. */
    public Issue issue() {
        Joueur joueur = niveau.joueur();
        if (joueur.vies() <= 0) {
            return Issue.PLUS_DE_VIES;
        }
        if (etape() == Etape.RENTRER && joueur.argent() >= LOYER && joueur.contact().intersects(Niveau.CHEZ_TOI)) {
            return Issue.VICTOIRE;
        }
        if (minutes >= FIN) {
            return Issue.TROP_TARD;
        }
        return Issue.EN_COURS;
    }

    /** Le client qui attend la prochaine commande. */
    public Client clientActuel() {
        return niveau.clients().get(Math.min(livraisons, niveau.clients().size() - 1));
    }

    /** L'abscisse de l'endroit où aller maintenant : pour la flèche qui guide le joueur. */
    public double destination() {
        return switch (etape()) {
            case CHERCHER_LA_COMMANDE -> Niveau.COMPTOIR.getCenterX();
            case LIVRER -> clientActuel().x();
            case RENTRER -> Niveau.CHEZ_TOI.getCenterX();
        };
    }

    /** La phrase de l'objectif, affichée en haut de l'écran. */
    public String objectif() {
        return switch (etape()) {
            case CHERCHER_LA_COMMANDE -> "PRENDS UNE COMMANDE AU MAÎTRE POULET";
            case LIVRER -> "LIVRE " + clientActuel().nom().toUpperCase();
            case RENTRER -> "RENTRE CHEZ TOI PAYER LA RÉGIE";
        };
    }

    /** L'heure, « 23:40 ». */
    public String heure() {
        int total = (int) minutes % (24 * 60);
        return String.format("%02d:%02d", total / 60, total % 60);
    }

    private void annoncer(String texte) {
        message = texte;
        dureeDuMessage = 4.5;
    }

    /** Le dernier message, tant qu'il est affiché ; une chaîne vide sinon. */
    public String message() {
        return dureeDuMessage > 0 ? message : "";
    }

    public int livraisons() {
        return livraisons;
    }

    /** Pour les tests : avancer l'horloge sans attendre. */
    void avancerLHorloge(double minutesDePlus) {
        minutes += minutesDePlus;
    }
}
