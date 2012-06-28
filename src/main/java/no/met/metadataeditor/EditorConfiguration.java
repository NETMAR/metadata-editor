package no.met.metadataeditor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import no.met.metadataeditor.dataTypes.EditorTemplate;
import no.met.metadataeditor.dataTypes.EditorTemplateFactory;
import no.met.metadataeditor.dataTypes.EditorVariable;



/**
 * Class used accessing editor configuration that are used to generate the editor UI.
 */
public class EditorConfiguration implements Serializable {
    
    /**
     * 
     */
    private static final long serialVersionUID = -6228315858621721527L;

    private static final Logger logger = Logger.getLogger(EditorConfiguration.class.getName());
    
    // list of widgets. Used to keep the order of the widgets as in the configuration
    private List<EditorWidget> widgets;
    
    private Map<String,EditorWidget> widgetMap;

    public EditorConfiguration(){
        widgets = new ArrayList<EditorWidget>();
        widgetMap = new HashMap<String,EditorWidget>();
    }

    public void addWidget(EditorWidget widget){        
        widgets.add(widget);
        widgetMap.put(widget.variableName, widget);
    }
    
    public List<EditorWidget> getWidgets(){
        return widgets;
    }
    
    public EditorWidget getWidget(String variableName){
        
        if(widgetMap.containsKey(variableName)){
            return widgetMap.get(variableName);
        }
        
        return null;
        
    }
    
    public boolean populate(String identifier) {

        EditorTemplate et = EditorTemplateFactory.getInstance(identifier);
        Map<String,EditorVariable> varMap = et.getTemplate();
        
        for(Map.Entry<String, EditorVariable> entry : varMap.entrySet()){
            
            if(widgetMap.containsKey(entry.getKey())){
                EditorWidget widget = widgetMap.get(entry.getKey());                        
                widget.populate(entry.getValue());
            }
        }
        
        return allPopulated();
    }
    
    private boolean allPopulated(){
        
        List<String> notPopulated = new ArrayList<String>();
        for(Map.Entry<String,EditorWidget> entry : widgetMap.entrySet() ){
            
            if(!entry.getValue().isPopulated()){
                logger.warning("EditorWidget '" + entry.getKey() + "' has not been populated" );
                notPopulated.add(entry.getKey());
            }
        }
        
        return notPopulated.isEmpty() ? true : false; 
    }

    public void save(String identifier) {

        
        
    }
    
    
    
}
