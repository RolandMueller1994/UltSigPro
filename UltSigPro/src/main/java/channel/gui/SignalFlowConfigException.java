package channel.gui;

import java.util.Collection;

public class SignalFlowConfigException extends Exception {

	private int errorCode = 0;
	
	public SignalFlowConfigException(String message, Collection<SignalFlowErrorCode> errorCodes) {
		super(message);
		
		for(SignalFlowErrorCode signalFlowErrorCode : errorCodes) {
			errorCode ^= signalFlowErrorCode.getValue();
		}
	}
	
	public int getErrorCode() {
		return errorCode;
	}
	
	public static class SignalFlowErrorCode {
		
		public static final SignalFlowErrorCode INPUT_ERROR = new SignalFlowErrorCode(0x1);
		public static final SignalFlowErrorCode OUTPUT_ERROR = new SignalFlowErrorCode(0x2);
		public static final SignalFlowErrorCode CONNECTION_ERROR = new SignalFlowErrorCode(0x4);		
		
		private int value;
		
		private SignalFlowErrorCode(int value) {
			this.value = value;
		}
		
		public int getValue() {
			return value;
		}
	}
}
