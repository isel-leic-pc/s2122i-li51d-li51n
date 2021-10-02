# Course outline

## Course introduction
- See [0-course-introduction.md](0-course-introduction.md).
- Goals.
- Evaluation.
- Programme.
- Calendar.
- Resources and logistics.
- Student requirements.

## Introduction to multi-threading and concurrenty

- Echo TCP server example - EchoServer_0_SingleThreaded - handling client connections sequentially (i.e. one at a time)

- Echo TCP server example - EchoServer_1_MultiThreaded_Unbound - handling multiple client connections simultaneously, using one thread per connection.
  - Common model on JVM based servers (with the difference the threads are bounded and reused)
  - Servlet example - multiple threads without thread creation

- Introduction to threads
  - See [1-threads-introduction.md](1-threads-introduction.md).
  - What are threads?
    - Sequential computation
    - Share the same process/VM resources, namely memory
    - No OS provided isolation
    - No special language or runtime provided isolation
  - JVM threads vs OS threads
    - Current 1-to-1 mapping
    - Future - project Loom - M-to-N mapping (much more VM threads than OS threads)
  - Time-sharing M threads between N CPUs
    - time-multiplexing
    - thread context and context switch
    - scheduler

- Threads in the JVM
  - See [2-threads-in-the-jvm.md](2-threads-in-the-jvm.md).
  - IntelliJ based visualization of threads

- Echo TCP server example - EchoServer_1_MultiThreaded_Unbound - shared counter
  
- Data synchronization
  - Threading hazards
  - Mutable data sharing between multiple threads
    - counter - loosing increments
    - list - loosing data
  - Solutions using proper data synchronization
    - AtomicNnnn classes
    - Mutual exclusion using `synchronized` blocks
  - Check-then-act
    - State mutation between state observation and dependent actions
    - Can happen even with thread-safe classes
    - Need for different operations - e.g. `computeIfAbsent`
  - Need for proper synchronization in the access to shared mutable data
    - Data synchronization
    - Data synchronization on Java
  - Need to correctly *identify* the mutable data being shared.
    - Avoid sharing (when sharing)
    - Correctly synchronize  sharing
  - Additional reading: industry example - [https://github.blog/2021-03-18-how-we-found-and-fixed-a-rare-race-condition-in-our-session-handling/]

- Echo TCP server example - EchoServer_3_MultiThreaded_Bounded_Semaphore
  - Semaphore usage
  - Control synchronization
