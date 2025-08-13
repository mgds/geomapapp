package haxby.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.FocusManager;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class ProcessingDialog extends JDialog {

	private Window owner;
	private List<TaskPanel> tasks = new ArrayList<TaskPanel>();
	private JPanel tasksPanel;
	private JComponent map;
	private Window stolenFocus;
	
	public ProcessingDialog(JFrame owner, JComponent map) {
		super((JFrame) null, "Processing...", false);
		this.owner = owner;
		this.map = map;
		

		// shutteling progress bar for macs
		if (System.getProperty("os.name").toLowerCase().contains("mac")) {
			UIManager.put("ProgressBar.foreground", new Color(70, 180, 252));
			UIManager.put("ProgressBarUI","javax.swing.plaf.metal.MetalProgressBarUI" );
		}
		
		tasksPanel = new JPanel();
		tasksPanel.setLayout( new BoxLayout(tasksPanel, BoxLayout.Y_AXIS));	
		tasksPanel.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));

		getContentPane().add(tasksPanel);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		setMinimumSize(new Dimension(200,40));
		setAlwaysOnTop(true);
	}

	public void setVisible(boolean tf) {
		if (tf && !isVisible()) {
			stolenFocus = FocusManager.getCurrentManager().getActiveWindow();
		}

		if (owner != null)	{
			setLocation(owner.getX() + 60, owner.getY() + 90);
		}
		super.setVisible(tf);

		if (!tf) {
			if (hasFocus()) {
				if (stolenFocus != owner && 
						stolenFocus != null && 
						stolenFocus.isVisible()) {
					stolenFocus.requestFocus();
				} else
				{
					owner.requestFocus();
					map.requestFocusInWindow();
				}
			}
			stolenFocus = null;
		}
	}

	public void addTask(String taskName, Thread task) {
		TaskPanel tp = new TaskPanel(taskName, task);
		synchronized (tasks) {
			for (TaskPanel oTask : tasks)
				if (oTask.name.equals(taskName)) {
					oTask.queuedTask = tp;
					return;
				}
			tasks.add(tp);
		}

		tasksChanged();
		toFront();
		setVisible(true);
		tp.startTask();
	}

	public void tasksChanged() {
		synchronized (tasks) {
			if (tasks.size() == 0)
			setVisible(false);
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				synchronized (tasks) {
					tasksPanel.removeAll();
					for (TaskPanel tp : tasks)
						tasksPanel.add(tp);

					tasksPanel.setMaximumSize(tasksPanel.getPreferredSize());
					tasksPanel.revalidate();
					tasksPanel.repaint();
					ProcessingDialog.this.pack();
				}
			}
		});
	}

	private class TaskPanel extends JPanel {
		public TaskPanel queuedTask;
		public String name;
		public Thread task;
		
		public TaskPanel(String name, Thread task) {
			super( new BorderLayout() );
	
			this.name = name;
			this.task = task;
	
			JLabel nameL = new JLabel(name);
			nameL.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 1));
			add(nameL, BorderLayout.NORTH);

			if (task instanceof StartStopThread) {
				JButton stopB = new JButton("X");
				stopB.setMargin( new Insets(0,1,0,1));
				stopB.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						TaskPanel.this.task.interrupt();
						((StartStopThread) TaskPanel.this.task).stopTask();
					}
				});
				add(stopB, BorderLayout.EAST);
			}
			JProgressBar pb = new JProgressBar();
			pb.setIndeterminate(true);
			add(pb);

			setBorder( BorderFactory.createLineBorder(Color.black));

			int minWidth = getMinimumSize().width;
			int minHeight = getMinimumSize().height;
			int maxWidth = getMaximumSize().width;

			setPreferredSize(new Dimension(minWidth, minHeight));
			setMaximumSize(new Dimension(maxWidth, minHeight));
		}

		public void startTask() {
			new Thread(new Runnable() {
				public void run() {
					task.start();
					try {
						task.join();
					} catch (InterruptedException e) {
					}

					synchronized (tasks) {
						tasks.remove(TaskPanel.this);
						if (queuedTask != null) {
							tasks.add(queuedTask);
							queuedTask.startTask();
						}
						tasksChanged();
					}
				}
			}).start();
		}
	}
	
	public static interface StartStopTask extends Runnable {
		public void stop();
	}
	
	public static class StartStopThread extends Thread {
		public StartStopTask runnable;
		
		public StartStopThread(StartStopTask runnable, String name) {
			super(runnable, name);
			this.runnable = runnable;
		}
		
		public StartStopThread(StartStopTask runnable) {
			super(runnable);
			this.runnable = runnable;
		}
		
		public void stopTask() {
			runnable.stop();
		}
	}
	
}
