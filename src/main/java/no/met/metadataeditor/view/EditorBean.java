package no.met.metadataeditor.view;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import no.met.metadataeditor.Config;
import no.met.metadataeditor.Editor;
import no.met.metadataeditor.EditorConfiguration;
import no.met.metadataeditor.EditorException;
import no.met.metadataeditor.EditorWidgetView;
import no.met.metadataeditor.datastore.DataStore;
import no.met.metadataeditor.datastore.DataStoreFactory;
import no.met.metadataeditor.validationclient.ValidationClient;
import no.met.metadataeditor.validationclient.ValidationResponse;
import no.met.metadataeditor.widget.EditorWidget;

import org.primefaces.component.tabview.TabView;
import org.primefaces.event.TabChangeEvent;

/**
 * Bean used to the hold the current state of the editor.
 *
 * The bean is in view scope.
 */
@ManagedBean
@ViewScoped
public class EditorBean implements Serializable {

    private static final long serialVersionUID = 243543721833686400L;

    private static final Logger logger = Logger.getLogger(EditorBean.class.getName());

    private Editor editor;

    // automatically set based on the query parameters
    private String recordIdentifier;

    // automatically set based on the query parameters
    private String project;

    // used to track the current active tab. We need this as some times the entire form is re-rendered and we lose
    // the current tab wihtout this
    private int activeTabId = 0;

    boolean initPerformed = false;

    public EditorBean() {

    }

    /**
     * Initialise the the bean based on the "project" and "recordIdentifier".
     * This method is automatically called in the preRenderView phase.
     * @param event
     * @throws IOException
     */
    public void init(ComponentSystemEvent event) throws IOException {

        if(!initPerformed){

            validateProject(project);
            validateRecordIdentifier(project, recordIdentifier);

            editor = new Editor(project, recordIdentifier);
            editor.init();

            // need to get the session before the view is rendered to avoid getting exception.
            // see http://stackoverflow.com/questions/7433575/cannot-create-a-session-after-the-response-has-been-committed
            FacesContext.getCurrentInstance().getExternalContext().getSession(true);

            initPerformed = true;
        }

    }

    public void save() {


        UserBean user = getUser();
        if(user.isValidated()){
            boolean success = editor.save(project, recordIdentifier, user.getUsername(), user.getPassword());

            if( success ){
                FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Changes has been saved.", "Changes has been saved.");
                FacesContext.getCurrentInstance().addMessage(null, msg);
            } else {
                FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Failed to save changes.", "Failed to save changes.");
                FacesContext.getCurrentInstance().addMessage(null, msg);
            }
        } else {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Login required before saving.", "Login required before saving.");
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }

    }

