package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;
    static int numChildMolo;
    static int numChildOahu;
    static int numAdultMolo;
    static int numAdultOahu;
    static int boatLocation; //1=Oahu,2=Molokai
    static int numChildOnBoat;
    static boolean OahuSupposedlyEmpty;
	static Condition2 isFinished;
	static Condition2 isBoatOahu;
	static Condition2 isBoatMolo;
	static Lock lock;
	static int testytesty;
    
    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
	System.out.println("\n ***Testing Boats with only 2 children***");
	begin(0, 2, b);

//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
//  	begin(1, 2, b);

//  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
//  	begin(3, 3, b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
	bg = b;
	// Instantiate global variables here
	lock = new Lock();
	numChildMolo = 0;
	numChildOahu = 0;
	numAdultMolo = 0;
	numAdultOahu = 0;
	boatLocation = 1; //1=Oahu,2=Molokai
	numChildOnBoat = 0;
	OahuSupposedlyEmpty = false;
	testytesty = 0; //delete later
	isFinished = new Condition2(lock);
	// Create threads here. See section 3.4 of the Nachos for Java
	// Walkthrough linked from the projects page.

	Runnable r = new Runnable() {
	    public void run() {
                SampleItinerary();
            }
        };
        KThread t = new KThread(r);
        t.setName("Sample Boat Thread");
        t.fork();
        
    System.out.println("\n ***Welp Shit Now What, Our design doc sucks... OAHUUUUU***");
    
    while (testytesty == 0) {
    	lock.acquire();
    	isFinished.sleep();
    	lock.release();
    }
    
    }

    static void AdultItinerary()
    {
	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/
    }

    static void ChildItinerary()
    {
    }

    static void SampleItinerary()
    {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
    String whereamI = "I AM ON MOLOKAI";
	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	System.out.println(whereamI);
	bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai();
	bg.ChildRideToMolokai();
	testytesty = 1; //delete later
	lock.acquire();
	isFinished.wake();
	lock.release();
    }
    
}
