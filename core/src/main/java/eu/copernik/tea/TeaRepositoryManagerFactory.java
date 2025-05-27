package eu.copernik.tea;

public interface TeaRepositoryManagerFactory {

    /**
     * Creates a new instance of TeaRepositoryManager.
     *
     * @return a new TeaRepositoryManager instance
     */
    TeaRepositoryManager newInstance(TeaRepository repository) throws IllegalArgumentException;
}
