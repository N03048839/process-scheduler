package scheduler;

public class PCB 
{

	final public int id;
	final public int arriveTime;
	final public int burstTime;
	final public int priority;
	
	public int responseTime;
	public int runTime;
	
	/**
	 * @param id			Identifying number
	 * @param arriveTime	Time at which the process becomes ready for execution
	 * @param burstTime		Amount of cpu time required to finish process
	 * @param priority 		the process' execution priority
	 */
	public PCB (int id, int arriveTime, int burstTime, int priority)
	{
		this.id = id;
		this.arriveTime = arriveTime;
		this.burstTime = burstTime;
		this.priority = priority;
	}
}
