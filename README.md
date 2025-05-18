# Log Processing Pipeline

Две Spring Boot-приложения для приёма XML, сохранения «сырых» логов и пакетного переноса записей:

1. **log-receiver**
   HTTP‑сервер на порту **9091**, принимает XML, конвертирует в JSON и пишет в каталог `logs/`
   Формат файла: `<Type>-YYYY-MM-DD.log`, первая строка — счётчик записей, далее — JSON‑строки.

2. **log-batcher**
   CLI‑утилита, читающая `logs/*.log`, берёт записи пачками по 100 и сохраняет в
   `<Type>-YYYY-MM-DD-0001.log`, `<Type>-YYYY-MM-DD-0002.log`…
   Хранит файл `batch.offset` с числом уже упакованных строк и продолжает с места после перезапуска.

---

## 📁 Структура репозитория

```text
log-project/                ← parent POM
├── log-receiver/           ← приёмник XML
│   ├── src/
│   └── pom.xml
├── log-batcher/            ← пакетировщик логов
│   ├── src/
│   └── pom.xml
└── pom.xml                 ← parent module
```

---

## ⚙️ Требования

* Java 21 (или новее)
* Maven 3.8+
* (Опционально) Docker, Docker Compose

---

## 🔧 Конфигурация

В каждом модуле в `src/main/resources/application.yml`:

```yaml
log-dir: ../logs     # общий каталог логов (относительно корня проекта или абсолютный)
```

Дополнительно для **log-receiver**:

```yaml
server:
  port: 9091
```

И для **log-batcher**:

```yaml
batch:
  size: 100   # размер пачки, по умолчанию 100
```

Можно переопределять через переменные окружения или параметры командной строки:

```bash
export LOG_DIR=/var/app/logs
export BATCH_SIZE=200
export SERVER_PORT=8080   # для log-receiver
```

---

## 🚀 Сборка и запуск

Из корня `log-project`:

```bash
# Сборка обоих модулей\mvn clean package
```

### 1. Запуск log-receiver

```bash
mvn -pl log-receiver spring-boot:run
```

* Приложение слушает `http://localhost:9091/`
* Отправьте тестовое XML (Postman, curl):

  ```bash
  curl -X POST http://127.0.0.1:9091/ \
       -H "Content-Type: application/xml" \
       --data @sample.xml
  ```
* В каталоге `logs/` появится файл `Information-YYYY-MM-DD.log`

### 2. Запуск log-batcher

```bash
mvn -pl log-batcher spring-boot:run
```

* Приложение найдёт все `*.log` в `logs/`, пропустит первую строку (счётчик)
* Сгенерирует файлы `*-0001.log`, `*-0002.log`… по `batch.size` записей
* Обновит `logs/batch.offset`

---


---

## ✅ Проверка (End-to-End)

1. Запустили `log-receiver` → отправили 5 XML → получили
   `Information-YYYY-MM-DD.log` с 5 записями.
2. Запустили `log-batcher` → в `logs/` появились
   `Information-YYYY-MM-DD-0001.log` и `batch.offset=5`.
3. Добавьте ещё >100 записей и убедитесь, что появятся `-0002.log`.

---

## 🧪 Тестирование

**log-receiver**:

* MockMvc-тест: отправка XML, проверка HTTP 200 и создания файла с корректным счётчиком и JSON-записью.

**log-batcher**:

* JUnit5 тест с `@TempDir`: подготовка `Information-YYYY-MM-DD.log` с 150 строк, запуск `BatchProcessor`, проверка создания 15 файлов по 10 строк и обновлённого `batch.offset=150`.

Запуск тестов:

```bash
mvn test
```

---

## Контакты

Если есть вопросы или предложения — пишите на [email@example.com](mailto:email@example.com)
GitHub: [https://github.com/username/log-project](https://github.com/username/log-project)
