package fr.centralesupelec.thuv.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserRemoveCourseTest {

    private static Course course(long id) {
        Course course = new Course();
        course.setId(id);
        return course;
    }

    // Hibernate fills the set with rows whose user is already set, so their hash includes the user
    private static User userLoadedWith(Course... courses) {
        User user = new User();
        user.setId(1L);
        for (Course course : courses) {
            UserCourse userCourse = new UserCourse();
            userCourse.setCourse(course);
            userCourse.setUser(user);
            user.getUserCourses().add(userCourse);
        }
        return user;
    }

    @Test
    void removesTheMembershipOfALoadedUser() {
        Course course = course(4L);
        User user = userLoadedWith(course);

        user.removeCourse(course);

        assertThat(user.getUserCourses()).isEmpty();
    }

    @Test
    void keepsTheOtherMemberships() {
        Course removed = course(4L);
        Course kept = course(5L);
        User user = userLoadedWith(removed, kept);

        user.removeCourse(removed);

        assertThat(user.getUserCourses())
                .extracting(UserCourse::getCourse)
                .containsExactly(kept);
    }

    @Test
    void ignoresACourseTheUserDidNotJoin() {
        User user = userLoadedWith(course(4L));

        user.removeCourse(course(5L));

        assertThat(user.getUserCourses()).hasSize(1);
    }
}
