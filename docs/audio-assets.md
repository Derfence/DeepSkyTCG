# Sons, musiques et crédits audio

[← Index documentation](README.md) | [Architecture](architecture.md) | [Catalogue et assets](catalog-assets.md)

Cette page sert de checklist pour remplacer les sons et musiques embarqués tout en créditant correctement les artistes.

## Fichiers runtime

Les fichiers joués par l'application sont dans `app/src/main/assets/sounds/`.

| Fichier | Usage |
| --- | --- |
| `ambient_starfield.mp3` | Ambiance générale de l'application |
| `ambient_minigames.mp3` | Ambiance du menu mini-jeux |
| `sound_ui_navigate.ogg` | Navigation dans l'interface |
| `sound_ui_library_open.ogg` | Ouverture du menu bibliothèque |
| `sound_ui_library_close.ogg` | Fermeture du menu bibliothèque |
| `sound_ui_equipment_open.ogg` | Ouverture du menu équipement |
| `sound_ui_equipment_close.ogg` | Fermeture du menu équipement |
| `sound_ui_badge_open.ogg` | Ouverture du menu badges |
| `sound_ui_badge_close.ogg` | Fermeture du menu badges |
| `sound_pack_burst.ogg` | Ouverture d'un pack |
| `sound_pack_reveal.ogg` | Révélation d'une carte |
| `sound_holographic_reveal.ogg` | Révélation holographique |
| `sound_minigame_success.ogg` | Réussite dans un mini-jeu |
| `sound_minigame_error.ogg` | Erreur dans un mini-jeu |
| `sound_minigame_completion.ogg` | Fin réussie d'un mini-jeu |
| `sound_badge_unlock.ogg` | Déblocage de badge |

## Mix runtime

Le routage et les réglages de mix sont centralisés dans `AudioMix.kt` :

- plusieurs cues métier peuvent réutiliser le même fichier audio, par exemple les entrées pack, artisanat ou mini-jeux peuvent pointer vers `sound_ui_navigate.ogg` tant qu'un son dédié n'existe pas ;
- les volumes, variations de pitch, cooldowns, fondus d'ambiance et ducking sont définis au même endroit ;
- les ambiances utilisent des fondus pour éviter les coupures entre les scènes ;
- les gros effets, comme ouverture de pack, holographique, fin de mini-jeu et badge, baissent temporairement l'ambiance pour rester lisibles.

Pour ajouter un son dédié à une future mise à jour :

1. Ajoute le fichier dans `app/src/main/assets/sounds/`.
2. Remplace uniquement `assetFileName` du cue concerné dans `AudioMix.kt`.
3. Ajoute l'entrée correspondante dans `audio_credits.json`.
4. Mets à jour le tableau ci-dessus si le nouveau fichier devient un fichier runtime distinct.

Règles importantes :

- garde le même nom de fichier que celui attendu par `AudioMix.kt`, par exemple `sound_pack_reveal.ogg` ;
- tu peux changer le format, mais il faudra alors mettre à jour `AudioMix.kt` et `audio_credits.json` ;
- retire de `AudioMix.kt` tout son qui n'existe plus dans `app/src/main/assets/sounds/` ;
- ne garde pas deux fichiers pour le même usage, par exemple `sound_pack_reveal.wav` et `sound_pack_reveal.ogg` en même temps ;
- utilise uniquement des noms en minuscules, chiffres et underscores ;
- préfère `.ogg` pour les musiques ou ambiances longues afin de réduire la taille de l'APK.

## Crédits runtime

Les crédits audio affichés dans le panneau `Paramètres > Crédits audio` viennent de :

```text
app/src/main/assets/sounds/audio_credits.json
```

Une entrée doit rester associée au fichier runtime correspondant :

```json
{
  "fileName": "sound_pack_reveal.ogg",
  "usage": "Révélation d'une carte",
  "title": "Nom du son ou du morceau",
  "artist": "Nom de l'artiste",
  "license": "CC BY",
  "licenseUrl": "https://creativecommons.org/licenses/by/4.0/",
  "sourcePage": "https://example.com/source",
  "downloadedAt": "2026-06-17",
  "changes": "Converti en OGG, volume normalisé",
  "notes": ""
}
```

Licences acceptées pour ce projet :

- `CC0` : attribution non obligatoire, mais garde quand même la source pour tracer l'origine ;
- `CC BY` : attribution obligatoire ;
- `CC BY-NC` : accepté tant que l'application reste gratuite, sans publicité, sans achat intégré et sans usage commercial.

À éviter par défaut :

- `ND`, car l'intégration, le montage, les boucles et les fades peuvent devenir problématiques ;
- `SA`, à signaler avant intégration parce que le partage à l'identique peut avoir des conséquences sur la distribution ;
- toute licence floue ou sans page source vérifiable.

## Marche à suivre

1. Télécharge le son depuis une source compatible : Kenney, Freesound, OpenGameArt, Incompetech, Musopen, Free Music Archive, ccMixter ou Openverse.
2. Note immédiatement le titre, l'artiste, la licence, l'URL de licence, la page source et la date de téléchargement.
3. Convertis le fichier si besoin, idéalement en `.ogg` pour les ambiances longues.
4. Remplace le fichier dans `app/src/main/assets/sounds/` en gardant le nom attendu par `AudioMix.kt`.
5. Mets à jour l'entrée correspondante dans `app/src/main/assets/sounds/audio_credits.json`.
6. Lance les tests et une compilation Android.

Commandes utiles :

```bash
./gradlew test
./gradlew assembleDebug
```

[← Index documentation](README.md)
