//------------------------------------------------------------------------
//File Name: simRaymond.java
//Input: User interface available to choose load and node count.
//Output: The screen will print the messages that are being sent.
//		At the end of the simulation the number of messages per critical
//		section will be displayed
//Basic Operation: The Mediator pattern is used. There is one mediator
//		and a number of nodes. All nodes are threads as well as the 
//		mediator. All messages that a node wishes to
//		send to another node is sent to the mediator's message queue. The
//		mediator will remove a message from the queue and service the 
//		message.
//Reference: Kerry Raymond's paper
//		"A Tree-Based Algorithm for Distributed Mutual Exclusion"
//		ACM Vol. 7, No. 1, February 1989, Pages 61-77
//------------------------------------------------------------------------


import java.util.LinkedList;
import java.io.*;

// Debug setting
interface Debug{
	boolean DEBUG = false;
	//boolean DEBUG = true;
}

//	Three types of load 
interface SimLoad {
	int LOW = 0,
		MED = 1,
		HIGH = 2;
}		
	
//Four message types.
//These message types come from Kerry Ramond's paper 
interface MessageType {
	int REQUEST_CS = 0,
		PASS_REQUEST = 1,
		PASS_TOKEN = 2,
		EXIT_CS = 3;
}

//Message structure
//The Mediator class will have a Message queue
class Message implements MessageType {
	public Node sender;
	public Node receiver;
	public int message;

	public Message(Node source, Node target, int type) {
		sender = source;
		receiver = target;
		message = type;
	}	
}

//class to hold the interarrival Time
//	and execution time.
//  The times are generated during the
//	contruction of a Node
class Time {
	public int intArrivalTime;
	public int executionTime;
}	

//Mediator class
//queue messages from nodes. Service messages
class Mediator extends Thread implements MessageType, SimLoad, Debug {
	private int messageCnt = 0;
	private int requestCnt = 0;
	private int tokenCnt = 0;
	private int servicedCSCnt = 0;
	private int simLoad;		//what process is sending/receiving
	private LinkedList <Message>messageQList;
	private Message currentMessage;
	private int nodeCnt;
	private LinkedList <Node>nodeList;
	private LinkedList <Node>nodeDoneList;
	private PrintWriter logPW;

	//Contructor
	//Create a message queue, and a number of semamphores
	public Mediator (int inNodeCnt, int inSimLoad) {
	//public Mediator (int inNodeCnt) {
		messageQList = new LinkedList<Message>();
		nodeList = new LinkedList<Node>();
		nodeDoneList = new LinkedList<Node>();
		nodeCnt = inNodeCnt;
		simLoad = inSimLoad;

		try {
			logPW = new PrintWriter( new FileWriter( "simRaymondLog.txt", true ) );
		}
		catch ( Exception e ) {
  	  		System.err.println("Error opening ");
		}
	}
	
	//sendMessage
	//In: Message
	//Out: void
	//messages are sent by nodes.
	//synchronzied so that only one node can access the message queue at a time 
	public synchronized void sendMessage(Message inMessage) {
		messageQList.add(inMessage);
	}

	//getMessageCnt
	//In: void
	//Out: messageCnt
	public int getMessageCnt() {
		return messageCnt;
	}
	
	//sendDone
	//In: Node
	//Out: void
	//When a node has serviced all 500 critical sections
	//then it notifies the mediator that it is done.
	public synchronized void sendDone(Node inNode) {
		nodeDoneList.add(inNode);
		if (DEBUG) {
			System.out.println("MED Done CNT: " + nodeDoneList.size());
		}
	}
	
	private synchronized int getNodeDoneListSize(){
		return nodeDoneList.size();
	}

	private synchronized int getMessageQSize() {
		return messageQList.size();
	}

