package utilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReportGenerator {

	private BufferedWriter bufferedWriter;
	private FileWriter fileWriter;

	public ReportGenerator(String filepath, boolean append, String ormSchemaName) throws IOException {
		File file = new File(filepath);
		if (!file.exists()) {
			file.createNewFile();
		}
		fileWriter = new FileWriter(file);
		bufferedWriter = new BufferedWriter(fileWriter);
		try {
			DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY);
			Date date = new Date();
			bufferedWriter.write(
					ormSchemaName + "\n" + dateFormat.format(date) + "\n ----------------------------------\n\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeLine(String text) {
		try {
			bufferedWriter.write(text + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void write(String text) {
		try {
			bufferedWriter.write(text);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void finish() throws IOException {
		bufferedWriter.close();
		fileWriter.close();
	}

}
