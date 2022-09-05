package net.countercraft.movecraft.repair.types;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.ArrayDeque;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.commons.lang.NotImplementedException;
import org.jetbrains.annotations.Nullable;

import net.countercraft.movecraft.repair.tasks.RepairTask;

public class RepairQueue implements Queue<RepairTask> {
    Queue<RepairTask> remaining = new PriorityQueue<>(new RepairComparator());
    Deque<RepairTask> blocked = new ArrayDeque<>();

    @Override
    public int size() {
        return remaining.size() + blocked.size();
    }

    @Override
    public boolean isEmpty() {
        return remaining.isEmpty() && blocked.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return remaining.contains(o) || blocked.contains(o);
    }

    private boolean isReady(RepairTask task) {
        RepairTask dependency = task.getDependency();
        if (dependency == null)
            return true;

        return dependency.isDone();
    }

    @Override
    @Nullable
    public RepairTask poll() {
        // Check the first element of the blocked queue
        RepairTask element = blocked.poll();
        if (element != null) {
            if (isReady(element)) {
                // Element is ready, return it
                return element;
            }

            // Else, add item back to the end of the blocked queue
            blocked.add(element);
        }

        // Else, look on the remaining queue
        while (!remaining.isEmpty()) {
            element = remaining.poll();
            if (element == null)
                break; // End of queue

            if (isReady(element)) {
                return element;
            } else {
                blocked.add(element);
            }
        }

        // If the remaining queue is done, traverse the dependencies of the first item
        element = blocked.peek();
        if (element == null)
            return null; // End of queue

        while (!isReady(element)) {
            element = element.getDependency();
        }
        blocked.remove(element);
        return element;
    }

    @Override
    public boolean add(RepairTask e) {
        return remaining.add(e);
    }

    @Override
    public boolean addAll(Collection<? extends RepairTask> c) {
        return remaining.addAll(c);
    }

    @Override
    public void clear() {
        remaining.clear();
        blocked.clear();
    }

    @Override
    public Iterator<RepairTask> iterator() {
        throw new NotImplementedException();
    }

    @Override
    public Object[] toArray() {
        throw new NotImplementedException();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new NotImplementedException();
    }

    @Override
    public boolean remove(Object o) {
        throw new NotImplementedException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new NotImplementedException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new NotImplementedException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new NotImplementedException();
    }

    @Override
    public boolean offer(RepairTask e) {
        throw new NotImplementedException();
    }

    @Override
    public RepairTask remove() {
        throw new NotImplementedException();
    }

    @Override
    public RepairTask element() {
        throw new NotImplementedException();
    }

    @Override
    public RepairTask peek() {
        throw new NotImplementedException();
    }
}
