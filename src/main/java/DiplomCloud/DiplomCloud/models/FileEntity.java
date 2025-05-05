package DiplomCloud.DiplomCloud.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "files")
@Getter
@Setter
public class FileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private Long size;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

}
