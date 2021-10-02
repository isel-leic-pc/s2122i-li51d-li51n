# Data synchronization in Java

_Data synchronization_ is the coordination between threads needed to ensure shared mutable data remains consistent.
Consider a JDBC connection pool, implemented as a list and shared by multiple threads. 
The access by multiple threads to this shared list must ensure proper data synchronization to ensure the list remains consistent, namely that no elements are lost or duplicated.
A way to achieve data synchronization it is via mutal exclusion.

_Control synchronization_ is also a form of coordination between threads, where threads need to wait for conditions that will 
be made true by other threads.
Using the same JDBC connection pool example, a thread getting a connection on an empty pool needs to wait for another thread to release a connection.
Note that it doesn't need to wait for another thread to end the pool manipulation - that would be data synchronization.
It needs to wait for another thread to give a connection back to the pool, which is a different thing.
Mutual exclusion is most often **not** the right way to implement control synchronization.

See Chapter 2 of [Joe Duffy, Concurrent Programming on Windows](https://www.oreilly.com/library/view/concurrent-programming-on/9780321434821/) for a discussion of data and control synchronization.

## Mutual exclusion and locks

In a broad sense, mutual exclusion aims to ensure that no more than one thread can simultaneously access the same data.
This is achieved by:
- Identifying all code blocks that access the shared data.
- Ensuring that the execution of any of those blocks requires the acquisition of the mutual exclusion object associated to the shared data.

A mutual exclusion object, also called _mutex_ or _lock_ has two states - _unlocked_ and _locked_ (by a specific thread) - and two operations - _acquire_ and _release_.
  - An _acquire_ operation made by thread _T_ on an _unlocked_ _mutex_ will atomically transition the mutex to the _locked(T)_ state.  
  - A _release_ operation on an _locked(T)_ _mutex_ transitions the mutex to the _unlocked_ state.
  - Any _acquire_ operation by thread _T1_ on an _locked(T2)_ _mutex_ waits until the _mutex_ transition to the _locked(T1)_.

Some locks have reentrant acquisition:
  - _acquire_ operation on a lock acquired by the requesting thread does succeed, and a reentrancy counter is incremented.
  - _release_ operation decrements the reentrancy counter. The lock only goes to the _unlocked_ state if the counter reaches zero.

Using locks for safely share mutable data between threads.
- Associate a lock *instance* to each shared data structure *instance*.
- _acquire_ the **associated** lock before entering **any** code block that accesses the data structure.
- _release_ lthe ock when leaving those those code blocks (normally or via an exception).
  
Mutual exclusion only works if **every** code block that accesses the shared data access follows this acquire-release protocol.

This requirement fits well with the encapsulation provided by object-oriented programming models.
- Make shared data private.
- Operations over the shared data are exclusively done via public methods.
  - These methods ensure the lock acquisition at the beggining and release at the end (including exception exits).

## Locks in Java ##

Java provides two type of locks:
- intrinsic locks, where every object has an associated a lock.
- The `Lock` interface and the associated implementing classes.

### Intrinsic locks ###

In java, every object has an associated lock, usually called the _intrinsic lock_.
This lock is acquired and release via the `synchronized` language construction, which has [two forms](https://docs.oracle.com/javase/tutorial/essential/concurrency/locksync.html):
- Synchronized Statements

```java
synchronized(theObjectWithTheIntrinsicLock) {
  // statement block executed while holding the theObjectWithTheIntrinsicLock lock
}
```

- Synchronized Methods

```java
public synchronized void someInstanceMethod(...) {
  // statements executed while holding the lock associated to the `this` object
}

public static synchronized void someStaticMethod(...) {
  // statements executed while holding the lock associated to the *Class* object
}
```

### The `Lock` interface

The Java class library also provides the [`Lock`](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/locks/Lock.html) interface and some implementing classes, namely the [Reentrant](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/locks/ReentrantLock.html) class.

Instances implementing the `Lock` interface are not used automatically by special language constructs.
Instead, they have `lock` and `unlock` methods that need to be explicitly used.

Special care needs to be taken to ensure that when a thread exits a lock-protected code block the lock is released.
This is done automatically for `synchronized` statements and methods but not for `Lock` based locks.

Note also that objects implementing the `Lock` interface also have intrinsic locks, because _any_ object has an intrinsic lock.
However they are **distinct** locks:
- The `synchronized(aLock)` statement acquires the intrinsic lock associated to `aLock` but not the `aLock` lock.
- The `aLock.lock()` statement acquires the `aLock` lock.

### Locks and the memory model

Locks, both intrinsic and `Lock` based, also ensure correct memory actions visibility between threads.
As we will see in the Java Memory Model module, the lock acquisition by a thread ensures all subsequent reads on that thread _see_ the writes made by another thread before it released the lock.

```
Thread 0                  Thread 1
========                  ========
a = 1
b = 2
lock.unlock()

                          lock.lock()
                          var l1 = a // "sees" the `a = 1` write
                          var l2 = b // "sees" the `b = 2` write           
```

Without the `lock.lock()` performed on `Thread 1`, there is no assurance the reads of `a` and `b` will see the values written by `Thread 0`.

## Locks and class invariants ##

A class invariant is a condition over the classe's fields.
For instance, a circular double-linked list may have the invariant that all nodes have non-null `next` and `previous` fields.

The class methods:
- **May assume** the invariant to be true when they begin execution.
- **Must ensure** the invariant to be true when end execution.

However, inside a method execution, the invariants don't have to be ensured (i.e. the objects may be in inconsistent states)
For instance, while inserting a new node, it is acceptable for the node's `next` field to be assigned while the `previous` is still null.
This is acceptable as long as this inconsistent state is transient and not observable by other methods.
On a single-threaded model, this is indeed true because method execution is serialized: a method cannot observe the data structure while another method is mutating it and it is in an inconsistent state.

However, on multi-thread models, this is no longer true by default.
The use of locks is a way to recover this property:
- Methods **may assume** the invariant to be true after aquiring the lock.
- Methods **must ensure** the invariant to be true before they release the lock.

## Locks and busy waiting ##

When using locks to implement data synchronization, it is expected that threads will hold on locks for very small amounts of time: the minimum necessary to observe and/or mutate data structures.
Due to this, lock implementations on multi-processor systems typically don't transition a thread to the non-ready state when it tries to acquired a locked lock. 
The rationale is that the thread owning the lock is running in another processor and will soon release it.
So, in a rather simplified way, the acquiring thread just keeps polling on the lock state until it becomes unlocked.
This is called busy waiting or spin waiting.
