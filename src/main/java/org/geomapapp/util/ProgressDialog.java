package org.geomapapp.util;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

public class ProgressDialog extends JPanel
		implements Runnable {
	JDialog dialog;
	Thread thread;
	JButton abort;
	boolean alive;
	Component comp;
	Abortable job;
	public ProgressDialog(JFrame frame) {
		super(new BorderLayout());
		abort = new JButton("abort");
		JPanel p = new JPanel(new BorderLayout());
		p.add(abort);
		abort.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				abort();
			}
		});
		add( abort, "South");
		dialog = new JDialog(frame);
		dialog.getContentPane().add(this);
	}
	void abort() {
		job.abort();
	}
	public boolean showProgress(Component comp, Abortable job) {
		if( thread!=null && thread.isAlive() ) return false;
		this.comp = comp;
		this.job = job;
		thread = new Thread(this);
		alive = true;
		thread.start();
		return true;
	}
	public void setAlive(boolean tf) {
		if( tf && !alive)return;
		alive = tf;
	}
	public void run() {
		add(comp);
		dialog.pack();
		dialog.show();
		while(alive) {
			try {
				Thread.currentThread().sleep(200L);
			} catch(InterruptedException e) {
			}
		}
		dialog.hide();
	}
}