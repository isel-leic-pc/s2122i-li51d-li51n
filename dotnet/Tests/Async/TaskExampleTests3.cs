using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Threading;
using System.Threading.Tasks;
using Xunit;
using Xunit.Abstractions;

namespace Tests.Async
{
    public class TaskExampleTest3
    {
        string Method(HttpClient client)
        {
            Task<HttpResponseMessage> responseTask = client.GetAsync("https://httpbin.org/delay/2");
            // .Result é potencialmente bloqueante (equivalente ao get() no Future da JVM)
            HttpResponseMessage response = responseTask.Result;
            Task<String> contentTask = response.Content.ReadAsStringAsync();
            // .Result é potencialmente bloqueante
            String s = contentTask.Result;
            return s.ToUpper();
        }

        [Fact]
        public void First()
        {
            var countDownEvent = new CountdownEvent(1);
            Log("Test started");
            var client = new HttpClient();
            var responseTask = client.GetAsync("https://httpbin.org/delay/2");
            Task<string> contentTask = responseTask.ContinueWith(t =>
                {
                    Log("First continuation");
                    // É garantidamente não bloqueante!
                    var response = t.Result;
                    // ReadAsStringAsync tem tipo Task<String>
                    return response.Content.ReadAsStringAsync();
                }
            ).Unwrap();
            Task<string> processedContentTask = contentTask.ContinueWith(t =>
            {
                Log("Second continuation");
                var s = t.Result;
                return s.ToUpper();
            });
            var finalTask = processedContentTask.ContinueWith(t =>
            {
                // Do something with the processed content
                try
                {
                    Log("Third continuation");
                    var processedResult = t.Result;
                    Log($"Processed content {processedResult}");
                }
                catch (Exception e)
                {
                    Log($"Exception: ${e.Message}");
                }
            });
            Log("Waiting for test to end");
            //countDownEvent.Wait(); // em alternativa: finalTask.Wait();
            finalTask.Wait();
            Log("Test about to end");
        }

        [Fact]
        public Task Second()
        {
            Log("Test started");
            var client = new HttpClient();
            var responseTask = client.GetAsync("https://httpbin.org/delay/2");
            Task<string> contentTask = responseTask.ContinueWith(t =>
                {
                    Log("First continuation");
                    // É garantidamente não bloqueante!
                    var response = t.Result;
                    // ReadAsStringAsync tem tipo Task<String>
                    return response.Content.ReadAsStringAsync();
                }
            ).Unwrap();
            Task<string> processedContentTask = contentTask.ContinueWith(t =>
            {
                Log("Second continuation");
                var s = t.Result;
                return s.ToUpper();
            });
            var finalTask = processedContentTask.ContinueWith(t =>
            {
                // Do something with the processed content
                try
                {
                    Log("Third continuation");
                    var processedResult = t.Result;
                    Log($"Processed content {processedResult}");
                }
                catch (Exception e)
                {
                    Log($"Exception: ${e.Message}");
                }
            });
            Log("Test about to end");
            return finalTask;
        }

        [Fact]
        public void Third()
        {
            Log("Test started");
            var client = new HttpClient();
            var responseTask = client.GetAsync("https://httpbin.org/delay/2");
            responseTask.Wait();
            Log("Before ContinueWith");
            var secondTask = responseTask.ContinueWith(t =>
            {
                Log("First continuation");
            }, TaskContinuationOptions.ExecuteSynchronously);
            Log("After ContinueWith");
            secondTask.Wait();
        }
        
        [Fact]
        public void Third_2()
        {
            Log("Test started");
            var tcs = new TaskCompletionSource<int>();
            var task = tcs.Task;
            Log("Before ContinueWith");
            var secondTask = task.ContinueWith(t =>
            {
                Log("First continuation");
            }, TaskContinuationOptions.RunContinuationsAsynchronously);
            Log("After ContinueWith");
            
            Log("Before SetResult");
            tcs.SetResult(42);
            Log("After SetResult");

            secondTask.Wait();
        }

        [Fact]
        public Task Fourth()
        {
            Log("Test started");
            var client = new HttpClient();
            Log("Before requests");
            var t1 = client.GetAsync("https://httpbin.org/delay/3");
            var t2 = client.GetAsync("https://httpbin.org/delay/4");
            Log("After requests");
            var t3 = Task.WhenAll(t1, t2);
            var finalTask = t3.ContinueWith(t =>
            {
                Log("Requests are completed");
            });
            Log("Test about to end");
            return finalTask;
        }

        [Fact]
        public void LoopsExample()
        {
            Log("Starting");
            async Task<ISet<int>> SomeLoop(int loopIndex)
            {
                ISet<int> threadIds = new HashSet<int>();
                for (int i = 0; i < 10; ++i)
                {
                    await Task.Delay(500);
                    //Log($"On iteration {i} of loop {loopIndex}");
                    threadIds.Add(Thread.CurrentThread.ManagedThreadId);
                }

                return threadIds;
            }

            var tasks = new List<Task<ISet<int>>>();
            for (int i = 0; i < 100000; ++i)
            {
                tasks.Add(SomeLoop(i));
            }
            Log("All loops started");

            var all = Task.WhenAll(tasks);
            all.Wait();
            var allThreadIds = new HashSet<int>();
            foreach (var set in all.Result)
            {
                allThreadIds.UnionWith(set);   
            }
            Log($"Number of used threads: {allThreadIds.Count}");
            Log("Ending");
        }
        
        private readonly ITestOutputHelper _output;

        public TaskExampleTest3(ITestOutputHelper output)
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