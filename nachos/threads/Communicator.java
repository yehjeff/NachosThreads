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
    	this.speakerWaiting(communicatorLock);
    	this.listenerWaiting(communicatorLock);
    	this.speakerConfirmed(communicatorLock);
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
    	communicatorLock.acquire();
    	speakerNotSentCount++;
    	while (listenerNotPairedCount == 0 || speakerNotConfirmed) {
    		speakerWaiting.sleep()
    	}
    	listenerWaiting.wake();
    	listenerNotPAired--;
    	wordToSend = word;
    	speakerNotConfirmed = true;
    	speakerConfirmed.sleep();
    	speakerNotConfirmed = false;
    	if (speakerCount > -)
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
    	if (speakerNotSentCount > 0 && !speakerNotConfirmed) {
    		speakerWaiting.wake();
    		speakerNotSentCount--;
    	}
    	listenerNotPairedCount++;
    	listenerWaiting.sleep();
    	int temp = wordToSend; 	
    	speakerConfirmed.wake()
    	return temp;
    	communicatorLock.release();
    }
    
    /**
     * speakerNotSent: The number of speakers who have not sent their word (same as not being paired yet)
     * listenerNotPairedCount: The number of listeners who have not been paired with a speaker yet
     * speakerNotConfirmed: A speaker who has sent its word and is waiting for confirmation from listener.
     * speakerWaiting: a condition variable for speakers waiting to be paired with a listener
     * listenerWaiting: a condition variable for listeners waiting to be paired with a speaer
     * speakerConfirmed: a condition variable to put the confirmed speaker to sleep (the one setting the word)
     */
    
    private Lock communicatorLock;	
    private int speakerNotSentCount = 0;
    private int listenerNotPairedCount = 0;
    private int wordToSend;
    private boolean speakerNotConfirmed = false;
    private Condition speakerWaiting;
    private Condition listenerWaiting;
    private Condition speakerConfirmed;
}
