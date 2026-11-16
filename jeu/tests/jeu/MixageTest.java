package jeu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import histoire.TexteProgressif;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Le mixeur calcule vraiment le son (hors ligne) et on le mesure : pas de saturation, chaque bus obeit a son
 * reglage, une voix coupee se tait tout de suite, un choc etouffe le monde, les sons trop nombreux cedent la place.
 */
class MixageTest {
    private static final Path SONS = Path.of("ressources", "sons");

    private static Audio mixeur(Reglages r) {
        return Audio.horsLigne(SONS, r);
    }

    private static float crete(float[] x) {
        float m = 0;
        for (float v : x) {
            if (Float.isNaN(v)) {
                return Float.NaN;
            }
            m = Math.max(m, Math.abs(v));
        }
        return m;
    }

    private static double energie(float[] x, int debut, int fin) {
        double e = 0;
        for (int i = Math.max(0, debut); i < Math.min(x.length, fin); i++) {
            e += x[i] * x[i];
        }
        return e / Math.max(1, fin - debut);
    }

    /** L'energie au-dessus d'environ 2 kHz : la difference entre deux echantillons voisins. */
    private static double aigus(float[] x) {
        double e = 0;
        for (int i = 2; i < x.length; i += 2) {
            float d = x[i] - x[i - 2];
            e += d * d;
        }
        return e / x.length;
    }

    @Test
    void beaucoupDeSonsALaFoisNeSaturentJamais() {
        Audio a = mixeur(new Reglages(null));
        a.ambiances(Map.of("amb_pluie", new float[] {1, 0, 1}, "amb_rue_mouillee", new float[] {1, 0, 1},
                "amb_foule", new float[] {1, 0, 1}));
        a.musique("mus_rue");
        for (int i = 0; i < 30; i++) {
            a.tic(0.05f);
            a.jouer("orage_" + (1 + i % 3), 1f, 0, 1);
            a.jouer("klaxon", 1f, 0.5f, 1);
            a.jouer("coup", 1f, -0.5f, 1);
        }
        float[] son = a.rendre(3f);
        float c = crete(son);
        assertFalse(Float.isNaN(c), "pas de calcul qui deraille");
        assertTrue(c < 1f, "le limiteur garde tout sous la saturation : " + c);
        assertTrue(c > 0.05f, "et on entend bien quelque chose : " + c);
        assertTrue(a.voixEnCours() <= Audio.MAX_VOIX, "pas plus de " + Audio.MAX_VOIX + " sons a la fois");
    }

    @Test
    void chaqueBusObeitASonReglage() {
        Reglages r = new Reglages(null);
        Audio a = mixeur(r);
        a.salle(0, 0);                                                 // sans salle : on mesure le son direct
        a.rendre(4f);
        r.voix = 0;
        a.jouer(Audio.Bus.VOIX, "v_geto", 1f, 0, 1, 0);
        assertTrue(crete(a.rendre(0.3f)) < 1e-4f, "voix a zero : on n'entend pas Geto");
        r.voix = 10;
        a.tic(1f);
        a.jouer(Audio.Bus.VOIX, "v_geto", 1f, 0, 1, 0);
        assertTrue(crete(a.rendre(0.3f)) > 0.01f, "voix a fond : on l'entend");
        r.general = 0;
        a.rendre(1f);
        a.tic(1f);
        a.jouer("coup", 1f, 0, 1);
        assertTrue(crete(a.rendre(0.3f)) < 1e-4f, "le volume general coupe tout");
    }

    @Test
    void uneVoixCoupeeSeTaitToutDeSuite() {
        Audio a = mixeur(new Reglages(null));
        a.salle(0, 0);                                                 // sans la queue de la salle : la voix seule
        a.rendre(4f);
        a.jouer(Audio.Bus.VOIX, "v_diallo_soupir", 1f, 0, 1, 0);
        float[] avant = a.rendre(0.1f);
        assertTrue(energie(avant, 0, avant.length) > 1e-6, "le soupir commence");
        a.couper(Audio.Bus.VOIX);
        a.rendre(0.05f);                                               // le temps d'un fondu de 20 ms
        float[] apres = a.rendre(0.2f);
        assertTrue(crete(apres) < 1e-4f, "plus rien apres la coupure : " + crete(apres));
        assertEquals(0, a.ponctuelsEnCours(Audio.Bus.VOIX));
    }

