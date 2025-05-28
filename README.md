<div align="center">

# 🚀 App2Proxy

<img src="https://img.shields.io/badge/Android-13%2B-green?logo=android&logoColor=white" alt="Android 13+">
<img src="https://img.shields.io/badge/Kotlin-100%25-purple?logo=kotlin&logoColor=white" alt="Kotlin">
<img src="https://img.shields.io/badge/License-GPL%20v3-blue?logo=gnu&logoColor=white" alt="License">

**Мощное Android-приложение для перенаправления трафика приложений через прокси**

*Управляйте сетевым трафиком отдельных приложений с помощью iptables*

[🐛 Сообщить об ошибке](https://github.com/RiddlerXenon/app2proxy/issues) • [💡 Предложить функцию](https://github.com/RiddlerXenon/app2proxy/issues)

<a href="https://apt.izzysoft.de/fdroid/index/apk/dev.rx.app2proxy">
    <img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" alt="Получить в IzzyOnDroid" height="80">
</a>
</div>

---

## ✨ Особенности

### 🎯 **Точное управление трафиком**
- **Выборочное перенаправление** - выбирайте конкретные приложения для проксирования
- **Автоматическое восстановление** правил iptables после перезагрузки
- **Поддержка системных приложений** с расширенным режимом
- **Управление DNS** - перенаправление DNS-запросов на порт 10853

### 🔧 **Продвинутые возможности**
- **Material 3 Design** с поддержкой темной темы и AMOLED
- **Автозапуск** с поддержкой Android 15+ и различных производителей
- **Диагностика автозагрузки** для устранения проблем
- **Менеджер правил** для просмотра и управления активными правилами

### 🛡️ **Безопасность и стабильность**
- **Безопасное выполнение** команд с проверкой root-доступа
- **Обработка ошибок** для предотвращения сбоев системы
- **Логирование** для отладки и диагностики
- **Уведомления** о состоянии работы сервисов

---

## 🚀 Быстрый старт

### Требования
- **Android 13+** (API 33)
- **Root-доступ** для работы с iptables
- **Xray/V2Ray** или другой прокси-сервер, работающий на портах:
  - `12345` - основной трафик
  - `10853` - DNS-запросы

### Первое использование

1. 🔍 **Выберите приложения** в списке для перенаправления трафика
2. ⚡ **Нажмите "Применить"** для активации правил iptables
3. 🔄 **Настройте автозапуск** для восстановления правил после перезагрузки
4. ✅ **Проверьте работу** в разделе "Правила"

---

## ⚙️ Конфигурация

### Порты по умолчанию

```kotlin
private const val XRAY_PORT = 12345      // Основной трафик
private const val XRAY_DNS_PORT = 10853  // DNS-запросы
```

### Настройка прокси-сервера

Убедитесь, что ваш Xray/V2Ray сервер настроен на прослушивание:

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

## 🎨 Функции интерфейса

### Material 3 Design
- **Динамические цвета** (Android 12+)
- **Темная тема** с поддержкой AMOLED
- **Адаптивная навигация** с ViewPager2
- **Material компоненты** для современного UX

### Возможности настройки
- 🌙 **Темная/светлая тема**
- 🖤 **AMOLED режим** для экономии батареи
- 🎨 **Material You** на Android 12+
- 📱 **Адаптивный интерфейс**

---

## 🛠️ Разработка

### Технологический стек

| Компонент | Технология |
|-----------|------------|
| **Язык** | Kotlin 100% |
| **UI** | Material 3, ViewBinding |
| **Архитектура** | Fragment-based navigation |
| **Async** | Coroutines + Dispatchers |
| **Хранение** | SharedPreferences |
| **DI** | Manual dependency injection |

### Сборка проекта

```bash
# Клонирование репозитория
git clone https://github.com/RiddlerXenon/app2proxy.git
cd app2proxy

# Сборка в Android Studio или через Gradle
./gradlew assembleDebug

# Установка на устройство
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Требования для разработки

- **Android Studio** Arctic Fox или новее
- **Kotlin** 1.9.24+
- **Android Gradle Plugin** 8.4.0+
- **Compile SDK** 36 (Android 15)
- **Target SDK** 36

---

## 🐛 Устранение неполадок

### Частые проблемы

<details>
<summary><strong>🚫 Правила не применяются после перезагрузки</strong></summary>

**Решение:**
1. Проверьте права на автозапуск в настройках системы
2. Убедитесь, что приложение не оптимизируется батареей
3. Включите диагностику в настройках приложения
4. Для Android 15+ используйте ручное восстановление в настройках

</details>

<details>
<summary><strong>⚠️ Нет root-доступа</strong></summary>

**Решение:**
1. Убедитесь, что устройство получило root-права
2. Установите актуальную версию Magisk или SuperSU
3. Предоставьте права суперпользователя приложению
4. Проверьте работу команды `su` в терминале

</details>

<details>
<summary><strong>🔄 Прокси не работает</strong></summary>

**Решение:**
1. Проверьте, что прокси-сервер работает на портах 12345 и 10853
2. Убедитесь в правильности конфигурации Xray/V2Ray
3. Проверьте логи приложения на предмет ошибок iptables
4. Используйте команду `iptables -t nat -L` для проверки правил

</details>

---

## 🤝 Вклад в проект

Мы приветствуем вклад в развитие проекта! 

### Как помочь

1. 🍴 **Fork** репозитория
2. 🌟 **Создайте ветку** для новой функции
3. 💻 **Внесите изменения** с подробными комментариями
4. ✅ **Протестируйте** на разных версиях Android
5. 📝 **Создайте Pull Request** с описанием изменений

---

## 📄 Лицензия

Этот проект распространяется под лицензией **GNU General Public License v3.0**.

См. файл [LICENSE](LICENSE) для получения подробной информации.

---

## ⭐ Поддержка проекта

Если App2Proxy оказался полезным, поддержите проект:

- ⭐ **Поставьте звезду** на GitHub
- 🐛 **Сообщайте о багах** в Issues
- 💡 **Предлагайте улучшения**
- 🔄 **Делитесь** с друзьями

---

## 📞 Контакты

- **GitHub**: [@RiddlerXenon](https://github.com/RiddlerXenon)
- **Issues**: [Создать issue](https://github.com/RiddlerXenon/app2proxy/issues)

---

<div align="center">

**Сделано с ❤️ для Android сообщества**

[![GitHub](https://img.shields.io/badge/GitHub-RiddlerXenon-black?logo=github)](https://github.com/RiddlerXenon)

</div>
