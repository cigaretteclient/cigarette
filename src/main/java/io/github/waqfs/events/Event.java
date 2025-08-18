package io.github.waqfs.events;

public class Event<T> {
    protected final T data;

    public Event(T data) {
        this.data = data;
    }

    public static <T> Event<T> create(T data) {
        return new Event<>(data);
    }

    public T getData() {
        return data;
    }
}

