package no.met.metadataeditor.datastore;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import no.met.metadataeditor.EditorException;

abstract class DataStoreImpl implements DataStore {

    private final String SETUPFILE = "setup.xml";
    private final String CONFIGDIR = "config";
    private final String XMLDIR = "XML";

    private Document setupDoc = null;
    private Date setupDocDate = null;

    private Document getSetupDoc(String project) {
        String path = makePath(project, CONFIGDIR, SETUPFILE);

        // use existing if not too old
        if (setupDoc != null && setupDocDate != null) {
            Date lastModified = getLastModified(path);
            if (!lastModified.after(setupDocDate)) {
                return setupDoc;
            }
        }

        // fetch new file
        setupDocDate = getLastModified(path);
        String setupDocStr = get(path);
        javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        try {
            javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
            setupDoc = db.parse(new InputSource(new java.io.StringReader(setupDocStr)));
        } catch (SAXException e) {
            Logger.getLogger(DataStoreImpl.class.getName()).log(Level.SEVERE, null, e);
        } catch (IOException e) {
            Logger.getLogger(DataStoreImpl.class.getName()).log(Level.SEVERE, null, e);
        } catch (ParserConfigurationException e) {
            Logger.getLogger(DataStoreImpl.class.getName()).log(Level.SEVERE, null, e);
        }

        return setupDoc;
    }

    /**
     * put a resource to the datastor
     * @param id
     * @param resource
     * @param username The username in the data store for the user doing the put action
     * @param password The password of the user doing the put action
     */
    abstract void put(String id, String resource, String username, String password);
    /**
     * fetch a resource as string from the datastore
     * @param id
     * @return
     */
    abstract String get(String id);
    /**
     * check the last change of a resource
     * @param id
     * @return date of last modification, or current date if not detectable
     */
    abstract java.util.Date getLastModified(String id);
    /**
     * check if a resource exists
     * @param id
     * @return
     */
    abstract boolean exists(String id);
    /**
     * build the full path for a datastore
     * @param paths
     * @return
     */
    abstract String makePath(String... paths);

    @Override
    public boolean writeMetadata(String project, String recordIdentifier, String xml, String username, String password) {

        String url = metadataUrl(project, recordIdentifier);
        put(url, xml, username, password);
        return true;
    }

    @Override
    public boolean projectExists(String project) {
        return exists(makePath(project));
    }

    @Override
    public boolean metadataExists(String project, String recordIdentifier) {
        return exists(metadataUrl(project, recordIdentifier));
    }

    @Override
    public String readMetadata(String project, String recordIdentifier) {

        return get(metadataUrl(project, recordIdentifier));
    }

    @Override
    public String readTemplate(String project, String recordIdentifier) {

        String metadata = readMetadata(project, recordIdentifier);

        SupportedFormat format = DataStoreUtils.getFormat(getSupportedFormats(project), metadata);
        String templateUrl = templateUrl(project, format);

        if (!exists(templateUrl)) {
            throw new EditorException("Template does not exist: " + templateUrl);
        }

        return get(templateUrl);
    }

    @Override
    public String readTemplate(String project, SupportedFormat format){
        String templateUrl = templateUrl(project, format);

        List<SupportedFormat> formats = getSupportedFormats(project);

        if( !formats.contains(format) ){
            throw new IllegalArgumentException("Format not supported by project: " + format);
        }

        if (!exists(templateUrl)) {
            throw new EditorException("Template does not exist: " + templateUrl);
        }

        return get(templateUrl);
    }

    @Override
    public String readEditorConfiguration(String project, String recordIdentifier) {

        String metadata = readMetadata(project, recordIdentifier);

        SupportedFormat format = DataStoreUtils.getFormat(getSupportedFormats(project), metadata);
        String configurationUrl = editorConfigUrl(project, format);

        if (!exists(configurationUrl)) {
            throw new EditorException("Template does not exist: " + configurationUrl);
        }

        return get(configurationUrl);

    }

    @Override
    public String readResource(String project, String resourceIdentifier) {

        String resourceUrl = resourceUrl(project, resourceIdentifier);

        if (!exists(resourceUrl)) {
            throw new EditorException("Resource does not exist: " + resourceUrl);
        }
        return get(resourceUrl);
    }

    private String templateUrl(String project, SupportedFormat format) {
        return makePath(project, CONFIGDIR, format.templateName());
    }

    private String editorConfigUrl(String project, SupportedFormat format){

        return makePath(project, CONFIGDIR, format.editorConfigName());
    }

    private String resourceUrl(String project, String resourceIdentifier){

        return makePath(project, resourceIdentifier);
    }

    private String metadataUrl(String project, String recordIdentifier) {

        return makePath(project, XMLDIR, recordIdentifier + ".xml");

    }

    @Override
    public List<SupportedFormat> getSupportedFormats(String project) {
        Document doc = getSetupDoc(project);
        return DataStoreUtils.parseSupportedFormats(doc);
    }

}
