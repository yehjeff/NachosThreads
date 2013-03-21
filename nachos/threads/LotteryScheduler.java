package nachos.threads;

import nachos.machine.*;
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
    
    protected ThreadState getThreadState(KThread thread) {
  		if (thread.schedulingState == null) {
			thread.schedulingState = new ThreadState(thread);
			totalNumberTickets += 1;
  		}
		return (ThreadState) thread.schedulingState;
		//error if totalNumberTickets > INTEGER.MAX_VALUE?
    }
    
    protected class LotteryQueue extends PriorityQueue {
    	LotteryQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
		}
    	
    	public KThread nextThread() {
    		Lib.assertTrue(Machine.interrupt().disabled());
    		if (threadWithResource != null) {
    			ThreadState previousThreadWithResource = threadWithResource;
    			threadWithResource = null;
    			previousThreadWithResource.resourceQueues.remove(this);
    			previousThreadWithResource.updateEffectivePriority();			// this is important, cuz now he doesnt have lock so less donations
    		}
    		if (waitQueue.isEmpty()) {
    			return null;
    		}

    		threadWithResource = pickNextThread();
    		threadWithResource.acquire(this);
    		return threadWithResource.thread;
    	}
    	
    	protected ThreadState pickNextThread() {
    		if (waitQueue.isEmpty()) {
    			return null;
    		} else {
    			int totalTickets = 0;
    			for (ThreadState threadState : waitQueue) {
    				totalTickets += threadState.getEffectivePriority();
    			}
    			int randomNumber = rand(0, totalTickets-1);	//????
    			for (ThreadState threadState : waitQueue) {
    				if randomNumber < threadState.getEffectivePriority() {
    					return threadState;
    				}
    				randomNumber -= threadState.getEffectivePriority();
    			}
    		}
    		// for loop should find a threadState to return....
    	}

    	
    	//private LinkedList<ThreadState> waitQueue = new LinkedList<ThreadState>(); //shouldnt this get extended?
    	//private boolean transferPriority; //shouldnt this get extended?
    }
    
    protected class LotteryThreadState extends ThreadState { //how do i do this? not sure..., do i need to extend in the extension?
    	public void setPriority(int priority) {
    		if (this.priority == priority) {
    			return;
    		}
    		this.priority = priority;
    		this.updateEffectivePriority();
    		for (ThreadState doneeThread : this.doneeList) {
    			doneeThread.updateEffectivePriority();
    		}
    	}
    	
    	public void waitForAccess(PriorityQueue waitQueue) {
    		if (waitQueue.threadWithResource == this) {
    		  waitQueue.threadWithResource.resourceQueues.remove(waitQueue);
    		  waitQueue.threadWithResource.updateEffectivePriority();
    		  waitQueue.threadWithResource = null;
    		}
    		if (waitQueue.threadWithResource != null) { 
    			waitQueue.threadWithResource.updateEffectivePriority();
    			this.doneeList.add(waitQueue.threadWithResource);
    		}
    		waitQueue.add(this);
    	}
    	
    	public void updateEffectivePriority() {
    		int ticketSum = this.cachedEffectivePriority();
    		for (PriorityQueue resourceQueue : this.resourceQueues) {
    			if (resourceQueue.transferPriority) {
    				for (ThreadState threadState : resourceQueue) {
    					ticketSum += threadState.getEffectivePriority();
    				}
    			}
    		}
    		for (ThreadState doneeThread : this.doneeList) {
    			doneeThread.updateEffectivePriority();
    		}


    	
    	
    }
    
}