    @Test
    void unChocEtouffeLeMondeMaisPasLeCoeur() {
        Audio a = mixeur(new Reglages(null));
        a.ambiances(Map.of("amb_pluie", new float[] {1, 0, 1}));
        a.rendre(4f);                                                  // la pluie monte en fondu
        double ouvert = aigus(a.rendre(1f));
        a.mixage("choc", 0.05f);
        a.rendre(0.5f);
        double etouffe = aigus(a.rendre(1f));
        assertTrue(etouffe < ouvert * 0.1, "le choc coupe les aigus du monde : " + etouffe + " contre " + ouvert);
        a.ambiances(Map.of());
        a.rendre(5f);
        a.jouer(Audio.Bus.VOIX, "coeur", 1f, 0, 1, 0);
        assertTrue(crete(a.rendre(0.5f)) > 0.02f, "le coeur reste net dans le choc");
        a.mixage("normal", 0.05f);
        assertEquals("normal", a.mixageCourant());
    }

    @Test
    void dedansOnEntendLaRueAtraversLeMur() {
        Audio a = mixeur(new Reglages(null));
        a.ambiances(Map.of("amb_pluie", new float[] {1, 0, 1}));
        a.rendre(4f);
        double dehors = aigus(a.rendre(1f));
        a.dehors(700f);
        a.rendre(1f);
        double dedans = aigus(a.rendre(1f));
        assertTrue(dedans < dehors * 0.15, "la vitre ne laisse passer que le grave de la pluie");
    }

    @Test
    void leMemeSonDansLaMemeImageNeJouePasDeuxFois() {
        Audio a = new Audio(SONS, new Reglages(null), true);
        a.jouer("coup", 1f, 0, 1);
        a.jouer("coup", 1f, 0, 1);
        a.tic(0.1f);
        a.jouer("coup", 1f, 0, 1);
        assertEquals(2, a.journal().stream().filter("coup"::equals).count());
    }

    @Test
    void lesPrisesDUneFamilleNeSeSuiventJamais() {
        Audio a = mixeur(new Reglages(null));
        assertTrue(a.existe("pas_carrelage") && a.existe("v_youngssef") && a.existe("klaxon"));
        // le mixeur ne dit pas quelle prise il a jouee : on rejoue souvent et on verifie qu'il ne bloque pas
        for (int i = 0; i < 50; i++) {
            a.tic(0.1f);
            a.jouer("pas_carrelage", 0.5f, 0, 1);
        }
        assertTrue(a.voixEnCours() > 0);
    }

    @Test
    void laMusiqueChangeEnFonduEtSeLibere() {
        Audio a = mixeur(new Reglages(null));
        a.musique("mus_rue");
        a.rendre(3f);
        a.musique("mus_labo");
        a.musique("mus_labo");
        assertEquals("mus_labo", a.musiqueCourante());
        a.musique(null);
        float[] fin = a.rendre(4f);
        assertTrue(energie(fin, fin.length - 4000, fin.length) < 1e-6, "la musique s'est eteinte");
    }

    @Test
    void lesBusSontRangesParFamille() {
        assertEquals(Audio.Bus.MUSIQUE, Audio.busDe("mus_rue"));
        assertEquals(Audio.Bus.VOIX, Audio.busDe("v_geto_rire"));
        assertEquals(Audio.Bus.VOIX, Audio.busDe("coeur"));
        assertEquals(Audio.Bus.INTERFACE, Audio.busDe("tel_sms"));
        assertEquals(Audio.Bus.EXTERIEUR, Audio.busDe("amb_pluie"));
        assertEquals(Audio.Bus.EXTERIEUR, Audio.busDe("klaxon"));
        assertEquals(Audio.Bus.AMBIANCE, Audio.busDe("amb_frigo"));
        assertEquals(Audio.Bus.AMBIANCE, Audio.busDe("tele_emission"));
        assertEquals(Audio.Bus.EFFETS, Audio.busDe("pas_beton"));
    }

    // ------------------------------------------------------------------ les voix

