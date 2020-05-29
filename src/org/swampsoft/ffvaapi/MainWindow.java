package org.swampsoft.ffvaapi;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

public class MainWindow extends main {
	
	boolean isSoftwareInstalled = false;
	boolean canEncodeH264 = false;
	boolean canEncodeH265 = false;
	boolean canDecodeH264 = false;
	boolean canDecodeH265 = false;
	boolean isMinimized = false;
	
	String versionNumber;
	boolean debug = false;
	
	Process process;
	BufferedWriter bufferedWriter;
	BufferedReader bufferedReader;
	BufferedReader errorReader;
	
	FFmpegThread ffmpegThread;
	
	Preferences preferences;
	
	String lastUsedFolder = "";
	String progress = "";
	
	JFrame mainFrame;
	JButton buttonConvert;
	JButton buttonOpenInputFile;
	JButton buttonOpenOutputFile;
	JTextField fileInputTextField;
	JTextField fileOutputTextField;
	JLabel labelFileInput;
	JLabel labelFileOutput;
	JCheckBox checkboxScale;
	JCheckBox checkboxMuteSound;
	JCheckBox checkboxSetBitRate;
	JTextField textFieldScaleX;
	JTextField textFieldScaleY;
	JLabel labelX;
	JTextField textFieldBitRate;
	//ButtonGroup radioButtonGroupCodec;
	//JRadioButton radioCodecVAAPI;
	//JRadioButton radioCodecOpenCL;
	ButtonGroup radioButtonGroupH26x;
	JRadioButton radioH264;
	JRadioButton radioH265;
	JLabel labelCodec;
	JLabel labelH26x;
	JLabel labelMegaBit;
	JCheckBox checkboxFrameRate;
	JTextField textFieldFrameRate;
	
