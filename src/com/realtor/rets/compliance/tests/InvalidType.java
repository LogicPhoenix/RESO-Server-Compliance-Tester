/* $Header$ 
 */
package com.realtor.rets.compliance.tests;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.realtor.rets.compliance.TestResult;

/**
 *  Negative Tests Result Evaluator class for GetMetadata Negative test that sends an
 *  Invalid Type, sending the  value "METADATA-RETSNegativeTesting" 
 *  in the <Format\> tag in the script. 
 *  In processNegativeTestResults() the param transRespStatus value
 *  MUST match the value of msf_EXPECTED_STATUS_RESPONSE_CODE, for the test to succeed.
 * 
 * @author pobrien
 */
public class InvalidType extends NegativeBaseEvaluator {
    
    private static Log log = LogFactory.getLog(InvalidType.class);
    
    private final static String msf_EXPECTED_STATUS_RESPONSE_CODE = "20501";

    /**
     * 
     */
    public InvalidType() {
        super();

        setCorrectResponseStatus(msf_EXPECTED_STATUS_RESPONSE_CODE);
    }

    protected TestResult processNegativeTestResults(String transName,
            String responseBody, String transRespStatus) {

        TestResult negativeTestResult   = null;

        if (log.isDebugEnabled()) {
            log.debug("Test Transaction: " + transName + " : " 
                      + transRespStatus);
        }

        if (!transRespStatus.equals(msf_EXPECTED_STATUS_RESPONSE_CODE)) {
         if (transRespStatus.equals("20501")||transRespStatus.equals("20502")||transRespStatus.equals("20503")||transRespStatus.equals("20508")||transRespStatus.equals("20509")||transRespStatus.equals("20513")||transRespStatus.equals("20512")) {
                setWarningResponse(responseBody, transName, transRespStatus);
            } else{
            setFailureResponse(responseBody, transName, transRespStatus);
            }
        } else {
            setSuccessResponse(responseBody, transName, transRespStatus);
        }

        negativeTestResult = reportResult(transName, testResultDesc, 
                                      testResultStatus, testResultNotes);
        
        return negativeTestResult;
    }

}
