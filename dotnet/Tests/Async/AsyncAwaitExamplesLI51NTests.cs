using System;
using System.Net.Http;
using System.Threading;
using System.Threading.Tasks;
using Xunit;
using Xunit.Abstractions;

namespace Tests.Async
{
    public class AsyncAwaitExamplesLI51NTests
    {
        [Fact]
        public async Task First()
        {
            async Task<string> AsyncFunction()
            {
                Task<int> t1 = DelayedValue(1, 1000);
                Log("before first await");
                int i = await t1;
                
                Log("after first await");
                Task<long> t2 = DelayedValue(2L, 1000);
                Log("before second await");
                long l = await t2;
                
                Log("after second await");
                return l.ToString();
            }

            Log("Before call to AsyncFunction");
            var s = await AsyncFunction();
            Log("After call to AsyncFunction");

            //task.Wait();
            
            Log("Ending test");

        }

        [Fact]
        public async Task Second()
        {
            var client1 = new HttpClient();
            var client2 = new HttpClient();
            Log("Before doing the requests");
            var t1 = await client1.GetAsync("https://httpbin.org/delay/3");
            var t2 = await client2.GetAsync("https://httpbin.org/delay/5");
            Log("Responses available");
        }

        [Fact]
        public void Third()
        {
            Task<string> SomeFunctionAsync(string s)
            {
                if(s == null) throw new ArgumentException();
                return SomeFunctionAsyncAux(s);
            }
            
            async Task<string> SomeFunctionAsyncAux(string s)
            {
                if(s == null) throw new ArgumentException();
                return "done";
            }

            async Task<string> AsyncFunction()
            {
                throw new Exception("Just for testing");
            }
            
            Log("Before call");
            var t = AsyncFunction();
            Log("After call");
            try
            {
                t.Wait();
            }
            catch (Exception e)
            {
                Log("Exception caught");
            }
        }
        
        [Fact]
        public async Task Fourth()
        {
            var client1 = new HttpClient();
            var client2 = new HttpClient();
            Log("Before doing the requests");
            var t1 = client1.GetAsync("https://httpbin.org/delay/3");
            var t2 = client2.GetAsync("https://httpbin.org/delay/5");
            await Task.WhenAll(t1, t2);
            var r1 = t1.Result;
            var r2 = t2.Result;
                
            Log("Responses available");
        }
        
        [Fact]
        public void Fifth()
        {
            async Task<string> AsyncFunction()
            {
                Task<int> t1 = DelayedValue(1, 1000);
                Log("before first await");
                int i = await t1;
                
                Log("after first await");
                Task<long> t2 = DelayedValue(2L, 1000);
                Log("before second await");
                long l = await t2;
                
                Log("after second await");
                return l.ToString();
            }

            Log("Before call to AsyncFunction");
            var t = AsyncFunction();
            Log("After call to AsyncFunction");

            var t2 = t.ContinueWith(_ =>
            {
                Log("Continuation");
            }, TaskContinuationOptions.ExecuteSynchronously);
            t2.Wait();
            
            Log("Ending test");

        }

        private Task<T> DelayedValue<T>(T value, int timeout)
        {
            return Task.Delay(timeout).ContinueWith(_ => value);
        }
         
        private readonly ITestOutputHelper _output;

        public AsyncAwaitExamplesLI51NTests(ITestOutputHelper output)
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