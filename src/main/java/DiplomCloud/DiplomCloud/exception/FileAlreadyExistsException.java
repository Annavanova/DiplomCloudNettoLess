package DiplomCloud.DiplomCloud.exception;

public class FileAlreadyExistsException extends RuntimeException {
    public FileAlreadyExistsException(String filename) {
        super("Файл '" + filename + "' уже существует");
    }

    public FileAlreadyExistsException(String filename, Throwable cause) {
        super("Файл '" + filename + "' уже существует", cause);
    }
}
