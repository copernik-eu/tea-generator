package eu.copernik.tea.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.packageurl.PackageURL;
import eu.copernik.tea.TeaRepositoryManager;
import eu.copernik.tea.model.Collection;
import eu.copernik.tea.model.Component;
import eu.copernik.tea.model.Identifier;
import eu.copernik.tea.model.IdentifierType;
import eu.copernik.tea.model.Product;
import eu.copernik.tea.model.Release;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.UUID;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class DefaultTeaRepositoryManager implements TeaRepositoryManager {

    private final ObjectMapper objectMapper =
            JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    private final Path basePath;

    protected DefaultTeaRepositoryManager(Path basePath) {
        this.basePath = basePath;
    }

    @Override
    public @Nullable Product getProductById(UUID id) throws IOException {
        Path productFolder = basePath.resolve("product");
        if (Files.exists(productFolder.resolve(id + ".json"))) {
            return loadModel(Product.class, productFolder, id.toString());
        }
        return null;
    }

    @Override
    public void saveProduct(Product product) throws IOException {
        Path productFolder = basePath.resolve("product");
        Files.createDirectories(productFolder);
        saveModel(product, productFolder, product.getUuid().toString());
    }

    @Override
    public Component getOrCreateComponentByPurl(PackageURL purl) throws IOException {
        @Nullable Component component = findComponentByPurl(purl);
        if (component != null) {
            return component;
        }
        Component newComponent = new Component();
        newComponent.setUuid(UUID.randomUUID());
        newComponent.addIdentifiersItem(
                new Identifier().idType(IdentifierType.PURL).idValue(purl.toString()));
        return newComponent;
    }

    @Nullable
    Component findComponentByPurl(PackageURL purl) throws IOException {
        Path componentFolder = basePath.resolve("component");
        if (Files.isDirectory(componentFolder)) {
            try (Stream<Path> files = Files.list(componentFolder)) {
                Iterator<Path> filesIterator = files.iterator();
                while (filesIterator.hasNext()) {
                    Path file = filesIterator.next();
                    Component component = loadModel(Component.class, componentFolder, stripExtension(file));
                    String componentPurl = component.getIdentifiers().stream()
                            .filter(id -> IdentifierType.PURL.equals(id.getIdType()))
                            .map(Identifier::getIdValue)
                            .findFirst()
                            .orElse(null);
                    if (componentPurl != null && componentPurl.equals(purl.toString())) {
                        return component;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void saveComponent(Component component) throws IOException {
        Path componentFolder = basePath.resolve("component");
        Files.createDirectories(componentFolder);
        saveModel(component, basePath.resolve("component"), component.getUuid().toString());
    }

    @Override
    public Release getOrCreateReleaseByVersion(Component component, String version) throws IOException {
        @Nullable Release release = findReleaseByVersion(component.getUuid(), version);
        if (release != null) {
            return release;
        }
        return new Release().uuid(UUID.randomUUID()).version(version);
    }

    @Nullable
    Release findReleaseByVersion(UUID uuid, String version) throws IOException {
        Path releasePath = basePath.resolve("component/" + uuid + "/release");
        if (Files.isDirectory(releasePath)) {
            try (Stream<Path> files = Files.list(releasePath)) {
                Iterator<Path> filesIterator = files.iterator();
                while (filesIterator.hasNext()) {
                    Path file = filesIterator.next();
                    Release release = loadModel(Release.class, releasePath, stripExtension(file));
                    if (version.equals(release.getVersion())) {
                        return release;
                    }
                }
            }
        }
        return null;
    }

    public void saveRelease(Component component, Release release) throws IOException {
        Path releaseFolder = basePath.resolve("component/" + component.getUuid() + "/release");
        Files.createDirectories(releaseFolder);
        saveModel(release, releaseFolder, release.getVersion());
    }

    @Override
    public @Nullable Collection getCollectionByReleaseAndVersion(Release release, int version) throws IOException {
        Path collectionPath = basePath.resolve("release/" + release.getUuid() + "/collection");
        if (Files.isDirectory(collectionPath)) {
            try (Stream<Path> files = Files.list(collectionPath)) {
                Iterator<Path> filesIterator = files.iterator();
                while (filesIterator.hasNext()) {
                    Path file = filesIterator.next();
                    Collection collection = loadModel(Collection.class, collectionPath, stripExtension(file));
                    Integer collectionVersion = collection.getVersion();
                    if (collectionVersion != null && collectionVersion == version) {
                        return collection;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public @Nullable Collection getLatestCollectionByRelease(Release release) throws IOException {
        Collection latestCollection = null;
        for (int version = 1; ; version++) {
            Collection collection = getCollectionByReleaseAndVersion(release, version);
            if (collection != null) {
                latestCollection = collection;
            } else {
                return latestCollection;
            }
        }
    }

    @Override
    public void saveCollection(Collection collection) throws IOException {
        Path collectionPath = basePath.resolve("release/" + collection.getUuid() + "/collection");
        Files.createDirectories(collectionPath);
        saveModel(collection, collectionPath, String.valueOf(collection.getVersion()));
    }

    private void saveModel(Object object, Path folder, String fileName) throws IOException {
        try {
            Path filePath = folder.resolve(fileName + ".json");
            objectMapper.writeValue(filePath.toFile(), object);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to save object to file: " + fileName, e);
        }
    }

    private <T> T loadModel(Class<T> type, Path folder, String fileName) throws IOException {
        try {
            Path filePath = folder.resolve(fileName + ".json");
            return objectMapper.readValue(filePath.toFile(), type);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to load object from file: " + fileName, e);
        }
    }

    private String stripExtension(Path path) {
        return path.getFileName().toString().replaceAll(".json$", "");
    }
}
