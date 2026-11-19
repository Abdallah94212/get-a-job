package decor;

/** Le personnage qu'on promene, vu de cote. x suit l'avenue, y la profondeur du trottoir a la chaussee. */
public final class Joueur {
    public static final float RAYON = 8f;
    public static final float MARCHE = 80f;
    public static final float COURSE = 170f;
    /** A pied, on part et on s'arrete presque aussitot. */
    public static final float REPRISE = 1500f;

    /** Vitesses du moment : a pied, en scooter ou charge, elles changent. */
    public float marche = MARCHE;
    public float course = COURSE;
    /** L'acceleration et le freinage, en pixels par seconde carree : un scooter met du temps a partir et a s'arreter. */
    public float reprise = REPRISE;
    public float frein = REPRISE;
    public float x;
    public float y;
    public boolean versDroite = true;
    public boolean enMouvement;
    /** La vitesse a laquelle on vient de heurter un obstacle, sur l'axe de l'avenue ; 0 si rien n'a ete heurte. */
    public float choc;
    private float vx;
    private float vy;

    /** Avance selon les touches, avec un peu d'elan, en glissant le long des obstacles. */
    public void avancer(Scene scene, float dx, float dy, boolean court, float dt) {
        float norme = (float) Math.sqrt(dx * dx + dy * dy);
        float vitesse = court ? course : marche;
        float butX = norme > 0 ? dx / norme * vitesse : 0f;
        float butY = norme > 0 ? dy / norme * vitesse * 0.6f : 0f;
        vx = approcher(vx, butX, dt);
        vy = approcher(vy, butY, dt);
        choc = 0f;
        if (vx != 0f) {
            if (!scene.bloque(x + vx * dt, y, RAYON)) {
                x += vx * dt;
            } else {
                choc = Math.abs(vx);
                vx = 0f;
            }
        }
        if (vy != 0f) {
            if (!scene.bloque(x, y + vy * dt, RAYON)) {
                y += vy * dt;
            } else {
                vy = 0f;
            }
        }
        enMouvement = norme > 0 || Math.abs(vx) + Math.abs(vy) > 12f;
        if (dx != 0) {
            versDroite = dx > 0;
        }
    }

    /** On freine plus fort qu'on n'accelere quand on lache les touches ou qu'on repart dans l'autre sens. */
    private float approcher(float v, float but, float dt) {
        boolean freine = but == 0f || Math.signum(but) != Math.signum(v) && v != 0f;
        float pas = (freine ? frein : reprise) * dt;
        return Math.abs(but - v) <= pas ? but : v + Math.signum(but - v) * pas;
    }

    /** Arrete net : une porte, une scene, une chute. */
    public void stopper() {
        vx = 0f;
        vy = 0f;
        enMouvement = false;
    }

    public float vitesse() {
        return (float) Math.hypot(vx, vy);
    }
}
