/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2016 Actelion Pharmaceuticals Ltd., Gewerbestrasse 16,
 * CH-4123 Allschwil, Switzerland.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 * @author Joel Freyss
 */

package com.actelion.research.util.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;


/**
 * Simple Wrapper to a SwingWorker. It is used to execute longer processes on the background, while showing that something is going on.
 * This class uses the SwingWorkerExecutor to maximize the number of possible threads (2 + the main thread)
 *  
 * @author jfreyss
 *
 */
public abstract class SwingWorkerExtended  {

	private final boolean DEBUG = false;
	
	public static final int FLAG_ASYNCHRONOUS = 0;
	public static final int FLAG_CANCELABLE = 1;
	public static final int FLAG_SYNCHRONOUS = 2;
	public static final int FLAG_ASYNCHRONOUS50MS = 4;
	public static final int FLAG_ASYNCHRONOUS200MS = 8;
	
	private final String name;	
	private JFrame frame = null;
	private JDialog dialog = null;
	
	
	/**
	 * 
	 * @param comp
	 * @param cancellable
	 */
	public void startBgProcess(final String title, final Component comp, final boolean cancellable) {
		if(comp==null || !comp.isShowing() || longTaskDone.get()) {
			return;
		}
		
		if(!(comp instanceof JComponent)) {			
			System.err.println("SwingWorkerExtended: component not a JComponent: " + comp.getClass());
			return;
		}
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				
				if(!comp.isShowing() || longTaskDone.get()) {
					return;
				}
				bgTaskStarted.set(true);
				
				if(((JComponent)comp).getTopLevelAncestor() instanceof JFrame) {
					frame = (JFrame) ((JComponent)comp).getTopLevelAncestor();
					if(frame.getGlassPane().isVisible()) return;
					if(frame.isUndecorated()) return;
				} else if(((JComponent)comp).getTopLevelAncestor() instanceof JDialog) {
					dialog = (JDialog) ((JComponent)comp).getTopLevelAncestor(); 
					if(dialog.getGlassPane().isVisible()) return;
				} else {
					System.err.println("SwingWorkerExtended: getTopLevelAncestor not a jframe or jdialog -> "+ ((JComponent)comp).getTopLevelAncestor());
					return;
				}

				
				final Point p1 = frame!=null? frame.getRootPane().getLocationOnScreen():  dialog.getRootPane().getLocationOnScreen();
				final Point p2 = comp.getLocationOnScreen();
				final Rectangle r = new Rectangle(p2.x-p1.x, p2.y-p1.y, comp.getWidth(), comp.getHeight());
				if(r.width==0 || r.height==0) {
					System.err.println("SwingWorkerExtended: component not yet realized");
					return;
				}

				final JComponent glassPane = new JPanel() {
					@Override
					public void paint(Graphics g) {			
						
						
						if (g instanceof Graphics2D) {
							Color c1 = new Color(220,120,120,0);
							Color c2 = new Color(255,255,255,255);
				
							Graphics2D g2d = (Graphics2D) g;
							g2d.setPaint(new GradientPaint(r.x, r.y, c1, r.x + r.width/2, r.y + r.height/2, c2, true));
							g2d.fillRect(r.x, r.y, r.width/2, r.height/2);
							
							g2d.setPaint(new GradientPaint(r.x + r.width, r.y + r.height, c1, r.x + r.width/2, r.y + r.height/2, c2, true));
							g2d.fillRect(r.x + r.width/2, r.y + r.height/2, r.width/2, r.height/2);
							
							g2d.setPaint(new GradientPaint(r.x + r.width, r.y, c1, r.x + r.width/2, r.y + r.height/2, c2, true));
							g2d.fillRect(r.x + r.width/2, r.y, r.width/2, r.height/2);
							
							g2d.setPaint(new GradientPaint(r.x, r.y + r.height, c1, r.x + r.width/2, r.y + r.height/2, c2, true));
							g2d.fillRect(r.x, r.y + r.height/2, r.width/2, r.height/2);
						}
						
						if(title!=null) {
							g.setFont(FastFont.REGULAR);
							g.setColor(Color.BLACK);
							g.drawString(title, r.x + r.width/2 - g.getFontMetrics().stringWidth(title)/2, r.y + r.height/2-42);
						}							
						super.paint(g);
					}
				};
				
				glassPane.setOpaque(false);
				glassPane.setLayout(null);

				JProgressBar bar = new JProgressBar();
				bar.setIndeterminate(true);
				bar.setBounds(r.x + r.width/2-60, r.y + r.height/2-40, 120, 42);
				glassPane.add(bar);
				
				
				if(cancellable) {
					JButton cancelButton = new JButton("Cancel");
					cancelButton.setBorderPainted(false);
//					cancelButton.setFont(FastFont.SMALL);
					Dimension d = cancelButton.getPreferredSize();
					cancelButton.setBounds(r.x + r.width/2 - d.width/2, r.y + r.height/2+32-d.height-4, d.width, d.height-4);
					glassPane.add(cancelButton);
					ActionListener cancelAction = new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							endBgProcess();
							sw.interrupt();
						}
					};
					cancelButton.addActionListener(cancelAction);
					KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
					glassPane.registerKeyboardAction(cancelAction, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
					cancelButton.addActionListener(cancelAction);

				}
				
				
				if(frame!=null) {
					frame.setGlassPane(glassPane);
				} else {
					dialog.setGlassPane(glassPane);
				}
				glassPane.setVisible(true);	
				glassPane.repaint();		
			}
		});
				
	}

	private Thread delayVisibleThread;
	private static int instances;
	private void endBgProcess() {
		longTaskDone.set(true);						
		if(bgTaskStarted.get()) {
			try {
				delayVisibleThread.interrupt();
				try {delayVisibleThread.join(1000);}catch(Exception e) {return;}								
			} finally {
				SwingUtilities.invokeLater(new Runnable() {			
					@Override
					public void run() {
						if(frame!=null) {
							frame.getGlassPane().setVisible(false);
						} else if(dialog!=null) {
							dialog.getGlassPane().setVisible(false);
						}
					}
				});
			}
		}		
	}
	
	
	private Thread sw;
	private final AtomicBoolean bgTaskStarted = new AtomicBoolean(false);
	private final AtomicBoolean longTaskDone = new AtomicBoolean(false);
	private final static Map<Component, Map<String, Thread>> currentThreads = new HashMap<>();
	
	public SwingWorkerExtended() {
		this(null, null, FLAG_ASYNCHRONOUS50MS);
	}
	
	public SwingWorkerExtended(Component myComp, final boolean cancellable) {
		this(null, myComp, cancellable);
	}
	
	public SwingWorkerExtended(final String title, Component myComp) {
		this(title, myComp, FLAG_ASYNCHRONOUS50MS);
	}
	public SwingWorkerExtended(final String title, Component myComp, final boolean cancellable) {
		this(title, myComp, cancellable? FLAG_CANCELABLE: FLAG_ASYNCHRONOUS);
	}
	
	
	/**
	 * Creates a SwingWorker
	 * @param comp
	 * @param cancellable
	 */
	public SwingWorkerExtended(final String title, final Component myComp, final int flags) {
		final long started = System.currentTimeMillis();
		this.name = (instances++) + "-" + (title==null?"SwingWorker":title);
		final Component comp = (myComp==null || myComp.getWidth()==0) && UIUtils.getMainFrame()!=null? ((JFrame)UIUtils.getMainFrame()).getContentPane(): myComp;
		
//		boolean fromEventDispatcher = SwingUtilities.isEventDispatchThread();
//		boolean fromPool = SwingWorkerExecutor.isFromPool(Thread.currentThread());

		String last = "";
		StackTraceElement[] t = Thread.currentThread().getStackTrace();
		for (StackTraceElement stackTraceElement : t) {
			if(stackTraceElement.getClassName().startsWith("com.actelion") && !stackTraceElement.getClassName().startsWith("com.actelion.research.gui.util")) {
				last = stackTraceElement.toString();
				break;
			}
		}
		if(DEBUG) System.out.println("SwingWorkerExtended " + name + " -START- " + last);
		
		final String callingThread = Thread.currentThread().getName();
		final Runnable doneRunnable = new Runnable() {					
			@Override
			public void run() {
				SwingWorkerExtended.this.done();
			}
		};
		
		if((flags & FLAG_SYNCHRONOUS)>0  /*&& (fromEventDispatcher || fromPool)*/) {
			//SYNCHRONOUS MODE: call doBackground in the same thread
			try {
				if(DEBUG) System.out.println("SwingWorkerExtended " + name + " -BG- " + (System.currentTimeMillis()-started) + "ms - " + callingThread);
				doInBackground();
				
				SwingUtilities.invokeLater(doneRunnable);
				
			} catch(Exception e) {
				JExceptionDialog.showError(comp, e);				
			} finally {
				endBgProcess();
			}
			if(DEBUG) System.out.println("SwingWorkerExtended "+name+" -DONE- " + (System.currentTimeMillis()-started) + "ms - " +callingThread);
		} else {
			//ASYNCHRONOUS MODE: call doBackground in a separate thread
			sw = new Thread() {
				@Override
				public void run() {		
					
					if(isCancelled()) {
						endBgProcess();
						if(DEBUG) System.out.println("SwingWorkerExtended "+name+" -STOP- " + (System.currentTimeMillis()-started) + "ms - " + callingThread);					
						return;
					}
					
					if(comp!=null && (flags & (FLAG_ASYNCHRONOUS50MS | FLAG_ASYNCHRONOUS200MS))>0) {
						try {Thread.sleep((flags & FLAG_ASYNCHRONOUS50MS)>0?50:200);} catch(Exception e) {
							endBgProcess();
							if(DEBUG) System.out.println("SwingWorkerExtended "+name+" -STOP- " +callingThread);
							return;
						}
					}
					
					bgPool.submit(new Runnable() {
						@Override
						public void run() {
							try {
								//In Background
								if(DEBUG) System.out.println("SwingWorkerExtended " + name + " -BG- " + (System.currentTimeMillis()-started) + "ms - " + callingThread);
								SwingWorkerExtended.this.doInBackground();
							} catch (final Throwable thrown) {		
								JExceptionDialog.showError(comp, thrown);
								
								if(DEBUG) System.out.println("SwingWorkerExtended " + name + " -DONE- " + (System.currentTimeMillis()-started) + "ms - " + callingThread);					
								return;
							} finally {
								endBgProcess();																				
							}
							
							if(sw.isInterrupted()) {
								endBgProcess();
								if(DEBUG) System.out.println("SwingWorkerExtended " + name + " -CANCEL- " + (System.currentTimeMillis()-started) + "ms - " + callingThread);					
								return;
							} 
							
							SwingUtilities.invokeLater(doneRunnable);
							if(DEBUG) System.out.println("SwingWorkerExtended "+name+" -DONE- " + (System.currentTimeMillis()-started) + "ms - " +callingThread);
						}
					});
					
				}			
			}; 
			sw.setDaemon(!callingThread.contains("main"));
			threadPool.submit(sw);

			

			//Interrupt threads with the same name if start was delayed 
			if(comp!=null && (flags & (FLAG_ASYNCHRONOUS50MS | FLAG_ASYNCHRONOUS200MS))>0) {				
				//ASynchronous, but wait 50ms and make sure nobody executed it during that delay
				if(currentThreads.get(comp)==null) currentThreads.put(comp, new HashMap<String, Thread>());
				Thread t2 = currentThreads.get(comp).get(title);
				if(t2!=null && !t2.isInterrupted()) {
					t2.interrupt();
				}	
				currentThreads.get(comp).put(title, sw);
				
			} 
						
			//Show the loading panel if the task takes more than 200m
			delayVisibleThread = new Thread("SwingWorkerExtended-GlassPane") {
				@Override
				public void run() {
					try{Thread.sleep(200);} catch(Exception e) {return;}
					startBgProcess(title, myComp, (flags & FLAG_CANCELABLE)>0);
				}
			};
			delayVisibleThread.setDaemon(true);
			threadPool.submit(delayVisibleThread);
		}
	}
	
	private ExecutorService threadPool = Executors.newCachedThreadPool();	
	private ExecutorService bgPool = Executors.newFixedThreadPool(1);	
	
	
	public void cancel() {
		sw.interrupt();
	}
	
	protected boolean isCancelled() {
		return sw.isInterrupted();
	}
	
	protected void doInBackground() throws Exception {}
	protected void done() {}
	
	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return "[SW:"+name+"]";
	}
	
}