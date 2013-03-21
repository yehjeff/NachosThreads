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
    
    // Version 1
    public void speak(int word) {
    	communicatorLock.acquire();
    	speakerNotSentCount++;
    	while (listenerNotPairedCount == 0 || speakerHasConfirmed) { // a 2nd speaker can screw over the first speaker, how to prevent?
    		speakerWaiting.sleep();
    	}
    	speakerHasConfirmed = true;
    	listenerWaiting.wake();
    	listenerNotPairedCount--;
    	wordToSend = word;
    	speakerConfirmed.sleep();
    	speakerHasConfirmed = false; //when we hand over to the next speaker why set speakerNotConfirmed to false, just leave it
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
    	communicatorLock.acquire();
    	listenerNotPairedCount++;
    	if (speakerNotSentCount > 0 && !speakerHasConfirmed && listenerConfirmation == false) {
    		speakerWaiting.wake();
    		speakerNotSentCount--;
    		listenerConfirmation = true; // we don't want a 2nd listener waking up a 2nd speaker.
    										// can this break other things? 
    										// as long as the 2nd listener comes after a speaker or a
    	}   	
    	listenerWaiting.sleep();
    	int temp = wordToSend; 	
    	speakerConfirmed.wake();
    	communicatorLock.release();
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

		/**
		 * Test 1: Basic S1, L1 Test
		 * Note: passed by version 1
		 */
		
		/*
		KThread firstListener = new KThread(new ListenerTest(com));
		firstListener.setName("listener1");
		firstListener.fork();
		
		
		SpeakerTest speaker1 = new SpeakerTest(10, com);
		KThread firstSpeaker = new KThread(speaker1); // word == 1
		firstSpeaker.setName("speaker1");
		firstSpeaker.fork(); 
	
		
		firstSpeaker.join();// start execution of speaker1
	//	speaker1.run(); // runs speaker 1, who should go straight to sleep
		
		System.out.println("\nThe set word is: " + com.wordToSend);
		System.out.println("\nSingle Listener-Speaker test complete.");
		
		
		*/
		
		/**
		 * Test 2: 2 speakers, 2 listeners
		 */
		
		// 2a) S1 S2 L1 L2
		// Note: is passed by version 1
		
		
		
		SpeakerTest speaker1 = new SpeakerTest(5, com);
		KThread firstSpeaker = new KThread(speaker1); // word == 5
		firstSpeaker.setName("speaker1");
		firstSpeaker.fork();
		
		SpeakerTest speaker2 = new SpeakerTest(10, com);
		KThread Speaker2 = new KThread(speaker2); // word == 10
		Speaker2.setName("speaker2");
		Speaker2.fork(); 
		
		
		KThread firstListener = new KThread(new ListenerTest(com));
		firstListener.setName("listener1");
		firstListener.fork();
		
		KThread Listener2 = new KThread(new ListenerTest(com));
		Listener2.setName("listener2");
		Listener2.fork();
		
		
		// 2b L1 L2 S1 S2  
		// Passed by version 1
		
		/*
		KThread firstListener = new KThread(new ListenerTest(com));
		firstListener.setName("listener1");
		firstListener.fork();
		
		KThread Listener2 = new KThread(new ListenerTest(com));
		Listener2.setName("listener2");
		Listener2.fork();
		
		SpeakerTest speaker1 = new SpeakerTest(5, com);
		KThread firstSpeaker = new KThread(speaker1); // word == 5
		firstSpeaker.setName("speaker1");
		firstSpeaker.fork();
		
		SpeakerTest speaker2 = new SpeakerTest(10, com);
		KThread Speaker2 = new KThread(speaker2); // word == 10
		Speaker2.setName("speaker2");
		Speaker2.fork(); 
		*/
	
		// Test 2c) Interleaving S1 L1 S2 L2
		// Passed by Version 1
		
		/*
		SpeakerTest speaker1 = new SpeakerTest(5, com);
		KThread firstSpeaker = new KThread(speaker1); // word == 5
		firstSpeaker.setName("speaker1");
		firstSpeaker.fork();
		
		
		KThread firstListener = new KThread(new ListenerTest(com));
		firstListener.setName("listener1");
		firstListener.fork();
		
		SpeakerTest speaker2 = new SpeakerTest(10, com);
		KThread Speaker2 = new KThread(speaker2); // word == 10
		Speaker2.setName("speaker2");
		Speaker2.fork(); 
		
		KThread Listener2 = new KThread(new ListenerTest(com));
		Listener2.setName("listener2");
		Listener2.fork();
		*/
		
		// Test 2d) L1 S1 L2 S2
		// Passed by version 1
		
		/*
		KThread firstListener = new KThread(new ListenerTest(com));
		firstListener.setName("listener1");
		firstListener.fork();
		
		
		SpeakerTest speaker1 = new SpeakerTest(5, com);
		KThread firstSpeaker = new KThread(speaker1); // word == 5
		firstSpeaker.setName("speaker1");
		firstSpeaker.fork();
	
		KThread Listener2 = new KThread(new ListenerTest(com));
		Listener2.setName("listener2");
		Listener2.fork();
		
		SpeakerTest speaker2 = new SpeakerTest(10, com);
		KThread Speaker2 = new KThread(speaker2); // word == 10
		Speaker2.setName("speaker2");
		Speaker2.fork(); 
		*/
		
		
		// Test 2e) L1 S1 S2 L2
		// Passed by version 1
				
			/*	
		KThread firstListener = new KThread(new ListenerTest(com));
		firstListener.setName("listener1");
		firstListener.fork();
				
				
		SpeakerTest speaker1 = new SpeakerTest(5, com);
		KThread firstSpeaker = new KThread(speaker1); // word == 5
		firstSpeaker.setName("speaker1");
		firstSpeaker.fork();
				
		SpeakerTest speaker2 = new SpeakerTest(10, com);
		KThread Speaker2 = new KThread(speaker2); // word == 10
		Speaker2.setName("speaker2");
		Speaker2.fork(); 
		
		KThread Listener2 = new KThread(new ListenerTest(com));
		Listener2.setName("listener2");
		Listener2.fork();
			*/	
						
		Speaker2.join();// join with the last thread to run

		System.out.println("\nThe set word is: " + com.wordToSend);
		System.out.println("\nSingle Listener-Speaker test complete.");
		
	}
    
    
    /**
     * speakerNotSent: The number of speakers who have not sent their word (same as not being paired yet)
     * listenerNotPairedCount: The number of listeners who have not been paired with a speaker yet
     * speakerNotConfirmed: A speaker who has sent its word and is waiting for confirmation from listener.
     * speakerWaiting: a condition variable for speakers waiting to be paired with a listener
     * listenerWaiting: a condition variable for listeners waiting to be paired with a speaker
     * speakerConfirmed: a condition variable to put the confirmed speaker to sleep (the one setting the word)
     */
    
    private Lock communicatorLock = new Lock();	
    private int speakerNotSentCount = 0;
    private int listenerNotPairedCount = 0;
    private int wordToSend;
    private boolean speakerHasConfirmed = false;
    private boolean listenerConfirmation = false;
    private Condition2 speakerWaiting;
    private Condition2 listenerWaiting;
    private Condition2 speakerConfirmed;
}
