/*
 * Test1.java
 *
 */
package com.realtor.rets.compliance.tests;

import com.realtor.rets.compliance.PropertyManager;
import com.realtor.rets.compliance.TestResult;
import com.realtor.rets.compliance.tests.util.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.realtor.rets.retsapi.RETSTransaction;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extends the BaseEvaluator Interface to validate against a DTD
 *
 * @author pobrien
 */
public class ValidateDTD extends BaseEvaluator {

    private static Log log = LogFactory.getLog(ValidateDTD.class);   
    
  public ValidateDTD() {
    super();
    specReference = "";
  }

private String notes="";
private String docTypeLocation="";
boolean error=false;
private String errorList="";
  /**
   * Evaluate a set of transactions. This method is called by the TestExecuter
   * Checks the XML response against the RETS DTD
   *
   *  *
   * @return just returns an empty string for now.
   */
  protected TestResult processResults(String transName, RETSTransaction t) {
      if (t == null) {
        return null;
      }

      Map resp = t.getResponseMap();
      String cName = t.getClass().getName();

      Map m = CollectionUtils.copyLowerCaseMap(t.getResponseHeaderMap());
      boolean isXml = CollectionUtils.hasValue(m,"content-type","text/xml");
      int cnt=CollectionUtils.keyCount(m,"content-type");

        if (cnt > 1)
        {
            String mContent = "Multiple Content-Type's are reported the response for transaction : "+
                               transName;
            String note = "Some clients may fail or be confused if multiple 'Content-Type' values returned ";
            return reportResult("CheckWellFormed", mContent,"Info",note,"n/a");

        }


        if (!isXml){
            notes+= "Transaction " + cName
             + " does not include a Content-Type of 'text/xml'";
        }

        String body = (String) resp.get("body");
        return validateDTD(transName,body);




  }

  /**
   * This is the class that does the actual check of a transaction body for well
   *  formed xml.
   *
   * @param transactionName Name of the Transaction checked (from the test script)
   * @param responseBody transaction body which should be well formed xml
   *
   * @return results of the test.
   */
  protected TestResult validateDTD(String transactionName,
                                       String responseBody) {
    Document document = null;
    boolean valid=true;
    String status = "Failure";
    notes="";
    errorList="";
     error=false;
    String jException="";


    try {
      if ((responseBody != null) && (responseBody.length() > 0)) {

        //check for xml version
        Pattern patXml = Pattern.compile("<\\?xml[ \\t]+.*version=\"1\\.0\".*\\?>");

        Matcher matchXml = patXml.matcher(responseBody);
        if(!matchXml.find()){
            appendNotes("Failure:  no xml version in response");
            appendNotes(responseBody);
                    return reportResult("ValidateDTD:  "+transactionName,
                                        "Checks to see if the transaction validated against the RETS DTD",
                                        status, "no xml version",jException,"n/a");
        }


        if(responseBody.indexOf("<!DOCTYPE")== -1)      {
            responseBody=responseBody.substring(0,matchXml.end())
                +"\n<!DOCTYPE RETS SYSTEM \""+docTypeLocation+"\">"
                +responseBody.substring(matchXml.end()+1);

        }else {
            Pattern patDocType = Pattern.compile("<\\!DOCTYPE[ \\t]+RETS[ \\t]+SYSTEM[ \\t]+\"(.*)\"[ \\t]*>");
            Matcher matchDocType = patDocType.matcher(responseBody);


            if (matchDocType.find()){

                if (!matchDocType.group(1).endsWith(docTypeLocation) ){
                    appendNotes("Failure:  incorrect dtd in response");
                    return reportResult("ValidateDTD:  "+transactionName,
                        "Checks to see if the transaction validated against the RETS DTD",
                        status,
                        notes,
                        jException,
                        "Incorrect dtd, expected "+docTypeLocation+" got "+matchDocType.group(1));
                }
            }else{
                if (responseBody.indexOf("PUBLIC")<0){
                    appendNotes("Failure:  unexpected doctype");
                    return reportResult("ValidateDTD:  "+transactionName,
                    "Checks to see if the transaction validated against the RETS DTD",
                    status, notes,jException,"unexpected doctype, expected <!DOCTYPE RETS SYSTEM \""+docTypeLocation+"\">");
                }
            }
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(
                                        (responseBody).getBytes());
        String inputSourceUrl = "file:" + PropertyManager.getConfigDirectory().replace(File.separatorChar, '/') + "/";
        InputSource is = new InputSource(inputSourceUrl);
        if (log.isDebugEnabled()) {
        	log.debug("InputSource for DTDs is " + inputSourceUrl);
        	log.debug("InputSource SystemID is " + is.getPublicId());
        	log.debug("InputSource SystemID is " + is.getSystemId());
        }
        is.setByteStream(bais);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new org.xml.sax.ErrorHandler(){
            public void warning(org.xml.sax.SAXParseException e){
                appendNotes("Warning: "+xmlParseErrorMessage(e)+"\n");
            }
            public void error(org.xml.sax.SAXParseException e){
                appendNotes("Error: "+xmlParseErrorMessage(e)+"\n");
                error=true;
            }
            public void fatalError(org.xml.sax.SAXParseException e)
            throws org.xml.sax.SAXParseException{
                appendNotes("Fatal Error: "+xmlParseErrorMessage(e)+"\n");
                error=true;
                throw e;
            }
        });

        document = builder.parse(is);

      }

      // if we can create an XML document from the stream, it is well formed XML.
      if ((document != null)&&(!error)) {
        status = "Success";
        notes += "The response for transaction [" + transactionName
                + "] validated against the RETS DTD.  Response is "+responseBody;
      } else {
        notes += "The response body for transaction \"" + transactionName
                      + "\" did not validate against the RETS DTD. View the diagnostic message and response body below.\n\n  \n \t XML Parse Error: "+errorList+" \n\n Response body:\n\n{" + responseBody + "}";

      }
    } catch (org.xml.sax.SAXParseException e) {
         e.printStackTrace();
      notes += "The response body for transaction \"" + transactionName
              + "\" did not validate against the RETS DTD. View the diagnostic message and response body below.\n\n  \n \t XML Parse Error: "+errorList+" \n\n Response body:\n\n{" + responseBody + "}";
      jException=e.toString();

    }
    catch (Exception e) {
          e.printStackTrace();
          notes += "The response body for transaction \"" + transactionName
                  + "\" did not validate against the RETS DTD. View the diagnostic message and response body below.\n\n  Diagnostic message: \n \t"+e.getMessage()+"\n\n Response body:\n\n{" + responseBody + "}";
          jException=e.toString();
    }


    return reportResult("ValidateDTD:  "+transactionName,
                        "Checks to see if the transaction validated against the RETS DTD",
                        status, notes,jException,"n/a");
  }

  String xmlParseErrorMessage(org.xml.sax.SAXParseException e){
      int line = e.getLineNumber();
       int col = e.getColumnNumber();
       String message = e.getMessage()
                        + "\t Line=\t" + line
                        + "\t Column=\t" + col;

      return message;
  }

  String appendNotes(String note)
  {
      this.errorList+=note;
      return errorList;
  }

  protected void setDocTypeLocation(String docName){
      docTypeLocation=docName;

    }



}
