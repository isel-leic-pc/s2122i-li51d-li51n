package org.pedrofelix.pc.lockfree;

import java.util.concurrent.atomic.AtomicReference;

// WIP
public class LockFreeStack<T> {

    private static class Node<T> {
        public final T value;
        public Node<T> next;
        public Node(T value) {
            this.value = value;
        }
    }

    private final AtomicReference<Node<T>> head = new AtomicReference<>(null);

    public void push(T value) {
        var node = new Node<T>(value);
        while(true) {
            Node<T> observedHead = head.get(); // Rh
            node.next = observedHead; // Wn
            if(head.compareAndSet(observedHead, node)) { // Wh
                return;
            }
            // repeat
        }
    }


}
