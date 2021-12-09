using System;
using System.Net.Http;
using System.Threading;
using Xunit;
using Xunit.Abstractions;

namespace Tests.Async
{
    public class CancellationExamplesLI51NTests
    {

        [Fact]
        public void First()
        {
            var httpClient = new HttpClient();
            var cts = new CancellationTokenSource();
            var ct = cts.Token;
            var task = httpClient.GetAsync("https://httpbin.org/delay/5", ct);
            Thread.Sleep(7000);
            cts.Cancel();
            var response = task.Result;
        }
        
        [Fact]
        public void Second()
        {
            var httpClient = new HttpClient();
            var cts = new CancellationTokenSource();
            cts.CancelAfter(TimeSpan.FromMilliseconds(7000));
            var ct = cts.Token;
            Log("Starting");
            var task = httpClient.GetAsync("https://httpbin.org/delay/5", ct);
            try
            {
                var response = task.Result;
            }
            catch (Exception e)
            {
                Log("Catching");
            }
            Log("Done");
            cts.Dispose();
        }
        
        
        private readonly ITestOutputHelper _output;

        public CancellationExamplesLI51NTests(ITestOutputHelper output)
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