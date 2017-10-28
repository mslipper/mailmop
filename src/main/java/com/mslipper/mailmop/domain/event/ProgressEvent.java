package com.mslipper.mailmop.domain.event;

public class ProgressEvent implements Event {
    private final boolean isDeterminate;

    private final int totalItems;

    private final int processedItems;

    private ProgressEvent(boolean isDeterminate, int totalItems, int processedItems) {
        this.isDeterminate = isDeterminate;
        this.totalItems = totalItems;
        this.processedItems = processedItems;
    }

    public boolean isDeterminate() {
        return isDeterminate;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getProcessedItems() {
        return processedItems;
    }

    public double asPercentage() {
        return ((double) processedItems) / totalItems;
    }

    public static class Factory {
        private final boolean isDeterminate;

        private final int totalItems;

        private Factory(boolean isDeterminate, int totalItems) {
            this.isDeterminate = isDeterminate;
            this.totalItems = totalItems;
        }

        public ProgressEvent create(int processedItems) {
            if (isDeterminate) {
                return new ProgressEvent(true, totalItems, processedItems);
            }

            return new ProgressEvent(false, 0, processedItems);
        }

        public static Factory determinate(int totalItems) {
            return new Factory(true, totalItems);
        }

        public static Factory indeterminate() {
            return new Factory(false, 0);
        }
    }
}
