package edu.seu.vcampus.client.view.shell;

/**
 * Java 7 compatible callback that consumes one string value.
 */
public interface StringAction {

    /**
     * Handles the supplied value.
     *
     * @param value callback value
     */
    void accept(String value);
}
