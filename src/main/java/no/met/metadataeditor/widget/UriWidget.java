package no.met.metadataeditor.widget;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="uri")
public class UriWidget extends EditorWidget {

    private static final long serialVersionUID = -1244778870767868773L;

    private int size = 100;
    
    private int maxlength = 400; 
    
    public UriWidget(){
        super();
    }
    
    public UriWidget(String label, String variableName) {
        super(label, variableName);
    }

    @Override
    public Map<String, String> getDefaultValue() {
        Map<String,String> defaultValue = new HashMap<String,String>();
        defaultValue.put("uri", "");
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
