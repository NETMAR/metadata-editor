package no.met.metadataeditor.widget;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;

public class StringWidget extends EditorWidget {

    private static final long serialVersionUID = -7542890498259559947L;
        
    private int size = 40;
    
    private int maxlength = 40;

    public StringWidget(){
        super();
    }
    
    public StringWidget(StringWidget cloneFrom){
        super(cloneFrom);
        
        this.size = cloneFrom.size;
        this.maxlength = cloneFrom.size;
    }    
    
    @Override
    public Map<String, String> getDefaultValue() {
        Map<String,String> defaultValue = new HashMap<String,String>();
        defaultValue.put("str", "");
        return defaultValue;
    }

    @XmlAttribute
    public int getSize() {
        return size;
    }


    public void setSize(int size) {
        this.size = size;
    }

    @XmlAttribute
    public int getMaxlength() {
        return maxlength;
    }


    public void setMaxlength(int maxlength) {
        this.maxlength = maxlength;
    }

}