	//medSetAllDone
	//In: void
	//Out: void
	//When the mediator has received a done signal from all
	//nodes then it will send a kill message to all nodes.
	private void medSetAllDone() {
		Node nTemp;
	
		while (nodeDoneList.size() != 0) {//
			nTemp = nodeDoneList.removeFirst();
			nTemp.nodeSetAllDone();
		}	
	}
	
	//showStats
	//In: void
	//Out: void
	//When all nodes are done then this function will display
	//the number of messages per critical section
	private void showStats() {
		System.out.println("************************");
		System.out.print("Load: ");
		if (simLoad == LOW)
			System.out.println("LOW");
		else if (simLoad == MED)
			System.out.println("MED");
		else if (simLoad == HIGH)
			System.out.println("HIGH");

		System.out.println("Number of nodes: " + nodeCnt);
		System.out.println("Number of critical section: " + requestCnt);
		System.out.println("Number of critical sections serviced: " + servicedCSCnt);
		System.out.println("Number of messages: " + messageCnt);
		System.out.println("Number of messages per request: " + (double)messageCnt/requestCnt);
		System.out.println("Number of token passes: " + tokenCnt);
		System.out.println("Number of token passes per critical section: " + (double)tokenCnt/requestCnt);
	}

	//writeStats
	//In: void
	//Out: void
	//Write all the nodes and critical section stats
	private void writeStats() {
		logPW.print("Load: ");
		if (simLoad == LOW)
			logPW.println("LOW");
		else if (simLoad == MED)
			logPW.println("MED");
		else if (simLoad == HIGH)
			logPW.println("HIGH");

		logPW.println("Number of nodes: " + nodeCnt);
		logPW.println("Number of critical section: " + requestCnt);
		logPW.println("Number of critical sections serviced: " + servicedCSCnt);
		logPW.println("Number of messages: " + messageCnt);
		logPW.println("Number of messages per request: " + (double)messageCnt/requestCnt);
		logPW.println("Number of token passes: " + tokenCnt);
		logPW.println("Number of token passes per critical section: " + (double)tokenCnt/requestCnt);
		
		logPW.close();
	}	

	private synchronized void processMessageQList(){
		Node sender;
		Node receiver;

		if (getMessageQSize() != 0) {
			currentMessage = messageQList.removeFirst();
			sender = currentMessage.sender;
			receiver = currentMessage.receiver;
			
			switch (currentMessage.message) {
				case REQUEST_CS:
					System.out.println(System.currentTimeMillis() + ": " + sender.getID() + 
						" requested the CS, " + (requestCnt - servicedCSCnt) + " Pending");
					sender.enqueue(sender);
					sender.assignPrivilege();
					sender.makeRequest();
					requestCnt++;
					break;
				
				case PASS_REQUEST:
					System.out.println(System.currentTimeMillis() + ": " + sender.getID() + 
										" sent request to " + receiver.getID());
					receiver.enqueue(sender);
					receiver.assignPrivilege();
					receiver.makeRequest();
					messageCnt++;
					break;

				case PASS_TOKEN:
					System.out.println(System.currentTimeMillis() + ": " + sender.getID() + 
										" passed the token to " + receiver.getID());
					receiver.ptrUpdate();
					receiver.assignPrivilege();
					receiver.makeRequest();
					messageCnt++;
					tokenCnt++;
					break;

				case EXIT_CS:
					System.out.println(System.currentTimeMillis() + ": " + sender.getID() + 
										" exited the CS ");
					sender.clearUsing();
					sender.assignPrivilege();
					sender.makeRequest();
					servicedCSCnt++;
					break;

				default:
					System.out.println("Unknow message type found");
			}//switch
		}//if
	}//processMessageQList

