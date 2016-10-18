package scheduler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.Scanner;


public class ProcessScheduler 
{
	static final boolean DEBUG = false;
	static final String IFNAME_DEF = "input.data";
	static final String OFNAME_DEF = "output.data";
	
	final Algorithm ALGORITHM = Algorithm.RR;
	final boolean PREEMPTIVE;
 	
	private LinkedList<PCB> jobQueue;
	private LinkedList<PCB> readyQueue;
	private PrintWriter outfile;
	
	private int time;
	private int timeStep;
	private boolean newProcess;		//Flag used for preemptive switch check
	private PCB runningProcess;
	
	
	
	public ProcessScheduler(final Scanner infile) throws FileNotFoundException
	{
		System.out.println("--- Constructing Scheduler ---");
		
		/* Set global vars */
		runningProcess = null;
		jobQueue = new LinkedList<PCB>();
		readyQueue = new LinkedList<PCB>();
		
		/* Parse input file header */
		final int processCount = infile.nextInt();
		this.PREEMPTIVE = (infile.nextInt() != 0);
		System.out.println("    preemptive? " + this.PREEMPTIVE);
		int quantum = infile.nextInt();
		this.timeStep = (quantum > 1 ? quantum : 1);
		System.out.println("    timeStep: " + this.timeStep);
		
		/* Create process nodes */
		System.out.print("    populating job queue...");
		
		for( int i = 1; i <= processCount; i++ )
		{
			PCB process = new PCB(i,
					infile.nextInt(),	//process arrival time
					infile.nextInt(), 	//process burst time
					infile.nextInt()	//process priority
			);
			
			
			/* Place node in job queue, in order of arrival time */
			int destination = 0;
			while ( destination < jobQueue.size() && process.arriveTime >= jobQueue.get(destination).arriveTime)
				destination++;
			
			jobQueue.add(destination, process);
		}
		System.out.print("...done\n");
		
		infile.close();
		System.out.print("--- Scheduler Constructed ---\n\n\n");
		
	}
	
	
	
	
	public void run()
	{
		System.out.println("--- Scheduler Execution ---");
		
		while( !(jobQueue.isEmpty() && readyQueue.isEmpty() ))
			step();
		
		outfile.close();
		System.out.println("--- Scheduler Execution Complete ---");
	}
	
	
	
	public void step()
	{
		this.newProcess = false;
		
		
		/* Check job queue for arriving jobs */
		while (!jobQueue.isEmpty() && time >= jobQueue.peek().arriveTime)
		{
			addProcess(jobQueue.remove(), ALGORITHM);
		}
		
		
		
		/* Prime cpu with first process */
		if (runningProcess == null)
		{
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
	}
	
	
	
	/**
	 * Gives cpu control to the next queued process.
	 * PRECONDITION: running process is not null
	 * 
	 * @param removeCurrent whether the current process has completed execution
	 */
	private void advance(final boolean removeCurrent) 
	{		
		if (DEBUG) {
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
		
		PCB temp = runningProcess;
		int n = readyQueue.indexOf(temp);
		
		if (runningProcess == null)
			runningProcess = readyQueue.peek();
		else
			outfile.print(time + "   " + temp.id + "\n");	// process end time
		
		if (readyQueue.isEmpty())
		{
			runningProcess = null;
			return;
		}
		
		
		switch (ALGORITHM) {
		case PH:
		case PL:
		case SJF:
			if (removeCurrent) {
				readyQueue.remove(temp);		
				print("    Process p" + temp.id + " complete");
			}
			
			runningProcess = readyQueue.peek();
			if (runningProcess != null)
				outfile.print(time + "   ");	// print new process start time
			break;
			
		case RR:
			runningProcess = (n+1 < readyQueue.size())? 
						readyQueue.get(n+1) : readyQueue.getFirst();
			if (removeCurrent) {
				readyQueue.remove(temp);
				if (readyQueue.isEmpty()) 
					runningProcess = null;
				print("    Process p" + temp.id + " complete");
			}
			if (runningProcess != null)
				outfile.print(time + "   ");	// print new process start time
			break;
		}
		
		print("        Loading process " + ( (runningProcess != null)? 
				"p" + runningProcess.id : "null") );
	}
	
	
	/**
	 * Inserts a process into the ready queue at the process' sorted location,
	 * according to the specified scheduling algorithm.
	 * 
	 * @param process 	the process to add
	 * @param alg		the scheduling algorithm being used
	 */
	private void addProcess(PCB process, Algorithm alg)
	{
		print("Process p" + process.id + " arrives");
		
		int destination = 0;
		switch(alg)
		{
		
		/* Smallest Job First */
		case SJF:
			while ( destination < readyQueue.size()
					&& process.burstTime >= readyQueue.get(destination).burstTime )
				destination++;
			readyQueue.add(destination, process);
			if (destination == 0) newProcess = true;
			break;
			
		/* Priority High */
		case PH:
			while ( destination < readyQueue.size()
					&& process.priority <= readyQueue.get(destination).priority )
				destination++;
			readyQueue.add(destination, process);
			if (destination == 0) newProcess = true;
			break;
			
		/* Priority Low */
		case PL:
			while ( destination < readyQueue.size()
					&& process.priority >= readyQueue.get(destination).priority )
				destination++;
			readyQueue.add(destination, process);
			if (destination == 0) newProcess = true;
			break;
			
		/* Round-Robin */
		case RR:
			readyQueue.addLast(process);
			break;
		}
	}
	
	
	
	public void print(String event)
	{
		System.out.println( time + (time < 10 ? "    " : "   ") + event);
	}
	
	
	
	public static void main(String[] args)
	{
		String infilename = IFNAME_DEF;
		String outfilename = OFNAME_DEF;
		
		try
		{
			ProcessScheduler s = new ProcessScheduler( new Scanner( new File(infilename) ));
			s.outfile = new PrintWriter(outfilename);
			s.run();
		}
		catch (FileNotFoundException e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		catch (InputMismatchException e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
}
