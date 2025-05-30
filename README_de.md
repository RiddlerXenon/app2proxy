
<div align="center">

# 🚀 App2Proxy

<img src="https://img.shields.io/badge/Android-13%2B-green?logo=android&logoColor=white" alt="Android 13+">
<img src="https://img.shields.io/badge/Kotlin-100%25-purple?logo=kotlin&logoColor=white" alt="Kotlin">
<img src="https://img.shields.io/badge/License-GPL%20v3-blue?logo=gnu&logoColor=white" alt="License">

**Leistungsstarke Android-App zur Weiterleitung von App-Traffic durch einen Proxy**

*Steuern Sie den Netzwerkverkehr einzelner Apps mit iptables*

[🐛 Fehler melden](https://github.com/RiddlerXenon/app2proxy/issues) • [💡 Funktion vorschlagen](https://github.com/RiddlerXenon/app2proxy/issues)

<a href="https://apt.izzysoft.de/fdroid/index/apk/dev.rx.app2proxy">
    <img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" alt="Bei IzzyOnDroid herunterladen" height="80">
</a>
</div>

---

[🇬🇧 Englisch](README.md) | [🇷🇺 Russisch](README_ru.md)

---

## ✨ Funktionen

### 🎯 **Präzise Traffic-Steuerung**
- **Selektive Weiterleitung** – wählen Sie bestimmte Apps für die Proxy-Nutzung aus
- **Automatische Wiederherstellung** der iptables-Regeln nach dem Neustart
- **Unterstützung von System-Apps** im erweiterten Modus
- **DNS-Verwaltung** – Weiterleitung von DNS-Anfragen an Port 10853

### 🔧 **Erweiterte Möglichkeiten**
- **Material 3 Design** mit Dark Theme und AMOLED-Unterstützung
- **Autostart**-Unterstützung für Android 15+ und verschiedene Hersteller
- **Autostart-Diagnose** zur Fehlerbehebung
- **Regelmanager** zum Anzeigen und Verwalten aktiver Regeln

### 🛡️ **Sicherheit & Stabilität**
- **Sichere Befehlsausführung** mit Root-Zugriffsprüfung
- **Fehlerbehandlung** zur Vermeidung von Systemabstürzen
- **Protokollierung** für Debugging und Diagnose
- **Benachrichtigungen** über den Status der Dienste

---

## 🚀 Schnellstart

### Anforderungen
- **Android 13+** (API 33)
- **Root-Zugriff** für iptables
- **Xray/V2Ray** oder anderer Proxy-Server, der auf den Ports läuft:
  - `12345` – Haupttraffic
  - `10853` – DNS-Anfragen

### Erste Nutzung

1. 🔍 **Wählen Sie Apps** aus der Liste zur Weiterleitung aus
2. ⚡ **Klicken Sie auf "Anwenden"**, um iptables-Regeln zu aktivieren
3. 🔄 **Autostart konfigurieren**, um Regeln nach Neustart wiederherzustellen
4. ✅ **Überprüfen Sie die Funktion** im Abschnitt "Regeln"

---

## ⚙️ Konfiguration

### Standard-Ports

```kotlin
private const val XRAY_PORT = 12345      // Haupttraffic
private const val XRAY_DNS_PORT = 10853  // DNS-Anfragen
```

### Proxy-Server-Konfiguration

Stellen Sie sicher, dass Ihr Xray/V2Ray-Server wie folgt konfiguriert ist:

```json
{
  "inbounds": [
    {
      "port": 12345,
      "protocol": "dokodemo-door",
      "settings": {
        "network": "tcp,udp",
        "followRedirect": true
      }
    },
    {
      "port": 10853,
      "protocol": "dokodemo-door",
      "settings": {
        "network": "udp",
        "port": 53
      }
    }
  ]
}
```

---

## 🎨 Interface-Funktionen

### Material 3 Design
- **Dynamische Farben** (Android 12+)
- **Dunkles Theme** mit AMOLED-Unterstützung
- **Adaptive Navigation** mit ViewPager2
- **Material-Komponenten** für modernes UX

### Anpassungsmöglichkeiten
- 🌙 **Dunkles/Helles Theme**
- 🖤 **AMOLED-Modus** für Akkusparen
- 🎨 **Material You** auf Android 12+
- 📱 **Adaptives Interface**

---

## 🛠️ Entwicklung

### Technologiestack

| Komponente      | Technologie         |
|-----------------|--------------------|
| **Sprache**     | Kotlin 100%        |
| **UI**          | Material 3, ViewBinding |
| **Architektur** | Fragment-basierte Navigation |
| **Async**       | Coroutines + Dispatchers |
| **Speicherung** | SharedPreferences  |
| **DI**          | Manuelle Dependency Injection |

### Projekt bauen

```bash
# Repository klonen
git clone https://github.com/RiddlerXenon/app2proxy.git
cd app2proxy

# In Android Studio oder per Gradle bauen
./gradlew assembleDebug

# Auf Gerät installieren
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Entwicklungsanforderungen

- **Android Studio** Arctic Fox oder neuer
- **Kotlin** 1.9.24+
- **Android Gradle Plugin** 8.4.0+
- **Compile SDK** 36 (Android 15)
- **Target SDK** 36

---

## 🐛 Fehlerbehebung

### Häufige Probleme

<details>
<summary><strong>🚫 Regeln werden nach Neustart nicht angewendet</strong></summary>

**Lösung:**
1. Überprüfen Sie Autostart-Berechtigungen in den Systemeinstellungen
2. Stellen Sie sicher, dass die App nicht akkuoptimiert wird
3. Aktivieren Sie die Diagnose in den App-Einstellungen
4. Für Android 15+ nutzen Sie die manuelle Wiederherstellung in den Einstellungen

</details>

<details>
<summary><strong>⚠️ Kein Root-Zugriff</strong></summary>

**Lösung:**
1. Stellen Sie sicher, dass das Gerät Root-Rechte hat
2. Installieren Sie die neueste Version von Magisk oder SuperSU
3. Gewähren Sie der App Superuser-Rechte
4. Prüfen Sie den Befehl `su` im Terminal

</details>

<details>
<summary><strong>🔄 Proxy funktioniert nicht</strong></summary>

**Lösung:**
1. Prüfen Sie, ob der Proxy-Server auf den Ports 12345 und 10853 läuft
2. Überprüfen Sie die Konfiguration von Xray/V2Ray
3. Prüfen Sie die App-Logs auf iptables-Fehler
4. Nutzen Sie `iptables -t nat -L` zum Prüfen der Regeln

</details>

---

## 🤝 Beitrag leisten

Beiträge sind willkommen!

### Wie helfen

1. 🍴 **Repository forken**
2. 🌟 **Branch für neue Funktion erstellen**
3. 💻 **Änderungen kommentiert einbringen**
4. ✅ **Auf verschiedenen Android-Versionen testen**
5. 📝 **Pull Request mit Beschreibung erstellen**

---

## 📄 Lizenz

Dieses Projekt steht unter der **GNU General Public License v3.0**.

Siehe [LICENSE](LICENSE) für Details.

---

## ⭐ Projekt unterstützen

Wenn App2Proxy hilfreich war, unterstützen Sie das Projekt:

- ⭐ **GitHub-Stern vergeben**
- 🐛 **Fehler in Issues melden**
- 💡 **Verbesserungen vorschlagen**
- 🔄 **Mit Freunden teilen**

---

## 📞 Kontakt

- **GitHub**: [@RiddlerXenon](https://github.com/RiddlerXenon)
- **Issues**: [Issue erstellen](https://github.com/RiddlerXenon/app2proxy/issues)

---

<div align="center">

**Mit ❤️ für die Android-Community gemacht**

[![GitHub](https://img.shields.io/badge/GitHub-RiddlerXenon-black?logo=github)](https://github.com/RiddlerXenon)

</div>
