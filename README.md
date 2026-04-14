**[Lire en français](README_FR.md)**

# RN2C — Radio Numérique Contre la Censure
*(Digital Radio Against Censorship)*

**Anonymous voice communication over Tor.** One APK, zero configuration.

RN2C combines a Mumble client ([Humla](https://github.com/acomminos/Humla)) and the Tor network ([tor-android](https://github.com/nicholasnjr/tor-android)) into a standalone Android application. Install, pick a username, and talk — encrypted, anonymous, censorship-resistant.

## Features

- **One-tap** : automatic connection via Tor, no configuration needed
- **Anonymity** : 3 identity modes (anonymous, temporary, permanent)
- **Encryption** : TLS + Opus via the Mumble protocol
- **Censorship-resistant** : the server is a Tor hidden service (.onion)
- **Echo cancellation** : native Android AEC + Speex echo suppress
- **Voice changer** : pitch shift for vocal anonymity
- **Forced auto-update** : check on launch, blocking download screen
- **Multilingual** : FR / EN, flag selection on home screen
- **Role management** : user / moderator / admin from the app
- **Self-registration** : lock your username without moderator intervention

## Install

Download the latest version from [Releases](../../releases).

Requirements: Android 7.0+ (API 24)

## Build

```bash
git clone git@github.com:Pierroons/rn2c.git
cd rn2c
./gradlew assembleFossDebug
```

APK output: `app/build/outputs/apk/foss/debug/`

### Build requirements

- JDK 21
- Android SDK 36
- NDK 25.1
- Gradle 8.13

## Architecture

```
RN2C APK (~55 MB)
├── Mumble Client (Humla)     — Mumble 1.2.x protocol, Opus codec
├── Tor (tor-android)         — embedded SOCKS5 proxy
├── Speex Preprocessor        — denoise, AGC, AEC, dereverberation
├── RN2C UI                   — cyberpunk theme, PTT, role management
└── Update System             — check + download + forced install
```

## Documentation

- [Whitepaper (EN)](docs/RN2C_Whitepaper_EN.docx) — Project overview
- [Technical Documentation (EN)](docs/RN2C_Technical_Doc_EN.docx) — Architecture, code, developer guide
- [Whitepaper (FR)](docs/RN2C_Whitepaper_FR.docx) — Présentation du projet
- [Documentation technique (FR)](docs/RN2C_Technical_Doc_FR.docx) — Architecture, code, guide dev

## Current status

RN2C is currently configured to connect to a specific Mumble server via Tor (hardcoded .onion address). The goal is to make it **customizable and modular** so anyone can deploy their own server and instance.

## Roadmap

### Short term
- Make the .onion address user-configurable
- Update system via Tor hidden service (instead of clearnet)
- Enhanced .onion web portal (blog, forum, IRC, wiki)

### Medium term — RN2C Host
- **RN2C Host** : embedded Mumble server on the phone (uMurmur cross-compiled via NDK) + automatic Tor hidden service + built-in client
- **RN2C Client** : lightweight version — paste a .onion address and a username, that's it
- Zero dependency on any external server — the host's phone becomes the server

### Long term
- Fully decentralized architecture
- Multi-server support (.onion list)
- End-to-end encryption on top of the Mumble protocol

## Contributing

Contributions are welcome:

1. Fork the repo
2. Create a branch (`git checkout -b feature/my-feature`)
3. Commit (`git commit -m "Add my feature"`)
4. Push (`git push origin feature/my-feature`)
5. Open a Pull Request

### Translations

Add a `app/src/main/res/values-XX/strings.xml` file and translate the `rn2c_*` keys.

## License

GPL-3.0 — see [LICENSE](LICENSE)

Based on [Mumla](https://github.com/liblumla/mumla) (GPL-3.0) and [tor-android](https://github.com/nicholasnjr/tor-android) (BSD).

---

*RN2C — Because free speech should never depend on a centralized server.*
