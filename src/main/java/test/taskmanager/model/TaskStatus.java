package test.taskmanager.model;

public enum TaskStatus {
    CREATED("Созданна"),
    IN_PROGRESS("В работе"),
    COMPLETE("Завершенна"),
    CANCELED("Отменнена");

    private final String description;

    TaskStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
