package histoire;

/**
 * Un texte qui s'ecrit lettre apres lettre, comme dans les jeux de l'epoque, en respirant comme on parle : un temps
 * apres une virgule, un plus long apres un point, plus long encore apres des points de suspension.
 */
public final class TexteProgressif {
    /** Les pauses a la vitesse normale (46 lettres par seconde) ; elles raccourcissent si le texte va plus vite. */
    private static final float VIRGULE = 0.14f;
    private static final float POINT = 0.3f;
    private static final float SUSPENSION = 0.42f;

    private final String texte;
    private final float lettresParSeconde;
    private float lettres;
    private float attente;

    public TexteProgressif(String texte, float lettresParSeconde) {
        this.texte = texte;
        this.lettresParSeconde = lettresParSeconde;
    }

    /** Le texte attend avant de commencer : le temps d'un soupir, d'une hesitation. */
    public void retarder(float secondes) {
        attente = Math.max(attente, secondes);
    }

    public void avancer(float dt) {
        while (dt > 0 && lettres < texte.length()) {
            if (attente > 0) {
                float pris = Math.min(attente, dt);
                attente -= pris;
                dt -= pris;
                continue;
            }
            int avant = (int) lettres;
            float voulu = Math.min(texte.length(), lettres + dt * lettresParSeconde);
            dt = 0;
            lettres = voulu;
            for (int i = avant; i < (int) voulu; i++) {
                float pause = pause(i);
                if (pause > 0 && i + 1 < texte.length()) {
                    lettres = i + 1;                                   // on s'arrete sur la ponctuation
                    float reste = (voulu - lettres) / lettresParSeconde;
                    attente = pause * Math.min(1.4f, 46f / lettresParSeconde);
                    dt = reste;
                    break;
                }
            }
        }
    }

    /** Le temps qu'on marque apres ce caractere. */
    private float pause(int i) {
        char c = texte.charAt(i);
        char suivant = i + 1 < texte.length() ? texte.charAt(i + 1) : ' ';
        if (suivant != ' ' && c != '…') {
            return 0;                                                  // "3,5" ou "..." en cours : pas de pause
        }
        return switch (c) {
            case ',', ';', ':' -> VIRGULE;
            case '…' -> SUSPENSION;
            case '.' -> i > 0 && texte.charAt(i - 1) == '.' ? SUSPENSION : POINT;
            case '!', '?' -> POINT;
            default -> 0;
        };
    }

    /** Montre tout le texte d'un coup : le joueur a appuye avant la fin. */
    public void terminer() {
        lettres = texte.length();
        attente = 0;
    }

    public boolean complet() {
        return lettres >= texte.length();
    }

    /** La partie du texte deja ecrite. */
    public String visible() {
        return texte.substring(0, (int) lettres);
    }

    public String entier() {
        return texte;
    }
}
