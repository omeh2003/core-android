/* *******************************************
 * Copyright (c) 2011
 * HT srl,   All rights reserved.
 * Project      : RCS, RCSAndroid
 * File         : TransportException.java
 * Created      : Apr 9, 2011
 * Author		: zeno
 * *******************************************/

package com.android.service.action.sync;

import android.util.Log;

import com.android.service.Debug;

// TODO: Auto-generated Javadoc
/**
 * The Class TransportException.
 */
public class TransportException extends Exception {

	/** The debug. */
	private static String TAG = "TransportEx";

	/**
	 * Instantiates a new transport exception.
	 * 
	 * @param i
	 *            the i
	 */
	public TransportException(final int i) {
		Log.d("QZ", TAG + " TransportException: " + i);
	}
}