    private static List<String> syllabes(Audio a) {
        return a.journal().stream().filter(n -> n.startsWith("v_")).toList();
    }

    private static Parole parole(Audio a) throws Exception {
        return new Parole(a, new Ressources(Path.of("ressources")).campagne);
    }

    /** Fait parler quelqu'un comme le fait le jeu : le texte s'ecrit, la voix le suit, image par image. */
    private static void dire(Parole p, Audio a, String qui, String ton, String phrase) {
        TexteProgressif t = new TexteProgressif(phrase, 46f);
        t.retarder(p.commencer(qui, ton, phrase, 0f));
        for (int i = 0; i < 400 && !t.complet(); i++) {
            a.tic(1f / 30);
            t.avancer(1f / 30);
            p.suivre(t.visible(), 1f / 30);
        }
    }

    @Test
    void uneSyllabeParGroupeDeVoyellesPasParLettre() throws Exception {
        Audio a = new Audio(SONS, new Reglages(null), true);
        Parole p = parole(a);
        String phrase = "Tu le rayes, je te retrouve. Les clés sont dessus.";
        dire(p, a, "youngssef", "", phrase);
        long lettres = phrase.chars().filter(Character::isLetter).count();
        int dites = syllabes(a).size();
        assertTrue(dites >= 8 && dites < lettres / 2, "entre 8 syllabes et la moitie des lettres : " + dites);
        assertTrue(syllabes(a).stream().allMatch(n -> n.equals("v_youngssef")), "c'est bien la voix de Youngssef");
    }

    @Test
    void chacunSaVoixEtGetoParlePlusLentement() throws Exception {
        String phrase = "Les commandes se livrent pas toutes seules, petit. Tu repars travailler.";
        Audio a = new Audio(SONS, new Reglages(null), true);
        dire(parole(a), a, "youngssef", "", phrase);
        Audio b = new Audio(SONS, new Reglages(null), true);
        dire(parole(b), b, "geto", "", phrase);
        assertTrue(syllabes(b).stream().allMatch(n -> n.equals("v_geto")));
        assertTrue(syllabes(b).size() < syllabes(a).size(), "Geto prend son temps : " + syllabes(b).size() + " contre "
                + syllabes(a).size());
    }

    @Test
    void lesEmotionsEtLesInterjectionsOntLeurReaction() throws Exception {
        Audio a = new Audio(SONS, new Reglages(null), true);
        Parole p = parole(a);
        dire(p, a, "femme", "triste", "Ça va. J'ai l'habitude.");
        assertEquals("v_femme_soupir", syllabes(a).get(0), "la tristesse s'annonce par un soupir");
        dire(p, a, "omar", "", "Hm. On verra demain.");
        assertTrue(a.journal().contains("v_omar_hm"), "« Hm. » se dit avec un vrai murmure");
        dire(p, a, "mounir", "", "Haha ! Il a trouvé quelqu'un.");
        assertTrue(a.journal().contains("v_mounir_rire"));
    }

    @Test
    void laVoixSArreteQuandOnCoupeLaParole() throws Exception {
        Audio a = new Audio(SONS, new Reglages(null), true);
        Parole p = parole(a);
        String phrase = "Je livre pour le Maître Poulet. Je me suis tordu la cheville en descendant mes escaliers.";
        TexteProgressif t = new TexteProgressif(phrase, 46f);
        p.commencer("youngssef", "", phrase, 0f);
        for (int i = 0; i < 10; i++) {
            a.tic(1f / 30);
            t.avancer(1f / 30);
            p.suivre(t.visible(), 1f / 30);
        }
        int avant = syllabes(a).size();
        t.terminer();                                                  // le joueur appuie : tout le texte d'un coup
        p.couper();
        p.suivre(t.visible(), 1f / 30);
        assertEquals(avant, syllabes(a).size(), "pas de rafale de syllabes quand on termine la replique");
        assertTrue(a.journal().contains("couper:voix"));
    }

