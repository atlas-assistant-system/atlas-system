package atlas.presentation.home.web;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.home.enums.NewsCategory;
import atlas.presentation.sharedkernel.http.HttpRequest;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class NewsHandlersTest {

    @Test
    void extractsAndDecodesTheIssueTitle() {
        var html = "<meta property=\"og:title\" content=\"AI &amp; Dev &#x27;today&#x27;\"/>";

        assertThat(NewsHandlers.extractTitle(html)).isEqualTo("AI & Dev 'today'");
    }

    @Test
    void extractsEveryIssueFromAnArchiveNewestFirst() {
        var html = "<a href=\"/dev/2026-08-19\"><div class=\"issue\">Java &amp; AI</div></a>"
            + "<a href=\"/dev/2026-08-18\"><div class=\"issue\">Rust <b>1.9</b></div></a>";

        var issues = NewsHandlers.archiveIssues(NewsCategory.DEVELOPMENT, html);

        assertThat(issues).extracting(NewsItem::title).containsExactly("Java & AI", "Rust 1.9");
        assertThat(issues).extracting(NewsItem::publishedAt)
            .containsExactly("2026-08-19", "2026-08-18");
        assertThat(issues.getFirst().url()).isEqualTo("https://tldr.tech/dev/2026-08-19");
    }

    @Test
    void ignoresAnArchiveEntryWithoutATitle() {
        var html = "<a href=\"/dev/2026-08-19\"><div class=\"issue\"></div></a>"
            + "<a href=\"/dev/2026-08-18\"><div class=\"issue\">Rust 1.9</div></a>";

        assertThat(NewsHandlers.archiveIssues(NewsCategory.DEVELOPMENT, html))
            .extracting(NewsItem::title).containsExactly("Rust 1.9");
    }

    /**
     * Antes el handler se traia las ocho fuentes en fila cuando la cache caducaba, asi que una
     * peticion de cada media hora se quedaba colgada hasta un minuto. Ahora nunca sale a la red:
     * si todavia no ha refrescado, contesta vacio y al momento.
     */
    @Test
    void answersFromTheCacheWithoutReachingTheNetwork() {
        try (var news = new NewsHandlers()) {
            var started = System.nanoTime();
            var response = news.latest(HttpRequest.of("GET", "/news"));
            var elapsed = Duration.ofNanos(System.nanoTime() - started);

            assertThat(response.status()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("[]");
            assertThat(elapsed).isLessThan(Duration.ofSeconds(1));
        }
    }

    @Test
    void parsesTheRequestedCategories() {
        assertThat(Values.categories("AI,development"))
            .containsExactlyInAnyOrder(NewsCategory.AI, NewsCategory.DEVELOPMENT);
    }
}
