package histoire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** La sante, le souffle et l'argent du heros : les regles, sans le jeu autour. */
class EtatTest {
    private final Etat etat = new Etat();

    @Test
    void courirVideLeSouffleEtIlFautLeReprendreAvantDeRecourir() {
        for (int i = 0; i < 200 && !etat.essouffle(); i++) {
            assertTrue(etat.respirer(true, true, false, 0.1f), "tant qu'il reste du souffle, on court");
        }
        assertTrue(etat.essouffle());
        assertEquals(0f, etat.endurance());
        assertFalse(etat.respirer(true, true, false, 0.1f), "essouffle, la touche de course ne fait plus courir");
        etat.respirer(false, false, false, 1f);
        assertTrue(etat.essouffle(), "une seconde ne suffit pas");
        etat.respirer(false, false, false, 1f);
        assertFalse(etat.essouffle(), "le souffle revenu, on peut recourir");
    }

    @Test
    void onRecupereMieuxALArretQuEnMarchant() {
        Etat autre = new Etat();
        for (Etat e : new Etat[] {etat, autre}) {
            while (!e.essouffle()) {
                e.respirer(true, true, false, 0.1f);
            }
        }
        etat.respirer(false, false, false, 1f);
        autre.respirer(true, false, false, 1f);
        assertTrue(etat.endurance() > autre.endurance());
    }

    @Test
    void porterFatigueSansJamaisEssouffler() {
        etat.respirer(true, false, true, 5f);
        assertTrue(etat.endurance() < 100f);
        assertFalse(etat.essouffle());
    }

    @Test
    void blesseOnAMoinsDeSouffle() {
        etat.blesser(70f, 0f);
        assertTrue(etat.blesse());
        assertEquals(60f, etat.enduranceMax());
        assertEquals(60f, etat.endurance(), "le souffle est ramene a ce que le corps peut encore donner");
        etat.soigner(45f);
        assertFalse(etat.blesse());
        assertEquals(100f, etat.enduranceMax());
    }

    @Test
    void unChocEnScooterSonneMaisNeTuePas() {
        for (int i = 0; i < 40; i++) {
            etat.blesser(6f, 15f);
        }
        assertEquals(15f, etat.sante());
        assertFalse(etat.ko());
        etat.blesser(30f, 0f);
        assertTrue(etat.ko());
    }

    @Test
    void onNePaiePasCeQuOnNAPas() {
        etat.gagner(7);
        assertFalse(etat.payer(8));
        assertEquals(7, etat.argent(), "rien n'est pris si l'argent manque");
        assertTrue(etat.payer(5));
        assertEquals(2, etat.argent());
        etat.gagner(-12);
        assertEquals(0, etat.argent(), "on ne descend pas sous zero");
    }

    @Test
    void tomberCouteUnTiersDeLArgentEtRendLeScooter() {
        etat.gagner(100);
        etat.louer(120f);
        etat.blesser(200f, 0f);
        assertEquals(30, etat.relever());
        assertEquals(70, etat.argent());
        assertEquals(50f, etat.sante());
        assertEquals(0f, etat.location());
    }

    @Test
    void laLocationSEcouleEtPrevientUneSeuleFois() {
        etat.louer(1f);
        assertFalse(etat.userLocation(0.6f));
        assertTrue(etat.userLocation(0.6f), "elle se termine");
        assertFalse(etat.userLocation(0.6f), "et ne se termine qu'une fois");
    }
}
