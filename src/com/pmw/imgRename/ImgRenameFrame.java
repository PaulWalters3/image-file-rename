/*
 * Copyright 2013-2023 Paul Walters
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pmw.imgRename;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

public class ImgRenameFrame extends JFrame
{
	private static final long serialVersionUID = 1L;

	private JFileChooser fileChooser;
	private JLabel statusLine;
	private JMenuItem cancelItem, openItem, startItem;
	private JCheckBox copyInsteadOfMove;
	private JTextField directoryField, searchPattern, replacePattern;
	private JProgressBar progressBar;
	private File directory = null;
	private JButton startButton, cancelButton;
	private SwingWorker<String, ProgressData> textReader = null;
	private ImgAction startAction, cancelAction;
	private String userDir = "";
	private Properties userProps = new Properties();
	private File propsFile;
	private File dialogDir = null;
		
	public ImgRenameFrame()
	{
		if ( System.getProperty("os.name").startsWith("Windows") ) {
			userDir = System.getenv("APPDATA");
		}
		else if ( System.getProperty("os.name").contains("Mac OS X") ) {
			userDir = System.getenv("HOME");
			if ( !userDir.equals("") ) {
				userDir += "/Library/Application Support";
			}
		}
		if ( !userDir.equals("") ) {
			userDir += "/com.pmw/ImgRename";
			File f = new File(userDir);
			if ( !f.exists() ) {
				f.mkdirs();
				userProps.put("dialogDir",  System.getProperty("user.dir"));
			}
			else {
				propsFile = new File(userDir + "/ImgRename.properties");
				try (FileInputStream is = new FileInputStream(propsFile)) {
					userProps.load(is);
					is.close();
				}
				catch ( Exception ignore ) {
					userProps.put("dialogDir",  System.getProperty("user.dir"));
				}
			}
		}

		if ( userProps.containsKey("dialogDir" ) ) {
			dialogDir = new File(userProps.getProperty("dialogDir"));
		}

		try {
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		}
		catch ( Exception e ) {}
		
		setResizable(false);

		fileChooser = new JFileChooser();
		System.out.println(dialogDir);
		fileChooser.setCurrentDirectory(dialogDir);
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		// BorderLayout
		//  |--> GridBagLayout
		
		JPanel centerPanel = new JPanel();
		centerPanel.setPreferredSize(new Dimension(480,150));
		centerPanel.setLayout(new GridBagLayout());

		// Directory [...]: ____
		JButton openButton = new JButton("...");
		openButton.setPreferredSize(new Dimension(20,20));
		openButton.addActionListener(new ImgAction("Open"));

		addToGridBag(0,0,1,1,null,centerPanel,new JLabel("Directory "));
		addToGridBag(1,0,1,1,null,centerPanel,openButton);
		addToGridBag(2,0,1,1,null,centerPanel,new JLabel(": "));

		directoryField = new JTextField("",30);
		directoryField.setEditable(false);
		addToGridBag(3,0,2,1,java.awt.GridBagConstraints.WEST,centerPanel,directoryField);
		
		// Search: IMG
		addToGridBag(0,1,3,1,java.awt.GridBagConstraints.EAST,centerPanel, new JLabel("Search: "));
		searchPattern = new JTextField("IMG",20);
		addToGridBag(3,1,2,1,java.awt.GridBagConstraints.WEST,centerPanel,searchPattern);
		
		// Replace: PW
		addToGridBag(0,2,3,1,java.awt.GridBagConstraints.EAST,centerPanel, new JLabel("Replace: "));
		replacePattern = new JTextField("",20);
       	addToGridBag(3,2,2,1,java.awt.GridBagConstraints.WEST,centerPanel,replacePattern);

		copyInsteadOfMove = new JCheckBox("Copy instead of rename");
		addToGridBag(3,3,3,1,GridBagConstraints.WEST,centerPanel,copyInsteadOfMove);
		
		// [Start][Cancel][Progress.....]
		JPanel buttonPanel = new JPanel();
		startButton = new JButton("Start");
		startButton.setEnabled(false);
		startAction = new ImgAction("Start");
		startButton.addActionListener(startAction);
		buttonPanel.add(startButton);
		
		cancelButton = new JButton("Cancel");
		cancelButton.setEnabled(false);
		cancelAction = new ImgAction("Cancel");
		cancelButton.addActionListener(cancelAction);
		buttonPanel.add(cancelButton);
		
		progressBar = new JProgressBar(0,100);
		buttonPanel.add(progressBar);
		
		addToGridBag(3,4,2,1,java.awt.GridBagConstraints.WEST,centerPanel,buttonPanel);
		
		add(centerPanel, BorderLayout.CENTER);
		
		// Status line and panel
		JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		statusLine = new JLabel("Select a directory...");
		statusLine.setFont(new Font("SansSerif", Font.ITALIC, 12));
		statusPanel.add(statusLine);
		statusPanel.setBorder(BorderFactory.createEmptyBorder(0,2,2,2));
		
		add(statusPanel, BorderLayout.SOUTH);
		
		createMenuBar();
		
		pack();
	}

	private void addToGridBag(int x, int y, int width, int height, Integer anchor, JPanel panel, JComponent component)
	{ 
		GridBagConstraints constraints = new GridBagConstraints();

		constraints.weightx = 0;
		constraints.weighty = 0;
		constraints.gridx = x;
		constraints.gridy = y;
		constraints.gridwidth = width;
		constraints.gridheight = height;
		
		int insetY = 0;
		if ( y == 0 ) {
			insetY = 2;
		}

		if ( x == 0 ) {
			constraints.insets = new Insets(insetY,2,0,0);
		}
		else if ( x == 3 ) {	// Right most column currently
			constraints.insets = new Insets(insetY,0,0,2);
		}
		
		if ( anchor != null )
			constraints.anchor = anchor;
		
		panel.add(component, constraints);
	}

	private void createMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
           
		String ctrl = "ctrl";
		String osName = System.getProperty("os.name").toLowerCase();
		if ( osName.indexOf("mac") != -1 ) {
			ctrl = "meta";
		}
   
		// Create "File" menu
		JMenu fileMenu = new JMenu("File");
		
		// File -> Open
		openItem = fileMenu.add(new ImgAction("Open"));
		openItem.setAccelerator(KeyStroke.getKeyStroke(ctrl + " O"));
		
		// File -> Start
		startItem = fileMenu.add(startAction);
		startItem.setAccelerator(KeyStroke.getKeyStroke(ctrl + " S"));
		startItem.setEnabled(false);
				
		// File -> Cancel
		cancelItem = fileMenu.add(cancelAction);
		cancelItem.setAccelerator(KeyStroke.getKeyStroke(ctrl + " C"));
		cancelItem.setEnabled(false);
				
		// File -> Quit
		fileMenu.addSeparator();
		JMenuItem quitItem = fileMenu.add(new ImgAction("Quit " + ImgRename.getAppName()));
		quitItem.setAccelerator(KeyStroke.getKeyStroke(ctrl + " Q"));
                           
		menuBar.add(fileMenu);
           
		// Create "Help" menu
		JMenu helpMenu = new JMenu("Help");
		helpMenu.add(new ImgAction("About"));
           
		menuBar.add(Box.createHorizontalGlue());	// Puts "Help" on right side of menu bar
		menuBar.add(helpMenu);
	}
	
	public void cancelRename(String reason)
	{
	   	statusLine.setText(reason);
	   	cancelItem.setEnabled(false);
	   	cancelButton.setEnabled(false);
	   	startItem.setEnabled(true);
	   	startButton.setEnabled(true);
	}
	  
	class ImgAction extends AbstractAction
    {
		private static final long serialVersionUID = 1L;

		public ImgAction(String name)
        {
                super(name);
        }
            
        @Override
		public void actionPerformed(ActionEvent event)
        {
         	if (getValue(Action.NAME).equals("Open")) {
         		progressBar.setValue(0);
                int result = fileChooser.showDialog(ImgRenameFrame.this,"Select");
                if (result == JFileChooser.APPROVE_OPTION) {
                	cancelRename(" ");
                	directory = fileChooser.getSelectedFile();
                	directoryField.setText(directory.toString());
                	replacePattern.setText(fileChooser.getName(directory));
                }
         	}
         	else if ( getValue(Action.NAME).equals("Start")) {
         		if ( searchPattern.getText().equals("") || replacePattern.getText().equals("") ) {
         			JOptionPane.showMessageDialog(null, "Please provide a search and replace pattern", "File pattern not provided", JOptionPane.ERROR_MESSAGE);
         			return;
         		}
               	statusLine.setText("Started...");
               	progressBar.setValue(0);
               	
               	userProps.put("dialogDir",  directory.getAbsolutePath());
               	if ( propsFile != null && !userDir.equals("") ) {
               		try (FileOutputStream os = new FileOutputStream(propsFile)) {
               			userProps.store(os,  null);
               			os.close();
               		}
               		catch ( Exception ignore ) {
               			ignore.printStackTrace();
               		}
               	}
               	startItem.setEnabled(false);
               	startButton.setEnabled(false);
               	cancelItem.setEnabled(true);
               	cancelButton.setEnabled(true);
               	textReader = new TextReader();
               	textReader.addPropertyChangeListener(	// For updating progress bar
               			new PropertyChangeListener() {
               				@Override
							public void propertyChange(PropertyChangeEvent evt)
               				{
               					if ( evt.getPropertyName().equals("progress") ) {
               						progressBar.setValue((Integer)evt.getNewValue());
               					}
               				}
               			}
               	);
               	textReader.execute();
         	}
         	else if ( getValue(Action.NAME).equals("Cancel")) {
         		cancelRename("Cancelled");
         		if ( textReader != null ) textReader.cancel(true);
         		progressBar.setValue(0);
         	}
         	else if ( getValue(Action.NAME).equals("About")) {
         		String message = ImgRename.getAppName() + " " 
						+ ImgRename.getAppVersion() + "\n"
						+ ImgRename.getAppAuthor() + "\n"
						+ ImgRename.getAppCopyright();
               
         		ImageIcon icon = new ImageIcon(ImgRename.class.getResource("/resources/ImgRename.png"));
		       	Image image = icon.getImage().getScaledInstance(75, 75, Image.SCALE_SMOOTH);
		       	icon.setImage(image);
		       	
		       	JOptionPane.showMessageDialog(null, message, "About " + ImgRename.getAppName(), JOptionPane.INFORMATION_MESSAGE, icon);
         	}
         	else if ( getValue(Action.NAME).equals("Quit " + ImgRename.getAppName())) {
         		System.exit(0);
         	}
        }
    }
            
    private class ProgressData
    {
    	public Path origFile, newFile;
    	
    	ProgressData(Path origFile, Path newFile)
    	{
    		this.origFile = origFile;
    		this.newFile = newFile;
    	}
    }
    
    private class TextReader extends SwingWorker<String, ProgressData>
    {
    	@Override
    	public String doInBackground() 
    	{
    		int total = 0;
    		
    		
    		ArrayList<Path> files = null;
    		try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory.toPath(),"*.{jpg,JPG,gif,GIF,mov,MOV,png,PNG,jpeg,JPEG,heic,HEIC,mp4,MP4,bmp,BMP}")) {
    			files = new ArrayList<Path>();
    			for (Path file: stream) {
    				files.add(file);
    				System.out.println(file);
    			}
    		}
    		catch ( Exception e ) {
    			System.out.println("Error: " + e);
    			e.printStackTrace();
    			((DirectoryIteratorException)e).getCause().printStackTrace();
    		}

			for (Path file: files) {
				ProgressData data = new ProgressData(file, Paths.get(file.toString().replaceFirst(searchPattern.getText(), replacePattern.getText())));
				total += 1;
				System.out.println(data.origFile + " => " + data.newFile);
				publish(data);
				setProgress(100 * total / files.size());
				
				try {
					if ( copyInsteadOfMove.isSelected() ) {
						Files.copy(data.origFile, data.newFile);
					}
					else {
						Files.move(data.origFile, data.newFile);
					}
				}
				catch ( Exception e ) {
					int selection = JOptionPane.showConfirmDialog(null, "Error renaming file: " + data.origFile + "\n" + e + "\nDo you wish to continue?", "Error renaming file", JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE);
					if ( selection == JOptionPane.CANCEL_OPTION ) {
						cancelRename("Error: " + e);
						if ( textReader != null )
							textReader.cancel(true);
							break;
					}
				}
			}
    		
    		return "Completed: " + total + " files";
    	}
    	
    	@Override 
    	protected void process(List<ProgressData> chunks)
    	{
    		ProgressData current = chunks.get(chunks.size()-1);
    		System.out.println(current.origFile + " => " + current.newFile);
    		statusLine.setText(current.origFile + " => " + current.newFile);
       	}
    	
    	@Override 
    	protected void done()
    	{
    		try {
    			cancelRename(get());
    		}
    		catch ( Exception ignore ) {}
    	}
    }
 }


