using System;
using System.Net.Http;
using System.Threading;
using ExamplesLib.Async;
using Xunit;
using Xunit.Abstractions;

namespace Tests.Async
{
    public class TaskExampleTests2
    {
        [Fact]
        public void First()
        {
            var cde = new CountdownEvent(1);
            var scheduler = new SingleThreadTaskScheduler(Log);

            var client = new HttpClient();
            var task = client.GetAsync("https://httpbin.org/delay/2");
            Log("GetAsync returned");
            task.ContinueWith(t =>
            {
                Log($"ContinueWith called: status {t.Result.StatusCode}");
                cde.Signal();
            }, scheduler);
            cde.Wait();
            Log("Test ending");
        }
        
        private readonly ITestOutputHelper _output;

        public TaskExampleTests2(ITestOutputHelper output)
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