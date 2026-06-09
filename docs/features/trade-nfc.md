# Échange NFC

[← Index documentation](../README.md) | [Bibliothèque](library-badges.md) | [Architecture](../architecture.md)

L'échange NFC permet à deux appareils proches d'échanger une variante de carte sans réseau.

## Accès

Depuis la bibliothèque :

1. ouvrir une carte obtenue ;
2. sélectionner une variante avec au moins `2` copies ;
3. appuyer sur `Échanger` ;
4. rapprocher deux appareils compatibles NFC.

Le NFC est déclaré comme fonctionnalité non obligatoire dans le manifest. L'application reste installable sans NFC.

## Conditions de validité

Un échange est accepté seulement si :

- les deux appareils utilisent la même empreinte de catalogue ;
- les deux cartes existent dans le catalogue ;
- la carte locale est disponible en doublon ;
- les raretés sont identiques ;
- les variantes `skyQuality::finish` sont identiques ;
- les deux cartes ne sont pas la même variante exacte ;
- l'identifiant d'échange n'a pas déjà été appliqué.

## Protocole

Version courante : `NfcTradeProtocolVersion = 1`.

Messages :

- `hello`
- `match`
- `commit`
- `committed`
- `ack`
- `fail`

`TradeLedgerState` conserve les échanges terminés pour éviter de réappliquer un même `tradeId`. La mémoire est bornée aux `32` derniers échanges.

## Données échangées

Les paquets NFC contiennent :

- type de message ;
- version protocole ;
- `tradeId` ;
- empreinte de catalogue ;
- nonce ;
- référence de carte quand le message l'exige.

Aucune connexion distante n'est utilisée.

## Réussite

Lorsqu'un échange est appliqué, la collection locale décrémente la variante envoyée et incrémente la variante reçue. L'écran de succès réutilise la `TradeCardRef` déjà transmise par NFC pour afficher la carte reçue, puis propose de la révéler avec un bouton ou un geste vers le haut.

Le protocole reste en version `1` : aucun champ NFC n'est ajouté pour cette présentation.

## Tests associés

- `NfcTradeProtocolTest`
- `TradeOperationsTest`
- `TradeRepositoryTest`
- `TradeViewModelTest`

[← Index documentation](../README.md)
