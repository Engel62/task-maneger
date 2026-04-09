Task Manager Service

Сервис управления задачами с REST API, пагинацией, асинхронной отправкой событий в Kafka и полной контейнеризацией.
Проект написан на Java 21 с использованием Spring Boot 4, PostgreSQL, Kafka и Docker Compose.

📋 Функциональность
✅ Создание задачи

✅ Получение задачи по ID

✅ Получение списка задач с пагинацией

✅ Назначение исполнителя задаче (с отправкой события в Kafka)

✅ Смена статуса задачи (CREATED, IN_PROGRESS, COMPLETED, CANCELLED)

✅ Асинхронная отправка событий в Kafka:

task-created – при создании задачи

executor-assigned – при назначении исполнителя

✅ Обработка ошибок и валидация входных данных

✅ Юнит- и интеграционные тесты (JUnit 5, Mockito, Testcontainers)

✅ Готов к нагрузке: до 10 000 пользователей и 100 000 задач

🛠 Технологический стек
Категория	Технологии
Язык	Java 21
Framework	Spring Boot 4, Spring Data JPA, Spring Kafka, Spring Web
БД	PostgreSQL 15, H2 (для тестов)
Брокер сообщений	Apache Kafka (через Spring Kafka)
Миграции	Hibernate DDL auto (update)
Сборка	Maven
Контейнеризация	Docker, Docker Compose
Тестирование	JUnit 5, Mockito, Testcontainers, Spring Boot Test
Логирование	SLF4J + Logback
🚀 Быстрый старт
Требования
Java 21 (JDK)

Docker & Docker Compose

Maven (опционально, можно использовать mvnw)

1. Клонирование репозитория
   bash
   git clone https://github.com/Engel62/task-maneger.git
   cd task-maneger
2. Запуск через Docker Compose (рекомендуется)
   bash
   docker-compose up --build -d
   Приложение будет доступно по адресу: http://localhost:8080

3. Локальный запуск (без Docker)
   Запустите PostgreSQL и Kafka вручную или через Docker:

bash
docker-compose up -d postgres zookeeper kafka
Затем запустите приложение:

bash
mvn spring-boot:run
📦 Конфигурация
Основной файл конфигурации – src/main/resources/application.yml.
Для Docker-среды используется профиль docker (файл application-docker.yml).

Переменные окружения (можно переопределить):

Переменная	Значение по умолчанию
SPRING_DATASOURCE_URL	jdbc:postgresql://localhost:5432/taskmanager
SPRING_DATASOURCE_USERNAME	taskuser
SPRING_DATASOURCE_PASSWORD	taskpass
SPRING_KAFKA_BOOTSTRAP_SERVERS	localhost:9092
SPRING_JPA_HIBERNATE_DDL_AUTO	update
🔌 API Эндпоинты
Базовый URL: http://localhost:8080/api/v1/tasks

Метод	Эндпоинт	Описание	Тело запроса
POST	/	Создать задачу	TaskRequest
GET	/{id}	Получить задачу по ID	–
GET	/	Получить все задачи (пагинация)	–
PUT	/{id}/assign	Назначить исполнителя	AssignExecutorRequest
PATCH	/{id}/status	Обновить статус задачи	StatusUpdateRequest
Примеры запросов
Создание задачи

bash
curl -X POST http://localhost:8080/api/v1/tasks \
-H "Content-Type: application/json" \
-d '{"name":"Реализовать API","description":"Документировать эндпоинты"}'
Получение задач с пагинацией (страница 0, размер 10, сортировка по createdAt DESC)

bash
curl "http://localhost:8080/api/v1/tasks?page=0&size=10&sort=createdAt,desc"
Назначение исполнителя

bash
curl -X PUT http://localhost:8080/api/v1/tasks/1/assign \
-H "Content-Type: application/json" \
-d '{"executorId":1}'
Обновление статуса

bash
curl -X PATCH http://localhost:8080/api/v1/tasks/1/status \
-H "Content-Type: application/json" \
-d '{"status":"IN_PROGRESS"}'
🐳 Docker Compose
Сервисы, поднимаемые docker-compose.yml:

postgres – база данных

zookeeper – координатор Kafka

kafka – брокер сообщений

app – само приложение (собирается из Dockerfile)

Команды управления:

bash
docker-compose up -d          # запуск в фоне
docker-compose logs -f app    # просмотр логов приложения
docker-compose down -v        # остановка и удаление томов (очистка данных)
🧪 Тестирование
Запуск всех тестов
bash
mvn test
Покрытие
Модульные тесты сервисов (TaskServiceTest, KafkaEventServiceTest) с использованием Mockito.

Интеграционные тесты с Testcontainers (PostgreSQL и Kafka поднимаются в Docker).

Тесты контроллеров (можно добавить @WebMvcTest при необходимости).

📁 Структура проекта
text
src/
├── main/
│   ├── java/test/taskmanager/
│   │   ├── config/               # Kafka, Jackson конфигурации
│   │   ├── controller/           # REST контроллеры
│   │   ├── dto/                  # Data Transfer Objects
│   │   ├── event/                # События Kafka
│   │   ├── exception/            # Кастомные исключения и глобальный обработчик
│   │   ├── model/                # JPA сущности (Task, User, TaskStatus)
│   │   ├── repository/           # Spring Data JPA репозитории
│   │   └── service/              # Бизнес-логика и Kafka сервис
│   └── resources/
│       ├── application.yml       # Основной конфиг
│       ├── application-docker.yml# Профиль для Docker
│       └── data.sql              # Начальные данные (пользователи)
├── test/                         # Юнит и интеграционные тесты
├── docker-compose.yml
├── Dockerfile
└── pom.xml
🧠 Архитектурные решения
Многоуровневая архитектура: Controller → Service → Repository → БД.

DTO: изоляция внутренних моделей от API.

Асинхронная отправка событий: после сохранения в БД отправляется сообщение в Kafka без блокировки.

Транзакционность: @Transactional для целостности данных.

Пагинация на уровне БД: Spring Data Pageable с OFFSET/LIMIT.

Lazy fetching: для связи Task.executor (не подгружается, пока не нужно).

Docker мультистейдж: минимизация размера образа.

🔧 Возможные улучшения (Roadmap)
Добавить аутентификацию и авторизацию (Spring Security + JWT)

Индексы в БД для полей executor_id, status, created_at

Кэширование (Redis) для часто запрашиваемых задач

Retry и Dead Letter Queue для Kafka

Метрики (Micrometer + Prometheus + Grafana)

OpenAPI (Swagger) документация