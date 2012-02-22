package com.mojang.mojam;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.Map;

import com.mojang.mojam.Controls.Key;

public class InputHandler implements KeyListener, MouseListener, MouseMotionListener {
	private Map<Integer, Key> mappings = new HashMap<Integer, Key>();
	private Controls controls;

	public InputHandler(Controls controls) {
		this.controls = controls;
		
		mappings.put(KeyEvent.VK_UP, controls.up);
		mappings.put(KeyEvent.VK_DOWN, controls.down);
		mappings.put(KeyEvent.VK_LEFT, controls.left);
		mappings.put(KeyEvent.VK_RIGHT, controls.right);

		mappings.put(KeyEvent.VK_NUMPAD8, controls.up);
		mappings.put(KeyEvent.VK_NUMPAD2, controls.down);
		mappings.put(KeyEvent.VK_NUMPAD4, controls.left);
		mappings.put(KeyEvent.VK_NUMPAD6, controls.right);

		mappings.put(KeyEvent.VK_W, controls.up);
		mappings.put(KeyEvent.VK_S, controls.down);
		mappings.put(KeyEvent.VK_A, controls.left);
		mappings.put(KeyEvent.VK_D, controls.right);

		mappings.put(KeyEvent.VK_SPACE, controls.fire);
		mappings.put(KeyEvent.VK_ALT, controls.fire);
		mappings.put(KeyEvent.VK_CONTROL, controls.fire);
		mappings.put(KeyEvent.VK_SHIFT, controls.fire);
        mappings.put(KeyEvent.VK_C, controls.fire);

        mappings.put(KeyEvent.VK_X, controls.build);
        mappings.put(KeyEvent.VK_Z, controls.use);
        mappings.put(KeyEvent.VK_C, controls.upgrade);
		mappings.put(KeyEvent.VK_R, controls.build);
		mappings.put(KeyEvent.VK_E, controls.use);
		mappings.put(KeyEvent.VK_Q, controls.upgrade);
		
		mappings.put(MouseEvent.BUTTON1, controls.fire);
		mappings.put(MouseEvent.BUTTON3, controls.use);
	}

	public void keyPressed(KeyEvent ke) {
		toggle(ke.getKeyCode(), true);
	}

	public void keyReleased(KeyEvent ke) {
		toggle(ke.getKeyCode(), false);
	}

	public void keyTyped(KeyEvent ke) {
	}

	private void toggle(int ke, boolean state) {
		Key key = mappings.get(ke);
		if (key != null) {
			key.nextState = state;
		}
	}
	
	public void mousePressed(MouseEvent e) {
		toggle(e.getButton(), true);
	}
	
	public void mouseReleased(MouseEvent e) {
		toggle(e.getButton(), false);
	}
	
	public void mouseMoved(MouseEvent e) {
		controls.mouseX = e.getX();
		controls.mouseY = e.getY();
	}
	
	public void mouseDragged(MouseEvent e) {
		controls.mouseX = e.getX();
		controls.mouseY = e.getY();
	}
	
	public void mouseClicked(MouseEvent e) {
	}
	
	public void mouseEntered(MouseEvent e) {
	}
	
	public void mouseExited(MouseEvent e) {
	}
}
