package jeu;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import decor.Scene;
import histoire.Progression;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** La rue perd ses couleurs a mesure que le heros monte, et Old Geto devient gris quand le heros le quitte. */
class RueQuiSEteintTest {
    private final Ressources res;
    private final Progression progression;

    RueQuiSEteintTest() throws IOException {
        res = new Ressources(Path.of("ressources"));
        progression = new Progression(res.campagne());
    }

    private void aller(String mission, String... drapeaux) {
        int rang = res.campagne().missions().indexOf(res.campagne().mission(mission));
        progression.restaurer(rang, 0, Set.of(), true, Set.of(drapeaux));
        res.suivre(progression);
    }

    @Test
    void lesEtapesSuiventLHistoire() {
        aller("0.1");
        assertEquals(0, progression.etapeDeLaRue());
        aller("1.1");
        assertEquals(0, progression.etapeDeLaRue());
        aller("1.2");
        assertEquals(1, progression.etapeDeLaRue());
        aller("1.4");
        assertEquals(1, progression.etapeDeLaRue());
        aller("1.4", "parti_de_chez_geto");
        assertEquals(2, progression.etapeDeLaRue());
        aller("2.2");
        assertEquals(2, progression.etapeDeLaRue());
        aller("2.3");
        assertEquals(3, progression.etapeDeLaRue());
    }

    @Test
    void leDecorChangeDeCouleursPuisRetrouveLesSiennes() {
        aller("0.1");
        Scene rue = res.carte("avenue_bas");
        int[] debut = rue.textures.get("ciel").pixels().clone();
        aller("2.3");
        assertEquals(3, res.carte("avenue_bas").etape());
        assertFalse(java.util.Arrays.equals(debut, rue.textures.get("ciel").pixels()), "le ciel doit avoir change de couleurs");
        aller("0.1");
        assertArrayEquals(debut, res.carte("avenue_bas").textures.get("ciel").pixels());
    }

    @Test
    void getoEstEnCouleursPuisGris() {
        aller("1.3");
        assertFalse(progression.getoEnnemi());
        var patron = res.image("geto");
        assertSame(patron, res.image("geto"));
        aller("1.4", "parti_de_chez_geto");
        assertTrue(progression.getoEnnemi());
        assertNotSame(patron, res.image("geto"));
        assertSame(res.image("geto_gris"), res.image("geto"));
        assertSame(res.image("buste_geto_gris"), res.image("buste_geto"));
    }
}
