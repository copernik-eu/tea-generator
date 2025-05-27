/*
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

import com.github.packageurl.PackageURL;
import eu.copernik.tea.model.Collection;
import eu.copernik.tea.model.Component;
import eu.copernik.tea.model.IdentifierType;
import eu.copernik.tea.model.Product;
import eu.copernik.tea.model.Release;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DefaultTeaRepositoryManagerTest {

    @TempDir
    private Path tempDir;

    private DefaultTeaRepositoryManager manager;

    @BeforeEach
    void setUp() {
        manager = new DefaultTeaRepositoryManager(tempDir);
    }

    @Test
    void testGetProductById_returnsNullForNonExistentProduct() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        assertThat(manager.getProductById(nonExistentId)).isNull();
    }

    @Test
    void testGetProductById_findsExistingProject() throws Exception {
        UUID existentId = UUID.randomUUID();
        Product product = new Product().uuid(existentId).name("Test Product");
        manager.saveProduct(product);

        Product foundProduct = manager.getProductById(existentId);
        assertThat(foundProduct).isNotNull().isEqualTo(product);
    }

    @Test
    void testGetOrCreateComponentByPurl_createsAndFindsComponent() throws Exception {
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
    void testFindComponentByPurl_returnsNullForNonExistentPurl() throws Exception {
        PackageURL purl = new PackageURL("pkg:maven/org.example/nonexistent@1.0.0");
        assertThat(manager.getOrCreateComponentByPurl(purl)).isNotNull();
    }

    @Test
    void testGetOrCreateReleaseByVersion_createsAndFindsRelease() throws Exception {
        PackageURL purl = new PackageURL("pkg:maven/org.example/bar@2.0.0");
        Component component = manager.getOrCreateComponentByPurl(purl);
        // Add required fields to the component
        component.name("Test Component Bar");
        manager.saveComponent(component);

        Release created = manager.getOrCreateReleaseByVersion(component, "2.0.0");
        assertThat(created).isNotNull();
        assertThat(created.getVersion()).isEqualTo("2.0.0");
        // Add required fields to the release
        created.releaseDate(OffsetDateTime.now(UTC));
        manager.saveRelease(component, created);

        Release found = manager.getOrCreateReleaseByVersion(component, "2.0.0");
        assertThat(found.getUuid()).isEqualTo(created.getUuid());
    }

    @Test
    void testFindReleaseByVersion_returnsNullForNonExistentVersion() throws Exception {
        PackageURL purl = new PackageURL("pkg:maven/org.example/nonexistent@2.0.0");
        Component component = manager.getOrCreateComponentByPurl(purl);
        // Add required fields to the component
        component.name("Test Component Nonexistent");
        manager.saveComponent(component);

        Release found = manager.getOrCreateReleaseByVersion(component, "2.0.0");
        assertThat(found).isNotNull();
        assertThat(found.getVersion()).isEqualTo("2.0.0");
    }

    @Test
    void testSaveAndGetCollectionByReleaseAndVersion() throws Exception {
        PackageURL purl = new PackageURL("pkg:maven/org.example/baz@3.0.0");
        Component component = manager.getOrCreateComponentByPurl(purl);
        // Add required fields to the component
        component.name("Test Component Baz");
        manager.saveComponent(component);
        Release release = manager.getOrCreateReleaseByVersion(component, "3.0.0");
        // Add required fields to the release
        release.releaseDate(OffsetDateTime.now(UTC));
        manager.saveRelease(component, release);

        Collection collection = new Collection();
        collection.setUuid(release.getUuid());
        collection.setVersion(1);
        manager.saveCollection(collection);

        Collection found = manager.getCollectionByReleaseAndVersion(release, 1);
        assertThat(found).isNotNull();
        assertThat(found.getVersion()).isEqualTo(1);
    }

    @Test
    void testGetLatestCollectionByRelease_returnsLatest() throws Exception {
        PackageURL purl = new PackageURL("pkg:maven/org.example/qux@4.0.0");
        Component component = manager.getOrCreateComponentByPurl(purl);
        // Add required fields to the component
        component.name("Test Component Qux");
        manager.saveComponent(component);
        Release release = manager.getOrCreateReleaseByVersion(component, "4.0.0");
        // Add required fields to the release
        release.releaseDate(OffsetDateTime.now(UTC));
        manager.saveRelease(component, release);

        for (int i = 1; i <= 3; i++) {
            Collection collection = new Collection();
            collection.setUuid(release.getUuid());
            collection.setVersion(i);
            manager.saveCollection(collection);
        }

        Collection latest = manager.getLatestCollectionByRelease(release);
        assertThat(latest).isNotNull();
        assertThat(latest.getVersion()).isEqualTo(3);
    }
}
