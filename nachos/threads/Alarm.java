package nachos.threads;

import java.util.*;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 *
	 * <p><b>Note</b>: Nachos will not function correctly with more than one
	 * alarm.
	 */
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() { timerInterrupt(); }
		});
		this.waitingThreads = new LinkedList<ThreadAndTime>();
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread
	 * that should be run.
	 */
	public void timerInterrupt() {
		for (ThreadAndTime pair : waitingThreads) {
			if (Machine.timer().getTime() >= pair.getTime()) {
				((nachos.threads.KThread) pair.getThread()).ready();
				waitingThreads.remove(pair);
//				System.out.println("Thread " + ((nachos.threads.KThread) pair.getThread()).getName() 
//						+ " was woken up at " + Machine.timer().getTime());
			}
		}
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks,
	 * waking it up in the timer interrupt handler. The thread must be
	 * woken up (placed in the scheduler ready set) during the first timer
	 * interrupt where
	 *
	 * <p><blockquote>
	 * (current time) >= (WaitUntil called time)+(x)
	 * </blockquote>
	 *
	 * @param	x	the minimum number of clock ticks to wait.
	 *
	 * @see	nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// for now, cheat just to get something working (busy waiting is bad)
		// long wakeTime = Machine.timer().getTime() + x;
		// while (wakeTime > Machine.timer().getTime())
		// KThread.yield();
		long wakeTime = Machine.timer().getTime() + x;
//		System.out.println("Thread " + KThread.currentThread().getName() 
//				+ " was added to the queue at " + Machine.timer().getTime());
//		System.out.println("Thread " + KThread.currentThread().getName() 
//				+ " will wait for " + x + " seconds");
		waitingThreads.add(new ThreadAndTime(KThread.currentThread(), wakeTime));
		boolean intStatus = Machine.interrupt().disable();
		KThread.currentThread().sleep();
		Machine.interrupt().restore(intStatus);
	}


	/**
	 * PUT COMMENTS HERE
	 */
	private class ThreadAndTime<KThread,Long> {
		public KThread thread;
		public long time;

		public ThreadAndTime(KThread kThread, long waitTime) {
			thread = kThread;
			time = waitTime;
		}
		public KThread getThread() {
			return thread;
		}
		public long getTime() {
			return time;
		}
	}

	/**
	 * waitingThreads: a queue of KThreads to keep track of the threads waiting on the condition
	 */
	private Queue<ThreadAndTime> waitingThreads;
	
	
	/**
	 * TESTING STUFF
	 */
	private static class PingTest implements Runnable {
		PingTest() {
		}
		public void run() {
			System.out.println("Thread " + KThread.currentThread().getName() + " will ring");
		}
	}

	public static void selfTest() {

		System.out.println("\n Entering Alarm.selfTest()");
		Alarm alarm = new Alarm();
		
		System.out.println("\nTesting 1 thread for 1 second");
		alarm.waitUntil(1);

		System.out.println("\nTesting 1 thread for 10 seconds");
		alarm.waitUntil(10);
		
		System.out.println("\nTesting 1 thread for 1000 seconds");
		alarm.waitUntil(1000);
		
		System.out.println("\nTesting 2 threads for various times");
		KThread wakeUp1 = new KThread(new PingTest()).setName("wakeUp1");
		wakeUp1.fork();
		wakeUp1.join();
		KThread wakeUp2 = new KThread(new PingTest()).setName("wakeUp2");
		wakeUp2.fork();
		wakeUp2.join();
		KThread wakeUp3 = new KThread(new PingTest()).setName("wakeUp3");
		wakeUp3.fork();
		wakeUp3.join();
		alarm.waitUntil(10000);
		alarm.waitUntil(700);
		alarm.waitUntil(1500);
		
		System.out.println("\n Finished testing Alarm.java");
	}
	
}
