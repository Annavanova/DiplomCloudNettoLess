package DiplomCloud.DiplomCloud.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileInfoResponse {
    private String filename;
    private Long size;

    public FileInfoResponse(String filename, Long size) {
        this.filename = filename;
        this.size = size;
    }
}