	public MainWindow() {
		versionNumber = "1.00";
		// check for required software
		isSoftwareInstalled = softwareCheck();
		
		//preferences = Preferences.systemNodeForPackage(MainWindow.class);
		preferences = Preferences.userRoot();
		lastUsedFolder = preferences.get("folder", "");
		
		// make window
		mainFrame = new JFrame("FFVAAPI H.26X Video Converter for GPUs - Version " + versionNumber);
		mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		mainFrame.setSize(600, 350);
		mainFrame.setResizable(false);
		mainFrame.setLayout(null); // no layout, using constant positions
		mainFrame.setLocationRelativeTo(null); // centers window on screen
		mainFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (ffmpegThread != null) {
					
					if (ffmpegThread.process.isAlive()) {
						int result = JOptionPane.showConfirmDialog(mainFrame,"Cancel conversion?", "Cancel?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

						if (result == JOptionPane.YES_OPTION) {
							System.out.println("Destroying ffmpeg process...");
							ffmpegThread.process.destroy();
							System.out.println("ffmpeg process sent kill signal");
							
							ffmpegThread.interrupt();
							ffmpegThread = null;
							
							buttonConvert.setBackground(Color.GREEN);
				        	buttonConvert.setForeground(Color.WHITE);
				        	buttonConvert.setText("Convert");
				        	mainFrame.setTitle("FFVAAPI H.26X Video Converter for GPUs - Version " + versionNumber);
				        }
					}
				}
				System.exit(0);
			}
			
			@Override
			public void windowDeiconified(WindowEvent arg0) {  
			    System.out.println("deiconified");
			    isMinimized = false;
			}  
			
			@Override
			public void windowIconified(WindowEvent arg0) {  
			    System.out.println("iconified");  
			    isMinimized = true;
			}  
		});
		
		labelFileInput = new JLabel("Enter file to convert:");
		labelFileInput.setBounds(20,  10,  200,  30);
		mainFrame.add(labelFileInput);
		
		labelFileOutput = new JLabel("Enter new file name:");
		labelFileOutput.setBounds(20,  80,  200,  30);
		mainFrame.add(labelFileOutput);
		
		// button CONVERT
		buttonConvert = new JButton("CONVERT");
		buttonConvert.setBackground(Color.GREEN);
		buttonConvert.setForeground(Color.WHITE);
		Font convertButtonFont = new Font(buttonConvert.getFont().getName(), buttonConvert.getFont().getStyle(), 20);
		buttonConvert.setFont(convertButtonFont);
		buttonConvert.setBounds(mainFrame.getWidth() - 170,  mainFrame.getHeight() - 90,  150,  50);
		buttonConvert.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				// convert button clicked, check if evreything is filled out if it needs to be and convert
				if (ffmpegThread != null && ffmpegThread.process.isAlive()) {
					int result = JOptionPane.showConfirmDialog(mainFrame,"Cancel conversion?", "Cancel?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
					if (result == JOptionPane.YES_OPTION) {
						if (ffmpegThread.process.isAlive()) {
							System.out.println("Destroying ffmpeg process...");
							ffmpegThread.process.destroy();
							System.out.println("ffmpeg process sent kill signal");
						}
						ffmpegThread.interrupt();
						ffmpegThread = null;
						
						buttonConvert.setBackground(Color.GREEN);
			        	buttonConvert.setForeground(Color.WHITE);
			        	buttonConvert.setText("Convert");
			        	mainFrame.setTitle("FFVAAPI H.26X Video Converter for GPUs - Version " + versionNumber);
					}
				} else if (checkboxFrameRate.isSelected() && textFieldFrameRate.getText().equals("")) {
					// frame rate is checked, but not filled in. no bueno
					JOptionPane.showMessageDialog(mainFrame,"Enter Frame Rate or un-check the box");
				} else if (textFieldBitRate.getText().equals("") && checkboxSetBitRate.isSelected()) {
					// bitrate is checked, but nothing is entered, thats no good
					JOptionPane.showMessageDialog(mainFrame,"Enter BitRate or un-check the box");
					return;
				} else if (checkboxScale.isSelected() && (textFieldScaleX.getText().equals("") || textFieldScaleY.getText().equals(""))) {
					// set scale is checked, but X and Y is empty
					JOptionPane.showMessageDialog(mainFrame,"Enter X and Y for scale please");
					return;
				} else if (isSoftwareInstalled) {
					// required software is installed, check if everything is filled out
					if (fileInputTextField.getText().equals("")) {
						JOptionPane.showMessageDialog(mainFrame,"No file is selected. Open a video file first.");
					} else {
						if (fileOutputTextField.getText().equals("")) {
							String output = fileInputTextField.getText();
							String outputs[] = output.split(Pattern.quote("."));
							if (radioH264.isSelected()) {
								fileOutputTextField.setText(output.replace("."+outputs[outputs.length-1], "-X264.mp4"));
							} else {
								fileOutputTextField.setText(output.replace("."+outputs[outputs.length-1], "-X265.mp4"));
							}
							System.out.println(output);
						}
						// CONVERT VIDEO
						convert();
					}
				} else {
					// required software is NOT installed
					System.out.println("required software is NOT installed");
					JOptionPane.showMessageDialog(mainFrame,"Required software is not installed. Make sure these apps are installed:\n\n"
							+ "ffmpeg\n"
							+ "VAAPI drivers for your GPU\n"
							+ "vainfo");
				}
			}
		});
		mainFrame.add(buttonConvert);
		
		// fileInputTextField
		fileInputTextField = new JTextField("");
		fileInputTextField.setBounds(20,  40,  470, 30);
		mainFrame.add(fileInputTextField);
		
		// fileOutputTextField
		fileOutputTextField = new JTextField("");
		fileOutputTextField.setBounds(20,  110,  470, 30);
		mainFrame.add(fileOutputTextField);
		
		// button openInputFile
		buttonOpenInputFile = new JButton("Open");
		buttonOpenInputFile.setBounds(fileInputTextField.getX() + fileInputTextField.getWidth() + 10, fileInputTextField.getY(), 80, 30);
		buttonOpenInputFile.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				// clicked open input file
				JFileChooser fileChooser = new JFileChooser();
				if (lastUsedFolder.equals("")) {
					fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
				} else {
					fileChooser.setCurrentDirectory(new File(lastUsedFolder));
					//System.out.println("Last used folder: " + lastUsedFolder);
				}
				
				int result = fileChooser.showOpenDialog(mainFrame);
				if (result == JFileChooser.APPROVE_OPTION) {
					File selectedFile = fileChooser.getSelectedFile();
					preferences.put("folder", fileChooser.getCurrentDirectory().getAbsolutePath());
					lastUsedFolder = fileChooser.getCurrentDirectory().getAbsolutePath();
					//System.out.println("Saving folder: " + fileChooser.getCurrentDirectory().getAbsolutePath());
					if (selectedFile.exists()) {
						System.out.println("Selected file: " + selectedFile.getAbsolutePath());
						fileInputTextField.setText(selectedFile.getAbsolutePath());
						//inputFileName = selectedFile.getAbsolutePath();
					} else {
						JOptionPane.showMessageDialog(mainFrame,"File Does not Exist!");  
					}
					
				}
			}
		});
		mainFrame.add(buttonOpenInputFile);
		
		// button openOutputFile
		buttonOpenOutputFile = new JButton("Open");
		buttonOpenOutputFile.setBounds(fileOutputTextField.getX() + fileOutputTextField.getWidth() + 10, fileOutputTextField.getY(), 80, 30);
		buttonOpenOutputFile.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				// clicked open output file
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
				int result = fileChooser.showOpenDialog(mainFrame);
				if (result == JFileChooser.APPROVE_OPTION) {
					File selectedFile = fileChooser.getSelectedFile();
					if (selectedFile.exists()) {
						JOptionPane.showMessageDialog(mainFrame,"Warning! File already exists and will be overwritten!");  
					}
					System.out.println("Selected file: " + selectedFile.getAbsolutePath());
					fileOutputTextField.setText(selectedFile.getAbsolutePath());
					//outputFileName = selectedFile.getAbsolutePath();
				}
			}
		});
		mainFrame.add(buttonOpenOutputFile);
		
		// checkbox SCALE
		checkboxScale = new JCheckBox("Set video resolution");
		checkboxScale.setBounds(fileOutputTextField.getX(), fileOutputTextField.getY() + 50, 170, 30);
		checkboxScale.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (checkboxScale.isSelected()) {
					// is checked
					textFieldScaleX.setVisible(true);
					textFieldScaleY.setVisible(true);
					labelX.setVisible(true);
				} else {
					// not checked
					textFieldScaleX.setVisible(false);
					textFieldScaleY.setVisible(false);
					labelX.setVisible(false);
				}
			}
			
		});
		mainFrame.add(checkboxScale);
		
		textFieldScaleX = new JTextField();
		textFieldScaleX.setBounds(checkboxScale.getX() + 170, checkboxScale.getY(), 60, 30);
		textFieldScaleX.setVisible(false);
		mainFrame.add(textFieldScaleX);
		
		textFieldScaleY = new JTextField();
		textFieldScaleY.setBounds(checkboxScale.getX() + 170 + 60 + 20, checkboxScale.getY(), 60, 30);
		textFieldScaleY.setVisible(false);
		mainFrame.add(textFieldScaleY);
		
		labelX = new JLabel("X");
		labelX.setBounds(textFieldScaleX.getX()+65, textFieldScaleX.getY(), 15, 30);
		labelX.setVisible(false);
		mainFrame.add(labelX);
		
		// checkbox MUTE
		checkboxMuteSound = new JCheckBox("Do not include audio, only video");
		checkboxMuteSound.setBounds(fileOutputTextField.getX(), fileOutputTextField.getY() + 100, 300, 30);
		mainFrame.add(checkboxMuteSound);
		
		// checkbox BITRATE
		checkboxSetBitRate = new JCheckBox("Set bitrate");
		checkboxSetBitRate.setBounds(fileOutputTextField.getX(), fileOutputTextField.getY() + 150, 110, 30);
		checkboxSetBitRate.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (checkboxSetBitRate.isSelected()) {
					// is checked
					textFieldBitRate.setVisible(true);
					labelMegaBit.setVisible(true);
				} else {
					// not checked
					textFieldBitRate.setVisible(false);
					labelMegaBit.setVisible(false);
				}
			}
			
		});
		mainFrame.add(checkboxSetBitRate);
		
		textFieldBitRate = new JTextField("");
		textFieldBitRate.setBounds(checkboxSetBitRate.getX() + 110, checkboxSetBitRate.getY(), 60, 30);
		textFieldBitRate.setVisible(false);
		mainFrame.add(textFieldBitRate);
		
		labelMegaBit = new JLabel("Mb");
		labelMegaBit.setBounds(textFieldBitRate.getX() + 65, textFieldBitRate.getY(), 30, 30);
		labelMegaBit.setVisible(false);
		mainFrame.add(labelMegaBit);
		
		/*
		radioCodecVAAPI = new JRadioButton("VAAPI", true);
		radioCodecVAAPI.setBounds(425, 165, 70, 30);
		mainFrame.add(radioCodecVAAPI);
		
		radioCodecOpenCL = new JRadioButton("OpenCL", false);
		radioCodecOpenCL.setBounds(500, 165, 80, 30);
		mainFrame.add(radioCodecOpenCL);
		
		radioButtonGroupCodec = new ButtonGroup();
		radioButtonGroupCodec.add(radioCodecVAAPI);
		radioButtonGroupCodec.add(radioCodecOpenCL);
		*/
		
		radioH264 = new JRadioButton("H.264", false);
		radioH264.setBounds(425, 220, 70, 30);
		mainFrame.add(radioH264);
		
		radioH265 = new JRadioButton("H.265", true);
		radioH265.setBounds(500, 220, 70, 30);
		mainFrame.add(radioH265);
		
		radioButtonGroupH26x = new ButtonGroup();
		radioButtonGroupH26x.add(radioH264);
		radioButtonGroupH26x.add(radioH265);
		
		//labelCodec = new JLabel("HW driver:");
		//labelCodec.setBounds(radioCodecVAAPI.getX(), radioCodecVAAPI.getY()-25, 100, 30);
		//mainFrame.add(labelCodec);
		
		labelH26x = new JLabel("Compression:");
		labelH26x.setBounds(radioH264.getX(), radioH264.getY()-25, 100, 30);
		mainFrame.add(labelH26x);
		
		// change some settings depending on if H264, H265 are supported
		if (!canEncodeH264 && canEncodeH265) {
			radioH264.setEnabled(false);
			radioH264.setSelected(false);
			radioH265.setSelected(true);
		} else if (canEncodeH264 && !canEncodeH265) {
			radioH265.setEnabled(false);
			radioH264.setSelected(true);
			radioH265.setSelected(false);
		} else if (canEncodeH264 && canEncodeH265) {
			radioH264.setSelected(false);
			radioH265.setSelected(true);
		}
		
		checkboxFrameRate = new JCheckBox("Set FPS");
		checkboxFrameRate.setBounds(425, 155, 80, 30);
		checkboxFrameRate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (checkboxFrameRate.isSelected()) {
					textFieldFrameRate.setVisible(true);
				} else {
					textFieldFrameRate.setVisible(false);
				}
			}
		});
		mainFrame.add(checkboxFrameRate);
		
		textFieldFrameRate = new JTextField("");
		textFieldFrameRate.setBounds(checkboxFrameRate.getX() + 90, checkboxFrameRate.getY(), 50, 30);
		textFieldFrameRate.setVisible(false);
		mainFrame.add(textFieldFrameRate);
		
		// Do this last
		mainFrame.setVisible(true);
	}
	
	private boolean softwareCheck() {
		// check if ffmpeg and if vaapi or opencl are installed
		boolean goodToGo = false;
		
		ProcessBuilder processBuilder = new ProcessBuilder("vainfo");
		Process process;
		try {
			process = processBuilder.start();
			bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			
			System.out.println("Checking for support of FFMPEG, VAAPI, H.264, and H.265...");
			
			String line = "";
			while((line = bufferedReader.readLine()) != null) {
				if (line.contains("H264") && line.contains("EncSlice")) {
					System.out.println(line + "  - H264 SUPPORTED!");
					canEncodeH264 = true;
					goodToGo = true;
				} else if ((line.contains("HEVC")) && (line.contains("EncSlice"))) {
					System.out.println(line + "  - HEVC SUPPORTED!");
					canEncodeH265 = true;
					goodToGo = true;
				} else if ((line.contains("H264")) && (line.contains("VLD"))) {
					canDecodeH264 = true;
				} else if ((line.contains("HVEC")) && (line.contains("VLD"))) {
					canDecodeH265 = true;
				} else {
					//System.out.println(line);
				}
			}
			bufferedReader.close();
			if (process.isAlive()) {
				process.destroy();
			}
			
		} catch (IOException e) {
			//e.printStackTrace();
			if (e.getMessage().contains("error=2")) {
				System.out.println("vainfo not found!");
				JOptionPane.showMessageDialog(mainFrame,"vainfo not found. Install vainfo so I can look at what is supported. Encoding disabled! \n\nInstall with command:\nsudo apt-get install vainfo");
			}
		}
		
		if (canEncodeH264 || canEncodeH265) {
			System.out.println("Found Hardware Acceleration Drivers :)");
		}
		
		processBuilder = new ProcessBuilder("ffmpeg", "-h");
		try {
			process = processBuilder.start();
			bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			
			String line = "";
			while((line = bufferedReader.readLine()) != null) {
				if (line.contains("Hyper fast Audio and Video encoder")) {
					// ffmpeg detected
					//System.out.println(line);
					System.out.println("ffmpeg detected  :)");
					break;
				} else {
					// ffmpeg not detected
					//System.out.println(line);
					System.out.println("ffmpeg NOT detected");
					goodToGo = false;
				}
			}
			bufferedReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (goodToGo) {
			System.out.println("Ready to Convert files!");
		}
		
		return goodToGo;
	}
	
	private void convert() {
		System.out.println("Converting...");
		buttonConvert.setBackground(Color.RED);
		buttonConvert.setText("Stop");
		
		ArrayList<String> fullCmd = new ArrayList();
		fullCmd.add("ffmpeg");
		fullCmd.add("-hwaccel");
		fullCmd.add("vaapi");
		fullCmd.add("-hwaccel_output_format");
		fullCmd.add("vaapi");
		
		if (radioH264.isSelected()) {
			fullCmd.add("-vaapi_device");
		} else {
			fullCmd.add("-hwaccel_device");
		}
		
		fullCmd.add("/dev/dri/renderD128");
		fullCmd.add("-i");
		String in = fileInputTextField.getText();
		fullCmd.add(in);
		
		if (checkboxFrameRate.isSelected()) {
			fullCmd.add("-r");
			fullCmd.add(textFieldFrameRate.getText());
		} 
		
		if (radioH264.isSelected()) {
			fullCmd.add("-c:v");
			fullCmd.add("h264_vaapi"); // use x264
			fullCmd.add("-preset");
			fullCmd.add("veryslow");
			fullCmd.add("-crf");
			fullCmd.add("20");
		} else {
			fullCmd.add("-c:v");
			fullCmd.add("hevc_vaapi"); // use x265 (HEVC)
		}
		if (checkboxSetBitRate.isSelected()) {
			fullCmd.add("-b:v");
			fullCmd.add(textFieldBitRate.getText() + "M");
		}
		if (radioH264.isSelected() || checkboxScale.isSelected()) {
			fullCmd.add("-vf");
			//String tempCmd = "'";
			String tempCmd = "";
			
			if (radioH264.isSelected()) {
				tempCmd = tempCmd + "hwupload";
			}
			
			if (radioH264.isSelected() && checkboxScale.isSelected()) tempCmd = tempCmd + ",";
			
			if (checkboxScale.isSelected()) {
				tempCmd = tempCmd + "scale_vaapi=";
				tempCmd = tempCmd + "w=";
				tempCmd = tempCmd + textFieldScaleX.getText();
				tempCmd = tempCmd + ":h=";
				tempCmd = tempCmd + textFieldScaleY.getText();
				//fullCmd.add("scale_vaapi=w=" + textFieldScaleX.getText() + ":h=" + textFieldScaleY.getText());
			}
			
			//tempCmd = tempCmd + "'";
			fullCmd.add(tempCmd);
		}
		
		if (checkboxMuteSound.isSelected()) {
			fullCmd.add("-an"); // no sound, only video channel
		} else {
			//fullCmd.add("-c:a"); // copy audio over from original without altering it
			//fullCmd.add("copy");
		}
		fullCmd.add("-y"); // this always overwrites existing files
		
		//String out = fileOutputTextField.getText().replace(" ", "\\ ");
		String out = fileOutputTextField.getText();
		fullCmd.add(out);
		executeFFMPEG(fullCmd);
	}
	
	private void executeFFMPEG(ArrayList<String> command) {
        String[] fullCmd = new String[command.size()];
        for (int i = 0; i < command.size(); i++) {
        	fullCmd[i] = command.get(i);
        }
        
        System.out.println("Using ffmpeg command:");
        for (int i = 0; i < fullCmd.length; i++){
            System.out.print(fullCmd[i] + " ");
        }
        System.out.println("");
        
        ffmpegThread = new FFmpegThread(this, fullCmd);
        ffmpegThread.start();
	}

}
