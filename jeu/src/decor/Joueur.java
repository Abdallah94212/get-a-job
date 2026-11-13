package decor;

/** Le personnage qu'on promene, vu de cote. x suit l'avenue, y la profondeur du trottoir a la chaussee. */
public final class Joueur {
    public static final float RAYON = 8f;
    public static final float MARCHE = 80f;
    public static final float COURSE = 170f;

    /** Vitesses du moment : a pied, en scooter ou charge, elles changent. */
    public float marche = MARCHE;
    public float course = COURSE;
    public float x;
    public float y;
    public boolean versDroite = true;
    public boolean enMouvement;

    /** Avance selon les touches, en glissant le long des obstacles. */
    public void avancer(Scene scene, float dx, float dy, boolean court, float dt) {
        float norme = (float) Math.sqrt(dx * dx + dy * dy);
        enMouvement = norme > 0;
        if (!enMouvement) {
            return;
        }
        float pas = (court ? course : marche) * dt / norme;
        if (!scene.bloque(x + dx * pas, y, RAYON)) {
            x += dx * pas;
        }
        if (!scene.bloque(x, y + dy * pas * 0.6f, RAYON)) {
            y += dy * pas * 0.6f;
        }
        if (dx != 0) {
            versDroite = dx > 0;
        }
    }
}
