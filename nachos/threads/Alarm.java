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
		this.waitingThreads = new PriorityQueue<ThreadAndTime>();
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
				pair.getThread().ready();
				waitingThreads.remove(pair.getThread());
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
		waitingThreads.add(new ThreadAndTime(KThread.currentThread(), wakeTime));
		KThread.currentThread().sleep();
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
	private PriorityQueue<ThreadAndTime> waitingThreads;
}
