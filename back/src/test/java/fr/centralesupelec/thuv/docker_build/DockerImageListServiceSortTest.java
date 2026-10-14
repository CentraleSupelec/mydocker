package fr.centralesupelec.thuv.docker_build;

import fr.centralesupelec.thuv.docker_build.repository.DockerImageRepository;
import fr.centralesupelec.thuv.docker_build.service.DockerImageListService;
import fr.centralesupelec.thuv.model.User;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * lastBuildOn is not an attribute of DockerImage: Spring Data would throw
 * PropertyReferenceException if it reached the repository as a sort property. The SQL itself
 * (order, never-built last, one row per image) needs PostgreSQL and is proven by the recette.
 */
class DockerImageListServiceSortTest {
    private static final Pageable BY_LAST_BUILD_ON =
            PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "lastBuildOn"));

    private final User user = new User();

    @Test
    void lastBuildOnSortNeverReachesSpringDataAsAProperty() {
        forEachBranch(BY_LAST_BUILD_ON, (branch, repository) -> pageablesReceived(repository).forEach(pageable ->
                assertFalse(
                        pageable.getSort().stream().anyMatch(order -> order.getProperty().equals("lastBuildOn")),
                        branch
                )
        ));
    }

    @Test
    void lastBuildOnSortKeepsThePage() {
        forEachBranch(BY_LAST_BUILD_ON, (branch, repository) -> {
            List<Pageable> pageables = pageablesReceived(repository);
            assertFalse(pageables.isEmpty(), branch);
            pageables.forEach(pageable -> {
                assertEquals(0, pageable.getPageNumber(), branch);
                assertEquals(50, pageable.getPageSize(), branch);
            });
        });
    }

    @Test
    void lastBuildOnSortReachesTheRepository() {
        forEachBranch(BY_LAST_BUILD_ON, (branch, repository) ->
                assertEquals(1, Mockito.mockingDetails(repository).getInvocations().size(), branch)
        );
    }

    @Test
    void nameSortUnchanged() {
        Pageable byName = PageRequest.of(0, 50, Sort.by(Sort.Direction.ASC, "name"));
        forEachBranch(byName, (branch, repository) ->
                assertEquals(List.of(byName), pageablesReceived(repository), branch)
        );
    }

    @Test
    void unsortedUnchanged() {
        Pageable unsorted = PageRequest.of(0, 10);
        forEachBranch(unsorted, (branch, repository) ->
                assertEquals(List.of(unsorted), pageablesReceived(repository), branch)
        );
    }

    /**
     * Runs the request on each of the four branches, each with a fresh repository whose Page
     * methods all answer an empty page, then hands that repository to the check.
     */
    private void forEachBranch(Pageable request, BiConsumer<String, DockerImageRepository> check) {
        for (boolean hasAdminAuthority : new boolean[]{true, false}) {
            for (String search : new String[]{null, "img"}) {
                String branch = "admin=" + hasAdminAuthority + ", search=" + search;
                DockerImageRepository repository = mock(
                        DockerImageRepository.class,
                        withSettings().defaultAnswer(invocation ->
                                Page.class.isAssignableFrom(invocation.getMethod().getReturnType())
                                        ? Page.empty()
                                        : Mockito.RETURNS_DEFAULTS.answer(invocation)
                        )
                );
                DockerImageListService service = new DockerImageListService(repository);

                assertDoesNotThrow(
                        () -> service.getViewableDockerImage(user, hasAdminAuthority, search, request),
                        branch
                );
                check.accept(branch, repository);
            }
        }
    }

    private static List<Pageable> pageablesReceived(DockerImageRepository repository) {
        List<Pageable> pageables = new ArrayList<>();
        for (Invocation invocation : Mockito.mockingDetails(repository).getInvocations()) {
            for (Object argument : invocation.getArguments()) {
                if (argument instanceof Pageable pageable) {
                    pageables.add(pageable);
                }
            }
        }
        return pageables;
    }
}
