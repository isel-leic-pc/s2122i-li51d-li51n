using System;
using System.Net.Http;
using System.Threading;
using System.Threading.Tasks;
using Xunit;
using Xunit.Abstractions;

namespace Tests.Async
{
    public class TaskExampleTests
    {
        string Method(HttpClient client)
        {
            var response = client.GetAsync("https://httpbin.org/delay/2").Result;
            var s = response.Content.ReadAsStringAsync().Result;
            return s.ToUpper();
        }

        Task<string> MethodAsync(HttpClient client)
        {
            Task<HttpResponseMessage> responseTask = client.GetAsync("https://httpbin.org/delay/2");
            Log($"Status = {responseTask.Status}");
            Task<string> bodyTask = responseTask.ContinueWith(t =>
                {
                    Log("Retrieving body");
                    Log($"Status = {t.Status}");
                    Log($"Exception = {t.Exception}");
                    throw new Exception("Just for testing");
                    return t.Result.Content.ReadAsStringAsync();
                }
            ).Unwrap();
            Task<string> toUpperTask = bodyTask.ContinueWith(t =>
            {
                Log($"Status = {t.Status}");
                Log("Converting to upperCase ");
                return t.Result.ToUpper();
            });

            return toUpperTask;
        }

        [Fact]
        public void Test()
        {
            var countdownEvent = new CountdownEvent(1);

            Log("Test started");
            var client = new HttpClient();
            var task = MethodAsync(client);
            task.ContinueWith(t =>
            {
                Log($"Status = {t.Status}");
                var s = t.Result;
                Log($"Result: {s}");
            }).ContinueWith(t =>
            {
                countdownEvent.Signal();
            });
            countdownEvent.Wait();
            Log("Test about to end");
        }

        [Fact]
        public void Test2()
        {
            var countdownEvent = new CountdownEvent(1);
            var client = new HttpClient();
            Log("Starting requests");
            var t1 = client.GetAsync("https://httpbin.org/delay/4");
            var t2 = client.GetAsync("https://httpbin.org/delay/5");
            var t3 = Task.WhenAll(t1, t2);
            t3.ContinueWith(t =>
            {
                Log("WhenAll continuation called");
                
            });
            t3.Wait();
        }

        [Fact]
        public Task Test3()
        {
            var client = new HttpClient();
            var t1 = client.GetAsync("https://httpbin.org/delay/4");
            var t2 = t1.ContinueWith(t =>
            {
                Log("Response received");
            });
            Log("Test about to end");
            return t2;
        }

        [Fact]
        public Task Test4()
        {
            var task = Task.FromResult(42);
            Log("Before ContinueWith");
            var task2 = task.ContinueWith(t =>
            {
                var res = t.Result;
                Log("Inside continuation");
            }, TaskContinuationOptions.ExecuteSynchronously);
            Log("After ContinueWith");
            return task2;
        }
        
        [Fact]
        public Task Test5()
        {
            var taskCompletionSource = new TaskCompletionSource<int>();
            var task = taskCompletionSource.Task;
            Log("Before ContinueWith");
            var task2 = task.ContinueWith(t =>
            {
                var res = t.Result;
                Log("Inside continuation");
            }, TaskContinuationOptions.RunContinuationsAsynchronously);
            Log("After ContinueWith");
            
            Log("Before SetResult");
            taskCompletionSource.SetResult(42);
            Log("After SetResult");
            
            return task2;
        }
        
        private readonly ITestOutputHelper _output;

        public TaskExampleTests(ITestOutputHelper output)
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