package com.ht.RCSAndroidGUI.agent.task;

public class PhoneInfo {
	private static final String TAG = "PhoneInfo";

	private long userId;
	private int phoneType;
	private String phoneNumber;
	
	public PhoneInfo(long userId, int phoneType, String phoneNumber) {
		this.userId = userId;
		this.phoneType = phoneType;
		this.phoneNumber = phoneNumber;
	}

	public long getUserId() {
		return userId;
	}

	public int getPhoneType() {
		return phoneType;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}
}