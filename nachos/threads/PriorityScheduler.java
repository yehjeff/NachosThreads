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
 * 
 * 
 * 
 * 
 * 
 * 
 * changes:
 * 		implemented print function to print for PriorityQueue to print thread with 
 * 				resource's priority/ep and those in the queue
 * 
 * 		Currently updating effective priortiy when we NEED it to be updated kinda (which is when we want to know the next thread in queue)
 * 			but in the doc they said something like you only need to update it when it oculd have changed (could this suggest we should update it whenever 
 * 			it can changes?)
 * 				#### changed:
 * 				#### currently assuming things within the waitingqueue dont change (Cuz they are sleeping). might not be true so...
 * 				####		we could have references to donees (if our priority changed, then just check donee's with mine to see if they shoudl change too)
 * 
 * might need to make sure none of the resourceQueues is the readyQueue 
 * testing:
 * 		transitive priority donation
 * 		lock priority donations 
 * 		maybe some more idk
 * 
 */
public class PriorityScheduler extends Scheduler {
	/**
	 * Allocatee a new priority scheduler.
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
			threadState.acquire(this);
		}
		/*
		 * When asking for next thread, old resource holder needs to relinquish resource and recalculate its effective priority
		 * 
		 */
		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());

			if (threadWithResource != null) {
				ThreadState previousThreadWithResource = threadWithResource;
				threadWithResource = null;
				previousThreadWithResource.resourceQueues.remove(this);
				previousThreadWithResource.updateEffectivePriority();			// this is important, cuz now he doesnt have lock so less donations
			}

			if (waitQueue.isEmpty())
				return null;

			threadWithResource = waitQueue.poll();
			threadWithResource.acquire(this);
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
			if (waitQueue.isEmpty())
				return null;
			return waitQueue.peek();



		}

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			System.out.println("Thread With Resource has priority/effective priority: " + this.threadWithResource.getPriority() +"/" + this.threadWithResource.getEffectivePriority());
			System.out.println("In waitQueue: ");
			for (ThreadState threadState : this.waitQueue){
				System.out.println(threadState.getPriority() + " " + threadState.getEffectivePriority() + " " + threadState.getTimeEnqueued());
			}
		}
		/*
		 * Added this method because I wasn't sure if PriorityQueue's waitQueue was accessable directly
		 */
		public boolean add(ThreadState threadState){
			return this.waitQueue.add(threadState);
		}
		/*
		 * Made this so I can iterator through the PriorityQueue's waitQueue (didnt know i could access it directly, but what evs)
		 */
		public Iterator<ThreadState> iterator(){
			return this.waitQueue.iterator();
		}


		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;

		protected java.util.PriorityQueue<ThreadState> waitQueue = new java.util.PriorityQueue<ThreadState>();
		protected ThreadState threadWithResource = null;
		private long age = 0;
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
			return cachedEffectivePriority;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 *
		 * @param	priority	the new priority.
		 */
		/*
		 * When changing the priority of a thread, those that it is donating to must update their priorities
		 */
		public void setPriority(int priority) {
			if (this.priority == priority)
				return;
			this.priority = priority;
			this.updateEffectivePriority();
			if (doneeList != null && doneeList.threadWithResource != null){
				if (doneeList.threadWithResource.getEffectivePriority() < this.getEffectivePriority()){
					doneeList.threadWithResource.updateEffectivePriority();
				}
			}

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
		/*
		 * When waiting for a resource, must donate to whoever holds the resource.
		 * Also, if for some reason the one trying to wait for the resource already has the resource but calls waitForAccess anyway,
		 * make sure it relinquishes the resources 
		 */
		public void waitForAccess(PriorityQueue waitQueue) {
			// implement me
			this.setTimeEnqueued(waitQueue.age++);
			if (waitQueue.threadWithResource == this){
				waitQueue.threadWithResource.resourceQueues.remove(waitQueue);
				waitQueue.threadWithResource.updateEffectivePriority();
				waitQueue.threadWithResource = null;
			}
			if (waitQueue.threadWithResource != null) {
				if (waitQueue.threadWithResource.getEffectivePriority() < this.getEffectivePriority())
					//				waitQueue.threadWithResource.cachedEffectivePriority = this.getEffectivePriority();
					waitQueue.threadWithResource.updateEffectivePriority();		// JUST IN CASE, actually yea kinda need it
				this.doneeList = waitQueue;
			}
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
		/*
		 * Called whenever a thread gains access to a resource.
		 * It gets a new set of potential donors, so need to update its effective priority
		 */
		public void acquire(PriorityQueue waitQueue) {
			this.resourceQueues.add(waitQueue);
			waitQueue.threadWithResource = this;
			if (doneeList == waitQueue)
				doneeList = null;
	//		if (this.doneeList.contains(waitQueue))		//NOT SURE IF NECESSARY
	//			this.doneeList.remove(waitQueue);		//NOT SURE IF NECESSARY, MAKES NO SENSE
			this.updateEffectivePriority();
		}	
		/*
		 * Returns the time when thread called waitForAccess to begin waiting for a resource
		 */
		public long getTimeEnqueued() {
			return timeEnqueued;
		}
		/*
		 * Sets the time when thread called waitForAccess to begin waiting for a resource
		 */
		public void setTimeEnqueued(long timeEnqueued){
			this.timeEnqueued = timeEnqueued;
		}
		/*
		 * Thread takes the max of hte effective priorities of all of the threads waiting for the resource that itself has,
		 * and sets its own effective priority as that value or its own priority, whichever is greater
		 * 
		 * Also, since its effective priority changes, whoever is receiving donations from it might need to change its effective priority
		 */
		public void updateEffectivePriority(){
			// do we even need a list of the donors? (how bout just values)
			int maxDonorPriority = -1;

			for (PriorityQueue resourceQueue : this.resourceQueues){
				if (resourceQueue.transferPriority) {
					for (ThreadState threadState : resourceQueue) {
						Lib.assertTrue(threadState != this);
						if (maxDonorPriority < threadState.getEffectivePriority())
							maxDonorPriority = threadState.getEffectivePriority();
					}
				}
			}


			if (this.priority < maxDonorPriority) 
				this.cachedEffectivePriority = maxDonorPriority;
			else 
				this.cachedEffectivePriority = this.priority;	
			if (this.doneeList != null && this.doneeList.threadWithResource != null){
				if (this.doneeList.threadWithResource.getEffectivePriority() < this.getEffectivePriority())
					this.doneeList.threadWithResource.updateEffectivePriority();
			}
		}

		/*
		 * Used by the PriorityQueue to order the ThreadStates 
		 */
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

		protected PriorityQueue doneeList = null;
		protected LinkedList<PriorityQueue> resourceQueues = new LinkedList<PriorityQueue>();
		private long timeEnqueued;
		protected int cachedEffectivePriority;
	}
	/*
	 * 
	 */
	public static void selfTest(){
		System.out.println("\n Entering PriorityScheduler.selfTest()");
		KThread currentThread = KThread.currentThread();

		int threadZeroPriority;
		int threadOnePriority;


		System.out.println("\nTesting thread priority (no donation):");
		System.out.println("Thread 0's priority same as  Thread 1");
		KThread newThread = new KThread(new PingTest(1));
		Machine.interrupt().disable();
		threadZeroPriority = ThreadedKernel.scheduler.getEffectivePriority(currentThread);
		threadOnePriority = ThreadedKernel.scheduler.getEffectivePriority(newThread);
		Machine.interrupt().enable();
		System.out.println("Thread 0's effecive priority: " + threadZeroPriority);
		System.out.println("Thread 1's effective priority: " + threadOnePriority);
		newThread.setName("forked thread");
		newThread.fork();
		new PingTest(0).run();
		newThread.join();


		System.out.println("\nTesting thread priority (no donation):");
		System.out.println("Thread 0's priority lower than Thread 1 (calls decreasePriority())");
		ThreadedKernel.scheduler.decreasePriority();
		Machine.interrupt().disable();
		threadZeroPriority = ThreadedKernel.scheduler.getEffectivePriority(currentThread);
		threadOnePriority = ThreadedKernel.scheduler.getEffectivePriority(newThread);
		Machine.interrupt().enable();
		System.out.println("Thread 0's effecive priority: " + threadZeroPriority);
		System.out.println("Thread 1's effective priority: " + threadOnePriority);
		newThread = new KThread(new PingTest(1));
		newThread.setName("forked thread");
		newThread.fork();
		new PingTest(0).run();
		newThread.join();
		ThreadedKernel.scheduler.increasePriority(); // reset them to equal priority



		System.out.println("\nTesting thread priority (no donation):");
		System.out.println("Thread 0's priority higher than Thread 1 (calls increasePriority())");
		ThreadedKernel.scheduler.increasePriority();
		Machine.interrupt().disable();
		threadZeroPriority = ThreadedKernel.scheduler.getEffectivePriority(currentThread);
		threadOnePriority = ThreadedKernel.scheduler.getEffectivePriority(newThread);
		Machine.interrupt().enable();
		System.out.println("Thread 0's effecive priority: " + threadZeroPriority);
		System.out.println("Thread 1's effective priority: " + threadOnePriority);
		newThread = new KThread(new PingTest(1));
		newThread.setName("forked thread");
		newThread.fork();
		new PingTest(0).run();
		newThread.join();
		ThreadedKernel.scheduler.decreasePriority(); // reset them to equal priorities



		System.out.println("\nTesting thread priority donation with joins:");
		System.out.println("Thread 0's priority higher than Thread 1");
		System.out.println("but Thread 0 calls join on Thread 1");
		newThread = new KThread(new PingTest(1));
		ThreadedKernel.scheduler.increasePriority();
		Machine.interrupt().disable();
		threadZeroPriority = ThreadedKernel.scheduler.getEffectivePriority(currentThread);
		threadOnePriority = ThreadedKernel.scheduler.getEffectivePriority(newThread);
		Machine.interrupt().enable();
		System.out.println("Before call to join: Thread 0's effecive priority: " + threadZeroPriority);
		System.out.println("\t\tThread 1's effective priority: " + threadOnePriority);
		newThread.setName("forked thread");
		newThread.fork();
		newThread.join();
		Machine.interrupt().disable();
		threadZeroPriority = ThreadedKernel.scheduler.getEffectivePriority(currentThread);
		threadOnePriority = ThreadedKernel.scheduler.getEffectivePriority(newThread);
		Machine.interrupt().enable();
		System.out.println("After call to join: Thread 0's effecive priority: " + threadZeroPriority);
		System.out.println("\t\tThread 1's effective priority: " + threadOnePriority);
		ThreadedKernel.scheduler.decreasePriority(); // reset them to equal priorities
		new PingTest(0).run();


		System.out.println("\nTesting thread priority donation with joins:");
		System.out.println("Thread 0's priority LOWER than Thread 1");
		System.out.println("but Thread 0 calls join on Thread 1");
		newThread = new KThread(new PingTest(1));
		ThreadedKernel.scheduler.decreasePriority();
		Machine.interrupt().disable();
		threadZeroPriority = ThreadedKernel.scheduler.getEffectivePriority(currentThread);
		threadOnePriority = ThreadedKernel.scheduler.getEffectivePriority(newThread);
		Machine.interrupt().enable();
		System.out.println("Before call to join: Thread 0's effecive priority: " + threadZeroPriority);
		System.out.println("\t\tThread 1's effective priority: " + threadOnePriority);
		newThread.setName("forked thread");
		newThread.fork();
		newThread.join();
		new PingTest(0).run();
		Machine.interrupt().disable();
		threadZeroPriority = ThreadedKernel.scheduler.getEffectivePriority(currentThread);
		threadOnePriority = ThreadedKernel.scheduler.getEffectivePriority(newThread);
		Machine.interrupt().enable();
		System.out.println("After call to join: Thread 0's effecive priority: " + threadZeroPriority);
		System.out.println("\t\tThread 1's effective priority: " + threadOnePriority);
		ThreadedKernel.scheduler.increasePriority(); // reset them to equal priorities



		System.out.println("\nTesting transitive thread priority donation with joins:");
		System.out.println("Thread 0's priority higher than Thread 1 which is higher than Thread 2 ");
		System.out.println("but Thread 0 has lock and Thread 1 waiting for lock");
		newThread = new KThread(new JoinTest(1));
		newThread.fork();
		ThreadedKernel.scheduler.increasePriority();
		ThreadedKernel.scheduler.increasePriority();	
		ThreadedKernel.scheduler.increasePriority();	
		ThreadedKernel.scheduler.increasePriority();	
		ThreadedKernel.scheduler.increasePriority();	
		Machine.interrupt().disable();
		threadZeroPriority = ThreadedKernel.scheduler.getEffectivePriority(KThread.currentThread());
		Machine.interrupt().enable();
		System.out.println("Before call to join: Thread 0's effective priority: " + threadZeroPriority);
		newThread.join();
		ThreadedKernel.scheduler.decreasePriority();
		ThreadedKernel.scheduler.decreasePriority();	
		ThreadedKernel.scheduler.decreasePriority();	
		ThreadedKernel.scheduler.decreasePriority();	
		ThreadedKernel.scheduler.decreasePriority();


	}
	protected static class JoinTest implements Runnable {
		JoinTest(int which){
			this.which = which;
		}
		public void run() {
			if (this.which == 1){
				KThread newThread = new KThread(new JoinTest(2));
				ThreadedKernel.scheduler.increasePriority();
				newThread.fork();	
				Machine.interrupt().disable();
				int threadZeroPriority = ThreadedKernel.scheduler.getEffectivePriority(KThread.currentThread());
				Machine.interrupt().enable();
				System.out.println("After Thread 0's join call: Thread 1's effecive priority: " + threadZeroPriority);

				newThread.join();
			} else {
				Machine.interrupt().disable();
				int threadZeroPriority = ThreadedKernel.scheduler.getEffectivePriority(KThread.currentThread());
				Machine.interrupt().enable();
				System.out.println("After Thread 1's join call: Thread 2's effecive priority: " + threadZeroPriority);
			}

		}
		private int which;
	}
	protected static class PingTest implements Runnable {
		PingTest(int which) {
			this.which = which;
		}

		public void run() {
			Machine.interrupt().disable();
			int threadZeroPriority = ThreadedKernel.scheduler.getEffectivePriority(KThread.currentThread());
			int threadOnePriority = ThreadedKernel.scheduler.getEffectivePriority(KThread.currentThread());
			Machine.interrupt().enable();
			System.out.println("After call to join: Thread 1's effecive priority: " + threadZeroPriority);
			for (int i=0; i<pingTestLoops; i++) {
				System.out.println("*** thread " + which + " looped " + i + " times");
				if ((which == 1) && (onedone == false)) {
					slowCount = i;
				}
				if (i == pingTestLoops-1) {
					if (onedone) {
						System.out.println("the other thread finished (" + pingTestLoops + " loops) after the lower priority thread loooped " + slowCount + " times");
					}
					else {
						onedone = true;
					}
				}
				KThread.yield();
			}
		}

		private int which;
	}

	//global variables used only for testing
	protected static int pingTestLoops = 10;
	protected static boolean onedone = false; //marks if one of the threads have finished
	protected static int slowCount = 0; //marks the loop iteration of the slower thread

}
