# ADR 0002 — Politique du mot de passe de sauvegarde

## Statut

Accepté.

## Contexte

Le mot de passe d'une sauvegarde portable sert à dériver la clé AES-256. Une règle de longueur ou de complexité améliore généralement la résistance aux essais, mais impose une contrainte produit non souhaitée.

## Décision

- Accepter toute chaîne contenant au moins un caractère après normalisation Unicode NFC.
- Ne pas supprimer les espaces et ne pas imposer de longueur maximale applicative.
- Ne pas afficher d'indicateur de robustesse.
- Demander une confirmation exacte, après normalisation, lors de l'export.
- Conserver PBKDF2-HMAC-SHA256 à 600 000 itérations et ne jamais persister le mot de passe.

## Conséquences

L'utilisateur peut choisir un mot de passe très faible. Dans ce cas, une personne ayant accès au fichier peut plus facilement retrouver le mot de passe, déchiffrer la progression ou reconstruire une sauvegarde authentifiée. L'application prévient de cette limite dans la documentation, sans évaluer ni bloquer le choix de l'utilisateur.
