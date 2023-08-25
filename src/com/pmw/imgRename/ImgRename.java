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

import java.awt.EventQueue;
import java.awt.Toolkit;

import javax.swing.*;

public class ImgRename
{
	private static final String appName = new String("Image Rename");
	private static final String appVersion = new String("v2023.1");
    private static final String appCopyright = new String("Copyright 2013-2023");
    private static final String appAuthor = new String("Paul Walters");
        
    public static void main(String[] args) 
    {
    	EventQueue.invokeLater(new Runnable()
    	{
    		@Override
			public void run()
    		{
    			ImgRenameFrame frame = new ImgRenameFrame();
    			// Set icon image for task bar after running
    			frame.setIconImage(Toolkit.getDefaultToolkit().getImage(ImgRename.class.getResource("/resources/ImgRename.png")));
    			frame.setTitle(getAppName() + " " + getAppVersion());
    			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    			frame.setVisible(true);
    		}
    	});
    }
    
    public static final String getAppName()
    {
    	return appName;
    }

    public static final String getAppVersion()
    {
	 	return appVersion;
    }
    
    public static final String getAppAuthor()
    {
    	return appAuthor;
    }
    
    public static final String getAppCopyright()
    {
    	return appCopyright;
    }
}
