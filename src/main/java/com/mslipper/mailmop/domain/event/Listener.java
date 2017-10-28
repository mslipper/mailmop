package com.mslipper.mailmop.domain.event;

public interface Listener<T extends Event> {
    default void onSuccess(T result) {}

    default void onFailure(Throwable e) {}
}
