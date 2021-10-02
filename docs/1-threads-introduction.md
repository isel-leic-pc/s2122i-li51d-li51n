# Concurrent Programming introduction

## What are threads?

Threads are a mechanism provided by operating systems to have multiple *sequential computations* executing simultaneously on the same process.
  
- The POSIX standard [defines a thread](https://pubs.opengroup.org/onlinepubs/9699919799.2018edition/basedefs/V1_chap03.html#tag_03_404) as

> A single flow of control within a process. Each thread has its own thread ID, scheduling priority and policy, errno value, floating point environment, thread-specific key/value bindings, and the required system resources to support a flow of control. Anything whose address may be determined by a thread, including but not limited to static variables, storage obtained via malloc(), directly addressable storage obtained through implementation-defined functions, and automatic variables, are accessible to all threads in the same process.

- In the POSIX standard a thread is created by calling the `pthread_created` function and passing in a `void * (*start_routine)(void*)`, that is, a pointer to the function defining the sequential computation.

- A similar thread concept is also available in _managed_ programming models, such as the ones provided by the JVM and .NET platforms. This is typically done by mapping 1-to-1 _managed_ threads to operating system threads.

- In the Java standard library a thread is created by calling the `Thread` class constructor and passing in a `Runnable`, that is an object with a `run` method defining the sequential computation.

## Why have multiple threads?

**Performance** - take advantage of having more than one CPU. 
  
  - Having multiple-CPU systems is becoming more common. As an example, a medium range smartphone has typically between 4 to 8 CPUs. 

  - A way to take advantage of these system resources is to have a different thread executing on different CPUs. This type of design applies well to scenarios where there are enough independent sequential computations to feed these threads. A good example are server systems (e.g. such as the information systems with HTTP interfaces that we developed in the LS course) that have the ability to process requests from different clients simultaneously. In this case the intrisic paralellism, i.e. the independent computations, come from the fact that the systems may be used simultaneously by different users.

**Code organization** - Sometimes it is simpler to organize a program in multiple sequential computations instead of in a single sequential computation. Server programs are again a good example of this, where the handling of each client request can be organized as an independent sequential computation.

Threads of the same process are **not isolated**, meaning that they share the same memory space, including both data and code.
  
- The advantage is that thread coordination and communication is less expensive since it doesn't have to cross any protection barrier (e.g. such as user-mode to kernel-mode).

- The disadvantage is that there isn't any automatic protection against hazardous thread interference implemented by the operating system. The burden of correctness lies almost entirely on the program and user-mode libraries. This aspect is one of the main motivations for this course.

- Note that each thread, i.e. each sequential computation, will have its own stack, allocated for it by the operating system. However the memory area where that stack resides is not protected from access by other threads in the same process.

## How are threads implemented?

Operating system implement the ability to have multiple threads, i.e. multiple simultanous sequential computations, by time-multiplexing a set of M threads into a set of N CPUs, where M is typically much larger than N. 
This is done by a component of the operating system called the **scheduler**.

Thread states and context switches.
  
- At any given moment in time, a thread (that has not yet ended) is in one of three states:
  - **Running** - the thread is assigned to a CPU.
  - **Ready** - the thread is not assigned to a CPU only because there isn't a CPU available for it.
  - **Not-ready** - the thread is not assigned to a CPU because it can only run after some condition becomes true.
    - Examples of these conditions are: waiting for an I/O operation to complete (e.g. a socket read), waiting for a thread coordination condition to be true (e.g. aquiring a mutex).

- Thread state changes:

  - **Running** to **Ready**
    - This can happen in two situations:
      - The running thread calls the operating system to voluntarily give away (i.e. yield) the owning CPU. This is controlled by the code running in the thread, and therefore deterministic from a program view point.
      - The scheduler decides that the running thread should be replaced by another ready thread. This can happen because the running thread is owning a CPU for too long. This is an asynchronously event (e.g. occurring on a system timer interrupt handler) and therefore is completely non-deterministic from a program viewpoint. Namely, it can happen at any assembly instruction boundary, therefore _in the middle_ of an higher-level language statement.
    - When a thread goes from the **Running** state to the **Ready** state, then there is what is called a **context switch**: the context of the previously running thread is saved from the CPU into memory, and the context of the newly running thread is loaded from memory into the CPU.

  - **Ready** to **Running**
    - This happens when the scheduler decides to assign a CPU to the ready thread.
    - This also implies a context swith - the ready thread context is loaded from memory into the CPU.

  - **Running** to **Not-ready**
    - This happens when the thread performs an operation whose result is not immediatly available, and therefore the thread doesn't have conditions to contiue running.
    - Examples of such operations are I/O operations (e.g. read from a socket) or thread coordination operations (e.g. acquire a mutex or an unit from a semaphore).

  - **Not-ready** to **Ready**
    - This happens when the condition required for a thread to continue execution becomes true: e.g. there are bytes available in the socket to read or the OS finally assigned the request mutex to the thread.

What is the thread context?

- The thread context is the information that needs to be saved from the CPU into memory so that the same thread can correctly resume execution by loading that information back into a CPU.

- Examples of such information are the _instruction pointer_ and the _stack pointer_.

- When using OS provided threads, a context switch needs to be performed by the OS, which implies a user-mode to kernel-mode transition.

## Why have more threads than CPUs?

The main answer is simpler code organization by allowing threads to block.
Lets use an example to illustrate what we mean by this. 

- Consider a typical information system with an HTTP-based interface and backed by a DBMS. When a request is received, a thread is created or selected to process that request and starts executing application-level code. Perhaps this code starts at a servlet, then calls an handler/controller, which performs some database operation, eventually through a JDBC helper of some sort. This database operation implies communication with the external DBMS and, depending on the query complexity and data size, may take hundreds on milliseconds. During this time, which is almost a figurative eternity for a CPU operating in Giga Hertz frequencies, the thread does not have any CPU-bound operation to perform. It would be a waste of resources to have a CPU allocated for this thread while this database operation is pending.

- An option is to reuse this thread to process another request in the meanwhile. However, this is much easier said than done. The thread cannot simply return from the function doing the database operation, because then the local state would be lost. And this state (e.g. the request parameters, the intermediate computations) are required when the database operation completes. While this is possible, typically it implies structuring applications differently and/or using mechaninsms such as asynchronous methods or coroutines.

- A much easier solution is to block the thread, freeing the CPU so that it can host a different thread. When the database operation finally concludes, the thread will become ready again and elligible to start running by the OS assigning a CPU to it. It is the fact that threads can block, freeing the CPU where they are executing, that justifies having more threads than CPUs.

- Later in this course we will see ways of avoiding blocking threads, by using what are called by asynchronous programming models, typically with the help of the programming language (C#'s asynchronous methods or Kotlin's coroutines). In this case, instead of blocking, the thread will available to start processing other requests right away. When absolutely no blocking exists, then we can go back and have exactly as many threads as CPUs.

Even if a blocked thread doesn't occupy a CPU, it still occupies other resources, such as memory. As a consequence, there is a practical limit to the number of threads a program can have, tipically in the range of hundreds.

Asynchronous programming models such as C#'s asynchronous methods or Kotlin's coroutines provide other ways of having sequential computations without requiring one thread per sequential computation.

A different approach is taken by [Java's project Loom](https://openjdk.java.net/projects/loom/): instead of reducing the number of required application threads, this project aims to reduce the cost of each application-level thread.

## Threads are everywhere

Most current application level programming models are multi-thread, meaning that application code runs in more than one thread, even if no threads are explicitly created by that application code.

As an example, on a servlet-based HTTP server, multiple requests can be handled simultaneously, with the processing of each request being made on a different thread. On the so called thread-per-request model, an available thread is selected to host the complete execution of each request. 

Another example are GUI-based programming models, such as the one defined by Android. There, a special thread, usually called UI thread or main thread, is responsible to host the execution of all GUI related events (e.g. button click handlers). As a consequence, this thread cannot be used to host operations that take more than some milliseconds, such as requests to external system or CPU-intensive operations. Making such operations on this thread would mean that the application would become unresponsive, i.e. not be able to handle events during these periods. A way to solve this is to handle these long-term blocking operations on distinct threads, freeing the UI thread to handle GUI events. This makes application code run in more than one thread, with the associated challenges that this course will help identify and overcome.

This means that in a significant number of cases, threads are not an optional feature that a program can decide to use or not. They are an intrinsic part of the program model and cannot be avoided.

Interaction between multiple threads, namely when accessing shared memory, presents a set of challenges. The goal of this course is to identify this challenges and present techniques to overcome them, namely by the use of proper synchronization and thread coordination techniques.
