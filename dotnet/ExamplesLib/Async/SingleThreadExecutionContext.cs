using System;
using System.Collections.Concurrent;
using System.Threading;

namespace ExamplesLib.Async
{
    public class SingleThreadExecutionContext : SynchronizationContext, IDisposable
    {
        private readonly Action<string> _log;
        private readonly BlockingCollection<Action?> _queue = new BlockingCollection<Action?>();
        private readonly Thread _thread;

        public SingleThreadExecutionContext(Action<String> log)
        {
            _log = log;
            _thread = new Thread(RunLoop);
            _thread.Start();
        }

        public override void Post(SendOrPostCallback continuation, object? state)
        {
            _log("Post called");
            _queue.Add(() => continuation(state));
            _log("Post returning");
        }

        public int ThreadId => _thread.ManagedThreadId;

        private void RunLoop()
        {
            while (true)
            {
                var action = _queue.Take();
                _log("Action retrieved from queue");
                if (action == null)
                {
                    // poison pill
                    _log("Ending RunLoop");
                    return;
                }
                
                SynchronizationContext.SetSynchronizationContext(this);
                _log("Running action");
                action();
            }
        }

        public void Dispose()
        {
            _log("Dispose called");
            _queue.Add(null);
            if (Thread.CurrentThread != _thread)
            {
                _thread.Join();
            }
        }
    }
}