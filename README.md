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

- [Whitepaper (FR)](docs/RN2C_Whitepaper_FR.pdf) — Présentation du projet
- [Documentation technique (FR)](docs/RN2C_Technical_Doc_FR.pdf) — Architecture, code, guide dev

## Contribuer

Les contributions sont les bienvenues :

1. Fork le repo
2. Crée une branche (`git checkout -b feature/ma-feature`)
3. Commit (`git commit -m "Ajout de ma feature"`)
4. Push (`git push origin feature/ma-feature`)
5. Ouvre une Pull Request

### Traductions

Ajouter un fichier `app/src/main/res/values-XX/strings.xml` et traduire les clés `rn2c_*`.

## Soutenir le projet

[![Ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/pierroons)

## Licence

GPL-3.0 — voir [LICENSE](LICENSE)

Basé sur [Mumla](https://github.com/liblumla/mumla) (GPL-3.0) et [tor-android](https://github.com/nicholasnjr/tor-android) (BSD).

---

*RN2C — Parce que la liberté d'expression ne devrait jamais dépendre d'un serveur centralisé.*
