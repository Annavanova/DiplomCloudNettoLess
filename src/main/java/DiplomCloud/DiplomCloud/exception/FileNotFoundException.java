package DiplomCloud.DiplomCloud.exception;

public class FileNotFoundException extends RuntimeException {
    public FileNotFoundException(String filename) {
        super("Файл не найден: " + filename);
    }
}
