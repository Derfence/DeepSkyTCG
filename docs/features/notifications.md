# Notifications locales

[← Index documentation](../README.md) | [Accueil](home.md) | [Météo et recharge](weather-recharge.md)

L'application propose deux notifications locales, sans serveur ni donnée transmise :

- `Stock de packs plein` lorsque la recharge fait réellement passer le stock à `10` ;
- `Rappel après 7 jours` une seule fois par période sans ouverture de l'application.

Les notifications utilisent le badge de marque version 17 comme grande icône couleur. La petite icône système reste une silhouette monochrome, conformément aux contraintes Android.

## Autorisation et réglages

Sur Android 13 et supérieur, `POST_NOTIFICATIONS` est demandée une fois avant le chargement de la scène Home. Une acceptation active les deux rappels ; un refus les laisse désactivés et n'entraîne pas de nouvelle demande automatique.

Le menu Home contient une entrée `Notifications` ouvrant une feuille avec deux interrupteurs indépendants. Une activation explicite peut redemander l'autorisation. Si Android bloque toujours les notifications, la feuille donne accès aux réglages système de l'application.

Les préférences sont conservées dans `dstcg_notification_settings.preferences_pb`. Elles sont locales à l'installation, séparées de la progression chiffrée et exclues des sauvegardes portables.

## Planification

Les échéances sont des travaux uniques WorkManager, remplacés après chaque passage de l'application entre premier plan et arrière-plan. WorkManager peut différer légèrement leur exécution pour respecter les optimisations de batterie.

### Stock plein

La date du stock plein est dérivée du moteur de recharge existant. Le calcul tient compte :

- de la charge partielle déjà accumulée ;
- de la météo UTC déterministe ;
- du multiplicateur et de l'expiration éventuelle de l'Observatoire ;
- du plafond de `10` packs.

Le worker relit et normalise la progression avant d'émettre. Un stock initialement plein, une réinitialisation ou l'activation du réglage alors que le stock vaut déjà `10` ne produisent aucune notification. Une consommation sous le plafond arme un nouveau cycle.

### Retour au jeu

Chaque ouverture de l'application annule l'ancienne échéance, enregistre la nouvelle activité et arme un rappel à `J+7`. Une fois la notification émise, aucune répétition n'est planifiée avant une nouvelle ouverture.

## Navigation

Un toucher sur l'une ou l'autre notification lance `MainActivity` avec la destination Home. L'activité existante est recréée afin que le résultat reste déterministe quelle que soit la scène précédemment affichée.

[← Index documentation](../README.md)
