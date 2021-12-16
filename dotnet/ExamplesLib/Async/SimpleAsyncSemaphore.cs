using System.Collections.Generic;
using System.Threading.Tasks;

namespace ExamplesLib.Async
{
    public class SimpleAsyncSemaphore
    {
        private static readonly Task<bool> TrueTask = Task.FromResult(true);
        
        private readonly object _lock = new object();
        private readonly LinkedList<Request> _requests = new LinkedList<Request>();
        private int _units;

        public SimpleAsyncSemaphore(int initialUnits)
        {
            _units = initialUnits;
        }
        private class Request : TaskCompletionSource<bool>
        {
            public Request()
            :base(TaskCreationOptions.RunContinuationsAsynchronously)
            {
                
            }
        }

        public Task<bool> AcquireAsync()
        {
            lock (_lock)
            {
                // fast-path
                if (_units > 0 && _requests.Count == 0)
                {
                    _units -= 1;
                    return TrueTask;
                }
                // asynchronous wait path
                var request = new Request();
                _requests.AddLast(request);

                return request.Task;
            }
        }

        public void Release()
        {
            lock (_lock)
            {
                var requestNode = _requests.First;
                if (requestNode == null)
                {
                    _units += 1;
                }
                else
                {
                    _requests.RemoveFirst();
                    requestNode.Value.SetResult(true);
                }
            }
        }
    }
}