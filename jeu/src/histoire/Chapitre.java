package histoire;

import java.util.ArrayList;
import java.util.List;

/** Un chapitre de la campagne : un acte de l'histoire et ses missions, dans l'ordre. */
public final class Chapitre {
    private final String id;
    private final String titre;
    private final String sousTitre;
    private final String accroche;
    private final List<Mission> missions = new ArrayList<>();

    public Chapitre(String id, String titre, String sousTitre, String accroche) {
        this.id = id;
        this.titre = titre;
        this.sousTitre = sousTitre;
        this.accroche = accroche;
    }

    public String id() {
        return id;
    }

    public String titre() {
        return titre;
    }

    public String sousTitre() {
        return sousTitre;
    }

    public String accroche() {
        return accroche;
    }

    public List<Mission> missions() {
        return missions;
    }
}
