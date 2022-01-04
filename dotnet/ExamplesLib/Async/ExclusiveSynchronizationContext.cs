using System;
using System.Threading;

namespace ExamplesLib.Async
{
    public class ExclusiveSynchronizationContext : SynchronizationContext
    {
        private readonly Action<string> _log;
        private readonly SemaphoreSlim sem = new SemaphoreSlim(1);

        public ExclusiveSynchronizationContext(Action<string> log)
        {
            _log = log;
        }

        public override void Post(SendOrPostCallback continuation, object? state)
        {
            _log("acquiring mutex");
            sem.Wait();
            var oldContext = SynchronizationContext.Current;
            try
            {
                _log("running continuation");
                SynchronizationContext.SetSynchronizationContext(this);
                continuation(state);
            }
            finally
            {
                _log("releasing mutex");
                sem.Release(1);
                SynchronizationContext.SetSynchronizationContext(oldContext);
            }
        }
    }
}