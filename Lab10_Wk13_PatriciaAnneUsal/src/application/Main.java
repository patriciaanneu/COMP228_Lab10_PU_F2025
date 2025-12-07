package application;
	
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.scene.control.*;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Set;


public class Main extends Application {
	
	//database connection
	private static final String URL = "jdbc:mysql://localhost:3306/lab10_wk13";
	private static final String USER = "root";
	private static String PASSWORD = "November2025";
	
	private TextArea outputArea;
	private TextField minField, maxField, countField;
	
	@Override
	public void start(Stage stage) {
		
		Label minLabel = new Label("Min: ");
		minField = new TextField("1");
		
		Label maxLabel = new Label("Max: ");
		maxField = new TextField("100");
		
		Label countLabel = new Label("How many numbers to generate: ");
		countField = new TextField("6");
		
		Button runButton = new Button("Generate Lotto Numbers");
		runButton.setOnAction(e -> startLottoRuns());
		
		outputArea = new TextArea();
		outputArea.setPrefHeight(300);
		outputArea.setWrapText(true);
		
		VBox layout = new VBox(
				minLabel, minField,
				maxLabel, maxField,
				countLabel, countField,
				runButton,
				outputArea
			);		
		
		layout.setStyle("-fx-padding: 20; -fx-font-size: 14;");
		
		Scene scene = new Scene(layout, 450, 550);
		
		stage.setTitle("Quick Pick Lotto");
		stage.setScene(scene);
		stage.show();
		
	}
	
	private void startLottoRuns() {
		
		outputArea.clear();
		
		int min = Integer.parseInt(minField.getText());
		int max = Integer.parseInt(maxField.getText());
		int count = Integer.parseInt(countField.getText());
		
		if (min < 1 || max > 100 || min >= max || count < 1) {
			outputArea.appendText("Invalid input. Please enter valid values.\n");
			return;
		}
		
		for (int i = 1; i <= 5; i++) {
			LottoTask task = new LottoTask("Run " + i, min, max, count);
			new Thread(task).start();
		}
	}
	
	private class LottoTask implements Runnable {
		
		private final String taskName;
		private final int min, max, count;
		private final int sleepTime;
		private final SecureRandom generator = new SecureRandom();
		
		public LottoTask(String name, int min, int max, int count) {
			this.taskName = name;
			this.min = min;
			this.max = max;
			this.count = count;
			this.sleepTime = generator.nextInt(5000);
		}
		
		@Override
		public void run() {
			
			try {
				Thread.sleep(sleepTime);
			} catch (Exception e) {
				showError("An error occured: " + e.getMessage());
			}
			
			Set<Integer> numbers = generateUniqueNumbers();
			
			Hashtable<String, Object> table = new Hashtable<>();
			table.put("run", taskName);
			table.put("numbers", numbers);
			
			saveToDatabase(taskName, numbers);
			
			Platform.runLater(() -> {
				outputArea.appendText(taskName + " - " + numbers + "\n");
			});
		}
		
		private Set<Integer> generateUniqueNumbers() {
			Set<Integer> nums = new LinkedHashSet<>();
			
			while (nums.size() < count) {
				nums.add(generator.nextInt(max - min + 1) + min);
			}
			
			return nums;
		}
		
		private void saveToDatabase(String taskName, Set<Integer> nums) {
			
			String sql = "INSERT INTO lab10_wk13_results(run_number, numbers) VALUES(?, ?)";
			
			try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
					PreparedStatement ps = conn.prepareStatement(sql)) {
				int runNum = Integer.parseInt(taskName.replace("Run ", ""));
				
				ps.setInt(1,  runNum);
				ps.setString(2, nums.toString());
				ps.executeUpdate();
			} catch (Exception e) {
				showError("Error during: " + taskName + ": " + e.getMessage());
			}
		}
		
		private void showError(String message) {
			Platform.runLater(() -> {
				outputArea.appendText(" " + message + "\n");
			});
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
