package scheduler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.Scanner;


public class Scheduler implements Runnable
{
	static boolean DEBUG = false;		// Flag: used to display extra screen output
	static boolean QUIET = false;		// Flag: used to suppress all screen output
	static final String IFNAME_DEF = "input.data";
	static final String OFNAME_DEF = "output.data";
	static final Algorithm ALG_DEF = Algorithm.SJF;	//Overriden by command args
	static int FORCE_PREEMPT = -1;		// Used to force preemptive mode from command line
	
	final Algorithm ALGORITHM;
	final boolean PREEMPTIVE;
	private final int PROCESS_COUNT;
 	
	private PrintWriter outfile;
	private LinkedList<PCB> jobQueue;
	private LinkedList<PCB> readyQueue;
	private PCB runningProcess;		// Process currently in cpu
	
	private int time;			// Elapsed run time
	private int timeStep;			// time quantum
	private boolean newProcess;		// Used for preemptive switch check
	private double wait;			// total process wait time
	private double response;		// total process response time
	
	
	
	public Scheduler(final Scanner infile, final Algorithm alg) throws FileNotFoundException
	{
		if (!QUIET)
			System.out.println("--- Constructing Scheduler ---");
		
		/* Set global vars */
		this.ALGORITHM = alg;
		runningProcess = null;
		jobQueue = new LinkedList<PCB>();
		readyQueue = new LinkedList<PCB>();
		
		/* Parse input file header */
		PROCESS_COUNT = infile.nextInt();
		int preemptiveVal = infile.nextInt();
		this.PREEMPTIVE = (FORCE_PREEMPT >= 0)?
				(FORCE_PREEMPT == 1) : (preemptiveVal != 0);
		int quantum = infile.nextInt();
		this.timeStep = (quantum > 1)? quantum : 1;
		
		if (!QUIET)
			System.out.println("    Using sorting algorithm: "
				+ (PREEMPTIVE? "Preemptive " : "")
				+ ALGORITHM
			);
		
		/* Create process nodes */
		if (DEBUG && !QUIET)
			System.out.print("    populating job queue...");
		
		for( int i = 1; i <= PROCESS_COUNT; i++ )
		{
			/* Create node based on next node-description */
			PCB process = new PCB(i,
					infile.nextInt(),	//read arrival time
					infile.nextInt(), 	//read burst time
					infile.nextInt()	//read priority
			);
			
			/* Place node in job queue, in order of arrival time */
			int destination = 0;
			while( destination < jobQueue.size() 
					&& process.arriveTime >= jobQueue.get(destination).arriveTime)
				destination++;
			jobQueue.add(destination, process);
		}
		
		infile.close();
		if (!QUIET)
			System.out.print("\n--- Scheduler Constructed ---\n\n\n");
	}
	
	
	
	public void run()
	{
		if (!QUIET)
			System.out.println("--- Scheduler Execution ---");
		
		/* Repeatedly invoked until the scheduler has completed all processes. */
		while( !(jobQueue.isEmpty() && readyQueue.isEmpty() ))
			step();
		
		
		outfile.close();
		if (!QUIET) {
			System.out.println("--- Scheduler Execution Complete ---\n");
			System.out.println("    Processes run: " + PROCESS_COUNT
				+ "\n    Avg. wait time:     " + (wait / PROCESS_COUNT)
				+ "\n    Avg. response time: " + (response / PROCESS_COUNT)
			);
		}
	}
	
	
	/**
	 * Execute one cycle of cpu execution.
	 */
	private void step()
	{
		this.newProcess = false;  // whether a new process has arrived this cycle
		
		/* Check job queue for arriving jobs */
		while (!jobQueue.isEmpty() && time >= jobQueue.peek().arriveTime)
			addProcess( jobQueue.removeFirst() );		
		
		
		/* Prime cpu with first process */
		if (runningProcess == null) {
			if (readyQueue.isEmpty())  return;			
			advance(false);
		}		

		
		/* Check for end-of-process process switch */
		else if ( runningProcess.runTime >= runningProcess.burstTime )
			advance(true);
		
		
		/* Check for preemptive process switch */
		else if( PREEMPTIVE && this.newProcess)
			advance(false);
		
		
		/* Check for Round-Robin process switch */
		else if( ALGORITHM == Algorithm.RR && time % timeStep == 0 && readyQueue.size() > 1)
			advance(false);
		
		
		/* Increment clock time */
		time++;
		if (runningProcess != null)
			runningProcess.runTime++;
		for (PCB waitingProcess : readyQueue) {
			if (waitingProcess != runningProcess)
				wait++;
			if (!waitingProcess.started)
				response++;
		}
	}
	
	
	
