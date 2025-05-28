/*
 * Copyright © 2025 Piotr P. Karwasz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.copernik.tea.internal;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.packageurl.PackageURL;
import eu.copernik.tea.model.Collection;
import eu.copernik.tea.model.Component;
import eu.copernik.tea.model.Identifier;
import eu.copernik.tea.model.IdentifierType;
import eu.copernik.tea.model.Product;
import eu.copernik.tea.model.Release;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DefaultTeaRepositoryManagerTest {

    @TempDir
    private Path tempDir;

    private DefaultTeaRepositoryManager manager;

    @BeforeEach
    void setUp() {
        manager = new DefaultTeaRepositoryManager(tempDir);
    }

    @Test
    void getProduct_returnsNullForNonExistentProduct() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        assertThat(manager.getProduct(nonExistentId)).isNull();
    }

    @Test
    void getProduct_findsExistingProject() throws Exception {
        UUID existentId = UUID.randomUUID();
        Product product = new Product().uuid(existentId).name("Test Product");
        manager.saveProduct(product);

        Product foundProduct = manager.getProduct(existentId);
        assertThat(foundProduct).isNotNull().isEqualTo(product);
    }

    @Test
    void saveProduct_savesAndRetrievesProduct() throws Exception {
        Product product = new Product().uuid(UUID.randomUUID()).name("Test Product");
        manager.saveProduct(product);

        Product retrieved = manager.getProduct(product.getUuid());
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getName()).isEqualTo("Test Product");

        Path productsFile = tempDir.resolve("products.json");
        ProductPaginationDetails products = manager.loadModelOrThrow(ProductPaginationDetails.class, productsFile);
        assertThat(products.getResults()).isNotEmpty().containsExactly(product);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void getOrCreateComponentByPurl_createsAndFindsComponent(boolean hasOtherComponents) throws Exception {
        if (hasOtherComponents) {
            // Create a dummy component to ensure the manager has some data
            PackageURL dummyPurl = new PackageURL("pkg:maven/org.example/dummy@1.0.0");
            Component dummyComponent = manager.getOrCreateComponentByPurl(dummyPurl);
            dummyComponent.name("Dummy Component");
            dummyComponent.addIdentifiersItem(
                    new Identifier().idType(IdentifierType.CPE).idValue("cpe:/a:example:dummy"));
            manager.saveComponent(dummyComponent);
        }
        PackageURL purl = new PackageURL("pkg:maven/org.example/foo@1.0.0");
        Component created = manager.getOrCreateComponentByPurl(purl);
        assertThat(created).isNotNull();
        assertThat(created.getIdentifiers())
                .hasSize(1)
                .allMatch(id ->
                        id.getIdType() == IdentifierType.PURL && purl.toString().equals(id.getIdValue()));
        // Add required fields
        created.name("Test Component Foo");
        manager.saveComponent(created);

        Component found = manager.getOrCreateComponentByPurl(purl);
        assertThat(found.getUuid()).isEqualTo(created.getUuid());
    }

    @Test
    void findComponentByPurl_returnsNullForNonExistentPurl() throws Exception {
        PackageURL purl = new PackageURL("pkg:maven/org.example/nonexistent@1.0.0");
        assertThat(manager.getOrCreateComponentByPurl(purl)).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void getOrCreateRelease_createsAndFindsRelease(boolean create) throws Exception {
        PackageURL purl = new PackageURL("pkg:maven/org.example/bar@2.0.0");
        Component component = manager.getOrCreateComponentByPurl(purl);
        // Add required fields to the component
        component.name("Test Component Bar");
        manager.saveComponent(component);

        if (create) {
            Release found = manager.getOrCreateRelease(component, "2.0.0");
            assertThat(found).isNotNull();
            assertThat(found.getUuid()).isNotNull();
        } else {
            Release created = manager.getOrCreateRelease(component, "2.0.0");
            assertThat(created).isNotNull();
            assertThat(created.getVersion()).isEqualTo("2.0.0");
            // Add required fields to the release
            created.releaseDate(OffsetDateTime.now(UTC));
            manager.saveRelease(component, created);

            Release found = manager.getOrCreateRelease(component, "2.0.0");
            assertThat(found.getUuid()).isEqualTo(created.getUuid());
        }
    }

    @Test
    void saveRelease_savesAndRetrievesRelease() throws Exception {
        PackageURL purl = new PackageURL("pkg:maven/org.example/baz");
        Component component = manager.getOrCreateComponentByPurl(purl);
        // Add required fields to the component
        component.name("Test Component Baz");
        manager.saveComponent(component);

        Release release = new Release().uuid(UUID.randomUUID()).version("3.0.0").releaseDate(OffsetDateTime.now(UTC));
        manager.saveRelease(component, release);

        Release retrieved = manager.getOrCreateRelease(component, "3.0.0");
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getVersion()).isEqualTo("3.0.0");

        // Retrieve all releases for the component
        Path releasesFile = tempDir.resolve("component/" + component.getUuid() + "/releases.json");
        List<Release> releases = manager.loadModelList(new TypeReference<>() {}, releasesFile);
        assertThat(releases).isNotEmpty().containsExactly(release);
    }

    @Test
    void getLatestCollectionByRelease_returnsLatest() throws Exception {
        PackageURL purl = new PackageURL("pkg:maven/org.example/qux");
        Component component = manager.getOrCreateComponentByPurl(purl);
        // Add required fields to the component
        component.name("Test Component Qux");
        manager.saveComponent(component);
        Release release = manager.getOrCreateRelease(component, "4.0.0");
        // Add required fields to the release
        release.releaseDate(OffsetDateTime.now(UTC));
        manager.saveRelease(component, release);

        // Test with no collections
        @Nullable Collection latestCollection = manager.getLatestCollection(release);
        assertThat(latestCollection).isNull();

        // Test with multiple collections
        Collection collection;
        for (int version = 1; version <= 10; version++) {
            collection = new Collection().uuid(release.getUuid()).version(version);
            collection.setUuid(release.getUuid());
            collection.setVersion(version);
            manager.saveCollection(collection);
        }

        latestCollection = manager.getLatestCollection(release);
        assertThat(latestCollection).isNotNull();
        assertThat(latestCollection.getVersion()).isEqualTo(10);
    }

    @Test
    void saveCollection_savesAndRetrievesCollection() throws Exception {
        PackageURL purl = new PackageURL("pkg:maven/org.example/quux");
        Component component = manager.getOrCreateComponentByPurl(purl);
        // Add required fields to the component
        component.name("Test Component Quux");
        manager.saveComponent(component);
        Release release = manager.getOrCreateRelease(component, "5.0.0");
        // Add required fields to the release
        release.releaseDate(OffsetDateTime.now(UTC));
        manager.saveRelease(component, release);

        Path collectionsFile = tempDir.resolve("release/" + release.getUuid() + "/collections.json");
        for (int version = 1; version <= 10; version++) {
            Collection collection = new Collection().uuid(release.getUuid()).version(version);
            manager.saveCollection(collection);

            Collection retrieved = manager.getCollection(release, version);
            assertThat(retrieved).isNotNull().isEqualTo(collection);
            Collection latest = manager.getLatestCollection(release);
            assertThat(latest).isNotNull().isEqualTo(collection);
            List<Collection> collections = manager.loadModelList(new TypeReference<>() {}, collectionsFile);
            assertThat(collections).isNotEmpty().contains(collection);
        }
    }
}