	//run
	//In: void
	//Out: void
	//Overloaded function of Thread. When a thread is started
	//this is the function that is run.
	//This funtion will pop a message and service it.
	public void run() {
		if (DEBUG) {	
			synchronized(this){
			System.out.println(System.currentTimeMillis() + ":----Mediator Unpaused----"); 
			System.out.println(System.currentTimeMillis() + ": Mediator.nodeDoneList.size: " + nodeDoneList.size());
			System.out.println(System.currentTimeMillis() + ": Mediator.nodeCnt: " + nodeCnt);
			System.out.println(System.currentTimeMillis() + ": Mediator.messageQList.size: " + messageQList.size());
			System.out.println("-------------------------------------");
			}
		}

		while(getNodeDoneListSize() != nodeCnt || getMessageQSize() != 0) {
			processMessageQList();
		}//while
		if (DEBUG) {
			System.out.println(System.currentTimeMillis() + ": Mediator Out of While loop");
		}
		medSetAllDone();
		showStats();
	}//run

}//Mediator

//Node class
//Basic operation: The main loop is wait some amount of time then
//send a message requesting the critical section. Sleep until the
//token is received.  Once the token is received sleep again for
//some amount of time to simulate execution time. Repeat until
//all 500 request have been serviced.
//class Node extends Thread implements MessageType, SimLoad{
class Node extends Thread implements MessageType, SimLoad, Debug{
	private static int idNum = 1;
	private int id;
	
	private Mediator nodeMediator;
	private int simLoad;
	private int nodeCnt;
	
	public Node	holderPtr;

	public Object tokenObject;
	private Object allDoneObject;
	private static Object allAliveObject = new Object();

	boolean	usingBoolean;
	boolean	askedBoolean;
	public LinkedList <Node> requestList;
	public LinkedList <Time> timeList;
	Time time;

	//Node constructor
	//create a linked list of Time, which holds the interarrival times
	//and execution time. Also create a number of semephores.
	public Node(int inNodeCnt, int inSimLoad, Mediator inMed) {
		usingBoolean = false;
		askedBoolean = false;
		requestList = new LinkedList<Node>();
		timeList = new LinkedList<Time>();
		tokenObject = new Object();
		allDoneObject = new Object();
		id = idNum++;
		nodeMediator = inMed;
		simLoad = inSimLoad;
		nodeCnt = inNodeCnt;

		genRanTime();

		//System.out.println(System.currentTimeMillis() + ": Node Constr: " + id);
	}		
	
	//setNode
	//In: Node
	//Out: void
	//Assign the holderPtr to this node's parent
	public void setNode(Node inNode) {
		holderPtr = inNode;
	}

	//getID
	//In: void
	//Out: int
	//Return the node ID.
	public synchronized int getID() {
		return id;
	}

	//ptrUpdate
	//In: void
	//Out: void
	//When the node gets the token it is its
	//own parent.
	public synchronized void ptrUpdate() {
		holderPtr = this;
	}

	//clearUsing
	//In: void
	//Out: void
	//Clear the usingBoolean variable. This means
	//that the node is no longer using the token
	public synchronized void clearUsing() {
		usingBoolean = false;
	}	

	//genRanTime
	//In: void
	//Out: void
	//Generate 500 exponentially distributed
	//interarrival and execution times.
	private void genRanTime() {
		
		double rn;
		int lambda = 1;
		double intArrivalTime;
		int scaledIAT;
		int mu;
		double invLambda;
		double invMu;
		double exeTime;
		int scaledET;
		int accumalateTime = 0;

		switch (simLoad) {
			case LOW:
					mu = (int)((nodeCnt * lambda) / (0.1));
					break;

			case MED:
					mu = (int)((nodeCnt * lambda) / (0.5));
					break;

			case HIGH:
					mu = (int)((nodeCnt * lambda) / (0.8));
					break;

			default :
					mu = 0;
				System.out.println("Invalid simload");
		}

		invLambda = (double) 1/lambda;
		invLambda = -1 * invLambda;
		invMu = (double) 1/mu;
		invMu = -1 * invMu;

		for (int i=0; i<500; i++) {
			Time t1 = new Time();

			scaledIAT = 0;
			while (scaledIAT == 0) {
				rn = Math.random();
				intArrivalTime =  invLambda * Math.log(rn);
				scaledIAT = (int)(100 * intArrivalTime);
			}
			t1.intArrivalTime = scaledIAT;

			scaledET = 0;
			while (scaledET == 0) {
				rn = Math.random();
				exeTime = invMu * Math.log(rn);
				scaledET = (int)(100 * exeTime);
			}
			t1.executionTime = scaledET;
			timeList.add(t1);
			if (DEBUG) {
				System.out.println(System.currentTimeMillis() + ": " + "id: " + id);
				System.out.println(System.currentTimeMillis() + ": " + "ArrivalTime: " + t1.intArrivalTime);
				System.out.println(System.currentTimeMillis() + ": " + "ExecutionTime: " + t1.executionTime);
				System.out.println("------------------");
			}
		}
	}

//--------------------Raymond's Algorithm---------------------
//This portion has been taken from Raymond's paper
//All three functions are called from the mediator.
	
