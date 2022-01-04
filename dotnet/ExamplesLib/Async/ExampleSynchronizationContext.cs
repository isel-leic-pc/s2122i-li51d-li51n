using System;
using System.Threading;

namespace ExamplesLib.Async
{
    public class ExampleSynchronizationContext : SynchronizationContext
    {
        private readonly Action<string> _logger;
        private readonly bool _preserveContext;

        public ExampleSynchronizationContext(Action<string> logger, bool preserveContext)
        {
            _logger = logger;
            _preserveContext = preserveContext;
        }

        public override void Post(SendOrPostCallback continuation, object? state)
        {
            _logger("Post called");
            if (_preserveContext)
            {
                RunWithContext(continuation, state);
            }
            else
            {
                continuation(state);
            }
        }

        public override void Send(SendOrPostCallback continuation, object? state)
        {
            _logger("Send called");
            base.Send(continuation, state);
        }

        private void RunWithContext(SendOrPostCallback continuation, object? state)
        {
            var oldContext = SynchronizationContext.Current;
            try
            {
                SynchronizationContext.SetSynchronizationContext(this);
                continuation(state);
            }
            finally
            {
                SynchronizationContext.SetSynchronizationContext(oldContext);
            }
        }
    }
}