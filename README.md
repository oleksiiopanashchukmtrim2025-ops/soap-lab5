# Лабораторна робота №4

## SOAP Chat Client

## Опис

У цій лабораторній роботі реалізовано клієнтський застосунок для взаємодії з SOAP веб-сервісом на Java.

Клієнт використовує WSDL-документ для генерації proxy-класів через `wsimport` і дозволяє виконувати віддалені виклики до сервера.

## Реалізовані можливості

* `ping` — перевірка з’єднання
* `echo <text>` — тестова передача тексту
* `login <user> <password>` — авторизація
* `list` — перегляд активних користувачів
* `msg <user> <text>` — надсилання повідомлення
* `file <user> <path>` — надсилання файлу
* приймання повідомлень і файлів через таймер
* `exit` — завершення роботи клієнта

## Необхідні умови

Перед запуском потрібно мати:

* **JDK 8**
* запущений SOAP сервер
* доступну команду `wsimport`

## Порядок запуску

### 1. Запустити SOAP сервер

У папці `server.soap` запустити:

```bash
start.cmd
```

Після цього WSDL має бути доступний за адресою:

```text
http://localhost:153/chat?wsdl
```

### 2. Згенерувати SOAP proxy-класи

У папці проєкту виконати команду:

```bash
wsimport http://localhost:153/chat?wsdl -Xnocompile -keep -s src -p soap
```

Якщо `wsimport` не доданий у PATH, можна запускати повним шляхом, наприклад:

```bash
"C:\Program Files\Eclipse Adoptium\jdk-8.0.482.8-hotspot\bin\wsimport.exe" http://localhost:153/chat?wsdl -Xnocompile -keep -s src -p soap
```

### 3. Запустити клієнт

Відкрити проєкт у IntelliJ IDEA і запустити:

```text
Main.java
```

## Доступні команди

```text
ping
echo <text>
login <user> <password>
list
msg <user> <text>
file <user> <path>
exit
```

