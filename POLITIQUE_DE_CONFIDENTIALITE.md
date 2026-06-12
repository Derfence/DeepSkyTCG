# Politique de confidentialité

Document officiel de confidentialité pour **Deep Sky Trading Card Game**.

- Application Android : `Deep Sky Trading Card Game`
- Identifiant d'application : `fr.aumombelli.dstcg`
- Version documentée : `2.6.2`
- Dernière mise à jour : 12 juin 2026

## Résumé

Deep Sky Trading Card Game est une application Android autonome et hors ligne. Elle ne crée pas de compte utilisateur, ne demande pas de connexion, ne contient pas de publicité et n'utilise pas de service d'analytics.

L'application ne collecte, ne transmet et ne vend aucune donnée personnelle.

## Données collectées

Aucune donnée personnelle n'est collectée par l'application.

En particulier, l'application ne collecte pas :

- nom, adresse e-mail, numéro de téléphone ou identifiant de compte ;
- position géographique ;
- contacts, photos, fichiers personnels ou contenu du presse-papiers ;
- identifiant publicitaire ;
- données d'usage, statistiques, diagnostics ou rapports de crash envoyés à un serveur ;
- données de paiement.

## Données stockées localement

L'application conserve uniquement des données nécessaires au fonctionnement du jeu, sur l'appareil de l'utilisateur :

- collection de cartes, variantes possédées et progression associée ;
- stock et recharge des packs ;
- étape d'onboarding ;
- équipements, badges, nouveautés et progression des mini-jeux ;
- historique borné des échanges NFC déjà appliqués, utilisé pour éviter qu'un même échange soit rejoué ;
- identifiant d'installation local généré aléatoirement ;
- informations locales de temps de confiance, utilisées pour limiter les manipulations de l'horloge de l'appareil.

Ces données restent locales. Elles ne sont pas envoyées à l'éditeur ni à un serveur distant.

## Sécurité du stockage local

La progression locale est enregistrée dans un fichier DataStore chiffré, `dstcg_standalone_secure_progress.json`.

Le contenu de progression est chiffré en AES-GCM avec une clé gérée par Android Keystore. La sauvegarde Android et le transfert d'appareil sont désactivés pour les données de l'application.

## Réseau, publicité et analytics

L'application ne nécessite aucun appel réseau pour jouer.

Elle ne déclare pas la permission Android `INTERNET`, n'intègre pas de SDK publicitaire, n'intègre pas de SDK analytics et n'utilise pas de backend.

## NFC

L'application peut utiliser le NFC pour échanger une variante de carte entre deux appareils proches. Cette fonctionnalité est optionnelle et l'application reste utilisable sans NFC.

Les messages NFC contiennent uniquement les informations nécessaires à l'échange :

- type de message ;
- version du protocole ;
- identifiant d'échange ;
- empreinte de catalogue ;
- jeton temporaire ;
- référence de carte lorsque le message l'exige.

Aucune connexion distante n'est utilisée pendant un échange NFC.

## Météo et recharge

La météo utilisée pour la recharge des packs est calculée localement de manière déterministe à partir d'une date UTC. Elle ne provient pas d'un service météo externe et ne nécessite pas la position de l'utilisateur.

## Partage avec des tiers

L'application ne partage aucune donnée avec des tiers.

Les seules données transmises volontairement par l'application sont les paquets NFC nécessaires à un échange entre deux appareils proches, lorsque l'utilisateur lance explicitement cette fonctionnalité.

## Suppression des données

Les données de progression sont stockées localement sur l'appareil. Elles peuvent être supprimées en désinstallant l'application ou en effaçant les données de l'application depuis les réglages Android.

## Évolutions de cette politique

Si l'application ajoute ultérieurement une collecte de données, un service réseau, des analytics, de la publicité ou un autre traitement de données personnelles, cette politique devra être mise à jour avant publication de la version concernée.

## Contact

Pour toute question relative à cette politique de confidentialité, utiliser le canal de contact indiqué sur la page officielle de distribution de l'application.