	public synchronized void assignPrivilege() {
		if (holderPtr == this && usingBoolean == false && requestList.size() != 0) {
			holderPtr = requestList.removeFirst();
			askedBoolean = false;
			if (DEBUG) {
				System.out.println(System.currentTimeMillis() + ":" +  id + ": holderPtr==this, usingBoolean==F, requestList.size!=0");
			}
			if (holderPtr == this) {
				usingBoolean = true;
				synchronized (tokenObject) {
				//enter CS
					tokenObject.notify();
				}
			}
			else {
				//send privilage to holder
				nodeMediator.sendMessage(new Message(this, holderPtr, PASS_TOKEN));
			}	
		}
	}

	public synchronized void makeRequest() {
		if (holderPtr != this && requestList.size() != 0 && askedBoolean == false) {
			//send request to holder
			nodeMediator.sendMessage(new Message(this, holderPtr, PASS_REQUEST));
			askedBoolean = true;
		}
	}

	public synchronized void enqueue(Node sender) {
			requestList.add(sender);
	}

//----------------------------------------------
	//run
	//In: void
	//Out: void
	//Basic Operation: Wait the interarrival time then
	//request for a critical section. Enter the critical
	//section and sleep. Exit the critical section. Repeat
	//500 times.
	public void run() {
		System.out.println(System.currentTimeMillis() + ": Started Node id: " + id);

		if (id == nodeCnt) {
			try {
				synchronized (this) {
					this.wait(550);
				}	
			}	
			catch (InterruptedException e) {}
			System.out.println(System.currentTimeMillis() + ": " + id + " waking everyone up");
			synchronized (allAliveObject) {
				allAliveObject.notifyAll();
			}
		}
		else {
			try {
				System.out.println(System.currentTimeMillis() + ": " +  id + " is waiting");
				synchronized (allAliveObject) {
					allAliveObject.wait();
				}	
			}	
			catch (InterruptedException e) {}
		}
		System.out.println(System.currentTimeMillis() + ": " + id + " is running");

		while (timeList.size() != 0) {
			stall();
			requestToEnterCS();
			enterCS();
			exitCS();
		}
		
		try {
			synchronized (allDoneObject) {
			nodeMediator.sendDone(this);
				allDoneObject.wait();
			}	
		}	
		catch (InterruptedException e) {}
	}

	private synchronized void stall () {
		if (timeList.size() != 0) {
			time = timeList.removeFirst();
		
			try {
				wait(time.intArrivalTime);
			}
			catch (InterruptedException e) {}
		}
	}

	//requestToEnterCS
	//In: void
	//Out: void
	//Send a message to the nodeMediatoriator requesting the CS. Wait until
	//the token is received.
	private void requestToEnterCS() {
		nodeMediator.sendMessage(new Message(this, holderPtr, REQUEST_CS));
		if (DEBUG) {
			System.out.println(System.currentTimeMillis() + ": " + id + ": Sent Message");
		}
		try {
			synchronized (tokenObject) {
				tokenObject.wait();
			}		
		}
		catch (InterruptedException e) {
			System.out.println(System.currentTimeMillis() + ": " + id + ": InterruptedException thrown");

		}
	}

