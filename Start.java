import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import java.awt.Panel;
import java.awt.TextArea;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JToolBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.TransferHandler;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Vector;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyleConstants;

import java.awt.SystemColor;

import org.languagetool.*;
import org.languagetool.language.*;
import org.languagetool.rules.Rule;
import org.languagetool.rules.RuleMatch;


public class Start extends JFrame {

	private JPanel contentPane;
	Vector<SubtitleCorrector> subtitleElements = new Vector<SubtitleCorrector>();
    private JCheckBoxMenuItem copyItem,nullItem,thItem;
    private boolean doProofReadCheck=false;
    
    private JTextArea console_left,console_right;
    
    private Action copyAction = new DefaultEditorKit.CopyAction();
    private JPopupMenu popup = new JPopupMenu();
    private PopupListener popupListener = new PopupListener();
    private JScrollPane scrollPane;//,scrollPane_1;
    
    public JLanguageTool langTool1,langTool2;
    private boolean initProofReadCheck=false;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Start frame = new Start();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
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

	                subtitleElements.clear();
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
	
	   private class Doc extends InternalFrameAdapter implements ActionListener {
	        String name;
	        JInternalFrame frame;
	        TransferHandler th;
	        JTextArea area;

	        public Doc(File file) {
	            this.name = file.getName();
	            //printConsole(0," ");
	            try {
	            	subtitleElements.add(new SubtitleCorrector());
	                String a = subtitleElements.lastElement().ReadFile(file.toURI().toURL());
	                printConsole("'" + a + "'" + " parsed OK\n",1);
	                //textpane.setBackground(Color.GREEN);
	            } catch (MalformedURLException e) {
	                e.printStackTrace();
	                printConsole("Reading file FAILED(" + e.getMessage() + ")",1);
	                //textpane.setBackground(Color.RED);
	            } catch (IOException e) {      	
					e.printStackTrace();
					printConsole("Reading file FAILED (" + e.getMessage() + ")",1);
					//textpane.setBackground(Color.RED);
				}
	        }
	       
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
        
        int option = chooser.showOpenDialog(this);
        if(option == JFileChooser.APPROVE_OPTION) {
        		
        	if (chooser.getSelectedFile()!=null) {
        		subtitleElements.clear();
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
    
    private JMenuBar createDummyMenuBar() {
        JMenuBar mb = new JMenuBar();
        mb.add(createFileMenu("File"));
        //mb.add(createDummyMenu("Edit"));
        //mb.add(createDummyMenu("Search"));
        //mb.add(createDummyMenu("View"));
        //mb.add(createDummyMenu("Tools"));
        mb.add(createHelpMenu("Help"));
        mb.add(createEditMenu("Edit"));
        
        
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
    
    private void About() {
    	
    	printConsole("SubtitleCorrector (version 2.0) by Janne K.\n",1);
    	
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
        
        return menu;
        
    }
    
    private JMenu createEditMenu(String str) {
        JMenu menu = new JMenu(str);
        JMenuItem item = new JMenuItem("Copy");
        item.setEnabled(true);
        menu.add(item);
        
        item.addActionListener(new ActionListener() { 
      	  public void actionPerformed(ActionEvent e) { 
      		setClipboardContents();
      	  } 
      	} );
        
        return menu;
        
    }
    
    public void setClipboardContents(){
        StringSelection selection = new StringSelection(console_right.getSelectedText());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
      }
	
    
    public void FixButtonPressed() {
    	    	    	
    	try {
    		if (subtitleElements.size()>0) {
    			console_right.setText(""); 
    		for (SubtitleCorrector a : subtitleElements) {
    			
    			if (!a.alreadyFixed && a.Fix()) {
    				printConsole("file '" + a.filename + "' fixed succesfully!\n",1);   				
    				printConsole("--ERRORS: " + a.errors.giveString()+"\n",1);
    				if (a.badlines.koko>0)
    					printConsole(" Possible bad lines: " + a.badlines.giveString()+"\n",1);
    				printConsole("\n",1);    				
    			}
    			
				if (doProofReadCheck) {
					printConsole("---- file '" + a.filename + "'----\n",2);					
					a.doProofRead(this);
				}
    		}
    		
    		}
    		else ;       		
        		//printConsole(1,"Read file(s) first!");    		
        } catch (IOException e) {
        	e.printStackTrace();
        	//printConsole(1,"File fix FAILED (" + e.getMessage() + ")");
        	//console_left.append("FAILEDDDDDDDD");
        }
    	
    	//printConsole("TESTTTTTTT\n",2);
    	
    }
        
    public void printConsole(String srt,int kumpi) {

    	//String aa = console_right.getText();
    	
        if (kumpi==1) {
			console_left.append(srt);
			console_left.setCaretPosition(console_left.getDocument().getLength());
        }
		else
			console_right.append(srt);
        
        //String bb = console_right.getText();
    }    
    
	/**
	 * Create the frame.
	 */
	public Start() {
		setTitle("SubtitleCorrector 2.0");
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 612, 608);
		
        setJMenuBar(createDummyMenuBar());
        getContentPane().add(new JToolBar(), BorderLayout.NORTH);
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JButton btnNewButton = new JButton("Fix my subtitle!");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				FixButtonPressed();
			}
		});
		btnNewButton.setBounds(10, 487, 172, 49);
		contentPane.add(btnNewButton);
		
