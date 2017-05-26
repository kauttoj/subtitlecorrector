import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import javax.swing.filechooser.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.util.*;    
 
public class GUI extends JFrame {
    /**
	 * 
	 */

	
	
	private static final long serialVersionUID = 1L;
	/**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */

   // private static boolean DEMO = false;

    //private JDesktopPane dp = new JDesktopPane();
    private DefaultListModel listModel = new DefaultListModel();
    //private JList list = new JList(listModel);
    private JTextPane textpane,textpane_second;
    private JScrollPane jsp;

    private JPanel ala = new JPanel();
    private JPanel yla = new JPanel();
    private static int left;
    private static int top;
    private JCheckBoxMenuItem copyItem;
    private JCheckBoxMenuItem nullItem;
    private JCheckBoxMenuItem thItem;
    Vector<Main> main = new Vector<Main>();
    StyledDocument console,console_second;
    Style style,style_second;

    private class Doc extends InternalFrameAdapter implements ActionListener {
        String name;
        JInternalFrame frame;
        TransferHandler th;
        JTextArea area;

        public Doc(File file) {
            this.name = file.getName();
            //printConsole(0," ");
            try {
                //init(file.toURI().toURL());
            	main.add(new Main());
                String a = main.lastElement().ReadFile(file.toURI().toURL());
                printConsole(0,"'" + a + "'" + " parsed OK");
                textpane.setBackground(Color.GREEN);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                printConsole(1,"Reading file FAILED(" + e.getMessage() + ")");
                textpane.setBackground(Color.RED);
            } catch (IOException e) {
				// TODO Auto-generated catch block            	
				e.printStackTrace();
				printConsole(1,"Reading file FAILED (" + e.getMessage() + ")");
				 textpane.setBackground(Color.RED);
			}
        }
        /*
        public Doc(String name) {
            this.name = name;
            init(getClass().getResource(name));
        }
        
        private void init(URL url) {
            frame = new JInternalFrame(name);
            frame.addInternalFrameListener(this);
            listModel.add(listModel.size(), this);

            area = new JTextArea();
            area.setMargin(new Insets(5, 5, 5, 5));

            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                String in;
                while ((in = reader.readLine()) != null) {
                    area.append(in);
                    area.append("\n");
                }
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            th = area.getTransferHandler();
            area.setFont(new Font("monospaced", Font.PLAIN, 12));
            area.setCaretPosition(0);
            area.setDragEnabled(true);
            area.setDropMode(DropMode.INSERT);
            frame.getContentPane().add(new JScrollPane(area));
            dp.add(frame);
            frame.show();
            //frame.setSize(300, 500);            
            
            frame.setResizable(true);
            frame.setSize(area.getWidth(),area.getHeight());
            frame.setClosable(true);
            frame.setIconifiable(true);
            frame.setMaximizable(true);
            frame.setLocation(left, top);
            incr();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    select();
                }
            });
            nullItem.addActionListener(this);
            setNullTH();
        }

        public void internalFrameClosing(InternalFrameEvent event) {
            listModel.removeElement(this);
            nullItem.removeActionListener(this);
        }

        /*
        public void internalFrameOpened(InternalFrameEvent event) {
            int index = listModel.indexOf(this);
            list.getSelectionModel().setSelectionInterval(index, index);
        }

        public void internalFrameActivated(InternalFrameEvent event) {
            int index = listModel.indexOf(this);
            list.getSelectionModel().setSelectionInterval(index, index);
        }
*/
        public String toString() {
            return name;
        }
        
        public void select() {
            try {
                frame.toFront();
                frame.setSelected(true);
            } catch (java.beans.PropertyVetoException e) {}
        }
        
        public void actionPerformed(ActionEvent ae) {
            setNullTH();
        }
        
        public void setNullTH() {
            if (nullItem.isSelected()) {
                area.setTransferHandler(null);
            } else {
                area.setTransferHandler(th);
            }
        }
    }
    