	/**
	 * Gives cpu control to the next queued process.
	 * 
	 * @param removeCurrent whether the current process has completed execution
	 */
	private void advance(final boolean removeCurrent) 
	{		
		if (DEBUG && !QUIET)
		{	
			/* Display ordered contents of ready queue */		
			System.out.print("        process execution order:");
			for (int i = 0; i < readyQueue.size(); i++)
				System.out.print("   p"
						+ readyQueue.get(i).id + "("
						+ readyQueue.get(i).arriveTime + ","
						+ readyQueue.get(i).burstTime + ","
						+ readyQueue.get(i).priority + ")"
				);
			System.out.println();
		}
		
		
		
		PCB temp = runningProcess;	// remember old process (for removal)
		int n = readyQueue.indexOf(temp);
		
		/* Write end time of old process */
		if (runningProcess == null) {
			runningProcess = readyQueue.peek();
			runningProcess.started = true;
		}
		else
			outfile.print(time + "   " + temp.id + "\n");	
		
		
		if (readyQueue.isEmpty())
		{
			runningProcess = null;
			return;
		}
		
		
		/* Advance to next process. 
		 * 
		 * PH, PL, and SJF advance to the process at the
		 * head of the ready queue (as it's already sorted).
		 * 
		 * RR moves iteratively, i.e. to the process following the
		 * current one in queue order.
		 */
		switch (ALGORITHM) 
		{
		case PH:
		case PL:
		case SJF:
			if (removeCurrent) 
			{
				readyQueue.remove(temp);		
				print("    Process p" + temp.id + " complete");
			}
			runningProcess = readyQueue.peek();	// Move to next process
			if (runningProcess != null) {
				runningProcess.started = true;
				outfile.print(time + "   ");	// Print start time of new process
			}
			break;
			
			
		case RR:
			runningProcess = ( n+1 < readyQueue.size() )? 
						readyQueue.get(n+1) : 	// Move to next process
						readyQueue.getFirst();	// Move to head of ready queue
			if (removeCurrent) {
				readyQueue.remove(temp);
				if (readyQueue.isEmpty()) 
					runningProcess = null;
				print("    Process p" + temp.id + " complete");
			}
			if (runningProcess != null) {
				outfile.print(time + "   ");	// Print start time of new process
				runningProcess.started = true;
			}
			break;
		}
		
		print("        Loading process " + ( (runningProcess != null)? 
				"p" + runningProcess.id : "null") );
	}
	
	
	
	/**
	 * Inserts a process into the ready queue at the process' sorted location,
	 * according to the specified scheduling algorithm.
	 * 
	 * SJF:		Sort by burst time, increasing
	 * PH:		Sort by priority, decreasing
	 * PL:		Sort by priority, increasing
	 * RR:		Not sorted (add to tail)
	 * 
	 * @param process 	the process to add
	 * @param alg		the scheduling algorithm being used
	 */
	private void addProcess(PCB process)
	{
		print("Process p" + process.id + " arrives");
		
		int destination = 0;	// Location for new process in ready queue 
		switch(this.ALGORITHM)
		{
		
		/* Smallest Job First */
		case SJF:
			while (destination < readyQueue.size()
					&& process.burstTime >= readyQueue.get(destination).burstTime )
				destination++;
			readyQueue.add(destination, process);
			if (destination == 0) 
				this.newProcess = true;
			break;
			
		/* Priority High */
		case PH:
			while (destination < readyQueue.size()
					&& process.priority <= readyQueue.get(destination).priority )
				destination++;
			readyQueue.add(destination, process);
			if (destination == 0) 
				this.newProcess = true;
			break;
			
		/* Priority Low */
		case PL:
			while (destination < readyQueue.size()
					&& process.priority >= readyQueue.get(destination).priority )
				destination++;
			readyQueue.add(destination, process);
			if (destination == 0) 
				this.newProcess = true;
			break;
			
		/* Round-Robin */
		case RR:
			readyQueue.addLast(process);
			break;
		}
	}
	
	
	
	private void print(String event)
	{
		if (!QUIET)
			System.out.println( time + "   " + (time < 10? " " : "") + event);
	}
	
	
	
	public static void main(String[] args)
	{
		String infilename = IFNAME_DEF;
		String outfilename = OFNAME_DEF;
		Algorithm alg = ALG_DEF;
		
		/* Parse Args: set algorithm */
		if (args.length > 0) {
			if (args[0].matches("[Rr][Rr]"))
				alg = Algorithm.RR;
			else if (args[0].matches("[Ss][Jj][Ff]"))
				alg = Algorithm.SJF;
			else if (args[0].matches("[Pp][Hh]"))
				alg = Algorithm.PH;
			else if (args[0].matches("[Pp][Ll]"))
				alg = Algorithm.PL;
			else
				alg = ALG_DEF;
		}
		for (int i = 1; i <= args.length; i++) 
		{
			/* Parse Args: suppress display output */
			if (args[i-1].matches("-s"))
				Scheduler.QUIET = true;
			/* Parse Args: force debug mode */
			if (args[i-1].matches("-d"))
				Scheduler.DEBUG = true;
			/* Parse Args: force preemptive mode */
			if (args[i-1].matches("-P"))
				Scheduler.FORCE_PREEMPT = 1;
			/* Parse Args: force non-preemptive mode */
			if (args[i-1].matches("-p"))
				Scheduler.FORCE_PREEMPT = 0;
			/* Parse Args: set new input file  */
			if (args[i-1].matches("-if")) {
				infilename = args[i];
				String[] newfn = args[i].split("[.]");
				outfilename = newfn[0] + "_out." + newfn[1];
			}
			/* Arg: set new output file */
			if (args[i-1].matches("-of"))
				outfilename = args[i];
		}
		
		/* Instantiate scheduler */
		try
		{
			Scheduler s = new Scheduler( new Scanner(new File(infilename)), alg);
			s.outfile = new PrintWriter(outfilename);
			s.run();
		}
		catch (FileNotFoundException e)
		{
			System.out.println("Scheduler error: cannot find input file \"" + infilename + "\" in current directory!");
			e.printStackTrace();
		}
		catch (InputMismatchException e)
		{
			System.out.println("Scheduler error: invalid format of input file \"" + infilename + "\"!");
			e.printStackTrace();
		}
	}
}
