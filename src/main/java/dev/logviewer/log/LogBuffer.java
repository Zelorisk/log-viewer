package dev.logviewer.log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

public class LogBuffer {
    private static final LogBuffer INSTANCE = new LogBuffer(2000);

    private final LogEntry[] buffer;
    private final int capacity;
    private int head = 0;
    private int size = 0;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public LogBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new LogEntry[capacity];
    }

    public static LogBuffer getInstance() {
        return INSTANCE;
    }

    public void add(LogEntry entry) {
        lock.writeLock().lock();
        try {
            buffer[head] = entry;
            head = (head + 1) % capacity;
            if (size < capacity) {
                size++;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<LogEntry> getAll() {
        lock.readLock().lock();
        try {
            List<LogEntry> result = new ArrayList<>(size);
            int start = (head - size + capacity) % capacity;
            for (int i = 0; i < size; i++) {
                result.add(buffer[(start + i) % capacity]);
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<LogEntry> getFiltered(Predicate<LogEntry> filter) {
        lock.readLock().lock();
        try {
            List<LogEntry> result = new ArrayList<>();
            int start = (head - size + capacity) % capacity;
            for (int i = 0; i < size; i++) {
                LogEntry entry = buffer[(start + i) % capacity];
                if (filter.test(entry)) {
                    result.add(entry);
                }
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            head = 0;
            size = 0;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int size() {
        lock.readLock().lock();
        try {
            return size;
        } finally {
            lock.readLock().unlock();
        }
    }
}
