package gui.guicomponents;

import java.util.HashSet;

import javafx.event.EventHandler;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

public class ValueKnob extends Pane {
	
	private static final double MIN_ROTATION = -95;
	private static final double MAX_ROTATION = -95 + 270;

	private static final double MIN_ANGLE = -135;
	private static final double MAX_ANGLE = 135;
	
	private ImageView outterView;
	private ImageView innerView;
	
	private double value;
	
	private HashSet<ValueKnobListener> changeListeners = new HashSet<>();
	
	public ValueKnob() {
		setMaxSize(50, 50);
		
		outterView = new ImageView("file:icons/gainIconOutter.png");
		innerView = new ImageView("file:icons/gainIconInner.png");
		
		getChildren().addAll(outterView, innerView);
		
		innerView.setLayoutY(2);
		innerView.setRotate(MIN_ROTATION);
		
		setOnMouseDragged(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				
				double diffY = -(event.getY() - 25);
				double diffX = event.getX() - 25;
				
				double angle = Math.atan2(diffX, diffY) * 180/Math.PI;
				
				if(angle < MIN_ANGLE) {
					angle = MIN_ANGLE;
				} else if(angle > MAX_ANGLE) {
					angle = MAX_ANGLE;
				}
				
				value = 100 * (angle + 135) / 270;
				
				for(ValueKnobListener listener : changeListeners) {
					listener.valueChanged(value);
				}
				
				innerView.setRotate(MIN_ROTATION + angle + 135);
				event.consume();
			}
			
		});
		
	}
	
	public void registerValueKnobListener(ValueKnobListener listener) {
		changeListeners.add(listener);
	}
	
	public void setValue(double value) {
		this.value = value;
		
		double angle = (value / 100) * 270 + MIN_ROTATION;
		
		innerView.setRotate(angle);
	}
	
	public interface ValueKnobListener {
		
		void valueChanged(double value);
		
	}
	
}
