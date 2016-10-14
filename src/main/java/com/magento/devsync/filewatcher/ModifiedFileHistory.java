package com.magento.devsync.filewatcher;

import java.util.ArrayList;
import java.util.HashSet;

public class ModifiedFileHistory {

    private static final int EXPIRY_SECONDS = 5;
    private long baseTime;
    private ArrayList<HashSet<String> > history = new ArrayList<>();
    private String inProgress;

    public ModifiedFileHistory() {
        for (int i = 0; i < EXPIRY_SECONDS; i++) {
            history.add(new HashSet<String>());
        }
        baseTime = System.currentTimeMillis() / 1000;
    }

    synchronized public void startingToWrite(String path) {
        inProgress = path;
    }

    synchronized public void writingCompleted() {
        history.get(0).add(inProgress);
        inProgress = null;
    }

    synchronized public boolean beingWrittenTo(String path) {
        return inProgress != null && inProgress.equals(path); 
    }

    synchronized public boolean contains(String path) {
        removeExpiredEntries();
        for (HashSet<String> timeSlice : history) {
            if (timeSlice.contains(path)) {
                return true;
            }
        }
        return false;
    }

    public void removeExpiredEntries() {
        long current = System.currentTimeMillis() / 1000;
        if (current > baseTime + EXPIRY_SECONDS) {
            // Long time ago, just wipe the history completely.
            for (HashSet<String> timeSlice : history) {
                timeSlice.clear();
            }
            baseTime = current;
        } else {
            // Move items along the expiry queue.
            while (baseTime < current) {
                HashSet<String> tail = history.remove(history.size() - 1);
                tail.clear();
                history.add(0, tail);
                baseTime++;
            }
        }
    }
}