		JCheckBox chckbxNewCheckBox = new JCheckBox("proof-read (en)");
		chckbxNewCheckBox.setSelected(false);
		chckbxNewCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doProofReadCheck = !doProofReadCheck;
				
				if (!initProofReadCheck) {
					
					try {
						console_right.setText("Starting proofread module..."); 
						
						langTool1 = new JLanguageTool(new AmericanEnglish());
						for (Rule rule : langTool1.getAllRules()) {
							if (!rule.isDictionaryBasedSpellingRule()) {
								langTool1.disableRule(rule.getId());
							}
						}
						langTool2 = new JLanguageTool(new BritishEnglish());
						for (Rule rule : langTool2.getAllRules()) {
							if (!rule.isDictionaryBasedSpellingRule()) {
								langTool2.disableRule(rule.getId());
							}
						}
						initProofReadCheck=true;
						console_right.setText("Module ready!"); 
					} catch (Exception ex) {
						ex.getMessage();
						console_right.setText("Failed to start proofread module!");
						doProofReadCheck=false;
					}		
				}
				
				/*
				if (doProofReadCheck) {
						String res = SubtitleCorrector.testProofRead();
						if (!res.isEmpty()) {
							doProofReadCheck = false;
							printConsole("Proof-read module failed with error:\n"+res,2);
						}
				}
				*/
			}
		});
		chckbxNewCheckBox.setBounds(202, 500, 117, 23);
		contentPane.add(chckbxNewCheckBox);
		
		scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBounds(0, 0, 353, 463);
		contentPane.add(scrollPane);
		
		JTextArea textArea = new JTextArea();
		textArea.setBackground(new Color(220, 220, 220));
		textArea.setEditable(false);
		console_left = textArea;
		textArea.setLineWrap(true);
		scrollPane.setViewportView(textArea);	
		
		JScrollPane scrollPane_1 = new JScrollPane();
		scrollPane_1.setBounds(355, 0, 251, 548);
		contentPane.add(scrollPane_1);
		
		JTextArea textArea_1 = new JTextArea();
		textArea_1.setBackground(SystemColor.controlHighlight);
		console_right = textArea_1;
		textArea_1.setEditable(false);
		scrollPane_1.setViewportView(textArea_1);
		
		textArea_1.addMouseListener(popupListener);
		/*
        textArea_1.addFocusListener(new FocusAdapter() {
           @Override
           public void focusGained(FocusEvent e) {
              ((JTextComponent)e.getSource()).selectAll();
           }
        });
        */		
		
		JMenuItem item = new JMenuItem("Copy");
		popup.add(item);
        item.addActionListener(new ActionListener() { 
      	  public void actionPerformed(ActionEvent e) { 
      		setClipboardContents();
      	  } 
      	} );
		
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
        contentPane.setTransferHandler(handler);	
        textArea.setTransferHandler(handler);	
        textArea_1.setTransferHandler(handler);
        
        
        
        //printConsole("TESTTETETTF FFD FDF",2);
	}
	/*
	private void pasteToClipBoard() {
	    Toolkit toolkit = Toolkit.getDefaultToolkit();
	    Clipboard clipboard = toolkit.getSystemClipboard();
	    StringSelection selection = new StringSelection(textArea_1.getText());
	    clipboard.setContents(selection, null);
	}
	*/
	
	private class PopupListener extends MouseAdapter {
	      public void mousePressed(MouseEvent e) {
	         maybeShowPopup(e);
	     }

	     public void mouseReleased(MouseEvent e) {
	         maybeShowPopup(e);
	     }

	     private void maybeShowPopup(MouseEvent e) {
	         if (e.isPopupTrigger()) {
	             popup.show(e.getComponent(),
	                        e.getX(), e.getY());
	         }
	     }
	   }
}
