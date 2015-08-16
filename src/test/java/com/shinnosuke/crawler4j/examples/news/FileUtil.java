package com.shinnosuke.crawler4j.examples.news;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {

	private static final Logger logger = LoggerFactory
			.getLogger(FileUtil.class);

	public static <T> void write(String filePath, T t) {
		File file = new File(filePath);
		if (!file.exists()) {
			if (!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			try {
				file.createNewFile();
			} catch (IOException e) {
				logger.error("cannot create result file ", e);
			}
		}
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new OutputStreamWriter(
					new FileOutputStream(file, true), "UTF-8"));
			writer.println(t.toString());
			writer.flush();
		} catch (IOException e) {
			logger.error("write data to file failed ", e);
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}
}
