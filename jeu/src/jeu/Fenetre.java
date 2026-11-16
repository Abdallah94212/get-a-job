package jeu;

import decor.Rendu;
import histoire.Sauvegarde;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * La fenetre du jeu. Un fil a part fait tourner le jeu a 60 images par seconde, d'un pas regulier : il lit le clavier,
 * fait avancer le jeu, le dessine, puis pose l'image a l'ecran par double tampon (pas de minuteur Swing, pas
 * d'attente du fil de l'interface). L'image du decor et le calque du texte restent "geres" par Java2D : la carte
 * graphique les garde et les agrandit elle-meme, au lieu que le processeur recalcule chaque pixel de l'ecran.
 */
public final class Fenetre extends Canvas {
    private static final long PERIODE = 1_000_000_000L / 60;
    /** -Dgaj.mesure=true : la fenetre compte ses images et le temps de chaque etape, et l'ecrit toutes les deux secondes. */
    private static final boolean MESURE = Boolean.getBoolean("gaj.mesure");
    /** -Dgaj.demo=true : un pilote automatique (nouvelle partie, scenes passees, course dans la rue), pour mesurer. */
    private static final boolean DEMO = Boolean.getBoolean("gaj.demo");

    private final Jeu jeu;
    private final Rendu rendu = new Rendu();
    private final Touches touches = new Touches();
    private final BufferedImage ecran = new BufferedImage(Rendu.L, Rendu.H, BufferedImage.TYPE_INT_RGB);
    private final JFrame cadre;
    /** Les touches arrivent sur le fil de l'interface : on les met en file, le fil du jeu les lit au debut de chaque image. */
    private final ConcurrentLinkedQueue<int[]> frappes = new ConcurrentLinkedQueue<>();
    private volatile boolean tailleAChanger;

    /** Le calque du texte sur la carte graphique : le texte y est trace et agrandi sans repasser par le processeur. */
    private java.awt.image.VolatileImage calqueTexte;
    private Graphics2D plumeTexte;
    private long tMaj;
    private long tRendu;
    private long tPeinture;
    private long pireImage;
    private int images;
    private long debutMesure = System.nanoTime();
    private float tempsDemo;

