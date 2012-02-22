package com.mojang.mojam.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.mojang.mojam.network.NetworkCommand;


public class MouseMoveCommand extends NetworkCommand {
	private int mouseX;
	private int mouseY;
	
	public MouseMoveCommand() {
	}
	
	public MouseMoveCommand(int x, int y) {
		this.mouseX = x;
		this.mouseY = y;
	}
	
	public void read(DataInputStream dis) throws IOException {
		mouseX = dis.readInt();
		mouseY = dis.readInt();
	}
	
	public void write(DataOutputStream dos) throws IOException {
		dos.writeInt(mouseX);
		dos.writeInt(mouseY);
	}
	
	public int getX() {
		return mouseX;
	}
	
	public int getY() {
		return mouseY;
	}
}
