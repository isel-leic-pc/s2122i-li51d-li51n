using System.Threading;

namespace ExamplesLib.Synchronizers
{
    // WIP
    public sealed class Condition
    {
        // The object used for the mutual-exclusion
        private readonly object _mlock;
        // the object used for the condition
        private readonly object _mcondition = new object();

        public Condition(object mlock)
        {
            _mlock = mlock;
        }
        
        public void Wait(int timeout) // throws ThreadInterruptedException
        {
            // - We can only exit from the outer lock after acquiring the inner lock
            //   to avoid loosing notifications
            // - The Enter can throw ThreadInterruptedException; in that case we leave the Wait with
            //   exception and with the outer lock, which is OK.
            Monitor.Enter(_mcondition);
            Monitor.Exit(_mlock);
            try
            {
                // throws ThreadInterruptedException 
                Monitor.Wait(_mcondition, timeout);
            }
            finally
            {
                // - The outer lock needs to be acquired, independently of how the Wait ends:
                //   regular return or exception
                // - Here we need to release the inner lock before acquiring the outer lock to avoid 
                //   a deadlock due to lock acquisition with reversed order.
                Monitor.Exit(_mcondition);
                // throws ThreadInterruptedException
                MonitorEx.EnterNonInterruptibly(_mlock, out var wasInterrupted);
                if (wasInterrupted)
                {
                    // So that the interrupt is not lost
                    Thread.CurrentThread.Interrupt();
                }
            }
        }

        public void Signal()
        {
            // Because a signal should never throw ThreadInterruptedException
            MonitorEx.EnterNonInterruptibly(_mcondition, out var wasInterrupted);
            Monitor.Pulse(_mcondition);
            Monitor.Exit(_mcondition);
            if (wasInterrupted)
            {
                // So that the interrupt is not lost
                Thread.CurrentThread.Interrupt();
            }
        }

        public void SignalAll()
        {
            // see comments in the previous method 
            MonitorEx.EnterNonInterruptibly(_mcondition, out var wasInterrupted);
            Monitor.PulseAll(_mcondition);
            Monitor.Exit(_mcondition);
            if (wasInterrupted)
            {
                Thread.CurrentThread.Interrupt();
            }
        }
    }
}