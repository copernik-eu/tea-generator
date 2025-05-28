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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.packageurl.PackageURL;
import eu.copernik.tea.TeaRepositoryManager;
import eu.copernik.tea.model.Collection;
import eu.copernik.tea.model.Component;
import eu.copernik.tea.model.Identifier;
import eu.copernik.tea.model.IdentifierType;
import eu.copernik.tea.model.Product;
import eu.copernik.tea.model.Release;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class DefaultTeaRepositoryManager implements TeaRepositoryManager {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    private final Path basePath;

    protected DefaultTeaRepositoryManager(Path basePath) {
        this.basePath = basePath;
    }

    @Override
    public @Nullable Product getProduct(UUID id) throws IOException {
        Path productFile = basePath.resolve("product/" + id + ".json");
        if (Files.exists(productFile)) {
            return loadModelOrThrow(Product.class, productFile);
        }
        return null;
    }

    private List<Product> findAllProducts() throws IOException {
        Path productFolder = basePath.resolve("product");
        return findAllModels(Product.class, productFolder);
    }

    @Override
    public void saveProduct(Product product) throws IOException {
        Path productFile = basePath.resolve("product/" + product.getUuid() + ".json");
        Files.createDirectories(productFile.getParent());
        saveModelOrThrow(product, productFile);
        updateProductsJson(OffsetDateTime.now(ZoneOffset.UTC));
    }

    private List<Component> findAllComponents() throws IOException {
        Path componentFolder = basePath.resolve("component");
        return findAllModels(Component.class, componentFolder);
    }

    Optional<Component> findComponentByPurl(PackageURL purl) throws IOException {
        return findAllComponents().stream()
                .filter(component -> component.getIdentifiers().stream()
                        .anyMatch(id -> IdentifierType.PURL.equals(id.getIdType())
                                && purl.toString().equals(id.getIdValue())))
                .findFirst();
    }

    @Override
    public Component getOrCreateComponentByPurl(PackageURL purl) throws IOException {
        return findComponentByPurl(purl).orElseGet(() -> {
            Component newComponent = new Component();
            newComponent.setUuid(UUID.randomUUID());
            newComponent.addIdentifiersItem(
                    new Identifier().idType(IdentifierType.PURL).idValue(purl.toString()));
            return newComponent;
        });
    }

    @Override
    public void saveComponent(Component component) throws IOException {
        Path componentFile = basePath.resolve("component/" + component.getUuid() + ".json");
        Files.createDirectories(componentFile.getParent());
        saveModelOrThrow(component, componentFile);
    }

    private List<Release> findAllReleases(UUID componentUuid) throws IOException {
        Path releaseFolder = basePath.resolve("component/" + componentUuid + "/release");
        return findAllModels(Release.class, releaseFolder);
    }

    private Optional<Release> findReleaseByUuidAndVersion(UUID componentUuid, String version) throws IOException {
        return findAllReleases(componentUuid).stream()
                .filter(release -> version.equals(release.getVersion()))
                .findFirst();
    }

    @Override
    public Release getOrCreateRelease(Component component, String version) throws IOException {
        return findReleaseByUuidAndVersion(component.getUuid(), version)
                .orElseGet(() -> new Release().uuid(UUID.randomUUID()).version(version));
    }

    public void saveRelease(Component component, Release release) throws IOException {
        Path releaseFile =
                basePath.resolve("component/" + component.getUuid() + "/release/" + release.getVersion() + ".json");
        Files.createDirectories(releaseFile.getParent());
        saveModelOrThrow(release, releaseFile);
        updateReleasesJson(component.getUuid());
    }

    private List<Collection> findAllCollections(UUID releaseUuid) throws IOException {
        Path collectionFolder = basePath.resolve("release/" + releaseUuid + "/collection");
        return findAllModels(Collection.class, collectionFolder);
    }

    @Override
    public @Nullable Collection getCollection(Release release, int version) throws IOException {
        Path collectionPath = basePath.resolve("release/" + release.getUuid() + "/collection/" + version + ".json");
        return Files.exists(collectionPath) ? loadModelOrThrow(Collection.class, collectionPath) : null;
    }

    @Override
    public @Nullable Collection getLatestCollection(Release release) throws IOException {
        Path collectionPath = basePath.resolve("release/" + release.getUuid() + "/collection.json");
        return Files.exists(collectionPath) ? loadModelOrThrow(Collection.class, collectionPath) : null;
    }

    @Override
    public void saveCollection(Collection collection) throws IOException {
        Path collectionPath = basePath.resolve(
                "release/" + collection.getUuid() + "/collection/" + collection.getVersion() + ".json");
        Files.createDirectories(collectionPath.getParent());
        saveModel(collection, collectionPath);
        updateCollectionsJson(collection.getUuid());
    }

    private void updateProductsJson(OffsetDateTime timestamp) throws IOException {
        Set<Product> products = new TreeSet<>(Comparator.comparing(Product::getUuid));
        products.addAll(findAllProducts());
        ProductPaginationDetails paginationDetails =
                new ProductPaginationDetails().results(products.stream().toList());
        paginationDetails
                .pageStartIndex(0L)
                .pageSize(Long.MAX_VALUE)
                .timestamp(timestamp)
                .setTotalResults((long) products.size());

        Path productsFile = basePath.resolve("products.json");
        saveModel(paginationDetails, productsFile);
    }

    private void updateReleasesJson(UUID componentUuid) throws IOException {
        Set<Release> releases = new TreeSet<>(Comparator.comparing(Release::getReleaseDate));
        releases.addAll(findAllReleases(componentUuid));

        Path releasesFile = basePath.resolve("component/" + componentUuid + "/releases.json");
        Files.createDirectories(releasesFile.getParent());
        saveModelOrThrow(releases.stream().toList(), releasesFile);
    }

    private void updateCollectionsJson(UUID releaseUuid) throws IOException {
        SortedSet<Collection> collections = new TreeSet<>(Comparator.comparing(Collection::getVersion));
        collections.addAll(findAllCollections(releaseUuid));

        Path collectionsFile = basePath.resolve("release/" + releaseUuid + "/collections.json");
        Files.createDirectories(collectionsFile.getParent());
        saveModelOrThrow(collections.stream().toList(), collectionsFile);

        Path lastCollectionFile = basePath.resolve("release/" + releaseUuid + "/collection.json");
        saveModelOrThrow(collections.last(), lastCollectionFile);
    }

    private void saveModelOrThrow(Object object, Path filePath) throws IOException {
        try {
            saveModel(object, filePath);
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private void saveModel(Object object, Path filePath) throws UncheckedIOException {
        try {
            validateModel(object);
            objectMapper.writeValue(filePath.toFile(), object);
        } catch (Exception e) {
            throw new UncheckedIOException(
                    e instanceof IOException
                            ? (IOException) e
                            : new IOException("Failed to save object " + object + " to file: " + filePath, e));
        }
    }

    // Package-private for tests
    <T> T loadModelOrThrow(Class<T> type, Path filePath) throws IOException {
        try {
            return loadModel(type, filePath);
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private <T> T loadModel(Class<T> type, Path filePath) throws UncheckedIOException {
        try {
            return validateModel(objectMapper.readValue(filePath.toFile(), type));
        } catch (Exception e) {
            throw new UncheckedIOException(
                    e instanceof IOException
                            ? (IOException) e
                            : new IOException("Failed to load object from file: " + filePath, e));
        }
    }

    // Package-private for tests
    <T> List<T> loadModelList(TypeReference<List<T>> typeRef, Path filePath) throws IOException {
        return validateModel(objectMapper.readValue(filePath.toFile(), typeRef));
    }

    private <T> List<T> findAllModels(Class<T> type, Path folder) throws IOException {
        if (!Files.exists(folder)) {
            return Collections.emptyList();
        }
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(Files::isRegularFile)
                    .map(file -> loadModel(type, file))
                    .collect(Collectors.toList());
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private <T> T validateModel(T object) {
        try (ValidatorFactory factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<T>> violations = validator.validate(object);
            if (!violations.isEmpty()) {
                StringBuilder sb = new StringBuilder("Validation error:\n");
                violations.forEach(v -> sb.append(v.getPropertyPath())
                        .append(": ")
                        .append(v.getMessage())
                        .append("\n"));
                throw new IllegalArgumentException(sb.toString());
            }
        }
        return object;
    }
}
