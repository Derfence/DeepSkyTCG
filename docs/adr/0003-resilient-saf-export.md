# ADR 0003 — Export SAF résistant à la recréation d'activité

## Statut

Accepté.

## Contexte

Le sélecteur de documents Android crée la destination avant que l'application n'écrive son contenu. Pendant son affichage, Android ou un constructeur peut détruire l'activité de l'application. Une enveloppe conservée uniquement dans un `ViewModel` est alors perdue et le sélecteur peut laisser un fichier de 0 octet.

## Décision

- Déposer l'enveloppe déjà chiffrée dans le stockage privé de l'application avant d'ouvrir le sélecteur.
- Ne jamais persister le mot de passe ni le contenu déchiffré.
- Au retour du sélecteur, relire la copie temporaire depuis un magasin injecté dans le nouveau `BackupViewModel`.
- Relire la destination et vérifier sa taille et son empreinte SHA-256 avant d'annoncer la réussite.
- Supprimer la destination incomplète quand le fournisseur SAF le permet, puis supprimer la copie privée après réussite, annulation ou échec.

## Conséquences

Le flux supporte la recréation de l'activité, notamment avec l'option développeur « Ne pas conserver les activités ». Un arrêt complet pendant l'écriture peut encore laisser une destination incomplète si le fournisseur refuse sa suppression. La copie privée reste confidentielle puisqu'elle contient uniquement l'enveloppe AES-GCM portable déjà chiffrée.
