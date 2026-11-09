package org.fuin.porelo.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Fails on purpose, so the Build is seen to go red; removed in the next commit. */
class RedBuildTest {

    @Test
    void theBuildIsSeenToFail() {

        assertThat(1 + 1).as("a sum made to fail the Build on purpose").isEqualTo(3);
    }
}