    public void reset() throws IOException {

        if( initPerformed ){
            initPerformed = false;
            init(null);

            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "All changes have been reset.", "All changes have been reset.");
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }

    }

    public void validate(){

        DataStore datastore = DataStoreFactory.getInstance(project);

        ValidationClient validationClient = datastore.getValidationClient(recordIdentifier);

        if( validationClient == null ){
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "No validation configured for this format.", "No validation configured for this format.");
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        String xmlContent = editor.editorContentToXML(project, recordIdentifier);
        ValidationResponse validationResponse = validationClient.validate(xmlContent);

        if( validationResponse.success ){
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Validation successfull", "Validation successfull");
            FacesContext.getCurrentInstance().addMessage(null, msg);
        } else {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation failed.", validationResponse.message);
            FacesContext.getCurrentInstance().addMessage("validation-messages", msg);
        }

    }

    public void export() {

        String editorContent = editor.export(project, recordIdentifier);


        FacesContext ctx = FacesContext.getCurrentInstance();
        final HttpServletResponse resp = (HttpServletResponse)ctx.getExternalContext().getResponse();

        resp.setContentType("application/octet-stream");
        resp.setContentLength(editorContent.length());
        resp.setHeader( "Content-Disposition", "attachment;filename=" + recordIdentifier + ".xml" );
        try {
            resp.getOutputStream().write(editorContent.getBytes());
            resp.getOutputStream().flush();
            resp.getOutputStream().close();
        } catch (IOException e) {
            throw new EditorException("Failed to write XML to response", e);
        }

        ctx.responseComplete();

    }


    public EditorConfiguration getEditorConfiguration() {
        return editor.getEditorConfiguration();
    }


    public String getRecordIdentifier() {
        return recordIdentifier;
    }


    public void setRecordIdentifier(String recordIdentifier) {
        this.recordIdentifier = recordIdentifier;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }


    public int getActiveTabId() {
        return activeTabId;
    }

    public void setActiveTabId(int activeTabId) {
        this.activeTabId = activeTabId;
    }

    /**
     * Track that the currently selected tab has changed.
     * @param event
     */
    public void tabChanged(TabChangeEvent event){
        TabView tv = (TabView)event.getTab().getParent();
        activeTabId = tv.getActiveIndex();
    }

    public void addValue(EditorWidget widget){
        widget.addNewValue();
    }

    public void removeValue(EditorWidget widget, EditorWidgetView widgetView){
        widget.removeValue(widgetView);
    }

    public List<String> getResourceValues(EditorWidget widget){

        DataStore dataStore = DataStoreFactory.getInstance(project);
        String resourceString = dataStore.readResource(widget.getResourceUri().toString());

        String[] resourceValues = resourceString.split("\n");
        List<String> values = new ArrayList<String>();
        for(String s : resourceValues ){
            values.add(s);
        }
        return values;

    }

    /**
     * Get the values from a resources as key values pairs. The keys and values
     * are taken from alternate lines in the resource file.
     * @param widget The widget to get resource values for
     * @return A map of key/value pairs
     */
    public Map<String,String> getKeyValueResourceValues(EditorWidget widget){

        DataStore dataStore = DataStoreFactory.getInstance(project);
        String resourceString = dataStore.readResource(widget.getResourceUri().toString());


        List<String> resourceValues = new ArrayList<String>(Arrays.asList(resourceString.split("\n")));

        // ensure that we have an even number of elements.
        if( resourceValues.size() % 2 != 0 ){
            logger.log(Level.WARNING, "Odd number of lines in key/value resource. Even number expected");
            resourceValues.add("");
        }

        Map<String, String> values = new LinkedHashMap<String,String>();
        for( int i = 0; i < resourceValues.size(); i += 2 ){
            values.put(resourceValues.get(i), resourceValues.get(i+1));
        }

        return values;
    }

    public List<String> getFilteredResourceValues(EditorWidget widget, String filterAttribute) {

        List<String> currentValues = new ArrayList<String>();
        List<EditorWidgetView> widgetViews = widget.getWidgetViews();
        for( EditorWidgetView view : widgetViews ){
            currentValues.add(view.getValues().get(filterAttribute));
        }

        List<String> filteredValues = getResourceValues(widget);
        for( String value : currentValues ){
            filteredValues.remove(value);
        }

        return filteredValues;
    }

    /**
     * Validates that the project parameter refers to an acutal project.
     * @param context
     * @param component
     * @param object
     * @throws IOException
     */
    public void validateProject(String project) throws IOException {

        Config config = new Config("/metadataeditor.properties", Config.ENV_NAME);
        List<String> projects = config.getRequiredList("projects");

        if( !projects.contains(project)){
            String msg = "The project '" + project + "' has not been configured correctly. Please check that the project is correct.";
            FacesContext.getCurrentInstance().getExternalContext().responseSendError(404, msg);
        }
    }

    /**
     * Validate that the record identifier exists
     * @param context
     * @param component
     * @param object
     * @throws IOException
     */
    public void validateRecordIdentifier(String project, String recordIdentifier) throws IOException {

        DataStore dataStore = DataStoreFactory.getInstance(project);
        if( !dataStore.metadataExists(recordIdentifier)){
            String msg = "The metadata record '" + recordIdentifier + "' does not exist for the project '" + project + "'. ";
            msg += "Please check that both the record identifier and the project is correct";
            FacesContext.getCurrentInstance().getExternalContext().responseSendError(404, msg);
        }
    }

    /**
     * @return The UserBean object for the current user.
     */
    private UserBean getUser(){

        // IMPLEMENTATION NOTE: This was first implemented as a @ManagedProperty, but that did not work
        // for unknown reasons. It seemed like the UserBean object changed between request even if should
        // stay the same. So this workaround was added instead.
        HttpServletRequest request = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
        return (UserBean) request.getSession().getAttribute("userBean");
    }
}