    private Fenetre(Jeu jeu, JFrame cadre) {
        this.jeu = jeu;
        this.cadre = cadre;
        setBackground(Color.BLACK);
        setFocusable(true);
        setIgnoreRepaint(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                frappes.add(new int[] {e.getKeyCode(), 1});
            }

            @Override
            public void keyReleased(KeyEvent e) {
                frappes.add(new int[] {e.getKeyCode(), 0});
            }
        });
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                frappes.add(new int[] {0, 2});                         // ce qui sera relache ailleurs ne nous arrivera pas
            }
        });
        jeu.brancher(() -> System.exit(0), () -> tailleAChanger = true);
    }

    /** Donne a la fenetre la taille choisie dans les options (sur le fil de l'interface). */
    private void ajusterTaille() {
        int taille = jeu.reglages().taille;
        if (taille == Reglages.TAILLES.length - 1) {
            cadre.setExtendedState(JFrame.MAXIMIZED_BOTH);
            return;
        }
        Dimension voulue = new Dimension(Rendu.L * (taille + 2), Rendu.H * (taille + 2));
        if (cadre.getExtendedState() != JFrame.NORMAL || !voulue.equals(getPreferredSize())) {
            cadre.setExtendedState(JFrame.NORMAL);
            setPreferredSize(voulue);
            cadre.pack();
            cadre.setLocationRelativeTo(null);
        }
    }

    /** La boucle du jeu, dans son propre fil : une image tous les 1/60 de seconde, sans derive. */
    private void boucle() {
        createBufferStrategy(2);
        BufferStrategy tampons = getBufferStrategy();
        long prochaine = System.nanoTime();
        long dernier = prochaine;
        while (true) {
            long maintenant = System.nanoTime();
            float dt = Math.min(0.05f, (maintenant - dernier) / 1e9f);
            if (MESURE) {
                pireImage = Math.max(pireImage, maintenant - dernier);
            }
            dernier = maintenant;
            try {
                image(dt, tampons);
            } catch (RuntimeException erreur) {                       // la peinture elle-meme : on recommence a l'image suivante
                System.err.println("Image perdue : " + erreur);
            }
            prochaine += PERIODE;
            long reste = prochaine - System.nanoTime();
            if (reste < -PERIODE * 3) {                                // tres en retard (la fenetre a ete deplacee) : on repart d'ici
                prochaine = System.nanoTime();
            }
            attendre(prochaine);
        }
    }

    /** Attend jusqu'a cet instant : on dort, puis on tourne la derniere milliseconde pour tomber juste. */
    private static void attendre(long instant) {
        while (true) {
            long reste = instant - System.nanoTime();
            if (reste <= 0) {
                return;
            }
            if (reste > 2_000_000L) {
                try {
                    Thread.sleep((reste - 1_000_000L) / 1_000_000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            } else {
                Thread.onSpinWait();
            }
        }
    }

    private void image(float dt, BufferStrategy tampons) {
        long debut = System.nanoTime();
        for (int[] f = frappes.poll(); f != null; f = frappes.poll()) {
            if (f[1] == 2) {
                touches.toutLacher();
            } else if (f[1] == 1) {
                touches.presser(f[0]);
                if (f[0] == KeyEvent.VK_T) {
                    jeu.reglages().trame = !jeu.reglages().trame;
                    jeu.reglagesChanges();
                }
            } else {
                touches.relacher(f[0]);
            }
        }
        if (tailleAChanger) {
            tailleAChanger = false;
            SwingUtilities.invokeLater(this::ajusterTaille);
        }
        if (DEMO) {
            piloter(dt);
        }
        jeu.maj(dt, touches);
        touches.finImage();
        long a = System.nanoTime();
        jeu.dessiner(rendu);
        preparerCalque();
        ecran.getRaster().setDataElements(0, 0, Rendu.L, Rendu.H, rendu.image);   // sans "voler" le tableau : l'image reste geree
        long b = System.nanoTime();
        peindre(tampons);
        long c = System.nanoTime();
        tMaj += a - debut;
        tRendu += b - a;
        tPeinture += c - b;
        if (MESURE && ++images % 120 == 0) {
            double duree = (c - debutMesure) / 1e9;
            System.err.printf(getWidth() + "x" + getHeight() + "  %5.1f images/s  maj %.2f ms  rendu %.2f ms  peinture %.2f ms  pire ecart %.1f ms%n", images / duree,
                    tMaj / 1e6 / images, tRendu / 1e6 / images, tPeinture / 1e6 / images, pireImage / 1e6);
            EcranPartie partie = jeu.partie();
            if (partie != null) {                                      // ou en est la partie : pour comprendre un gel
                histoire.Mission m = partie.progression().courante();
                System.err.println("  " + (m == null ? "fin" : m.id()) + "/" + partie.progression().etape() + " " + partie.monde().carte()
                        + " x " + Math.round(partie.monde().joueur.x) + " y " + Math.round(partie.monde().joueur.y) + " scene " + partie.positionScene()
                        + " tel " + partie.auTelephone() + " encart " + partie.encartOuvert() + " combat " + partie.combat().actif()
                        + " focus " + isFocusOwner() + " incidents " + jeu.incidents());
            }
            images = 0;
            tMaj = tRendu = tPeinture = pireImage = 0;
            debutMesure = c;
        }
    }

    /** Le calque du texte sur la carte graphique : cree au besoin, recree s'il a ete perdu (veille, changement d'ecran). */
    private void preparerCalque() {
        java.awt.GraphicsConfiguration config = getGraphicsConfiguration();
        if (config == null) {
            return;
        }
        int etat = calqueTexte == null ? java.awt.image.VolatileImage.IMAGE_INCOMPATIBLE : calqueTexte.validate(config);
        if (etat == java.awt.image.VolatileImage.IMAGE_INCOMPATIBLE) {
            if (plumeTexte != null) {
                plumeTexte.dispose();
            }
            calqueTexte = config.createCompatibleVolatileImage(Rendu.L * Rendu.F, Rendu.H * Rendu.F, java.awt.Transparency.TRANSLUCENT);
            plumeTexte = calqueTexte.createGraphics();
            plumeTexte.setComposite(java.awt.AlphaComposite.Src);
        }
        if (rendu.avecTexte()) {                                       // une copie d'un bloc, pixel pour pixel : rien ne reste
            plumeTexte.drawImage(rendu.textes, 0, 0, null);
        }
    }

    /** Pose l'image a l'ecran : le decor en pixels nets, le texte lisse par-dessus, dans le tampon cache, puis on l'affiche. */
    private void peindre(BufferStrategy tampons) {
        int largeurEcran = getWidth();
        int hauteurEcran = getHeight();
        if (largeurEcran <= 0 || hauteurEcran <= 0) {
            return;
        }
        int zoom = Math.max(1, Math.min(largeurEcran / Rendu.L, hauteurEcran / Rendu.H));
        int largeur = Rendu.L * zoom;
        int hauteur = Rendu.H * zoom;
        int x = (largeurEcran - largeur) / 2;
        int y = (hauteurEcran - hauteur) / 2;
        do {
            do {
                Graphics2D g = (Graphics2D) tampons.getDrawGraphics();
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, largeurEcran, hauteurEcran);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(ecran, x, y, largeur, hauteur, null);
                if (rendu.avecTexte()) {
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.drawImage(calqueTexte != null ? calqueTexte : rendu.textes, x, y, largeur, hauteur, null);
                }
                g.dispose();
            } while (tampons.contentsRestored());
            tampons.show();
        } while (tampons.contentsLost());
        Toolkit.getDefaultToolkit().sync();
    }

    /** Le pilote de mesure : il lance une partie, passe les scenes, puis court dans la rue, a gauche et a droite. */
    private void piloter(float dt) {
        tempsDemo += dt;
        int tic = (int) (tempsDemo * 4);
        if (tempsDemo < 25 && tic % 2 == 0) {
            touches.presser(KeyEvent.VK_E);
        } else {
            touches.relacher(KeyEvent.VK_E);
        }
        if (tempsDemo > 25) {
            touches.presser(KeyEvent.VK_SHIFT);
            boolean droite = ((int) (tempsDemo / 4)) % 2 == 0;
            touches.relacher(droite ? KeyEvent.VK_LEFT : KeyEvent.VK_RIGHT);
            touches.presser(droite ? KeyEvent.VK_RIGHT : KeyEvent.VK_LEFT);
        }
        String photo = System.getProperty("gaj.photo");
        if (photo != null && (tempsDemo > 3 && tempsDemo - dt <= 3 || tempsDemo > 18 && tempsDemo - dt <= 18
                || tempsDemo > 35 && tempsDemo - dt <= 35)) {
            photographier(photo + "_" + (int) tempsDemo + ".png");   // ce que la carte graphique a vraiment recu
        }
        if (tempsDemo > 45) {
            System.exit(0);
        }
    }

    /** L'image telle qu'elle est composee a l'ecran : le decor agrandi, et le calque du texte relu sur la carte graphique. */
    private void photographier(String fichier) {
        BufferedImage photo = new BufferedImage(Rendu.L * 3, Rendu.H * 3, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = photo.createGraphics();
        g.drawImage(ecran, 0, 0, photo.getWidth(), photo.getHeight(), null);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(calqueTexte != null ? calqueTexte.getSnapshot() : rendu.textes, 0, 0, photo.getWidth(), photo.getHeight(), null);
        g.dispose();
        try {
            javax.imageio.ImageIO.write(photo, "png", new java.io.File(fichier));
        } catch (IOException erreur) {
            System.err.println("Photo impossible : " + erreur.getMessage());
        }
    }

    /** Lance le jeu. Argument facultatif : le dossier des ressources. */
    public static void main(String[] args) throws IOException {
        Sauvegarde sauvegarde = DEMO ? new Sauvegarde(Files.createTempFile("demo", ".properties")) : Sauvegarde.parDefaut();
        if (DEMO) {
            sauvegarde.effacer();
        }
        Reglages reglages = DEMO ? new Reglages(null) : Reglages.parDefaut();
        if (DEMO) {
            reglages.taille = Integer.getInteger("gaj.taille", 0);
        }
        Jeu jeu = new Jeu(Path.of(args.length > 0 ? args[0] : "ressources"), sauvegarde, reglages, false);
        SwingUtilities.invokeLater(() -> {
            JFrame cadre = new JFrame("GET A JOB");
            cadre.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            cadre.getContentPane().setBackground(Color.BLACK);
            Fenetre fenetre = new Fenetre(jeu, cadre);
            cadre.add(fenetre);
            fenetre.ajusterTaille();
            if (cadre.getExtendedState() == JFrame.NORMAL && fenetre.getWidth() == 0) {
                cadre.pack();
                cadre.setLocationRelativeTo(null);
            }
            cadre.setVisible(true);
            fenetre.requestFocusInWindow();
            Thread jeuFil = new Thread(fenetre::boucle, "jeu");
            jeuFil.setDaemon(true);
            jeuFil.setPriority(Thread.MAX_PRIORITY);
            jeuFil.start();
        });
    }
}
