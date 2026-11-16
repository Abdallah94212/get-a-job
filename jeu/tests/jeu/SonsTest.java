package jeu;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Chaque son que le code demande existe dans ressources/sons : sinon le jeu se tairait sans prevenir. */
class SonsTest {
    @Test
    void chaqueSonAppeleParLeCodeExiste() throws IOException {
        Audio audio = new Audio(Path.of("ressources", "sons"), new Reglages(null), true);
        Pattern appel = Pattern.compile("jouer\\(\"([a-z_0-9]+)\"|\"(aie_|orage_)\"|\\{\"([a-z_]+)\", \"0\\.[0-9]+\"\\}|\\{\"([a-z_]+)\", [0-9.]+f");
        Pattern liste = Pattern.compile("\"(vaisselle|micro_ondes|verre|metal|chien_loin|sirene_loin|bouteille|amb_periph|coeur_vite|coeur)\"");
        TreeSet<String> noms = new TreeSet<>();
        for (String fichier : new String[] {"Effets", "Sonorisation", "EcranPartie", "Scenographe", "Telephone"}) {
            String code = Files.readString(Path.of("src", "jeu", fichier + ".java"));
            Matcher m = appel.matcher(code);
            while (m.find()) {
                for (int g = 1; g <= 4; g++) {
                    if (m.group(g) != null && !m.group(g).endsWith("_")) {
                        noms.add(m.group(g));
                    }
                }
            }
            Matcher l = liste.matcher(code);
            while (l.find()) {
                noms.add(l.group(1));
            }
        }
        for (int i = 1; i <= 3; i++) {
            noms.add("aie_" + i);
            noms.add("orage_" + i);
        }
        assertTrue(noms.size() > 40, "on a bien trouve les sons du code : " + noms.size());
        for (String nom : noms) {
            assertTrue(audio.existe(nom), "son introuvable : " + nom);
        }
    }
}
