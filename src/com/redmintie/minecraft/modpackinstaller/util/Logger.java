package com.redmintie.minecraft.modpackinstaller.util;

import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class Logger {
	private static JFrame frame;
	public static void setFrame(JFrame frame) {
		Logger.frame = frame;
	}
	public static JFrame getFrame() {
		return frame;
	}
	public static void info(String msg) {
		System.out.println(msg);
	}
	public static void warning(String msg) {
		System.err.println(msg);
	}
	public static void error(String msg) {
		if (frame == null) {
			throw new NullPointerException("Frame has not been set.");
		}
		Toolkit.getDefaultToolkit().beep();
		JOptionPane.showMessageDialog(frame, msg, frame.getTitle(), JOptionPane.ERROR_MESSAGE);
		System.exit(0);
	}
}