package no.met.metadataeditor.dataTypes;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import no.met.metadataeditor.EditorException;

/**
 * Abstract base class for all DataAttributes
 */
public abstract class DataAttributes {

    private static final Logger logger = Logger.getLogger(DataAttributes.class.getName());
    
    /**
     * @return A mapping between field names and DataType for all attributes 
     * supported by the class. 
     */
    public Map<String, DataType> getAttributesSetup(){
        return getAttributesSetup(getClass());
    }
    
    private Map<String, DataType> getAttributesSetup(Class<? extends Object> inClass){
        
        Map<String, DataType> fields = new HashMap<String,DataType>();
        for (Field f : inClass.getDeclaredFields()) {

            if (f.isAnnotationPresent(IsAttribute.class)) {
                
                IsAttribute ia = f.getAnnotation(IsAttribute.class);
                DataType type = ia.value();
                fields.put(f.getName(), type);
            }
        }

        // attribute might be declared in super class so call this function recursively on the super
        // class
        if( inClass.getSuperclass() != null ){
            Map<String,DataType> superFields = getAttributesSetup(inClass.getSuperclass());
            fields.putAll(superFields);
        }
        
        return fields;             
        
    }

    /**
     * @return a new Instance of the subtype
     */
    public abstract DataAttributes newInstance();

    /**
     * add a attributes value by a string
     * @param attr The name of the attribute to set.
     * @param value The value that the attribute should be set to.
     * @throws AttributeMismatchException If the class does not have the attribute.
     */
    public void addAttribute(String attr, String value){
        
        Map<String,DataType> attributes = getAttributesSetup();        
        if(!(attributes.containsKey(attr))){
            throw new AttributesMismatchException(attr + " is not a valid attribute for " + getClass().getName());
        }        
        
        addAttribute(attr, value, getClass());
    }
    
    private void addAttribute(String attr, String value, Class<? extends Object> inClass){

        boolean isSet = false;
        try {
            for (Field f : inClass.getDeclaredFields()) {

                if (!f.getName().equals(attr)) {
                    continue;
                }

                if (f.isAnnotationPresent(IsAttribute.class)) {
                    f.set(this, value);
                    isSet = true;
                }
            }

            // attribute might be declared in super class so call this function recursively on the super
            // class
            if( !isSet && inClass.getSuperclass() != null ){
                addAttribute(attr, value, inClass.getSuperclass());
            }

        } catch (IllegalAccessException e) {
            logger.log(Level.SEVERE, "Illegal to access the get method of the property: " + attr, e);
            throw new EditorException("Illegal to access the get method of the property: " + attr, e);
        } catch (SecurityException e) {
            logger.log(Level.SEVERE, "Security problem to access the get method of the property: " + attr, e);            
            throw new EditorException("Security problem to access the get method of the property: " + attr, e);
        }         
        
    }
    
    /**
     * @param attr The name of the attribute.
     * @throws AttributeMismatchException Thrown if the attribute does not exist for the class.
     * @return The value of an attribute. 
     */
    public String getAttribute(String attr) {        
        
        Map<String,DataType> attributes = getAttributesSetup();
        
        if(!(attributes.containsKey(attr))){
            throw new AttributesMismatchException(attr + " is not a valid attribute for " + getClass().getName());
        }
        
        return getAttribute(attr,getClass());                
    }
    
    private String getAttribute(String attribute, Class<? extends Object> inClass) {
        
        String value = null;
        boolean isFetched = false;
        try {
            for (Field f : inClass.getDeclaredFields()) {

                if (!f.getName().equals(attribute)) {
                    continue;
                }

                if (f.isAnnotationPresent(IsAttribute.class)) {
                    value = "" + f.get(this);
                    if (value.trim().equalsIgnoreCase("null"))
                        value = "";

                    isFetched = true;
                }

            }

            // attribute might be declared in super class so call this function recursively on the super
            // class
            if( !isFetched && inClass.getSuperclass() != null ){
                value = getAttribute(attribute, inClass.getSuperclass());
            }

        } catch (IllegalAccessException e) {
            logger.log(Level.SEVERE, "Illegal to access the get method of the property: " + attribute, e);
            throw new EditorException("Illegal to access the get method of the property: " + attribute, e);
        } catch (SecurityException e) {
            logger.log(Level.SEVERE, "Security problem to access the get method of the property: " + attribute, e);            
            throw new EditorException("Security problem to access the get method of the property: " + attribute, e);
        } 
        
        return value;        
        
    }
}
