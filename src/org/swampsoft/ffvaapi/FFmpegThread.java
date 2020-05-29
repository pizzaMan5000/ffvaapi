package org.swampsoft.ffvaapi;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.SwingUtilities;

public class FFmpegThread extends Thread {
	
	Process process;
	Scanner scanner;
	
	String[] fullCmd;
	
	MainWindow mainWindow;
	
	public FFmpegThread(MainWindow mainWindow, String[] fullCmd) {
		this.mainWindow = mainWindow;
		this.fullCmd = fullCmd;
	}
	
	public void run(){
		System.out.println("Thread running...");
		
		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.command(fullCmd);
		try {
			process = processBuilder.start();
			scanner = new Scanner(process.getErrorStream());
			
			String line;
			String duration = "";
			String bitrate = "";
			String progress = "";
			String speed = "";
			String title = "";
			long durationMilli = 0;
			boolean changeTitle = false;
			while(scanner.hasNext()) {
				line = scanner.nextLine();
				changeTitle = false;
				if (line.contains("Duration:")) {
					//    Duration: 00:08:35.35, start: 0.000000, bitrate: 6375 kb/s
					int durationStart = line.indexOf("Duration:")+10;
					duration = "" + line.substring(durationStart, durationStart + 11);
					System.out.println("Duration: " + duration);
					int bitrateStart = line.indexOf("bitrate:")+9;
					bitrate = "" + line.substring(bitrateStart) + "\n";
					System.out.println("bitrate: " + bitrate);
					
					// get duration in milliseconds
					int hours = Integer.parseInt(duration.substring(0, 2));
					int minutes = Integer.parseInt(duration.substring(3, 5));
					int seconds = Integer.parseInt(duration.substring(6, 8));
					int milli = Integer.parseInt(duration.substring(9));
					durationMilli = (hours*3600000) + (minutes*60000) + (seconds*1000) + (milli * 10);
					
				} else if (line.contains("Stream") && line.contains("Video")) {
					//     Stream #0:0(und): Video: h264 (Main) (avc1 / 0x31637661), yuv420p, 960x540 [SAR 1:1 DAR 16:9], Closed Captions, 6057 kb/s, 73.40 fps, 29.97 tbr, 90k tbn, 59.94 tbc (default) -=- 
					// do nothing with this info so far
				} else if (line.startsWith("frame=")) {
					//  frame=  212 fps=212 q=-0.0 size=    1536kB time=00:00:07.29 bitrate=1725.8kbits/s speed=7.28x
					int progressStart = line.indexOf("time=") + 5; // 11 characters long
					progress = "" + line.substring(progressStart, progressStart+11);
					System.out.print("Progress: "+progress);
					int speedStart = line.indexOf("speed=") + 6;
					speed = "" + line.substring(speedStart).trim();
					System.out.println(" - Speed: " + speed);
					changeTitle = true;
					
					long newProgress;
					int hours = Integer.parseInt(progress.substring(0, 2));
					int minutes = Integer.parseInt(progress.substring(3, 5));
					int seconds = Integer.parseInt(progress.substring(6, 8));
					int milli = Integer.parseInt(progress.substring(9));
					
					newProgress = (hours*3600000) + (minutes*60000) + (seconds*1000) + (milli * 10);
					
					double percent = ((double)newProgress/durationMilli)*100;
					
					System.out.println((int)percent + "%");
					title = (int)percent + "%" + " Speed: " + speed;
				}
				final String finalTitle = title;
				final boolean finalChangeTitle = changeTitle;
				if (finalChangeTitle) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
			        		// update progress String in mainWindow
			        		mainWindow.mainFrame.setTitle(finalTitle);
			        		//System.out.println(finalTitle);
			        	}
					});
				
				}
				if (mainWindow.debug) System.out.println(line); // DEBUG
			} // end while loop
			
			System.out.println("ffmpeg thread closed");
			
			SwingUtilities.invokeLater(new Runnable() {
		        public void run() {
		        	// change Convert button back to green
		        	mainWindow.buttonConvert.setBackground(Color.GREEN);
		        	mainWindow.buttonConvert.setForeground(Color.WHITE);
		        	mainWindow.buttonConvert.setText("Convert");
		        	mainWindow.mainFrame.setTitle("FFVAAPI H.26X Video Converter for GPUs - Version " + mainWindow.versionNumber);
		        }
		    });
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// change Convert button
	}
	
	private long convertTime() {
		return 0;
	}

}
