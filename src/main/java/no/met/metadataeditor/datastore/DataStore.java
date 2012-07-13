package no.met.metadataeditor.datastore;

/**
 * Interface supported by all data stores.
 * 
 * A data store is where the editor will find metadata, configurations and resources.
 */
public interface DataStore {


    /**
     * Write the metadata to the datastore.
     * @param project The project to write to.
     * @param recordIdentifier The recordIdentifer for the metadata record to write to.
     * @param xml The XML that should be written. The XML will be written directly without further processing.
     * @return Returns true on success and throws a EditorException on error.
     */
    boolean writeMetadata(String project, String recordIdentifier, String xml);
    
    /**
     * @param project The project to check.
     * @return Returns true if the project exist in the data store. Returns false otherwise.
     */
    boolean projectExists(String project);
    
    /**
     * @param project The project to look in.
     * @param recordIdentifier The metadata record to check if exists.
     * @return Returns true if the metadata record exists in the project. Returns false otherwise.
     */
    boolean metadataExists(String project, String recordIdentifier);
    
    /**
     * @param project The project to read from. 
     * @param recordIdentifier The record identifier for the metadata to read.
     * @return The raw XML for the metadata on success. Throws and EditorException on error.
     */
    String readMetadata(String project, String recordIdentifier);

    /**
     * @param project The project to read from. 
     * @param recordIdentifier The record identifier for the metadata we want a template for.
     * @return The raw XML for the template on success. Throws and EditorException on error.
     */
    String readTemplate(String project, String recordIdentifier);

    /**
     * @param project The project to read from. 
     * @param recordIdentifier The record identifier for the metadata we want a configuration for.
     * @return The raw XML for the metadata on success. Throws and EditorException on error.
     */
    String readEditorConfiguration(String project, String recordIdentifier);

    /**
     * @param project The project to read from.
     * @param resourceIdentifier The identifier of the resource to read.
     * @return The raw string content of the resource on success. Throws and EditorException on error.
     */
    String readResource(String project, String resourceIdentifier);
    
}
