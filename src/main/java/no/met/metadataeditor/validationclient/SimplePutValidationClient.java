package no.met.metadataeditor.validationclient;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import no.met.metadataeditor.EditorException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Validation client to a SimplePutValidation service.
 */
public class SimplePutValidationClient implements ValidationClient {

    private String url;

    public SimplePutValidationClient(String url){
        this.url = url;
    }

    @Override
    public ValidationResponse validate(String xml) {

        try {
            List<NameValuePair> formParams = new ArrayList<NameValuePair>();
            formParams.add( new BasicNameValuePair("xml", xml));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams, "UTF-8");
            HttpPost post = new HttpPost(url);
            post.setEntity(entity);

            HttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(post);


            int statusCode = response.getStatusLine().getStatusCode();
            boolean success;
            if( 200 == statusCode ){
                success = true;
            } else if ( 400 == statusCode ) {
                success = false;
            } else {
                throw new EditorException("HTTP status code from SimplePutValidation service not as expected: " + statusCode );
            }

            String message = getValidationMessage(IOUtils.toString(response.getEntity().getContent()));
            ValidationResponse validationResponse = new ValidationResponse(success, message);
            return validationResponse;

        } catch (UnsupportedEncodingException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Unsuppoerted encoding", e);
            throw new EditorException("Unsupported encoding in validation client", e);
        } catch (ClientProtocolException e) {
            String msg = "Error contacting SimplePutValidation service at:" + url;
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, msg, e);
            throw new EditorException(msg, e);
        } catch (IOException e) {
            String msg = "IOException when contacting SimplePutValiation service at: " + url;
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, msg, e);
            throw new EditorException(msg, e);
        }
    }

    private String getValidationMessage(String responseXML){

        String validationMsg = null;
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(responseXML)));
            XPath xpath = XPathFactory.newInstance().newXPath();

             validationMsg = xpath.evaluate("//validation/text()", doc);

        } catch (ParserConfigurationException e) {
            String msg = "Parser configuration error while parsing SimplePutService response";
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, msg, e);
            throw new EditorException(msg, e);
        } catch (SAXException e) {
            String msg = "SAX Exception while parsing SimplePutService response";
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, msg, e);
            throw new EditorException(msg, e);
        } catch (IOException e) {
            String msg = "IOException while parsing SimplePutService response";
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, msg, e);
            throw new EditorException(msg, e);
        } catch (XPathExpressionException e) {
            String msg = "XPathExpression error while parsing SimplePutService response";
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, msg, e);
            throw new EditorException(msg, e);
        }
        return validationMsg;

    }



}
