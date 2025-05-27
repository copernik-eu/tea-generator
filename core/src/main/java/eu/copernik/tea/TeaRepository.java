package eu.copernik.tea;

import java.nio.file.Path;

/**
 * This class represents a resource to store and manage TEA objects such as products, components, releases, and collections.
 */
public final class TeaRepository {

    private final Path basePath;

    /**
     * Constructs a new TeaRepository with the specified base path.
     *
     * @param basePath the base path where TEA objects will be stored
     */
    public TeaRepository(Path basePath) {
        this.basePath = basePath;
    }

    /**
     * Returns the base path of this TeaRepository.
     *
     * @return the base path where TEA objects are stored
     */
    public Path getBasePath() {
        return basePath;
    }
}
