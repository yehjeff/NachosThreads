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
	static Alarm alarm;
	static int OAHU = 1;
	static int MOLOKAI = 2;

	public static void selfTest()
	{
		BoatGrader b = new BoatGrader();
		//base case
		System.out.println("\n ***Testing Boats with only 2 children***");
		begin(0, 2, b);
		//add one child to base case
		System.out.println("\n ***Testing Boats with 3 children***");
		begin(0, 3, b);
		//add one adult to base case
		System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
		begin(1, 2, b);
		//add one of each to base case
		System.out.println("\n ***Testing Boats with 3 children, 1 adult***");
		begin(1, 3, b);
		//add multiple children to base case
		System.out.println("\n ***Testing Boats with 13 children***");
		begin(0, 13, b);
		//add multiple adults to base case
		System.out.println("\n ***Testing Boats with 2 children, 13 adults***");
		begin(13, 2, b);
		//add multiple of each to base case
		System.out.println("\n ***Testing Boats with 13 children, 13 adults***");
		begin(13, 13, b);
	}

	public static void begin( int adults, int children, BoatGrader b )
	{
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b;
		// Instantiate global variables here
		lock = new Lock();
		alarm = new Alarm();
		
		numChildMolo = 0;
		numChildOahu = 0;
		numAdultMolo = 0;
		numAdultOahu = 0;
		
		boatLocation = OAHU;
		numChildOnBoat = 0;
		OahuSupposedlyEmpty = false;
		
		isFinished = new Condition2(lock);
		isBoatMolo = new Condition2(lock);
		isBoatOahu = new Condition2(lock);
		
		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.

		//Runnable r = new Runnable() {
		//	public void run() {
		//		SampleItinerary();
		//	}
		//};
		//KThread t = new KThread(r);
		//t.setName("Sample Boat Thread");
		//t.fork();

		//adult thread runnable
		Runnable ad = new Runnable() {
			public void run() {
				AdultItinerary();
			}
		};
		//child thread runnable
		Runnable ch = new Runnable() {
			public void run() {
				ChildItinerary();
			}
		};
		
		//adult thread creation
		for(int i=adults;i>0;i--) {
			KThread adThread = new KThread(ad);
			adThread.setName("Adult Thread #"+i);
			//System.out.println("\n --Creating Adult Thread #"+i);
			adThread.fork();
		};
		//child thread creation
		for(int j=children;j>0;j--) {
			KThread chThread = new KThread(ch);
			chThread.setName("Child Thread #"+j);
			//System.out.println("\n --Creating Child Thread #"+j);
			chThread.fork();
		};
		
		//Jeffnote:does this work for thread creation? I'm not overriding them right?
		
		while (numAdultMolo + numChildMolo  != adults + children) {
			lock.acquire();
			isFinished.sleep();
			lock.release();
		}
		
		//System.out.println("\n Boat.begin() finishing!");
		return;
	}

	static void AdultItinerary()
	{
		/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
		 */
		//Adult Thread Startup
		numAdultOahu++;
		int whereAmI = OAHU;
		lock.acquire();
		//confirm if safe to row
		while (boatLocation != OAHU || numChildMolo == 0) {
			if (numChildOnBoat == 1) {
				isBoatOahu.wake();
			}
			isBoatOahu.sleep();
		}
		//passed while loop, safe to row to Molokai and perma-sleep
		bg.AdultRowToMolokai();
		numAdultOahu--;
		numAdultMolo++;
		boatLocation = MOLOKAI;
		whereAmI = MOLOKAI;
		//wake up a child to row back
		isBoatMolo.wake();
		lock.release();
		sleep(); //Jeffnote:how2currentThread.sleep?
	}

	static void ChildItinerary()
	{
		//Child Thread Startup
		numChildOahu++;
		int whereAmI = OAHU;
		lock.acquire();
			
		while (true) {
			if (whereAmI == MOLOKAI) {	
				numChildOnBoat = 0;
				//Awake on Molokai==Guaranteed Empty Boat
				if (OahuSupposedlyEmpty) {
					OahuSupposedlyEmpty = false; 
					//System.out.println("The Islanders Believe They Are Done");
					isFinished.wake(); //notify begin()
					alarm.waitUntil(Xseconds); //Jeffnote:what to do here? How long2sleep? Maybe like 1.0?
				} else {
					bg.ChildRowToOahu();
					numChildMolo--;
					numChildOahu++;
					boatLocation = OAHU;
					whereAmI = OAHU;
					//only wake and sleep if island not empty
					if (numChildOahu > 1 || numAdultOahu > 0) { //if still more people on Oahu
						isBoatOahu.wake();
						isBoatOahu.sleep(); 
					}
				}
			} else { //whereAmI must = OAHU
				while (boatLocation != OAHU || numChildOnBoat >= 2) { //second check may be unneeded because of lock
					isBoatOahu.sleep();
				}
				numChildOnBoat++;	//this value is reset when they get to Molokai
				if (numChildOnBoat == 1) {
					//first child in boat is rower
					bg.ChildRowToMolokai();
					numChildOahu--;
					numChildMolo++;
					whereAmI = MOLOKAI;
					//check if there is another child to wake to be rider
					if (numChildOahu==1) { //if child alone on Oahu (last child checking and empty)
						boatLocation = MOLOKAI;
						OahuSupposedlyEmpty = true;
					} else { //child not alone, there is at least 2 children on island (prev. rower is asleep)
						isBoatOahu.wake(); //wake child to ride
						isBoatMolo.sleep();	//let rider deal with Molo and rowing back
					}
				} else { //second child in boat is rider, numChildOnBoat==2
					bg.ChildRideToMolokai();
					boatLocation = MOLOKAI;
					numChildOahu--;
					numChildMolo++;
					whereAmI = MOLOKAI;
					//check if island is empty
					if (numChildOahu == 0 && numAdultOahu == 0) {
						OahuSupposedlyEmpty = true;
					}
					//rider doesn't need to sleep or wake anyone on Molo, outer while-loop will happen again
				}
			}
		}	
		lock.release();
	}

	static void SampleItinerary()
	{
		// Please note that this isn't a valid solution (you can't fit
		// all of them on the boat). Please also note that you may not
		// have a single thread calculate a solution and then just play
		// it back at the autograder -- you will be caught.
		System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
		bg.AdultRowToMolokai();
		bg.ChildRideToMolokai();
		bg.AdultRideToMolokai();
		bg.ChildRideToMolokai();
		//lock.acquire();
		//isFinished.wake();
		//lock.release();
	}

}
