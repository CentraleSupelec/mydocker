package fr.centralesupelec.thuv.security.controllers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class CasSimpleNameTest {

    private static void assertSplit(String simpleName, String expectedName, String expectedLastName) {
        assertArrayEquals(
                new String[]{expectedName, expectedLastName},
                CasController.splitSimpleName(simpleName)
        );
    }

    @Test
    void firstAndLastName() {
        assertSplit("Alice Martin", "Alice", "Martin");
    }

    @Test
    void leadingSpaceKeepsTheFirstName() {
        assertSplit(" Alice Martin", "Alice", "Martin");
    }

    @Test
    void doubleSpaceLeavesNoLeadingSpaceInTheLastName() {
        assertSplit("Alice  Martin", "Alice", "Martin");
    }

    @Test
    void trailingSpaceIsDropped() {
        assertSplit("Alice Martin ", "Alice", "Martin");
    }

    @Test
    void severalWordsGoToTheLastName() {
        assertSplit("Jean Paul Dupont", "Jean", "Paul Dupont");
    }

    @Test
    void severalWordsBadlySpacedAreJoinedByOneSpace() {
        assertSplit(" Jean   Paul  Dupont ", "Jean", "Paul Dupont");
    }

    @Test
    void oneWordGivesNoLastName() {
        assertSplit("Alice", "Alice", null);
    }

    @Test
    void emptyGivesNoNames() {
        assertSplit("", null, null);
    }

    @Test
    void spacesOnlyGiveNoNames() {
        assertSplit("   ", null, null);
    }

    @Test
    void nullGivesNoNames() {
        assertSplit(null, null, null);
    }
}
