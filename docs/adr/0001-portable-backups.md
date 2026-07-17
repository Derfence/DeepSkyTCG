# ADR 0001 — Sauvegardes portables chiffrées

## Statut

Accepté.

## Contexte

Deep Sky TCG fonctionne sans compte ni serveur. Une sauvegarde doit être transférable entre installations, rester confidentielle et détecter les modifications, tout en limitant la restauration répétée d'anciens états.

La clé Android Keystore de la progression locale n'est pas exportable et ne peut donc pas protéger directement un fichier portable.

## Décision

- Sérialiser un modèle portable excluant les identifiants et preuves temporelles de l'installation.
- Dériver une clé AES-256 du mot de passe avec PBKDF2-HMAC-SHA256, un sel aléatoire et 600 000 itérations.
- Chiffrer et authentifier le contenu avec AES-GCM et un nonce aléatoire.
- Stocker séparément, sous une autre clé Android Keystore, la dernière date d'import et un marqueur provisoire d'import.
- Refuser les sauvegardes créées à la date limite ou avant.
- Fusionner les identifiants d'échanges déjà terminés lors de la restauration.

## Conséquences

Le format est portable et ne requiert aucune dépendance ou connexion supplémentaire. L'import est récupérable après une interruption grâce au marqueur provisoire, avec un comportement volontairement bloquant en cas d'ambiguïté.

La limite anti-retour ne survit pas à l'effacement des données. Le détenteur du mot de passe peut recalculer la clé et ne peut donc pas être considéré comme un adversaire totalement exclu sans autorité distante.
