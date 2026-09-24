package atlas.presentation.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ClasspathAssetsTest {

    @Test
    void mapsAMountedPathOntoItsClasspathResource() {
        assertThat(ClasspathAssets.resolve("/vendor", "/web-vendor", "/vendor/human/models/faceres.bin"))
            .contains("/web-vendor/human/models/faceres.bin");
    }

    @Test
    void refusesToEscapeTheResourceRoot() {
        assertThat(ClasspathAssets.resolve("/vendor", "/web-vendor", "/vendor/../logging.properties"))
            .isEmpty();
        assertThat(ClasspathAssets.resolve("/vendor", "/web-vendor", "/vendor/a/../../b")).isEmpty();
    }

    @Test
    void refusesAPathOutsideTheMountAndAnEmptyOne() {
        assertThat(ClasspathAssets.resolve("/vendor", "/web-vendor", "/assets/app.js")).isEmpty();
        assertThat(ClasspathAssets.resolve("/vendor", "/web-vendor", "/vendor")).isEmpty();
    }

    @Test
    void refusesCharactersThatAreNotPartOfAFileName() {
        assertThat(ClasspathAssets.resolve("/vendor", "/web-vendor", "/vendor/human/mo dels.bin"))
            .isEmpty();
        assertThat(ClasspathAssets.resolve("/vendor", "/web-vendor", "/vendor/human/%2e%2e/x"))
            .isEmpty();
    }

    @Test
    void namesTheContentTypeThatEachRuntimeNeeds() {
        assertThat(ClasspathAssets.contentTypeOf("/a/vision.wasm")).isEqualTo("application/wasm");
        assertThat(ClasspathAssets.contentTypeOf("/a/bundle.mjs")).startsWith("text/javascript");
        assertThat(ClasspathAssets.contentTypeOf("/a/model.json")).startsWith("application/json");
        assertThat(ClasspathAssets.contentTypeOf("/a/faceres.bin")).isEqualTo("application/octet-stream");
        assertThat(ClasspathAssets.contentTypeOf("/a/gesture_recognizer.task"))
            .isEqualTo("application/octet-stream");
        assertThat(ClasspathAssets.contentTypeOf("/a/noextension")).isEqualTo("application/octet-stream");
    }
}
