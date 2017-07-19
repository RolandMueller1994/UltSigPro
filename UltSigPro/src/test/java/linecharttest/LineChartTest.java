package linecharttest;

import static org.junit.Assert.*;

import java.util.Optional;

import org.junit.Test;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

public class LineChartTest {

	private int samplingFreq = 44100;
	private int freq = 100;
	private int timeDisplayed = 10; 
	
	@Test
	public void test() {
		LineChartDialog dialog = new LineChartDialog();
		Optional<ButtonType> result = dialog.showAndWait();
		
	}

	private class LineChartDialog extends Dialog<ButtonType> {
		
		public LineChartDialog() {
			getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
		}
		
		public void addData(int[] data) {
			
		}
	}
}
