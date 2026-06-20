# Échange Bluetooth

[← Index documentation](../README.md) | [Bibliothèque](library-badges.md) | [Architecture](../architecture.md)

L'échange Bluetooth permet à deux appareils proches d'échanger une variante de carte sans serveur et sans appairage système.

## Accès

Depuis la bibliothèque :

1. ouvrir une carte obtenue ;
2. sélectionner une variante avec au moins `2` copies ;
3. appuyer sur `Échanger` ;
4. autoriser et activer Bluetooth si nécessaire ;
5. vérifier le nom visible local, limité à 12 octets pour tenir dans l'annonce BLE ;
6. choisir le partenaire détecté ;
7. comparer le code court et confirmer des deux côtés.

L'échange fonctionne uniquement lorsque l'écran d'échange est ouvert au premier plan. Les appareils sans support d'annonce Bluetooth LE affichent une erreur d'incompatibilité pour ce flux.

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

Version courante : `TradeProtocolVersion = 1`.

Messages :

- `hello`
- `confirm`
- `prepare`
- `prepared`
- `commit`
- `committed`
- `ack`
- `resume`
- `fail`

Les paquets sont sérialisés en JSON compact via Kotlin serialization, puis fragmentés pour le transport BLE GATT. `TradeLedgerState` conserve les échanges terminés et l'échange préparé en cours pour éviter de réappliquer un même `tradeId`.

La découverte BLE annonce aussi un statut de présence. Quand l'utilisateur ferme l'écran, quitte le flux, change de nom visible ou termine un échange avec succès, l'app publie brièvement une annonce `leaving` afin que les autres appareils retirent immédiatement ce partenaire de leur liste. Lors d'un changement de nom, la nouvelle annonce active démarre après cette annonce de départ.

## Données échangées

Les paquets Bluetooth contiennent :

- type de message ;
- version protocole ;
- `tradeId` ;
- empreinte de catalogue ;
- identifiant de session ;
- nonce ;
- nom visible local quand nécessaire ;
- référence de carte quand le message l'exige ;
- code court de vérification quand il est disponible.

Aucune connexion distante n'est utilisée. Le nom visible est stocké dans `dstcg_trade_settings.preferences_pb`, avec un défaut court du type `Obs. 4821`.

## Réussite

Lorsqu'un échange est appliqué, la collection locale décrémente la variante envoyée et incrémente la variante reçue. L'écran de succès réutilise la `TradeCardRef` transmise par Bluetooth pour afficher la carte reçue, puis propose de la révéler avec un bouton ou un geste vers le haut.

## Tests associés

- `BluetoothTradeProtocolTest`
- `TradeOperationsTest`
- `TradeRepositoryTest`
- `TradeViewModelTest`

[← Index documentation](../README.md)
