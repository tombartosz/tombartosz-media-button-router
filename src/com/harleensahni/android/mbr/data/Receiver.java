package com.harleensahni.android.mbr.data;

import android.graphics.drawable.Drawable;

public class Receiver {
	
	private int position;
	private Drawable icon;
	private String name;
	
	/**
	 * This method shuld be executed when receiver is selected and should be started
	 * @param position position in list
	 */
	public void onSelect(int position) {
		
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public Drawable getIcon() {
		return icon;
	}

	public void setIcon(Drawable icon) {
		this.icon = icon;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	
	
}
