using System;
using System.Threading;
using System.Threading.Tasks;
using ExamplesLib.Async;
using Xunit;
using Xunit.Abstractions;

namespace Tests.Async
{
    public class SynchronizationContextExamples
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
                _log("Post: before running continuation");
                // continuation(state);
                RunInThisContext(continuation, state);
                _log("Post: after running continuation");
            }

            private void RunInThisContext(SendOrPostCallback continuation, object? state)
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

        [Fact]

        public async Task simple_example()
        {
            Log("Test started");
            var sc = new SimpleSynchronizationContext(Log);
            SynchronizationContext.SetSynchronizationContext(sc);
            
            await Task.Delay(100);
            Log("After first delay");

            await Task.Delay(100);
            Log("After second delay");
            
            await Task.Delay(100);
            Log("After third delay");
        }

        [Fact]
        public async Task single_thread_example()
        {
            Log("Test started");
            using var sc = new SingleThreadExecutionContext(Log);
            SynchronizationContext.SetSynchronizationContext(sc);

            for (var i = 0; i < 2; ++i)
            {
                await Task.Delay(100);
                Log($"After Delay on iteration {i}");
            }
            
            //Dispose - continuation - inside the thread of the synchronization context
        }

        [Fact]
        public async Task<int> single_thread_example_with_deadlock()
        {
            static async Task<int> Aux()
            {
                await Task.Delay(100);
                return 42;
            }
            using var sc = new SingleThreadExecutionContext(Log);
            SynchronizationContext.SetSynchronizationContext(sc);

            await Task.Delay(100); // <~~~

            var task = Aux();

            var i = task.Result; // 

            return i;
        }
        
        [Fact]
        public async Task<int> exclusive_example_with_deadlock()
        {
            async Task<int> Aux()
            {
                await Task.Delay(100).ConfigureAwait(false);
                Log("before returning 42");
                return 42;
            }
            var sc = new ExclusiveSynchronizationContext(Log);
            SynchronizationContext.SetSynchronizationContext(sc);

            await Task.Delay(100); // <~~~

            Log("before calling Aux");
            var task = Aux();

            Log("before calling Result");
            var i = task.Result; // 

            return i;
        }

        private readonly ITestOutputHelper _output;

        public SynchronizationContextExamples(ITestOutputHelper output)
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