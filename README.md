<div align="center">

# 🚀 App2Proxy

<img src="https://img.shields.io/badge/Android-13%2B-green?logo=android&logoColor=white" alt="Android 13+">
<img src="https://img.shields.io/badge/Kotlin-100%25-purple?logo=kotlin&logoColor=white" alt="Kotlin">
<img src="https://img.shields.io/badge/License-GPL%20v3-blue?logo=gnu&logoColor=white" alt="License">

**Powerful Android app to redirect app traffic through a proxy**

*Control network traffic of individual apps using iptables*

[🐛 Report a bug](https://github.com/RiddlerXenon/app2proxy/issues) • [💡 Suggest a feature](https://github.com/RiddlerXenon/app2proxy/issues)

<a href="https://apt.izzysoft.de/fdroid/index/apk/dev.rx.app2proxy">
    <img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" alt="Get on IzzyOnDroid" height="80">
</a>
</div>

---

[🇷🇺 Russian](README_ru.md) | [🇩🇪 German](README_de.md)

---

## ✨ Features

### 🎯 **Precise Traffic Control**
- **Selective redirection** – choose specific apps for proxying
- **Automatic restoration** of iptables rules after reboot
- **System app support** with advanced mode
- **DNS management** – redirect DNS queries to port 10853

### 🔧 **Advanced Capabilities**
- **Material 3 Design** with dark theme and AMOLED support
- **Autostart** support for Android 15+ and various vendors
- **Autostart diagnostics** to troubleshoot issues
- **Rule manager** for viewing and controlling active rules

### 🛡️ **Security & Stability**
- **Safe command execution** with root access verification
- **Error handling** to prevent system crashes
- **Logging** for debugging and diagnostics
- **Service state notifications**

---

## 🚀 Quick Start

### Requirements
- **Android 13+** (API 33)
- **Root access** to work with iptables
- **Xray/V2Ray** or other proxy server running on ports:
  - `12345` – main traffic
  - `10853` – DNS queries

### First Use

1. 🔍 **Select apps** in the list to redirect their traffic
2. ⚡ **Tap "Apply"** to activate iptables rules
3. 🔄 **Set up autostart** to restore rules after reboot
4. ✅ **Check operation** in the "Rules" section

---

## ⚙️ Configuration

### Default Ports

```kotlin
private const val XRAY_PORT = 12345      // Main traffic
private const val XRAY_DNS_PORT = 10853  // DNS queries
```

### Proxy Server Setup

Ensure your Xray/V2Ray server is configured to listen:

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

## 🎨 Interface Features

### Material 3 Design
- **Dynamic colors** (Android 12+)
- **Dark theme** with AMOLED support
- **Adaptive navigation** with ViewPager2
- **Material components** for a modern UX

### Customization Options
- 🌙 **Dark/Light theme**
- 🖤 **AMOLED mode** for battery saving
- 🎨 **Material You** on Android 12+
- 📱 **Adaptive UI**

---

## 🛠️ Development

### Tech Stack

| Component      | Technology         |
|----------------|-------------------|
| **Language**   | Kotlin 100%       |
| **UI**         | Material 3, ViewBinding |
| **Architecture** | Fragment-based navigation |
| **Async**      | Coroutines + Dispatchers |
| **Storage**    | SharedPreferences |
| **DI**         | Manual dependency injection |

### Build Project

```bash
# Clone repository
git clone https://github.com/RiddlerXenon/app2proxy.git
cd app2proxy

# Build in Android Studio or via Gradle
./gradlew assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Development Requirements

- **Android Studio** Arctic Fox or newer
- **Kotlin** 1.9.24+
- **Android Gradle Plugin** 8.4.0+
- **Compile SDK** 36 (Android 15)
- **Target SDK** 36

---

## 🐛 Troubleshooting

### Common Issues

<details>
<summary><strong>🚫 Rules do not apply after reboot</strong></summary>

**Solution:**
1. Check autostart permissions in system settings
2. Ensure the app is not battery optimized
3. Enable diagnostics in app settings
4. For Android 15+ use manual restore in settings

</details>

<details>
<summary><strong>⚠️ No root access</strong></summary>

**Solution:**
1. Ensure the device has root rights
2. Install the latest Magisk or SuperSU version
3. Grant superuser rights to the app
4. Check the `su` command in terminal

</details>

<details>
<summary><strong>🔄 Proxy does not work</strong></summary>

**Solution:**
1. Check that the proxy server is running on ports 12345 and 10853
2. Verify Xray/V2Ray configuration
3. Check app logs for iptables errors
4. Use `iptables -t nat -L` to check rules

</details>

---

## 🤝 Contributing

We welcome contributions!

### How to Help

1. 🍴 **Fork** the repository
2. 🌟 **Create a branch** for a new feature
3. 💻 **Make changes** with detailed comments
4. ✅ **Test** on different Android versions
5. 📝 **Create a Pull Request** describing your changes

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0**.

See [LICENSE](LICENSE) for details.

---

## ⭐ Support the Project

If App2Proxy helped you, support the project:

- ⭐ **Star on GitHub**
- 🐛 **Report bugs** in Issues
- 💡 **Suggest improvements**
- 🔄 **Share** with friends

---

## 📞 Contacts

- **GitHub**: [@RiddlerXenon](https://github.com/RiddlerXenon)
- **Issues**: [Create issue](https://github.com/RiddlerXenon/app2proxy/issues)

---

<div align="center">

**Made with ❤️ for the Android community**

[![GitHub](https://img.shields.io/badge/GitHub-RiddlerXenon-black?logo=github)](https://github.com/RiddlerXenon)

</div>
