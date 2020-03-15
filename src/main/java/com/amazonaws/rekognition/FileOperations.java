package com.amazonaws.rekognition;

import java.io.*;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

public class FileOperations {
  private static int i = 1;

  public void saveFile(String fileName, String content) {

    try {
      File file = new File("output");

      file.mkdirs();

      file = new File("output" + File.separator + i++ + ". " + fileName + ".txt");

      if (!file.exists()) {
        file.createNewFile();
      }

      FileWriter fileWriter = new FileWriter(file.getAbsoluteFile());
      BufferedWriter bw = new BufferedWriter(fileWriter);
      bw.write(content);
      bw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public String readFile(String fileName) {
    StringBuilder content = new StringBuilder();
    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName))) {
      String line;

      while ((line = bufferedReader.readLine()) != null) {
        content.append(line).append("\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return content.toString();
  }

  public void writeS3ToFileSystem(S3Object o, String keyName) {
    S3ObjectInputStream s3is = o.getObjectContent();
    try {
      FileOutputStream fos = new FileOutputStream(new File(keyName));
      byte[] read_buf = new byte[1024];
      int read_len = 0;
      while ((read_len = s3is.read(read_buf)) > 0) {
        fos.write(read_buf, 0, read_len);
      }
      s3is.close();
      fos.close();
    } catch (FileNotFoundException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    } catch (IOException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
//    System.out.println("Done!");
  }
}
