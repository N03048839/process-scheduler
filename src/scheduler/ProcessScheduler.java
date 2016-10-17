package scheduler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.Scanner;


public class ProcessScheduler 
{
	static final boolean DEBUG = true;
	static final String IFNAME_DEF = "input.data";
	static final String OFNAME_DEF = "output.data";
	
	final Algorithm ALGORITHM = Algorithm.SJF;
	final boolean PREEMPTIVE;
 	
	private LinkedList<PCB> jobQueue;
	private LinkedList<PCB> readyQueue;
	private PrintWriter outfile;
	
	private int time;
	private int timeStep;
	private PCB runningProcess;
	
	
	
	public ProcessScheduler(final Scanner infile) throws FileNotFoundException
	{
		System.out.println("--- Constructing Scheduler ---");
		
		/* Set global vars */
		time = 0;
		runningProcess = null;
		jobQueue = new LinkedList<PCB>();
		readyQueue = new LinkedList<PCB>();
		
		/* Parse input file header */
		final int processCount = infile.nextInt();
		this.PREEMPTIVE = (infile.nextInt() != 0);
		System.out.println("    preemptive? " + this.PREEMPTIVE);
		int quantum = infile.nextInt();
		this.timeStep = (quantum > 1? quantum : 1);
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
			
			if (DEBUG)
				System.out.print("\n    process id: p" + i
						+ "\n    arrival time: " + process.arriveTime
						+ "\n    burst: " + process.burstTime
						+ "\n    priority: " + process.priority + "\n    ");
			
			/* Place node in job queue, in order of arrival time */
			int destination = 0;
			while ( destination < jobQueue.size() && process.arriveTime >= jobQueue.get(destination).arriveTime)
				destination++;
			
			jobQueue.add(destination, process);
		}
		System.out.print("...done\n");
		
		infile.close();
		System.out.print("--- Scheduler Constructed ---\n\n\n");
		
		if (DEBUG) {
			System.out.print("    Job arrival order:");
			for (int i = 0; i < jobQueue.size(); i++)
				System.out.print("   p" + jobQueue.get(i).id + "("
						+ jobQueue.get(i).arriveTime + ")");
			System.out.print("\n\n\n");
		}
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
		System.out.println(time + "            Process p" + process.id + " arrives");
		
		int destination = 0;
		switch(alg)
		{
		
		/* Smallest Job First */
		case SJF:
			while ( destination < readyQueue.size()
					&& process.burstTime >= readyQueue.get(destination).burstTime )
				destination++;
			readyQueue.add(destination, process);
			break;
			
		/* Priority High */
		case PH:
			while ( destination < readyQueue.size()
					&& process.priority <= readyQueue.get(destination).priority )
				destination++;
			readyQueue.add(destination, process);
			break;
			
		/* Priority Low */
		case PL:
			while ( destination < readyQueue.size()
					&& process.priority >= readyQueue.get(destination).priority )
				destination++;
			readyQueue.add(destination, process);
			break;
			
		/* Round-Robin */
		case RR:
			readyQueue.addLast(process);
			break;
		}
	}
	
	
	
	public void print(String currentid, String newid, String event)
	{
		System.out.println( time + "    " + currentid + "    " + newid + "    " + event);
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
