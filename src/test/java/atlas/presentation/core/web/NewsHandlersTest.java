package atlas.presentation.core.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NewsHandlersTest {

    @Test
    void extractsAndDecodesTheIssueTitle() {
        var html = "<meta property=\"og:title\" content=\"AI &amp; Dev &#x27;today&#x27;\"/>";

        assertThat(NewsHandlers.extractTitle(html)).isEqualTo("AI & Dev 'today'");
    }

    @Test
    void extractsTheFirstIssueFromAnArchive() {
        var html = "<a href=\"/dev/2026-08-19\"><div class=\"issue\">Java &amp; AI</div></a>";

        assertThat(NewsHandlers.extractArchiveTitle("dev", html)).isEqualTo("Java & AI");
    }
}
