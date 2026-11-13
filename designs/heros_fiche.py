"""Fiche de personnage du heros, facon ecole d'art : un PDF A4 paysage de trois pages (et une image par page).
Lancement : python heros_fiche.py   (depuis designs/) -> outputs/heros_fiche.pdf, outputs/heros_fiche_p1..3.png
"""
import textwrap
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

import heros_sprite

RACINE = Path(__file__).resolve().parent
INTERFACE = RACINE.parent / "jeu" / "ressources" / "interface"
SORTIE = RACINE / "outputs"
PAGE_W, PAGE_H, MARGE = 2480, 1754, 110
PAPIER, ENCRE, DOUX, ACCENT, PLAQUE = (241, 237, 227), (32, 30, 28), (120, 114, 104), (214, 92, 8), (62, 62, 70)
SERIF = "/System/Library/Fonts/Supplemental/Georgia.ttf"
MONO = "/System/Library/Fonts/Supplemental/Courier New.ttf"
MONO_G = "/System/Library/Fonts/Supplemental/Courier New Bold.ttf"
H1, H2, H3 = ImageFont.truetype(SERIF, 84), ImageFont.truetype(SERIF, 44), ImageFont.truetype(MONO_G, 30)
CORPS, PETIT = ImageFont.truetype(SERIF, 32), ImageFont.truetype(MONO, 24)
TOTAL = 3


def page(numero, titre, sous_titre):
    img = Image.new("RGB", (PAGE_W, PAGE_H), PAPIER)
    d = ImageDraw.Draw(img)
    d.text((MARGE, 80), titre, font=H1, fill=ENCRE)
    d.text((MARGE, 190), sous_titre, font=H3, fill=ACCENT)
    d.line([(MARGE, 250), (PAGE_W - MARGE, 250)], fill=ENCRE, width=3)
    d.text((MARGE, PAGE_H - 80), "GET A JOB  -  fiche de personnage  -  le héros", font=PETIT, fill=DOUX)
    d.text((PAGE_W - MARGE - 90, PAGE_H - 80), f"{numero} / {TOTAL}", font=PETIT, fill=DOUX)
    return img, d


def bloc(d, x, y, titre, texte, largeur=46):
    d.text((x, y), titre, font=H3, fill=ACCENT)
    y += 48
    for paragraphe in texte.split("\n"):
        for ligne in textwrap.wrap(paragraphe, largeur) or [""]:
            d.text((x, y), ligne, font=CORPS, fill=ENCRE)
            y += 44
        y += 12
    return y + 20


def plaque(img, d, x, y, sprite, zoom, legende):
    grand = sprite.resize((sprite.width * zoom, sprite.height * zoom), Image.NEAREST)
    d.rectangle([x, y, x + grand.width + 40, y + grand.height + 40], fill=PLAQUE)
    img.paste(grand, (x + 20, y + 20), grand)
    d.text((x, y + grand.height + 52), legende, font=PETIT, fill=DOUX)
    return x + grand.width + 80


def page1():
    img, d = page(1, "Le héros", "CELUI QU'ON JOUE  -  « TOI »  -  20 ANS ET QUELQUES, LE KREMLIN-BICÊTRE")
    face = heros_sprite.heros()
    plaque(img, d, MARGE, 300, face.crop(face.getbbox()), 11, "de face, design de référence (110 px de haut)")
    x = 760
    y = bloc(d, x, 300, "QUI IL EST", "Un garçon du quartier, sans nom dans le jeu : les autres disent « petit », « le nouveau », "
             "« toi ». Trois mois de loyer en retard, plus de sommeil, une télé qui reste allumée.\n"
             "Il ressemble à ces héros qui portent un plan trop grand pour eux : calme en surface, le regard dur, "
             "la mâchoire serrée. Il parle peu et il écoute tout.", 50)
    y = bloc(d, x, y, "CE QU'IL VEUT", "Au début, payer son loyer. Puis ne plus livrer pour personne. Puis toute l'avenue, "
             "du Maître Poulet jusqu'à la tour. Il croit monter seul.", 50)
    bloc(d, x, y, "CE QU'IL NE SAIT PAS", "Chaque barreau de l'échelle a été posé pour lui. Tout en haut l'attend Baba, son père : "
         "« Tu voulais un job ? Voilà le mien. »", 50)
    x = 1700
    y = bloc(d, x, 300, "SILHOUETTE", "Tout est trop grand sur lui : le haut tombe sur les mains, le pantalon casse sur les baskets. "
             "De loin, on le reconnaît à trois choses : la casquette claire, le rouge du haut, les deux verres noirs.", 36)
    y = bloc(d, x, y, "ATTITUDE", "Épaules basses, mains le long du corps, tête un peu rentrée sous la visière. "
             "Il ne sourit jamais à l'écran. Les lunettes ne quittent pas son visage : on ne voit ses yeux nulle part.", 36)
    bloc(d, x, y, "VOIX", "Phrases courtes. Jamais d'explication. Quand il ment, il répond par une question.", 36)
    return img


