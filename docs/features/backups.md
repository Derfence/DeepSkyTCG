# Sauvegardes

[← Index documentation](../README.md) | [Accueil](home.md) | [Architecture](../architecture.md)

## Accès

Depuis l'accueil, ouvrir **Paramètres**, puis **Sauvegarde**. L'écran indique la dernière importation réalisée sur l'installation courante.

## Export

1. Choisir **Exporter**.
2. Saisir et confirmer un mot de passe non vide, sans règle de longueur ou de complexité.
3. Choisir la destination du fichier `.dstcgsave` avec le sélecteur Android.

La sauvegarde contient la collection, les packs, les équipements, les badges, le tutoriel, les mini-jeux et le ledger d'échange. Les préférences audio et le nom Bluetooth ne sont pas inclus.

Le mot de passe n'est pas conservé. Sa perte rend la sauvegarde irrécupérable. Un mot de passe faible facilite le déchiffrement ou la fabrication d'une sauvegarde valide par une personne connaissant le format.

## Import

1. Choisir **Importer** et sélectionner un fichier de 5 Mio maximum.
2. Saisir le mot de passe utilisé à l'export.
3. Vérifier la date et le résumé de progression.
4. Confirmer le remplacement de la progression courante.

Un mauvais mot de passe, une altération, une version future ou des données incohérentes sont refusés avant toute modification locale.

Après un import réussi, toute sauvegarde créée à la date de cet import ou auparavant est refusée. Cette limite est propre à l'installation : elle disparaît après effacement des données ou désinstallation.

## Limites de sécurité

AES-GCM détecte une modification réalisée sans la clé dérivée du mot de passe. Un utilisateur connaissant le mot de passe et capable de reconstruire le format peut toutefois fabriquer un autre fichier valide. Une protection globale contre ce cas et contre le retour arrière après réinstallation nécessiterait un service distant.

[← Accueil](home.md) | [Architecture](../architecture.md)
