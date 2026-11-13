package histoire;

/** Un texte qui s'ecrit lettre apres lettre, comme dans les jeux de l'epoque. */
public final class TexteProgressif {
    private final String texte;
    private final float lettresParSeconde;
    private float lettres;

    public TexteProgressif(String texte, float lettresParSeconde) {
        this.texte = texte;
        this.lettresParSeconde = lettresParSeconde;
    }

    public void avancer(float dt) {
        lettres = Math.min(texte.length(), lettres + dt * lettresParSeconde);
    }

    /** Montre tout le texte d'un coup : le joueur a appuye avant la fin. */
    public void terminer() {
        lettres = texte.length();
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
