package nachos.threads;

import nachos.machine.*;

import java.util.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
	/**
	 * Allocate a new condition variable.
	 *
	 * @param	conditionLock	the lock associated with this condition
	 *				variable. The current thread must hold this
	 *				lock whenever it uses <tt>sleep()</tt>,
	 *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
	 */
	public Condition2(Lock conditionLock) {
		this.conditionLock = conditionLock;

		this.waitQueue = new LinkedList<KThread>();

	}

	/**
	 * Atomically release the associated lock and go to sleep on this condition
	 * variable until another thread wakes it using <tt>wake()</tt>. The
	 * current thread must hold the associated lock. The thread will
	 * automatically reacquire the lock before <tt>sleep()</tt> returns.
	 */
	public void sleep() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		waitQueue.add(KThread.currentThread());
		Machine.interrupt().disable();
		conditionLock.release();
		KThread.currentThread().sleep();
		conditionLock.acquire();
		Machine.interrupt().enable();
	}

	/**
	 * Wake up at most one thread sleeping on this condition variable. The
	 * current thread must hold the associated lock.
	 */
	public void wake() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		if (waitQueue.peek() != null) {
			KThread threadToSignal = waitQueue.poll();
			boolean intStatus = Machine.interrupt().disable();
			threadToSignal.ready();
			Machine.interrupt().restore(intStatus);
		}
	}

	/**
	 * Wake up all threads sleeping on this condition variable. The current
	 * thread must hold the associated lock.
	 */
	public void wakeAll() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());    
		while (waitQueue.peek() != null) {
			this.wake();
		}
	}


	/**
	 * waitQueue: a queue of KThreads to keep track of the threads waiting on the condition
	 */

	private Lock conditionLock;
	private Queue<KThread> waitQueue;
	private int numThreadsInQueue;


	/**
	 * TESTING STUFF
	 */
	private static class PingTest implements Runnable {
		PingTest(int which, Condition2 cond, Lock lock) {
			this.which = which;
			this.cond = cond;
			this.lock = lock;
		}

		public void run() {
			lock.acquire();
			if (this.which < 3) {
				numWaiting++;
				cond.sleep();
			} else {
				while (numWaiting > 0) {
					cond.wake();
					numWaiting--;
//					cond.wakeAll();
//					numWaiting = 0;
				}
			}
			lock.release();
		}
		private int which;
		private Condition2 cond;
		private Lock lock;
		private static int numWaiting = 0;
		private static boolean wakeAll = false;
	}


	public static void selfTest() {

		System.out.println("\n Entering Condition2.selfTest()");
		Lock lock = new Lock();
		Condition2 conditionVar = new Condition2(lock);


		System.out.println("\n***Testing sleep and wake***");
		for (int i = 0; i < 3; i++) {
			KThread newThread = new KThread(new PingTest(i+1,conditionVar,lock));
			newThread.setName("" + i);
			newThread.fork();
		}
		new PingTest(0,conditionVar, lock).run();


		System.out.println("\n Finished testing Condition2.java");

	}
}
