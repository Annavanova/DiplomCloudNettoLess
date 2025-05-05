package DiplomCloud.DiplomCloud.dto;

public class ErrorResponse {
    private String message;
    private int id;

    public ErrorResponse(String message, int i) {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
