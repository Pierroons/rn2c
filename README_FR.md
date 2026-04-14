# RN2C — Radio Numérique Contre la Censure

**Communication vocale anonyme via Tor.** Un seul APK, zéro configuration.

RN2C combine un client Mumble ([Humla](https://github.com/acomminos/Humla)) et le réseau Tor ([tor-android](https://github.com/nicholasnjr/tor-android)) en une application Android autonome. L'utilisateur installe, choisit un pseudo, et parle — chiffré, anonyme, résistant à la censure.

## Fonctionnalités

- **One-tap** : connexion automatique via Tor, aucune configuration requise
- **Anonymat** : 3 modes d'identité (anonyme, temporaire, permanent)
- **Chiffrement** : TLS + Opus via le protocole Mumble
- **Anti-censure** : le serveur est un hidden service Tor (.onion)
- **Annulation d'écho** : AEC Android natif + Speex echo suppress
- **Modification de voix** : pitch shift pour l'anonymat vocal
- **Mise à jour automatique** : vérification au lancement, installation forcée
- **Multilingue** : FR / EN, sélection par drapeaux
- **Gestion des rôles** : utilisateur / modérateur / administrateur depuis l'app
- **Auto-enregistrement** : verrouiller son pseudo sans intervention d'un modérateur

## Installation

Télécharger la dernière version depuis les [Releases](../../releases).

Prérequis : Android 7.0+ (API 24)

## Build

```bash
git clone git@github.com:Pierroons/rn2c.git
cd rn2c
./gradlew assembleFossDebug
```

L'APK se trouve dans `app/build/outputs/apk/foss/debug/`

### Prérequis de build

- JDK 21
- Android SDK 36
- NDK 25.1
- Gradle 8.13

## Architecture

```
RN2C APK (~55 Mo)
├── Client Mumble (Humla)     — protocole Mumble 1.2.x, codec Opus
├── Tor (tor-android)         — proxy SOCKS5 embarqué
├── Préprocesseur Speex       — débruitage, AGC, AEC, déréverbération
├── UI RN2C                   — thème cyberpunk, PTT, gestion des rôles
└── Système de MAJ            — vérification + téléchargement + install forcée
```

## Documentation

- [Whitepaper (FR)](docs/RN2C_Whitepaper_FR.docx) — Présentation du projet
- [Documentation technique (FR)](docs/RN2C_Technical_Doc_FR.docx) — Architecture, code, guide dev
- [Whitepaper (EN)](docs/RN2C_Whitepaper_EN.docx) — Project overview
- [Technical Documentation (EN)](docs/RN2C_Technical_Doc_EN.docx) — Architecture, code, developer guide

## État actuel

RN2C est actuellement configuré pour se connecter à un serveur Mumble spécifique via Tor (adresse .onion hardcodée). L'objectif est de le rendre **personnalisable et modulable** pour que chacun puisse déployer son propre serveur et sa propre instance.

## Feuille de route

### Court terme
- Rendre l'adresse .onion configurable par l'utilisateur
- Mise à jour via hidden service .onion (au lieu du clearnet)
- Portail web .onion enrichi (blog, forum, IRC, wiki)

### Moyen terme — RN2C Host
- **RN2C Host** : serveur Mumble embarqué dans le téléphone (uMurmur cross-compilé via NDK) + Tor hidden service automatique + client intégré
- **RN2C Client** : version légère, l'utilisateur colle une adresse .onion et un pseudo — c'est tout
- Zéro dépendance à un serveur externe — le téléphone de l'hébergeur devient le serveur

### Long terme
- Architecture entièrement décentralisée
- Support multi-serveurs (liste de .onion)
- Chiffrement end-to-end au-dessus du protocole Mumble

## Contribuer

Les contributions sont les bienvenues :

1. Fork le repo
2. Crée une branche (`git checkout -b feature/ma-feature`)
3. Commit (`git commit -m "Ajout de ma feature"`)
4. Push (`git push origin feature/ma-feature`)
5. Ouvre une Pull Request

### Traductions

Ajouter un fichier `app/src/main/res/values-XX/strings.xml` et traduire les clés `rn2c_*`.

## Licence

GPL-3.0 — voir [LICENSE](LICENSE)

Basé sur [Mumla](https://github.com/liblumla/mumla) (GPL-3.0) et [tor-android](https://github.com/nicholasnjr/tor-android) (BSD).

---

*RN2C — Parce que la liberté d'expression ne devrait jamais dépendre d'un serveur centralisé.*
