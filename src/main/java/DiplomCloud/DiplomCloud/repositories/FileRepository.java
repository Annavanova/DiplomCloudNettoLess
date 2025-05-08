package DiplomCloud.DiplomCloud.repositories;

import DiplomCloud.DiplomCloud.models.FileEntity;
import DiplomCloud.DiplomCloud.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
    // Автоматически реализуется Spring Data JPA
    boolean existsByIdAndFilename(Long id, String filename);

    List<FileEntity> findByOwner(User owner);

    Optional<FileEntity> findByOwnerAndFilename(User owner, String filename);

    void deleteByOwnerAndFilename(User owner, String filename);
}