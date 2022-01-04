using System;
using System.Collections.Concurrent;
using System.Threading;
using System.Threading.Tasks;
using ExamplesLib.Async;
using Xunit;
using Xunit.Abstractions;
using Xunit.Sdk;

namespace Tests.Async
{
    public class SynchronizationContextExamplesLI51N
    {

        class SimpleSynchronizationContext : SynchronizationContext
        {
            private readonly Action<string> _log;

            public SimpleSynchronizationContext(Action<string> log)
            {
                _log = log;
            }
            public override void Post(SendOrPostCallback continuation, object? state)
            {
                _log("before running continuation");
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

                _log("after running continuation");
            }
        }

        class SingleThreadSynchronizationContext : SynchronizationContext, IDisposable
        {
            private readonly Action<string> _log;
            private readonly Thread _thread;
            private readonly BlockingCollection<Action?> _queue = new BlockingCollection<Action?>();

            public SingleThreadSynchronizationContext(Action<string> log)
            {
                _log = log;
                _thread = new Thread(RunLoop);
                _thread.Start();
            }
            
            public int ThreadId => _thread.ManagedThreadId;

            public override void Post(SendOrPostCallback continuation, object? state)
            {
                _log("Post called");
                _queue.Add(() => continuation(state));
            }

            private void RunLoop()
            {
                while (true)
                {
                    var action = _queue.Take();
                    if (action == null)
                    {
                        // poison-pill
                        return;
                    }

                    SynchronizationContext.SetSynchronizationContext(this);
                    _log("starting running continuation");
                    action();
                    _log("ending running continuation");
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

        [Fact]
        public async Task First()
        {
            Log("started");
            
            var sc = new SimpleSynchronizationContext(Log);
            SynchronizationContext.SetSynchronizationContext(sc);

            await Task.Delay(100);
            Log("first continuation");
            
            await Task.Delay(100);
            Log("second continuation");
            
            await Task.Delay(100);
            Log("third continuation");
        }
        
        [Fact]
        public async Task Second()
        {
            Log("started");
            
            using var sc = new SingleThreadSynchronizationContext(Log);
            SynchronizationContext.SetSynchronizationContext(sc);
            
            Log($"SingleThreadSynchronizationContext has internal thread with ID = {sc.ThreadId}");

            await Task.Delay(100);
            Log("first continuation");
            
            await Task.Delay(100);
            Log("second continuation");
            
            await Task.Delay(100);
            Log("third continuation");
            // sc.Dispose()
        }
        
        [Fact]
        public async Task Third()
        {
            async Task<int> Aux()
            {
                await Task.Delay(100);
                return 42;
            }
            
            Log("started");
            
            var sc = new SingleThreadSynchronizationContext(Log);
            SynchronizationContext.SetSynchronizationContext(sc);

            await Task.Delay(100);

            var task = Aux();

            var i = task.Result;
            
            Log($"{i}");
        }
        
        [Fact]
        public async Task Fourth()
        {
            async Task<int> Aux()
            {
                await Task.Delay(100).ConfigureAwait(false);
                return 42;
            }
            
            Log("started");
            
            var sc = new SingleThreadSynchronizationContext(Log);
            SynchronizationContext.SetSynchronizationContext(sc);

            await Task.Delay(100);

            var task = Aux();

            var i = task.Result;
            
            Log($"{i}");
        }

        private readonly ITestOutputHelper _output;

        public SynchronizationContextExamplesLI51N(ITestOutputHelper output)
        {
            _output = output;
        }

        private void Log(string s)
        {
            _output.WriteLine("[{0,2}|{1,8}|{2:hh:mm:ss.fff}]{3}",
                Thread.CurrentThread.ManagedThreadId,
                Thread.CurrentThread.IsThreadPoolThread ? "pool" : "non-pool", DateTime.Now, s);
        }
    }
}