	//enterCS
	//In: void
	//Out: void
	//Enter the critical section just waits the execution time.
	private void enterCS() {
		//System.out.println(System.currentTimeMillis() + ", " + id + ", entered CS");
		try {
			synchronized (this) {
				this.wait(time.executionTime);
			}	
		}
		catch (InterruptedException e) {}
	}
	
	//exitCS
	//In: void
	//Out: void
	//Send an EXIT_CS message to the nodeMediatoriator. 
	private void exitCS() {
		nodeMediator.sendMessage(new Message(this, holderPtr, EXIT_CS));
	}

	//nodeSetAllDone
	//In: void
	//Out: void
	//When a node is all done the thread will not die
	//unit all nodes are done. This is called from the
	//mediator.
	public synchronized void nodeSetAllDone() {
		synchronized (allDoneObject) {
			allDoneObject.notify();
		}	
	}
	
	//getSimLoad
	//In: void
	//Out: simLoad
	public synchronized int getSimLoad() {
		return simLoad;
	}
}

//Class: simRaymond
//The client class. Creates a simulation by creating
//a mediator and a number of nodes
class simRaymond implements SimLoad {
	public static LinkedList <Node>nodeList = new LinkedList<Node>();
	public static Mediator simMediator;
	public static Node nNode;
	public static int numNode;
	public static int simLoad;
	public static int i;

	public static final boolean DEV_MODE = true;
	
	public static String getInput() { 		
		String strGen;
		try {
			InputStreamReader isr = new InputStreamReader(System.in);
			BufferedReader br = new BufferedReader (isr);
			strGen = br.readLine();
		}
		catch (IOException e)
		{ 
			strGen = "ERROR";
			System.out.print("Input Error!!!"); 
		}
		return strGen;				
	}

	public static void simUI() {
		String temp;
		
		System.out.println("---------------------------"); 
		System.out.println("-       Simulation        -"); 
		System.out.println("---------------------------"); 
		System.out.println(); 
		System.out.print("Please enter the number of nodes: "); 
		temp = getInput();
		
		try {
			numNode = Integer.parseInt(temp);
		}
		catch (NumberFormatException e) {
			System.out.println("INT PARSE ERROR");
			numNode = 3;
		}

		simLoad = -1;
		while(simLoad < LOW || simLoad > HIGH) {
			System.out.println("1. LOW"); 
			System.out.println("2. MED"); 
			System.out.println("3. HIGH"); 
			System.out.println(); 
			System.out.print("Please enter the load: ");
			temp = getInput();
    
			try {
				simLoad = Integer.parseInt(temp);
				simLoad--;
			}
			catch (NumberFormatException e) {
				System.out.println("\nINT PARSE ERROR");
				simLoad = LOW;
			}
			if(simLoad < LOW || simLoad > HIGH) 
				System.out.println("\nInvalid load, please renter load value\n\n");
		}
	}

	public static void simSetup () {
		simMediator = new Mediator(numNode, simLoad);
		for(i=0; i<numNode; i++) {
			nodeList.add(new Node(numNode, simLoad, simMediator));
		}

		
		nNode = nodeList.get(0); //root
		nNode.setNode(nNode);
		for(i=1; i<numNode; i++) {
			nNode = nodeList.get(i);
			nNode.setNode(nodeList.get(((i+1)/2)-1));
		}
	}

	public static void simStart() {
		simMediator.start(); 
		for(i=0; i<numNode; i++) {
			nNode = nodeList.get(i);
			nNode.start();
		}
	}

    public static void main(String[] args) {

		simUI();
		simSetup();
		simStart();
			
	}
}
