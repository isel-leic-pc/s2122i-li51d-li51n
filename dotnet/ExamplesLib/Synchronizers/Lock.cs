using System.Threading;

namespace ExamplesLib.Synchronizers
{
    // WIP
    public sealed class Lock
    {
        private readonly object _mlock = new object();
        
        public void Enter()
        {
            Monitor.Enter(_mlock);
        }

        public void Exit()
        {
            Monitor.Exit(_mlock);
        }

        public Condition NewCondition()
        {
            return new Condition(_mlock);
        }
    }
}
