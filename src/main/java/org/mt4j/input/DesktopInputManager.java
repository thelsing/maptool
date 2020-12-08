package org.mt4j.input;

import java.awt.*;
import java.awt.image.MemoryImageSource;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

//import org.mt4j.input.inputSources.KeyboardInputSource;
import org.mt4j.input.inputData.MTInputEvent;
import org.mt4j.input.inputProcessors.IGestureEventListener;
import org.mt4j.input.inputProcessors.MTGestureEvent;
import org.mt4j.input.inputProcessors.componentProcessors.dragProcessor.DragProcessor;
import org.mt4j.input.inputProcessors.componentProcessors.panProcessor.PanProcessorTwoFingers;
import org.mt4j.input.inputProcessors.componentProcessors.rotateProcessor.RotateProcessor;
import org.mt4j.input.inputProcessors.componentProcessors.tapProcessor.TapProcessor;
import org.mt4j.input.inputProcessors.componentProcessors.zoomProcessor.ZoomProcessor;
import org.mt4j.input.inputProcessors.globalProcessors.InputRetargeter;
import org.mt4j.input.inputSources.MouseInputSource;
import org.mt4j.input.inputSources.TuioInputSource;

import javax.swing.*;


/**
 * The Class DesktopInputManager.
 */
public class DesktopInputManager extends InputManager implements IMTInputEventListener, IGestureEventListener {
	private ComponentInputProcessorSupport inputProcessorSupport;
	private Map<Component, Set<IGestureEventListener>> componentToGestureListener;

	/** The Constant EMPTY. */
	private static final IGestureEventListener[] EMPTY = {};
	/**
	 * Instantiates a new desktop input manager.
	 *
	 * @param app the app
	 */
	public DesktopInputManager(JFrame app) {
		this(app, true);
	}
	
	/**
	 * Instantiates a new desktop input manager.
	 *
	 * @param app the app
	 * @param registerDefaultSources the register default sources
	 */
	public DesktopInputManager(JFrame app, boolean registerDefaultSources) {

		super(app, registerDefaultSources);

		componentToGestureListener = new HashMap<Component, Set<IGestureEventListener>>();

		inputProcessorSupport = new ComponentInputProcessorSupport(app, this);
		inputProcessorSupport.registerInputProcessor(new TapProcessor(app));
		inputProcessorSupport.registerInputProcessor(new DragProcessor(app));
		inputProcessorSupport.registerInputProcessor(new PanProcessorTwoFingers(app));
		inputProcessorSupport.registerInputProcessor(new ZoomProcessor(app));
		inputProcessorSupport.registerInputProcessor(new RotateProcessor(app));

		registerDefaultGlobalInputProcessors();

	}

	public synchronized void addGestureEvtListenerForComponent(Component component, IGestureEventListener listener) {
		if (listener == null) {
			return;
		}

		Set<IGestureEventListener> set = this.componentToGestureListener.get(component);
		if(set == null)
		{
			set = new HashSet<IGestureEventListener>();
			componentToGestureListener.put(component, set);
		}

		if(!set.contains(listener))
			set.add(listener);

	}


	/**
	 * Removes a IGestureEventListener to the listener map.
	 * Throws no error if the listener isnt found.
	 *
	 * @param component the Component to attach to
	 * @param listener the listener
	 */
	public synchronized void removeGestureEventListener(Component component, IGestureEventListener listener) {
		if (listener == null || component == null) {
			return;
		}
		if (this.componentToGestureListener == null)
			return;


		Set<IGestureEventListener> set = componentToGestureListener.get(component);
		if(set == null)
			return;

		set.remove(listener);
		if(set.isEmpty())
			componentToGestureListener.remove(component);
	}

