package haxby.util;

public class SilentProcessingTask extends Thread {
	protected volatile boolean isCancelled;
	protected boolean cancelExisting;
	protected boolean existingTasksMustFullyTerminate;
	private boolean done;
	
	final public String taskID;
	
	/**
	 * Create a new SilentProceessingTask with the designated properties
	 * @param cancelExisting whether to cancel currently running tasks with the same taskID
	 * @param existingTasksMustFullyTerminate whether to wait for all currently running tasks 
	 * with the same taskID to finish before starting the new task
	 * @param taskID descriptive string identifier of the task; can be used to create task classes
	 */
	public SilentProcessingTask(boolean cancelExisting, boolean existingTasksMustFullyTerminate, String taskID) {
		this.isCancelled = false;
		this.cancelExisting = cancelExisting;
		this.existingTasksMustFullyTerminate = existingTasksMustFullyTerminate;
		this.taskID = taskID;
		this.done = false;
	}
	
	/**
	 * Accessor method for cancelExisting property.
	 * @return whether to cancel currently running tasks with the same taskID
	 */
	final public boolean cancelExisting() {
		return cancelExisting;
	}
	
	/**
	 * Accessor method for existingTasksMustFullyTerminate property.
	 * @return whether to wait for existing tasks with the same taskID
	 */
	final public boolean existingTasksMustFullyTerminate() {
		return existingTasksMustFullyTerminate;
	}
	
	/**
	 * Set the isCancelled flag to true so the SilentProcessingTask can check it to
	 * safely terminate.
	 */
	public void cancel() {
		earlyCleanup();
		isCancelled = true;
	}
	
	/**
	 * Override this method to do any cleanup necessary for an early termination.
	 * It is guaranteed that cleanup completes before the task terminates.
	 */
	protected void earlyCleanup() {
	}
	
	/**
	 *  Whether the task is finished. The task is expected to set this flag at the
	 *  end of its run method.
	 *  @return true if the task is complete, false otherwise
	 */
	protected boolean isDone() {
		return done;
	}
	
	/**
	 * To be called by a task when all of its processing is complete (i.e., it
	 * can be removed from the SilentProcessingQueue without cancellation).
	 */
	protected void finish() {
		this.done = true;
	}
}
