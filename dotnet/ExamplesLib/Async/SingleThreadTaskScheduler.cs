using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace ExamplesLib.Async
{
    public class SingleThreadTaskScheduler : TaskScheduler
    {
        private readonly BlockingCollection<Task> _queue = new BlockingCollection<Task>();

        public SingleThreadTaskScheduler(Action<string> logger)
        {
            var thread = new Thread(() =>
            {
                while (true)
                {
                    var task = _queue.Take();
                    logger("About to run task");
                    TryExecuteTask(task);
                }
            });
            thread.Start();
        }
        
        protected override IEnumerable<Task> GetScheduledTasks()
        {
            throw new System.NotImplementedException();
        }

        protected override void QueueTask(Task task)
        {
            _queue.Add(task);
        }

        protected override bool TryExecuteTaskInline(Task task, bool taskWasPreviouslyQueued)
        {
            return false;
        }
    }
}