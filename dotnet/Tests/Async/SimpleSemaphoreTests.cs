using System;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using ExamplesLib;
using ExamplesLib.Async;
using Xunit;
using Xunit.Abstractions;

namespace Tests.Async
{
    public class SimpleSemaphoreTests
    {

        [Fact]
        public void Test()
        {
            const int nReps = 100;
            const int nLoops = 10;
            var semaphore = new SimpleAsyncSemaphore(2);
            var counter = 2;

            async Task Loop(int index)
            {
                for (var i = 0; i < nReps; ++i)
                {
                    Log($"{index}: before AcquireAsync");
                    await semaphore.AcquireAsync();
                    try
                    {
                        Log($"{index}: after AcquireAsync");
                        var decremented = Interlocked.Decrement(ref counter);
                        Assert.True(decremented >= 0);
                        await Task.Delay(10);
                    }
                    finally
                    {
                        Interlocked.Increment(ref counter);
                        semaphore.Release();
                        Log($"{index}: after Release");
                    }
                }
            }

            var tasks = Enumerable.Range(0, nLoops)
                .Select(Loop)
                .ToArray();

            Task.WaitAll(tasks);

        }
        
        private readonly ITestOutputHelper _output;

        public SimpleSemaphoreTests(ITestOutputHelper output)
        {
            SynchronizationContext.SetSynchronizationContext(null);
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