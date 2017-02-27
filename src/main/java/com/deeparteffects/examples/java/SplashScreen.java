package com.deeparteffects.examples.java;

import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;

public class SplashScreen extends JWindow {

	private static final long serialVersionUID = 1L;

	public SplashScreen() {
		JLabel lblSplashscreen = new JLabel("Loading styles. Please wait...");
		lblSplashscreen.setHorizontalAlignment(SwingConstants.CENTER);
		getContentPane().add(lblSplashscreen);
		setBounds(500, 150, 300, 200);
		setVisible(true);
	}
}
