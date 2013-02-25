package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 * 
 * Row Row Fight da Powah!
 * 
 * Currently uses Condition.java to create condition variables, but
 * probably should switch to condition2 when that gets finished.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     * Need to initialize condition variables with the lock
     * This looks slightly problematic...
     */
    public Communicator() {
    	this.speakerWaiting = new Condition2(communicatorLock);
    	this.listenerWaiting = new Condition2(communicatorLock);
    	this.speakerConfirmed= new Condition2(communicatorLock);
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
    	System.out.println("\nName of current thread: " + KThread.currentThread().getName());
    	System.out.println("Speaker tries to acquire lock.");
    	communicatorLock.acquire();
    	System.out.println("Lock Successfully acquired.");
    	speakerNotSentCount++;
    	System.out.println("Number of seakers not sent: " + speakerNotSentCount);
    	while (listenerNotPairedCount == 0 || speakerNotConfirmed) {
    		System.out.println("Speaker going to sleep!.");
    		speakerWaiting.sleep();
    	}
    	System.out.println("\nSpeaker waking up!.");
    	listenerWaiting.wake();
    	listenerNotPairedCount--;
    	wordToSend = word;
    	speakerNotConfirmed = true;
    	speakerConfirmed.sleep();
    	speakerNotConfirmed = false;
    	if (speakerNotSentCount > 0)
    		speakerWaiting.wake();
    	communicatorLock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
    	System.out.println("\nListener tries to acquire lock.");
    	communicatorLock.acquire();
    	if (speakerNotSentCount > 0 && !speakerNotConfirmed) {
    		speakerWaiting.wake();
    		speakerNotSentCount--;
    	}
    	listenerNotPairedCount++;
    	System.out.println("\nListener going to sleep!.");
    	listenerWaiting.sleep();
    	System.out.println("\nListener waking up!.");
    	int temp = wordToSend; 	
    	speakerConfirmed.wake();
    	communicatorLock.release();
    	System.out.println("\nSending word!.");
    	return temp;
    }
    
    private static class SpeakerTest implements Runnable {
		SpeakerTest(int word, Communicator com) {
			this.word = word;
			this.com = com;
		}

		public void run() {
			com.speak(word);
		}

		private int word;
		public Communicator com;
	}
    
    private static class ListenerTest implements Runnable {
		ListenerTest(Communicator com) {
			this.com = com;
		}

		public void run() {
			com.listen();
		}

		public Communicator com;
	}
    
    /**
	 * Tests whether this module is working.
	 */
	public static void selfTest() {
		System.out.println("\nCommunicator selfTest Execution");
		System.out.println("Test 1: make two threads");
		System.out.println("Thread 1 = speaker 1, thread 2 = listener 1");
		
		
		Communicator com = new Communicator();
		System.out.println("\nCreating speaker 1, who should go to sleep");
		
		SpeakerTest speaker1 = new SpeakerTest(1, com);
		KThread firstSpeaker = new KThread(speaker1); // word == 1
		firstSpeaker.setName("speaker1");
		firstSpeaker.fork(); 
		KThread firstListener = new KThread(new ListenerTest(com));
		firstListener.fork();
		firstSpeaker.join();// start execution of speaker1
	//	speaker1.run(); // runs speaker 1, who should go straight to sleep
		
	
		
		System.out.println("\nThe set word is: " + com.wordToSend);
		System.out.println("\nSingle Listener-Speaker test complete.");
	}
    
    
    /**
     * speakerNotSent: The number of speakers who have not sent their word (same as not being paired yet)
     * listenerNotPairedCount: The number of listeners who have not been paired with a speaker yet
     * speakerNotConfirmed: A speaker who has sent its word and is waiting for confirmation from listener.
     * speakerWaiting: a condition variable for speakers waiting to be paired with a listener
     * listenerWaiting: a condition variable for listeners waiting to be paired with a speaer
     * speakerConfirmed: a condition variable to put the confirmed speaker to sleep (the one setting the word)
     */
    
    private Lock communicatorLock = new Lock();	
    private int speakerNotSentCount = 0;
    private int listenerNotPairedCount = 0;
    private int wordToSend;
    private boolean speakerNotConfirmed = false;
    private Condition2 speakerWaiting;
    private Condition2 listenerWaiting;
    private Condition2 speakerConfirmed;
}
