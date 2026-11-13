package jeu;

import decor.Rendu;
import histoire.Sauvegarde;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.nio.file.Path;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** La fenetre du jeu : elle lit le clavier, fait avancer le jeu soixante fois par seconde et agrandit l'image. */
public final class Fenetre extends JPanel {
    private final Jeu jeu;
    private final Rendu rendu = new Rendu();
    private final Touches touches = new Touches();
    private final BufferedImage ecran = new BufferedImage(Rendu.L, Rendu.H, BufferedImage.TYPE_INT_RGB);
    private final JFrame cadre;
    private long dernier = System.nanoTime();

    private Fenetre(Jeu jeu, JFrame cadre) {
        this.jeu = jeu;
        this.cadre = cadre;
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                touches.presser(e.getKeyCode());
                if (e.getKeyCode() == KeyEvent.VK_T) {
                    jeu.reglages().trame = !jeu.reglages().trame;
                    jeu.reglagesChanges();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                touches.relacher(e.getKeyCode());
            }
        });
        jeu.brancher(() -> System.exit(0), this::ajusterTaille);
        new Timer(16, e -> image()).start();
    }

    /** Donne a la fenetre la taille choisie dans les options. */
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

    private void image() {
        long maintenant = System.nanoTime();
        float dt = Math.min(0.05f, (maintenant - dernier) / 1e9f);
        dernier = maintenant;
        jeu.maj(dt, touches);
        touches.finImage();
        jeu.dessiner(rendu);
        int[] pixels = ((DataBufferInt) ecran.getRaster().getDataBuffer()).getData();
        System.arraycopy(rendu.image, 0, pixels, 0, pixels.length);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int zoom = Math.max(1, Math.min(getWidth() / Rendu.L, getHeight() / Rendu.H));
        int largeur = Rendu.L * zoom;
        int hauteur = Rendu.H * zoom;
        g.drawImage(ecran, (getWidth() - largeur) / 2, (getHeight() - hauteur) / 2, largeur, hauteur, null);
    }

    /** Lance le jeu. Argument facultatif : le dossier des ressources. */
    public static void main(String[] args) throws IOException {
        Jeu jeu = new Jeu(Path.of(args.length > 0 ? args[0] : "ressources"), Sauvegarde.parDefaut(), Reglages.parDefaut(), false);
        SwingUtilities.invokeLater(() -> {
            JFrame cadre = new JFrame("GET A JOB");
            cadre.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            Fenetre fenetre = new Fenetre(jeu, cadre);
            cadre.add(fenetre);
            fenetre.setPreferredSize(new Dimension(1, 1));
            fenetre.ajusterTaille();
            cadre.setVisible(true);
            fenetre.requestFocusInWindow();
        });
    }
}