    void OpenFileButton() {
    	
    	JFileChooser chooser = new JFileChooser();
    	String aa;
        
        int option = chooser.showOpenDialog(this);
        if(option == JFileChooser.APPROVE_OPTION) {
        		
        	if (chooser.getSelectedFile()!=null) {
        		main.clear();
        		new Doc(chooser.getSelectedFile());
        	}
        	else
        	   ;
        	
         }
    
        if(option == JFileChooser.CANCEL_OPTION) {
        	;//display.setText("You canceled.");
        }        
    //}
    }

    private TransferHandler handler = new TransferHandler() {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public boolean canImport(TransferHandler.TransferSupport support) {
            if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                return false;
            }

            if (copyItem.isSelected()) {
                boolean copySupported = (COPY & support.getSourceDropActions()) == COPY;

                if (!copySupported) {
                    return false;
                }

                support.setDropAction(COPY);
            }

            return true;
        }

        public boolean importData(TransferHandler.TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            
            Transferable t = support.getTransferable();

            try {
                @SuppressWarnings("unchecked")
				java.util.List<File> l =
                    (java.util.List<File>)t.getTransferData(DataFlavor.javaFileListFlavor);

                main.clear();
                for (File f : l) {
                    new Doc(f);
                }
            } catch (UnsupportedFlavorException e) {
                return false;
            } catch (IOException e) {
                return false;
            }

            return true;
        }
    };
