using System;
using System.Threading;

namespace ExamplesLib.Synchronizers
{
    public static class MonitorEx
    {

        public static void EnterNonInterruptibly(
            object monitor,
            out bool wasInterrupted
            )
        {
            wasInterrupted = false;
            while (true)
            {
                try
                {
                    Monitor.Enter(monitor);
                    return;
                }
                catch (ThreadInterruptedException)
                {
                    wasInterrupted = true;
                }
            }
        }
        
    }
}