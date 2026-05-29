# Echange NFC

[← Index documentation](../README.md) | [Bibliotheque](library-badges.md) | [Architecture](../architecture.md)

L'echange NFC permet a deux appareils proches d'echanger une variante de carte sans reseau.

## Acces

Depuis la bibliotheque :

1. ouvrir une carte obtenue ;
2. selectionner une variante avec au moins `2` copies ;
3. appuyer sur `Échanger` ;
4. rapprocher deux appareils compatibles NFC.

Le NFC est declare comme fonctionnalite non obligatoire dans le manifest. L'application reste installable sans NFC.

## Conditions de validite

Un echange est accepte seulement si :

- les deux appareils utilisent la meme empreinte de catalogue ;
- les deux cartes existent dans le catalogue ;
- la carte locale est disponible en doublon ;
- les raretes sont identiques ;
- les variantes `skyQuality::finish` sont identiques ;
- les deux cartes ne sont pas la meme variante exacte ;
- l'identifiant d'echange n'a pas deja ete applique.

## Protocole

Version courante : `NfcTradeProtocolVersion = 1`.

Messages :

- `hello`
- `match`
- `commit`
- `committed`
- `ack`
- `fail`

`TradeLedgerState` conserve les echanges termines pour eviter de reappliquer un meme `tradeId`. La memoire est bornee aux `32` derniers echanges.

## Donnees echangees

Les paquets NFC contiennent :

- type de message ;
- version protocole ;
- `tradeId` ;
- empreinte catalogue ;
- nonce ;
- reference de carte quand le message l'exige.

Aucune connexion distante n'est utilisee.

## Tests associes

- `NfcTradeProtocolTest`
- `TradeOperationsTest`
- `TradeRepositoryTest`
- `TradeViewModelTest`

[← Index documentation](../README.md)