/*
    private static void incr() {
        left += 30;
        top += 30;
        if (top == 150) {
            top = 0;
        }
    }
*/
    
    public void FixButtonPressed() {
    	
    	try {
    		if (main.size()>0) {
    		for (Main a : main) {
    			if (!a.alreadyFixed && a.Fix()) {
    				printConsole(0,"file '" + a.filename + "' fixed succesfully!");   				
    				printConsole(0," Error count: " + a.errors.giveString());
    				if (a.badlines.koko>0)
    					printConsole(0," Possibly bad lines: " + a.badlines.giveString());
    				a.doProofRead(this);
    			}
    		}
    		
    		}
    		else        		
        		printConsole(1,"Read file(s) first!");    		
        } catch (IOException e) {
        	e.printStackTrace();
        	printConsole(1,"File fix FAILED (" + e.getMessage() + ")");
        }
    	
    	printConsole(0,"");
    	
    	//System.out.println("Hello, World");
    }
    public void printConsole(int vari,String srt) {
    	
    	if (vari == 0) {
    		StyleConstants.setForeground(style, Color.blue);
    	}
    	else {
    		StyleConstants.setForeground(style, Color.black);
    	}
    	
        try {
			console.insertString(console.getLength(),srt+"\n",style);
		} catch (BadLocationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    }
    
    public void printConsole(String srt) {

        try {
			console.insertString(console.getLength(),srt+"\n",style);
		} catch (BadLocationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    }
    
    public GUI() {
    	
    	    	    	    	
        super("Subtitle Corrector");
        
       
        
        //getContentPane().add(jsp);
        setJMenuBar(createDummyMenuBar());
        getContentPane().add(createDummyToolBar(), BorderLayout.NORTH);
                                   
        //JPanel panel = new JPanel();
        JButton reset = new JButton("FIX MY SUBTITLE!");        
        reset.addActionListener(new ActionListener() { 
      	  public void actionPerformed(ActionEvent e) { 
      	    FixButtonPressed();
      	  } 
      	} );        
        
        
        textpane = new JTextPane();
        textpane.setEditable(false);
        console = textpane.getStyledDocument();
        style = textpane.addStyle("style1", null);
        
               
        //JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, textpane, dp);
        //sp.setResizeWeight(0.5);
        //getContentPane().add(sp);
        reset.setMinimumSize(new Dimension(100,35));
        
         JSplitPane aa = new JSplitPane(JSplitPane.VERTICAL_SPLIT,new JScrollPane(textpane), reset);   
        getContentPane().add(aa);
        aa.setResizeWeight(1.0);
        //aa.setDividerLocation(10);
        
 
        
        
        //jsp.setBounds(3, 3, 300, 200);
		//add(jsp);	
        
        /*
        StyleConstants.setForeground(style, Color.red);
        try { doc.insertString(doc.getLength(), "BLAH ",style); }
        catch (BadLocationException e){}
*/
         printConsole(0,"Drag and drop subtitle(s)");

        
      
/*
        list.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                
                Doc val = (Doc)list.getSelectedValue();
                if (val != null) {
                    val.select();
                }
             }
        });
        
        final TransferHandler th = list.getTransferHandler();

        nullItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (nullItem.isSelected()) {
                    list.setTransferHandler(null);
                } else {
                    list.setTransferHandler(th);
                }
            }
        });
        */
        thItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (thItem.isSelected()) {
                    setTransferHandler(handler);
                } else {
                    setTransferHandler(null);
                }
            }
        });
       // textpane.setTransferHandler(handler);
        textpane.setTransferHandler(handler);
    }

    private static void createAndShowGUI(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }

        GUI test = new GUI();
        test.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        test.setSize(600,500);

        test.setLocationRelativeTo(null);
        test.setVisible(true);
        //test.list.requestFocus();
    }
    
    public static void main(final String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                //Turn off metal's use of bold fonts
	        UIManager.put("swing.boldMetal", Boolean.FALSE);
                createAndShowGUI(args);
            }
        });
    }
    
    private JComponent createControlPanel() {
        JPanel panel = new JPanel();
        JButton reset = new JButton("Fix");
        //reset.addActionListener(this);
        panel.add(reset);
        return panel;
    }
    
    private JToolBar createDummyToolBar() {
    	
        JToolBar tb = new JToolBar();
        
        /*
        JButton b;        
        b = new JButton("New");
        b.setRequestFocusEnabled(false);
        tb.add(b);
        
        b = new JButton("Open");
        b.setRequestFocusEnabled(false);
        tb.add(b);
        b = new JButton("Save");
        b.setRequestFocusEnabled(false);
        tb.add(b);
        
        b = new JButton("Print");
        b.setRequestFocusEnabled(false);
        tb.add(b);
        
        b = new JButton("Preview");
        b.setRequestFocusEnabled(false);
        tb.add(b);
        tb.setFloatable(false);
        */
        return tb;
    }
    
    private JMenuBar createDummyMenuBar() {
        JMenuBar mb = new JMenuBar();
        mb.add(createFileMenu("File"));
        //mb.add(createDummyMenu("Edit"));
        //mb.add(createDummyMenu("Search"));
        //mb.add(createDummyMenu("View"));
        //mb.add(createDummyMenu("Tools"));
        mb.add(createHelpMenu("Help"));
        
        
        JMenu demo = new JMenu("Demo");
        demo.setMnemonic(KeyEvent.VK_D);
//        mb.add(demo);

        thItem = new JCheckBoxMenuItem("Use Top-Level TransferHandler");
        thItem.setMnemonic(KeyEvent.VK_T);
        demo.add(thItem);

        nullItem = new JCheckBoxMenuItem("Remove TransferHandler from List and Text");
        nullItem.setMnemonic(KeyEvent.VK_R);
        demo.add(nullItem);

        copyItem = new JCheckBoxMenuItem("Use COPY Action");
        copyItem.setMnemonic(KeyEvent.VK_C);
        demo.add(copyItem);

        return mb;
    }
    
    private JMenu createFileMenu(String str) {
        JMenu menu = new JMenu(str);
        JMenuItem item = new JMenuItem("Open file");
        item.setEnabled(true);
        menu.add(item);
        
        item.addActionListener(new ActionListener() { 
        	  public void actionPerformed(ActionEvent e) { 
        	    OpenFileButton();
        	  } 
        	} );
        
        return menu;
               
        
    }
    
    private void About() {
    	
    	printConsole("Version 0.1, copyright Janne K.\n" );
    	
    }
    
    private JMenu createHelpMenu(String str) {
        JMenu menu = new JMenu(str);
        JMenuItem item = new JMenuItem("About");
        item.setEnabled(true);
        menu.add(item);
        
        item.addActionListener(new ActionListener() { 
      	  public void actionPerformed(ActionEvent e) { 
      	    About();
      	  } 
      	} );
        
        /*
        item.addActionListener(new ActionListener() { 
        	  public void actionPerformed(ActionEvent e) { 
        	    OpenFileButton();
        	  } 
        	} );
        */
        return menu;
               
        
    }
    
}