def page2():
    img, d = page(2, "Dans le jeu", "LES IMAGES QUE LE JOUEUR VOIT  -  DESSINÉES À LEUR TAILLE FINALE, PIXEL PAR PIXEL")
    x = MARGE
    for nom, legende in (("heros", "à l'arrêt"), ("heros_2", "en marche"), ("heros_charge", "avec les caisses"),
                         ("heros_scooter", "sur le scooter de Youngssef")):
        x = plaque(img, d, x, 300, Image.open(INTERFACE / f"{nom}.png").convert("RGBA"), 12, legende)
    buste = Image.open(INTERFACE / "buste_toi.png").convert("RGBA")
    plaque(img, d, MARGE, 960, buste, 5, "le portrait des dialogues (96 x 112)")
    x = 760
    y = bloc(d, x, 960, "LE SPRITE DE RUE  -  22 x 48", "De profil, tourné vers la droite ; le jeu le retourne quand il va à gauche. "
             "Deux poses de marche. Chaque détail a ses pixels : la bande verte, rouge, verte de la casquette (3 rangées), "
             "le verre noir (3 x 2), l'écouteur (1 pixel blanc et son fil), la mitaine, l'empiècement rouge, la semelle.", 56)
    bloc(d, x, y, "LE PORTRAIT", "Dessiné directement en 96 x 112, sans agrandissement : deux fois plus de détails que les autres "
         "portraits du jeu. Monogramme et coutures de la casquette, ombre de la visière sur le front, reflet des verres, "
         "dragon pâle et deux lignes de texte sur le t-shirt délavé.", 56)
    x = 1780
    bloc(d, x, 960, "CE QUI CHANGE", "Avec les caisses de viande (mission 1.2), il les porte à deux bras et marche plus lentement.\n"
         "Sur le scooter (mission 1.1), il est assis, une main gantée sur le guidon.\n"
         "Il garde ses couleurs pendant que la rue s'éteint : à la fin, il est la seule tache rouge de l'avenue.", 36)
    return img


def page3():
    img, d = page(3, "Couleurs, objets, introduction", "CE QU'IL PORTE ET CE QUE ÇA DIT DE LUI")
    couleurs = [((198, 172, 130), "casquette"), ((34, 112, 66), "bande verte"), ((196, 40, 44), "bande rouge"),
                ((96, 70, 46), "visière"), ((12, 12, 16), "verres"), ((22, 20, 26), "cheveux"), ((216, 170, 136), "peau"),
                ((178, 54, 50), "t-shirt"), ((232, 176, 158), "dragon"), ((40, 38, 46), "pantalon"), ((128, 32, 38), "cuir rouge"),
                ((220, 218, 212), "baskets"), ((240, 240, 236), "écouteurs")]
    d.text((MARGE, 300), "PALETTE", font=H3, fill=ACCENT)
    for i, (c, nom) in enumerate(couleurs):
        x, y = MARGE + (i % 7) * 190, 360 + (i // 7) * 190
        d.rectangle([x, y, x + 150, y + 90], fill=c, outline=ENCRE)
        d.text((x, y + 98), nom, font=PETIT, fill=ENCRE)
        d.text((x, y + 126), "#%02X%02X%02X" % c, font=PETIT, fill=DOUX)
    y = bloc(d, MARGE, 770, "SES OBJETS", "La casquette de luxe : la seule chose chère qu'il possède. Il l'a déjà quand le jeu commence ; personne ne dit d'où elle vient.\n"
             "Les petites lunettes noires : on ne voit jamais ses yeux. Dans l'introduction, c'est dans ses verres que la télé se reflète.\n"
             "Les écouteurs à fil : il vit dans sa tête. On ne sait pas ce qu'il écoute.\n"
             "Le dragon du t-shirt et l'affiche de sa chambre (un homme qui se tient la tête, un squelette derrière lui) disent la même chose : "
             "quelque chose de plus grand que lui le regarde.", 70)
    x = 1500
    d.text((x, 300), "L'INTRODUCTION", font=H3, fill=ACCENT)
    yy = 360
    for k, (nom, legende) in enumerate((("gp_yeux", "1. calme : la télé dans ses verres"), ("gp_main", "2. la cigarette, la fumée"),
                                        ("gp_choc", "3. le choc : la bouche s'ouvre"), ("gp_lache", "4. la main lâche"))):
        im = Image.open(INTERFACE / f"{nom}.png").convert("RGB").resize((420, 236), Image.NEAREST)
        px, py = x + (k % 2) * 440, yy + (k // 2) * 290
        img.paste(im, (px, py))
        d.text((px, py + 244), legende, font=PETIT, fill=DOUX)
    yy += 580
    bloc(d, x, yy + 10, "", "Aucun texte, aucune parole. On ne montre jamais ce que la télé diffuse : seulement ce que ça lui fait. "
         "Il garde ses lunettes ; le choc passe par les sourcils, la bouche, puis la main. Ensuite il se lève.", 48)
    return img


if __name__ == "__main__":
    pages = [page1(), page2(), page3()]
    for i, p in enumerate(pages, 1):
        p.save(SORTIE / f"heros_fiche_p{i}.png")
    pages[0].save(SORTIE / "heros_fiche.pdf", save_all=True, append_images=pages[1:], resolution=300)
    print(SORTIE / "heros_fiche.pdf")