	protected void registerDefaultGlobalInputProcessors(){
		InputRetargeter inputRetargeter = new InputRetargeter(new SwingHitTestInfoProvider(app));
		inputRetargeter.addProcessorListener(this);
		this.registerGlobalInputProcessor(inputRetargeter);

	}
	/* (non-Javadoc)
	 * @see org.mt4j.input.InputManager#registerDefaultInputSources()
	 */
	@Override
	/**
	 * Initialize default input sources.
	 */
	protected void registerDefaultInputSources(){
		super.registerDefaultInputSources();
		
		boolean enableMultiMouse = false;
		Properties properties = new Properties();

		try {
			FileInputStream fi = new FileInputStream("Settings.txt");
			properties.load(fi); 
			enableMultiMouse = Boolean.parseBoolean(properties.getProperty("MultiMiceEnabled", "false").trim());
		}catch (Exception e) {
			logger.debug("Failed to load Settings.txt from the File system. Trying to load it from classpath..");
			try {
				InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("Settings.txt");
				if (in != null){
					properties.load(in);
					enableMultiMouse = Boolean.parseBoolean(properties.getProperty("MultiMiceEnabled", "false").trim());
				}else{
					logger.debug("Couldnt load Settings.txt as a resource. Using defaults.");
				}
			} catch (IOException e1) {
				logger.error("Couldnt load Settings.txt. Using defaults.");
				e1.printStackTrace();
			}
		}
/*
		if (enableMultiMouse){
			try {
				//Register single or multiple mice input source
				int connectedMice = MultipleMiceInputSource.getConnectedMouseCount();
				//	    		/*
				logger.info("Found mice: " + connectedMice);
				if (connectedMice >= 2){ //FIXME should be > 1, but manymouse often detects more that arent there!?
					logger.info("-> Multiple Mice detected!");
					MultipleMiceInputSource multipleMice = new MultipleMiceInputSource(app);
	    			multipleMice.setMTApp(app);
	    			this.registerInputSource(multipleMice);
	    			this.hideCursorInFrame();
	    		}else{
//
	    			MouseInputSource mouseInput = new MouseInputSource(app);
	    			this.registerInputSource(mouseInput);
	    		}
//
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    		//Use default mouse input source
	    		MouseInputSource mouseInput = new MouseInputSource(app);
	    		this.registerInputSource(mouseInput);
	    	}
	    }*/
//	    else{
	    	MouseInputSource mouseInput = new MouseInputSource(app);
	    	this.registerInputSource(mouseInput);
//	    }
//	    */
		TuioInputSource tuioInput = new TuioInputSource(app);
		this.registerInputSource(tuioInput);
	    //Check if we run windows 7
/*	    if (System.getProperty("os.name").toLowerCase().contains("windows")){
	    	Win7NativeTouchSource win7NativeInput = new Win7NativeTouchSource(app);
	    	if (win7NativeInput.isSuccessfullySetup()){
	    		this.registerInputSource(win7NativeInput);
	    	}
	    }*/
	    
	    //check which versions it supports and only start there!
	    /*
	    if (System.getProperty("os.name").toLowerCase().contains("mac os x")){
	    	this.registerInputSource(new MacTrackpadSource(app));
	    }
	    */

	    //Register keyboard multitouch-emulation input source
	    //KeyboardInputSource keyInput = new KeyboardInputSource(app);
		//this.registerInputSource(keyInput);
		
//		MuitoInputSource muitoInput = new MuitoInputSource(pa, "localhost", 6666);


		//Register TUIO protocol input sources
	/*	if (app instanceof MTApplication) {
			MTApplication desktopApp = (MTApplication) app;
			this.registerInputSource(new Tuio2DCursorInputSource(desktopApp));
			this.registerInputSource(new Tuio2dObjectInputSource(desktopApp));
		}*/
	}



	/**
	 * Hides the mousecursor in multiple mice mode.
	 */
	protected void hideCursorInFrame(){
		int[] pixels = new int[16 * 16];
		Image image = Toolkit.getDefaultToolkit().createImage(
				new MemoryImageSource(16, 16, pixels, 0, 16));
		Cursor transparentCursor =
			Toolkit.getDefaultToolkit().createCustomCursor
			(image, new Point(0, 0), "invisibleCursor");
		//MapTool.getFrame().setCursor(transparentCursor);
	}


	/**
	 * Fires the events to the listeners.
	 *
	 * @param listeners the listeners
	 * @param event the event
	 */
	private void fire(Set<IGestureEventListener> listeners, MTGestureEvent event) {
		if (listeners != null) {
			for (IGestureEventListener listener : listeners) {
				try {
					listener.processGestureEvent(event);
				} catch (Exception e) {
					logger.error("Exception in GestureEventhandler", e);
				}
			}
		}
	}

	@Override
	public boolean processInputEvent(MTInputEvent inEvt) {
		if(inEvt.hasTarget() && componentToGestureListener.get(inEvt.getTarget()) != null)
			inEvt.setEventPhase(MTInputEvent.AT_TARGET);
		return inputProcessorSupport.processInputEvent(inEvt);
	}

	@Override
	public boolean processGestureEvent(MTGestureEvent ge) {

		// TODO: respect the bubbling, stop propergating properties of the event, the return value of the handler etc.
		// Otherwise: Remove them
		Component component = ge.getTarget();
		while(component != null) {
			Set<IGestureEventListener> listeners = componentToGestureListener.get(component);
			fire(listeners, ge);

			component = component.getParent();
		}
		return true;
	}
}