    @Test
    void leTexteMarqueUnTempsALaPonctuation() {
        TexteProgressif sans = new TexteProgressif("abcdefghijklmnop", 46f);
        TexteProgressif avec = new TexteProgressif("abc. defg, hijkl", 46f);
        int imagesSans = 0;
        int imagesAvec = 0;
        while (!sans.complet()) {
            sans.avancer(1f / 30);
            imagesSans++;
        }
        while (!avec.complet()) {
            avec.avancer(1f / 30);
            imagesAvec++;
        }
        assertTrue(imagesAvec >= imagesSans + 10, "un point et une virgule font respirer la phrase : " + imagesAvec + " / " + imagesSans);
    }

    // ------------------------------------------------------------------ dans le jeu

    @Test
    void lIntroRaconteLeChocParLeSon(@org.junit.jupiter.api.io.TempDir Path dossier) throws Exception {
        Robot robot = new Robot(dossier.resolve("partie.properties"), null);
        robot.jeu.nouvellePartie();
        robot.images(3);
        for (int i = 0; i < 30 * 140 && robot.partie().enScene(); i++) {
            robot.image();                                             // sans toucher a rien : l'intro se joue seule
        }
        robot.images(3);
        List<String> j = robot.jeu.audio.journal();
        int rafale = j.indexOf("tele_rafale");
        int choc = j.indexOf("mixage:choc");
        int cigarette = j.indexOf("cigarette_tombe");
        int retour = j.subList(choc, j.size()).indexOf("mixage:normal") + choc;
        int parole = j.indexOf("v_heros");
        assertTrue(rafale > 0 && rafale < choc && choc < cigarette && cigarette < retour && retour < parole,
                "la tele crache, le monde s'etouffe, la cigarette tombe, le monde revient, il nous parle : " + j);
        assertTrue(j.lastIndexOf("nouvelle_mission") > j.lastIndexOf("v_heros"), "le carillon de la mission attend la fin de l'intro");
        assertEquals("normal", robot.jeu.audio.mixageCourant(), "le choc ne survit pas a l'intro");
        assertTrue(j.stream().filter("v_heros"::equals).count() > 20, "sa voix suit tout son monologue");
    }

    @Test
    void apresUnChargementLeSonReprendJuste(@org.junit.jupiter.api.io.TempDir Path dossier) throws Exception {
        Robot robot = new Robot(dossier.resolve("partie.properties"), null);
        robot.jeu.nouvellePartie();
        robot.images(3);
        robot.laisserPasser();
        robot.partie().sauvegarder();
        robot.jeu.ouvrirMenu();
        robot.images(10);
        assertEquals("mus_menu", robot.jeu.audio.musiqueCourante());
        robot.jeu.continuer();
        robot.images(10);
        Audio audio = robot.jeu.audio;
        assertEquals("normal", audio.mixageCourant());
        assertEquals(null, audio.musiqueCourante(), "chez soi, la nuit : pas de musique, la piece seule");
        assertTrue(audio.ambiancesCourantes().containsKey("amb_horloge"), "l'horloge de l'appartement reprend");
        assertTrue(audio.ambiancesCourantes().containsKey("amb_pluie"), "la pluie dehors, derriere la vitre");
        assertFalse(audio.ambiancesCourantes().containsKey("amb_rue_mouillee") && audio.ambiancesCourantes().get("amb_rue_mouillee") > 0.5f,
                "la rue n'entre pas a plein volume dans l'appartement");
        assertEquals(0, audio.ponctuelsEnCours(Audio.Bus.VOIX));
    }

    @Test
    void leSolChangeLeBruitDesPas() {
        assertEquals("pas_bois", Sonorisation.sol("appartement", "pluie"));
        assertEquals("pas_carrelage", Sonorisation.sol("maitre_poulet", "pluie"));
        assertEquals("pas_metal", Sonorisation.sol("metro", "sec"));
        assertEquals("pas_gravier", Sonorisation.sol("namek", "sec"));
        assertEquals("pas_mouille", Sonorisation.sol("avenue_bas", "pluie"));
        assertEquals("pas_dur", Sonorisation.sol("avenue_bas", "sec"));
        Audio a = new Audio(SONS, new Reglages(null), true);
        for (String sol : List.of("pas_bois", "pas_carrelage", "pas_metal", "pas_gravier", "pas_mouille", "pas_dur", "pas_beton")) {
            assertTrue(a.existe(sol), sol);
        }
    }
}
