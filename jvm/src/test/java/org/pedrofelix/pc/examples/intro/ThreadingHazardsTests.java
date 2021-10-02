package org.pedrofelix.pc.examples.intro;

import org.junit.Test;
import org.pedrofelix.pc.utils.TestUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ThreadingHazardsTests {

    // Number of repetitions performed by each thread
    private static final int N_OF_REPS = 1_000_000;

    private static final int N_OF_THREADS = 10;

    /**************************************************************************
     * This test illustrates the problem of mutating a shared integer,
     * namely the loss of increments.
     */
    private int simpleCounter = 0;
    @Test
    public void loosing_increments() {

        var ths = new ArrayList<Thread>(N_OF_THREADS);
        for (int i = 0; i < N_OF_THREADS; ++i) {
            var th = new Thread(() -> {
                // Note that this 'for' runs in different threads
                for (int j = 0; j < N_OF_REPS; ++j) {
                    ++simpleCounter;
                }
            });
            th.start();
            ths.add(th);
        }

        ths.forEach(TestUtils::uninterruptibleJoin);

        // notice that the assertion is NOT equals
        assertNotEquals(N_OF_THREADS * N_OF_REPS, simpleCounter);
    }


    /**************************************************************************
     * This test illustrates the problem of mutating a shared integer,
     * namely the loss of increments.
     * This happens even if the shared counter is marked as volatile,
     * which doesn't ensure atomicity of increments.
     */
    private volatile int volatileCounter = 0;
    @Test
    public void loosing_increments_even_with_volatile() {

        var ths = new ArrayList<Thread>(N_OF_THREADS);
        for (int i = 0; i < N_OF_THREADS; ++i) {
            var th = new Thread(() -> {
                for (int j = 0; j < N_OF_REPS; ++j) {
                    ++volatileCounter;
                }
            });
            th.start();
            ths.add(th);
        }

        ths.forEach(TestUtils::uninterruptibleJoin);

        // notice that the assertion is NOT equals
        assertNotEquals(N_OF_THREADS * N_OF_REPS, volatileCounter);
    }

    /**************************************************************************
     * This test illustrates how the shared counter can be correctly
     * implemented using an AtomicInteger
     */
    private final AtomicInteger atomicCounter = new AtomicInteger();
    @Test
    public void not_loosing_increments_with_atomic() {

        var ths = new ArrayList<Thread>(N_OF_THREADS);
        for (int i = 0; i < N_OF_THREADS; ++i) {
            var th = new Thread(() -> {
                for (int j = 0; j < N_OF_REPS; ++j) {
                    atomicCounter.incrementAndGet();
                }
            });
            th.start();
            ths.add(th);
        }

        ths.forEach(TestUtils::uninterruptibleJoin);

        // notice that here the assertion is equals
        assertEquals(N_OF_THREADS * N_OF_REPS, atomicCounter.get());
    }

    /**************************************************************************
     * This test is just one more illustration of the hazards associated to
     * mutable data sharing between threads, without proper synchronization.
     */
    public static class SimpleLinkedStack<T> {

        static class Node<T> {
            final T item;
            final Node<T> next;

            Node(T item, Node<T> next) {
                this.item = item;
                this.next = next;
            }
        }

        Node<T> head = null;

        void push(T value) {
            head = new Node<>(value, head);
        }

        Optional<T> pop() {
            Node<T> observedHead = head;
            if (observedHead == null) {
                return Optional.empty();
            }
            head = observedHead.next;
            return Optional.of(observedHead.item);
        }

        boolean isEmpty() {
            return head == null;
        }
    }

    private final SimpleLinkedStack<Integer> nonThreadSafelist = new SimpleLinkedStack<>();

    @Test
    public void loosing_items_on_a_linked_list() {

        var ths = new ArrayList<Thread>(N_OF_THREADS);
        for (int i = 0; i < N_OF_THREADS; ++i) {
            Thread th = new Thread(() -> {
                // producer
                for (int j = 0; j < N_OF_REPS; ++j) {
                    nonThreadSafelist.push(1);
                }
            });
            th.start();
            ths.add(th);
        }

        ths.forEach(TestUtils::uninterruptibleJoin);
        int acc = 0;
        while (!nonThreadSafelist.isEmpty()) {
            acc += nonThreadSafelist.pop().orElseThrow();
        }

        // notice that the assertion is NOT equals
        assertNotEquals(N_OF_THREADS * N_OF_REPS, acc);
    }

    /**************************************************************************
     * This test shows how the previous problem can be solved by adding
     * mutual exclusion to all methods.
     */
    static class SynchronizedLinkedStack<T> {

        private final SimpleLinkedStack<T> stack = new SimpleLinkedStack<>();
        private final Object lock = new Object();

        void push(T item) {
            synchronized (lock) {
                stack.push(item);
            }
        }

        Optional<T> pop() {
            synchronized (lock) {
                return stack.pop();
            }
        }

        boolean isEmpty() {
            synchronized (lock) {
                return stack.isEmpty();
            }
        }
    }

    private final SynchronizedLinkedStack<Integer> synchronizedLinkedStack = new SynchronizedLinkedStack<>();

    @Test
    public void not_loosing_items_on_a_synchronized_list() {

        var ths = new ArrayList<Thread>(N_OF_THREADS);
        for (int i = 0; i < N_OF_THREADS ; ++i) {

            var th = new Thread(() -> {
                // producer
                for (int j = 0; j < N_OF_REPS; ++j) {
                    synchronizedLinkedStack.push(1);
                }
            });
            th.start();
            ths.add(th);
        }

        ths.forEach(TestUtils::uninterruptibleJoin);

        int acc = 0;
        while (!synchronizedLinkedStack.isEmpty()) {
            acc += synchronizedLinkedStack.pop().orElseThrow();
        }
        assertEquals(N_OF_THREADS * N_OF_REPS, acc);
    }

    /**************************************************************************
     * This test illustrates another problem when sharing
     * mutable data structures, even if they have some data synchronization.
     * In this case, the problem is typically called a 'check-then-act' hazard
     * and happens because the shared state can change between
     * the 'check' and the 'act'
     */
    private final Map<Integer, AtomicInteger> map = Collections.synchronizedMap(new HashMap<>());
    @Test
    public void loosing_increments_with_a_synchronized_map_and_atomics() {

        var ths = new ArrayList<Thread>(N_OF_THREADS);
        for (int i = 0; i < N_OF_THREADS; ++i) {
            var th = new Thread(() -> {
                for (int j = 0; j < N_OF_REPS; ++j) {
                    // check-then-act
                    AtomicInteger data = map.get(j);
                    // check...
                    if (data == null) {
                        data = new AtomicInteger(1);
                        // ... then act
                        map.put(j, data);
                    } else {
                        data.incrementAndGet();
                    }
                }
            });
            th.start();
            ths.add(th);
        }

        ths.forEach(TestUtils::uninterruptibleJoin);

        int totalCount = map.values().stream()
                .map(AtomicInteger::get)
                .reduce(0, Integer::sum);

        // notice that the assertion is NOT equals
        assertNotEquals(N_OF_THREADS * N_OF_REPS, totalCount);
    }

    /**************************************************************************
     * This test illustrates how the previous problem can be solved by
     * providing an atomic increment operation, using a synchronized block
     * to ensure mutual exclusion.
     */
    static class SynchronizedMapCounter {

        static class MutableInt {
            private int value;

            MutableInt(int value) {
                this.value = value;
            }

            void increment() {
                value += 1;
            }

            int get() {
                return value;
            }
        }

        private final Map<Integer, MutableInt> map = new HashMap<>();
        private final Object lock = new Object();

        public void increment(int key) {
            synchronized (lock) {
                // Notice how the check-then-act is performed while holding the lock,
                // so that no other thread can observe or mutate the data-structure while
                // doing this composite operation
                // We say that the operation is "protected" by the lock
                MutableInt data = map.get(key);
                if (data == null) {
                    data = new MutableInt(1);
                    map.put(key, data);
                } else {
                    data.increment();
                }
            }
        }

        public List<Map.Entry<Integer, Integer>> toList() {
            synchronized (lock) {
                // Here we create and return a snapshot copy of the data structure contents
                // This way the caller can use that data structure without it being "disturbed" by mutations
                // in the instance data structure
                return map.entrySet().stream()
                        .map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue().get()))
                        .collect(Collectors.toList());
            }
        }
    }

    private final SynchronizedMapCounter synchronizedMapCounter = new SynchronizedMapCounter();

    @Test
    public void not_loosing_increments_with_a_synchronized_class() {

        var ths = new ArrayList<Thread>(N_OF_THREADS);
        for (int i = 0; i < N_OF_THREADS; ++i) {
            var th = new Thread(() -> {
                for (int j = 0; j < N_OF_REPS; ++j) {
                    synchronizedMapCounter.increment(j);
                }
            });
            th.start();
            ths.add(th);
        }

        ths.forEach(TestUtils::uninterruptibleJoin);

        int totalCount = synchronizedMapCounter.toList().stream()
                .map(Map.Entry::getValue)
                .reduce(0, Integer::sum);

        assertEquals(N_OF_THREADS * N_OF_REPS, totalCount);
    }

    /**************************************************************************
     * This test illustrates how the same problem can be solved by using a
     * ConcurrentHashMap and the computeIfAbsent that ensures that no more than
     * one value is inserted for the same key.
     * Note that the problem requires the Map to expose a different method
     * - computeIfAbsent. Just providing thread-safe put and get is not enough,
     * because the 'check-then-act' problem still exists in that case.
     */
    private static final ConcurrentHashMap<Integer, AtomicInteger> concurrentMap = new ConcurrentHashMap<>();

    @Test
    public void not_loosing_increments_with_a_concurrent_map() {

        var ths = new ArrayList<Thread>(N_OF_THREADS);
        for (int i = 0; i < N_OF_THREADS; ++i) {
            var th = new Thread(() -> {
                for (int j = 0; j < N_OF_REPS; ++j) {
                    int key = j;
                    concurrentMap.computeIfAbsent(key, ignore -> new AtomicInteger(0)).incrementAndGet();
                }
            });
            th.start();
            ths.add(th);
        }

        ths.forEach(TestUtils::uninterruptibleJoin);

        int totalCount = concurrentMap.values().stream()
                .map(AtomicInteger::get)
                .reduce(0, Integer::sum);

        assertEquals(N_OF_THREADS * N_OF_REPS, totalCount);
    }
}
