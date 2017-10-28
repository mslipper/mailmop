package com.mslipper.mailmop.domain.event;

public interface ProgressListener<T extends Event> extends Listener<T> {
    default void onProgress(ProgressEvent event) {}
}
