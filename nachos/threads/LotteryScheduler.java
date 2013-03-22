package nachos.threads;

import nachos.machine.*;
import nachos.threads.PriorityScheduler.PingTest;
import nachos.threads.PriorityScheduler.PriorityQueue;
import nachos.threads.PriorityScheduler.ThreadState;

import java.util.LinkedList;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
    /**
     * Allocate a new lottery scheduler.
     */
    public LotteryScheduler() {
    }
    
    public static final int priorityMinimum = 1; //or 0 if we need that
    public static final int priorityMaximum = Integer.MAX_VALUE;
//    private static int totalNumberTickets = 0;
    
    /**
     * Allocate a new lottery thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer tickets from waiting threads
     *					to the owning thread.
     * @return	a new lottery thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
    	return new LotteryQueue(transferPriority);
    }
    
    protected LotteryThreadState getThreadState(KThread thread) {
  		if (thread.schedulingState == null) {
			thread.schedulingState = new LotteryThreadState(thread);
//			totalNumberTickets += 1;
  		}
		return (LotteryThreadState) thread.schedulingState;
		//error if totalNumberTickets > INTEGER.MAX_VALUE?
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
    
    protected class LotteryQueue extends PriorityQueue{
    	
    	LotteryQueue(boolean transferPriority) {
			super(transferPriority);
		}

		public KThread nextThread() {
    		Lib.assertTrue(Machine.interrupt().disabled());
    		if (threadWithResource != null) {
    			LotteryThreadState previousThreadWithResource = (LotteryThreadState) threadWithResource;
    			threadWithResource = null;
    			previousThreadWithResource.resourceQueues.remove(this);
    			previousThreadWithResource.updateEffectivePriority();			// this is important, cuz now he doesn't have lock so less donations
    		}
    		if (waitQueue.isEmpty()) {
    			return null;
    		}

    		threadWithResource = pickNextThread();
    		this.waitQueue.remove(threadWithResource);
    		threadWithResource.acquire(this);
    		return threadWithResource.thread;
    	}
    	
    	protected LotteryThreadState pickNextThread() {
    		if (waitQueue.isEmpty()) {
    			return null;
    		} else {
    			int totalTickets = 0;
    			for (ThreadState threadState : waitQueue) {
    				totalTickets += threadState.getEffectivePriority();
    			}
    			int randomNumber = (int) (Math.random() * totalTickets); //should range from 0-(totalTickets-1)
    			LotteryThreadState nextThreadState = null;
    			for (ThreadState threadState : waitQueue) {
    				if (randomNumber < threadState.getEffectivePriority()) {
    					nextThreadState = (LotteryThreadState) threadState;
    					break;
    				}
    				randomNumber -= threadState.getEffectivePriority();
    			}
    			return nextThreadState;
    		}
    		// for loop should find a threadState to return....
    	}

    	//protected LinkedList<LotteryThreadState> waitQueue = new LinkedList<LotteryThreadState>();
		//protected LotteryThreadState threadWithResource = null;
    }
    
    protected class LotteryThreadState extends ThreadState { 
    	
    	public LotteryThreadState(KThread thread) {
    		super(thread);
    	}
    	
    	public void setPriority(int priority) {
    		if (this.priority == priority) {
    			return;
    		}
    		this.priority = priority;
    		this.updateEffectivePriority();
    		if (this.doneeList != null && this.doneeList.threadWithResource != null)
    			this.doneeList.threadWithResource.updateEffectivePriority();
    	}
    	
    	public void waitForAccess(PriorityQueue waitQueue) {
    		if (waitQueue.threadWithResource == this) {
    		  waitQueue.threadWithResource.resourceQueues.remove(waitQueue);
    		  waitQueue.threadWithResource.updateEffectivePriority();
    		  waitQueue.threadWithResource = null;
    		}
    		if (waitQueue.threadWithResource != null) { 
    			waitQueue.threadWithResource.updateEffectivePriority();
    			this.doneeList = waitQueue;
    		}
    		waitQueue.add(this);
    	}
    	
    	public void updateEffectivePriority() {
    		int ticketSum = this.priority;
    		for (PriorityQueue resourceQueue : this.resourceQueues) {
    			if (resourceQueue.transferPriority) {
    				for (ThreadState threadState : resourceQueue) {
    					ticketSum += threadState.getEffectivePriority();
    				}
    			}
    		}
    		
    		this.cachedEffectivePriority = ticketSum;
    		
    		if (this.doneeList != null && this.doneeList.threadWithResource != null)
    			this.doneeList.threadWithResource.updateEffectivePriority();
    		
    	}

//		protected LinkedList<LotteryThreadState> doneeList = new LinkedList<LotteryThreadState>();
//		protected LinkedList<LotteryQueue> resourceQueues = new LinkedList<LotteryQueue>();
    }
    
    
    public static void selfTest() {
    	System.out.println("\n Entering PriorityScheduler.selfTest()");
		KThread currentThread = KThread.currentThread();
		
		int threadZeroPriority;
		int threadOnePriority;
		ThreadedKernel.scheduler.decreasePriority();	
		ThreadedKernel.scheduler.increasePriority();	
		ThreadedKernel.scheduler.increasePriority();	
		ThreadedKernel.scheduler.increasePriority();	
		


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
		System.out.println("hi");
		/*
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
		*/
    }
    
}
