package com.ht.RCSAndroidGUI.agent.task;

public class UserInfo {
	private static final String TAG = "UserInfo";
	
	private String completeName, userNote, userNickname;
	private long userId;
	
	public UserInfo(long userId, String userName, String userNote, String userNickname) {
		this.userId = userId;
		this.completeName = userName;
		this.userNote = userNote;
		this.userNickname = userNickname;
	}

	public String getCompleteName() {
		return completeName;
	}

	public String getUserNote() {
		return userNote;
	}

	public String getUserNickname() {
		return userNickname;
	}

	public long getUserId() {
		return userId;
	}
}