import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.util.Terser;

public class DebugTest {
    public static void main(String[] args) {
        String VALID_ADT_MESSAGE = 
            "MSH|^~\\&|SENDING_APP|SENDING_FACILITY|RECEIVING_APP|RECEIVING_FACILITY|20240715120000||ADT^A01|12345|P|2.4\r" +
            "PID|1||123456789^^^MRN||DOE^JOHN^MIDDLE||19800101|M|||123 MAIN ST^^ANYTOWN^ST^12345||(555)123-4567|||S||987654321||||||||||||\r" +
            "PV1|1|I|ICU^101^1|||DOCTOR123^SMITH^JANE|||SUR||||A|||DOCTOR123^SMITH^JANE|INP|CAT|||||||||||||||||||||||||20240715120000";
        
        try {
            HapiContext hapiContext = new DefaultHapiContext();
            PipeParser parser = new PipeParser();
            
            ca.uhn.hl7v2.validation.impl.NoValidation noValidation = new ca.uhn.hl7v2.validation.impl.NoValidation();
            hapiContext.setValidationContext(noValidation);
            parser.setValidationContext(noValidation);
            
            Message message = parser.parse(VALID_ADT_MESSAGE);
            Terser terser = new Terser(message);
            
            System.out.println("Message structure: " + message.getClass().getSimpleName());
            System.out.println("PID-5: " + terser.get("PID-5"));
            System.out.println("PID-3: " + terser.get("PID-3"));
            System.out.println("PID-3-1: " + terser.get("PID-3-1"));
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}