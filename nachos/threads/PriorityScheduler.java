package nachos.threads;

import nachos.machine.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() {
	}

	/**
	 * Allocate a new priority thread queue.
	 *
	 * @param	transferPriority	<tt>true</tt> if this queue should
	 *					transfer priority from waiting threads
	 *					to the owning thread.
	 * @return	a new priority thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}

	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getPriority();
	}

	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getEffectivePriority();
	}

	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= priorityMinimum &&
				priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
	}

	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			return false;

		setPriority(thread, priority+1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			return false;

		setPriority(thread, priority-1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	public static final int priorityDefault = 1;
	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMinimum = 0;
	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = 7;    

	/**
	 * Return the scheduling state of the specified thread.
	 *
	 * @param	thread	the thread whose scheduling state to return.
	 * @return	the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class PriorityQueue extends ThreadQueue implements Iterable<ThreadState>{
		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			
			Lib.assertTrue(this.threadWithResource == null);
			ThreadState threadState = getThreadState(thread);
			//Lib.assertTrue(threadState.compareTo(this.pickNextThread()) == 1);
			this.threadWithResource = threadState;
			threadState.acquire(this);
		}
		// 
		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me
			if (threadWithResource != null) {
				threadWithResource.resourceQueues.remove(this);
				threadWithResource = null;
			}
			
			if (waitQueue.isEmpty())
				return null;
			ArrayList<ThreadState> waitingThreads = new ArrayList<ThreadState>();
			for (ThreadState threadState : waitQueue){
				waitingThreads.add(threadState);
				threadState.updateEffectivePriority();
			}
			for (ThreadState threadState : waitingThreads){
				waitQueue.remove(threadState);
				threadState.updateEffectivePriority();
				waitQueue.add(threadState);
			}
			threadWithResource = waitQueue.poll();
			threadWithResource.resourceQueues.add(this);
			return threadWithResource.thread;
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 *
		 * @return	the next thread that <tt>nextThread()</tt> would
		 *		return..
		 */
		protected ThreadState pickNextThread() {
			// implement me
			if (waitQueue.isEmpty())
				return null;
			
			for (ThreadState threadState : waitQueue){
				waitQueue.remove(threadState);
				threadState.updateEffectivePriority();
				waitQueue.add(threadState);
			}
			ThreadState nextThreadState = waitQueue.peek();
			return nextThreadState;		
		}

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me (if you want)
		}
		
		public boolean add(ThreadState threadState){
			return this.waitQueue.add(threadState);
		}
		public Iterator<ThreadState> iterator(){
			return this.waitQueue.iterator();
		}


		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;
		
		private java.util.PriorityQueue<ThreadState> waitQueue = new java.util.PriorityQueue<ThreadState>();
		private ThreadState threadWithResource = null;
	}

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue
	 * it's waiting for, if any.
	 *
	 * @see	nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState implements Comparable<ThreadState>{
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 *
		 * @param	thread	the thread this state belongs to.
		 */
		public ThreadState(KThread thread) {
			this.thread = thread;

			setPriority(priorityDefault);
		}

		/**
		 * Return the priority of the associated thread.
		 *
		 * @return	the priority of the associated thread.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 *
		 * @return	the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {
			// implement me
			return cachedEffectivePriority;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 *
		 * @param	priority	the new priority.
		 *///own priority, effective priority of donor, transitive
		public void setPriority(int priority) {
			if (this.priority == priority)
				return;
			int previousPriority = this.priority;
			this.priority = priority;
			if (this.priority > this.cachedEffectivePriority || previousPriority == this.cachedEffectivePriority)
				this.updateEffectivePriority();
			//sadfdfadsSADF

			// implement me
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the
		 * resource guarded by <tt>waitQueue</tt>. This method is only called
		 * if the associated thread cannot immediately obtain access.
		 *
		 * @param	waitQueue	the queue that the associated thread is
		 *				now waiting on.
		 *
		 * @see	nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue) {
			// implement me
			this.setTimeEnqueued(Machine.timer().getTime());
			waitQueue.add(this);
		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 *
		 * @see	nachos.threads.ThreadQueue#acquire
		 * @see	nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue waitQueue) {
			// implement me
			this.resourceQueues.add(waitQueue);
		}	

		
		public long getTimeEnqueued() {
			return timeEnqueued;
		}

		public void setTimeEnqueued(long timeEnqueued){
			this.timeEnqueued = timeEnqueued;
		}
		public void updateEffectivePriority(){
			// do we even need a list of the donors? (how bout just values)
			this.donorList.clear();
			int maxDonorPriority = -1;
			
			for (PriorityQueue resourceQueue : resourceQueues){
				for (ThreadState threadState : resourceQueue) {
					threadState.updateEffectivePriority();
					donorList.add(threadState);
					if (maxDonorPriority < threadState.getEffectivePriority())
						maxDonorPriority = threadState.getEffectivePriority();
				}
			}
			
			if (this.priority < maxDonorPriority)
				this.cachedEffectivePriority = maxDonorPriority;
			else
				this.cachedEffectivePriority = this.priority;
			
			
		}
		
		public int compareTo(ThreadState t){
			if (this.getEffectivePriority() > t.getEffectivePriority())
				return -1;
			else if (this.getEffectivePriority() < t.getEffectivePriority())
				return 1;
			else if (this.getTimeEnqueued() > t.getTimeEnqueued())
				return 1;
			else if (this.getTimeEnqueued() < t.getTimeEnqueued())
				return -1;
			else
				return 0;
		}
		/** The thread with which this object is associated. */	   
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;

		private LinkedList<ThreadState> donorList = new LinkedList<ThreadState>();
		private LinkedList<PriorityQueue> resourceQueues = new LinkedList<PriorityQueue>();
		private long timeEnqueued;
		private int cachedEffectivePriority;
	}
	

	
}
