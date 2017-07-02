package channel;

import java.util.Collection;

import gui.USPGui;
import i18n.LanguageResourceHandler;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TitledPane;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import resourceframework.ResourceProviderException;

public class ChannelPane extends TitledPane {

	private LanguageResourceHandler lanHandler = LanguageResourceHandler.getInstance();
	
	private BorderPane centralPane = new BorderPane();
	private ChannelConfig config;
	
	private boolean play;
	private InputPane inputPane;
	private OutputPane outputPane;
	private WaveFormPane waveFormPane;
	private String name;
	
	private ContextMenu contextMenu;
	
	public ChannelPane(ChannelConfig config) throws ResourceProviderException {
		super.setText(config.getName());
		super.setContent(centralPane);
		name = config.getName();
		inputPane = new InputPane(config.getInputDevices());
		outputPane = new OutputPane();
		waveFormPane = new WaveFormPane();
		
		centralPane.setLeft(inputPane);
		centralPane.setRight(outputPane);
		centralPane.setCenter(waveFormPane);
		
		centralPane.setPrefWidth(Double.MAX_VALUE);
		
		MenuItem deleteMenuItem = new MenuItem(lanHandler.getLocalizedText("delete"));
		
		deleteMenuItem.setOnAction(new EventHandler<ActionEvent> () {

			@Override
			public void handle(ActionEvent event) {
				deleteThisChannel();
			}
			
		});
		
		contextMenu = new ContextMenu();
		contextMenu.getItems().add(deleteMenuItem);
		
		super.setContextMenu(contextMenu);
		
		setPlay(false);
	}
	
	public String getName() {
		return name;
	}
	
	public void setPlay(boolean play) {
		this.play = play;
		inputPane.setEditable(!play);
		outputPane.setEditable(!play);
	}
	
	private void deleteThisChannel() {
		USPGui.deleteChannel(this);
	}
	
	private class InputPane extends Pane {
		
		private ListView<String> table;
		private Button addButton;
		private Button removeButton;	
		
		public InputPane(Collection<String> inputDevices) {
			addButton = new Button("+");
			addButton.setMaxWidth(Double.MAX_VALUE);
			removeButton = new Button("-");
			removeButton.setMaxWidth(Double.MAX_VALUE);
			table = new ListView<> ();
			table.setPrefSize(200, 100);
			
			for(String cur : inputDevices) {
				table.getItems().add(cur);
			}
			
			GridPane gridPane = new GridPane();
			gridPane.setPadding(new Insets(5));
			gridPane.setHgap(5);
			gridPane.setVgap(5);
			
			gridPane.add(addButton, 0, 0);
			gridPane.add(removeButton, 1, 0);
			gridPane.add(table, 0, 1, 2, 1);
			
			GridPane.setHgrow(addButton, Priority.ALWAYS);
			GridPane.setHgrow(removeButton, Priority.ALWAYS);
			GridPane.setVgrow(table, Priority.ALWAYS);
			
			ColumnConstraints cc = new ColumnConstraints();
			cc.setPercentWidth(50);
			
			gridPane.getColumnConstraints().addAll(cc, cc);
			
			super.getChildren().add(gridPane);
		}
		
		private void setEditable(boolean b) {
			addButton.setDisable(!b);
			removeButton.setDisable(!b);
		}
		
	}
	
	private class OutputPane extends Pane {
		
		private ListView<String> table;
		private Button addButton;
		private Button removeButton;	
		
		public OutputPane() {
			addButton = new Button("+");
			addButton.setMaxWidth(Double.MAX_VALUE);
			removeButton = new Button("-");
			removeButton.setMaxWidth(Double.MAX_VALUE);
			table = new ListView<> ();
			table.setPrefSize(200, 100);
			
			GridPane gridPane = new GridPane();
			gridPane.setPadding(new Insets(5));
			gridPane.setHgap(5);
			gridPane.setVgap(5);
			
			gridPane.add(addButton, 0, 0);
			gridPane.add(removeButton, 1, 0);
			gridPane.add(table, 0, 1, 2, 1);
			
			GridPane.setHgrow(addButton, Priority.ALWAYS);
			GridPane.setHgrow(removeButton, Priority.ALWAYS);
			GridPane.setVgrow(table, Priority.ALWAYS);
			
			ColumnConstraints cc = new ColumnConstraints();
			cc.setPercentWidth(50);
			
			gridPane.getColumnConstraints().addAll(cc, cc);
			
			super.getChildren().add(gridPane);
		}
		
		private void setEditable(boolean b) {
			addButton.setDisable(!b);
			removeButton.setDisable(!b);
		}
	}
	
	private class WaveFormPane extends Pane {
		
		public WaveFormPane () {
			setBackground(new Background(new BackgroundFill(javafx.scene.paint.Color.DARKGRAY, null, new Insets(5))));
			setPadding(new Insets(5));
		}
		
	}
	
}
