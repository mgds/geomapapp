package haxby.util;

import java.util.LinkedList;

/**
 * The SilentProcessingDialog class was created to give GMA the ability to process background
 * computations without displaying a dialog box. The naming is probably oxymoronic, but the
 * intention is for the naming to mirror that of the haxby.util.ProcessingDialog class.
 * 
 * Unlike ProcessingDialog, SilentProcessingDialog also supports task canceling and task
 * waiting, a critical feature for GMA workflows that can get called while another instance
 * is already running.
 * @author Benjamin Barg
 */
public class SilentProcessingDialog {

	//TODO change to HashTable structure for better speed
	private LinkedList<SilentProcessingTask> tasks;
	
	/**
	 * Create a new SilentProcessingDialog with no tasks in its collection.
	 */
	public SilentProcessingDialog() {
		tasks = new LinkedList<SilentProcessingTask>();
	}

	/**
	 * Add a task to the silent processing queue. Removes all previous tasks
	 * of this type, and if indicated in the SilentlyProcessable task also terminates
	 * all previous tasks of this type.
	 * @param task the task to add
	 */
	public void addTask(SilentProcessingTask task) {

		LinkedList<SilentProcessingTask> existing = new LinkedList<SilentProcessingTask>();
		
		synchronized(tasks) {
			for (SilentProcessingTask eTask : tasks) {
				if ( eTask.taskID.equals(task.taskID) ) {
					existing.add(eTask);
					if (task.cancelExisting()) {
						tasks.remove(eTask);
						eTask.cancel(); //TODO right now this cancels a task even if it's done
						break; // for cancelExisting, we only add a task when there
							   // are 0 in the queue
					}
				}
			}
		}
		
		if (task.existingTasksMustFullyTerminate() && !existing.isEmpty())  {
			for(SilentProcessingTask eTask : existing) {
				try {
					eTask.join();
                } catch (InterruptedException e) {
                	// we know that if the task is interrupted it does not continue
                	continue; 
                }
			}
		}
		
		tasks.add(task);
		task.start();
	}
}
