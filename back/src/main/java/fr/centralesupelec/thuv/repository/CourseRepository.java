package fr.centralesupelec.thuv.repository;

import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.CourseStatus;
import fr.centralesupelec.thuv.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@Transactional
public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {
    List<Course> findByUserCoursesUserAndStatusInOrderById(User user, List<CourseStatus> status);
    Set<Course> findByCreator(User admin);
    Course getCourseByLink(String courseLink);

    @Query("""
        SELECT c FROM Course c
        WHERE 
            (LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) 
            OR LOWER(c.creator.name) LIKE LOWER(CONCAT('%', :search, '%')) 
            OR LOWER(c.creator.lastname) LIKE LOWER(CONCAT('%', :search, '%')))
            AND c.status IN :status
    """)
    Page<Course> searchCoursesByTitleOrCreatorName(
        @NotBlank @Param("search") String search,
        @Param("status") List<CourseStatus> status,
        Pageable pageable
    );

    @Transactional
    @Modifying
    @Query("delete from Course c where c.id=?1")
    void deleteQueryById(Long id);

    Optional<Course> findByUuid(UUID uuid);
}
