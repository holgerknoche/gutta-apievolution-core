package gutta.apievolution.core.resolution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gutta.apievolution.core.apimodel.AtomicType;
import gutta.apievolution.core.apimodel.ListType;
import gutta.apievolution.core.apimodel.NumericType;
import gutta.apievolution.core.apimodel.Optionality;
import gutta.apievolution.core.apimodel.StringType;
import gutta.apievolution.core.apimodel.Usage;
import gutta.apievolution.core.apimodel.consumer.ConsumerApiDefinition;
import gutta.apievolution.core.apimodel.consumer.ConsumerEnumMember;
import gutta.apievolution.core.apimodel.consumer.ConsumerEnumType;
import gutta.apievolution.core.apimodel.consumer.ConsumerField;
import gutta.apievolution.core.apimodel.consumer.ConsumerOperation;
import gutta.apievolution.core.apimodel.consumer.ConsumerRecordType;
import gutta.apievolution.core.apimodel.provider.ProviderApiDefinition;
import gutta.apievolution.core.apimodel.provider.ProviderEnumMember;
import gutta.apievolution.core.apimodel.provider.ProviderEnumType;
import gutta.apievolution.core.apimodel.provider.ProviderField;
import gutta.apievolution.core.apimodel.provider.ProviderOperation;
import gutta.apievolution.core.apimodel.provider.ProviderRecordType;
import gutta.apievolution.core.apimodel.provider.RevisionHistory;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Test cases for definition resolution.
 */
class DefinitionResolverTest {

    /**
     * Test case: Mapping on matching basic type fields.
     */
    @Test
    void matchingBasicTypeFields() {
        // Consumer API definition
        ConsumerApiDefinition consumerApi = ConsumerApiDefinition.create("test", 0);

        ConsumerRecordType consumerType = ConsumerRecordType.createRecordType("TestType", 0, consumerApi);

        ConsumerField.create("int32Field", consumerType, AtomicType.INT_32, Optionality.MANDATORY);
        ConsumerField.create("int64Field", consumerType, AtomicType.INT_64, Optionality.MANDATORY);
        ConsumerField.create("unboundedStringField", consumerType, StringType.unbounded(), Optionality.MANDATORY);
        ConsumerField.create("boundedStringField", consumerType, StringType.bounded(10), Optionality.MANDATORY);
        ConsumerField.create("unboundedListField", consumerType, ListType.unbounded(StringType.unbounded()),
                Optionality.MANDATORY);
        ConsumerField.create("boundedListField", consumerType, ListType.bounded(StringType.unbounded(), 10),
                Optionality.MANDATORY);
        ConsumerField.create("numericField", consumerType, NumericType.bounded(5, 0), Optionality.MANDATORY);

        consumerApi.finalizeDefinition();

        // Provider API definition
        ProviderApiDefinition providerApi = ProviderApiDefinition.create("test", 0);

        ProviderRecordType providerType = ProviderRecordType.createRecordType("TestType", 0, providerApi);

        ProviderField.create("int32Field", providerType, AtomicType.INT_32, Optionality.MANDATORY);
        ProviderField.create("int64Field", providerType, AtomicType.INT_64, Optionality.MANDATORY);
        ProviderField.create("unboundedStringField", providerType, StringType.unbounded(), Optionality.MANDATORY);
        ProviderField.create("boundedStringField", providerType, StringType.bounded(10), Optionality.MANDATORY);
        ProviderField.create("unboundedListField", providerType, ListType.unbounded(StringType.unbounded()),
                Optionality.MANDATORY);
        ProviderField.create("boundedListField", providerType, ListType.bounded(StringType.unbounded(), 10),
                Optionality.MANDATORY);
        ProviderField.create("numericField", providerType, NumericType.bounded(5, 0), Optionality.MANDATORY);

        providerApi.finalizeDefinition();

        RevisionHistory revisionHistory = new RevisionHistory(providerApi);
        Set<Integer> supportedRevisions = Collections.singleton(0);

        DefinitionResolution resolution = new DefinitionResolver().resolveConsumerDefinition(revisionHistory,
                supportedRevisions, consumerApi);

        String expected = "TestType -> TestType@revision 0\n" + " int32Field -> int32Field@TestType@revision 0\n" +
                " int64Field -> int64Field@TestType@revision 0\n" +
                " unboundedStringField -> unboundedStringField@TestType@revision 0\n" +
                " boundedStringField -> boundedStringField@TestType@revision 0\n" +
                " unboundedListField -> unboundedListField@TestType@revision 0\n" +
                " boundedListField -> boundedListField@TestType@revision 0\n" +
                " numericField -> numericField@TestType@revision 0\n";

        String actual = new DefinitionResolutionPrinter().printDefinitionResolution(resolution);

        assertEquals(expected, actual);
    }

    /**
     * Assert that a missing mapping of a mandatory field is appropriately reported.
     */
    @Test
    void testMissingMappingForMandatoryField() {
        ConsumerApiDefinition consumerApi = ConsumerApiDefinition.create("test", 0);

        ConsumerRecordType consumerType = ConsumerRecordType.createRecordType("Test", 1, consumerApi);

        ConsumerField.create("optionalField", consumerType, AtomicType.INT_32, Optionality.OPTIONAL);

        // Define a provider API with a mandatory field
        ProviderApiDefinition revision = ProviderApiDefinition.create("test", 0);

        ProviderRecordType recordType = ProviderRecordType.createRecordType("Test", 1, revision);

        ProviderField.create("mandatoryField", recordType, AtomicType.INT_32, Optionality.MANDATORY);
        ProviderField.create("optionalField", recordType, AtomicType.INT_32, Optionality.OPTIONAL);

        // Resolve the consumer definition against the revision history
        RevisionHistory revisionHistory = new RevisionHistory(revision);
        Set<Integer> supportedRevision = new HashSet<>(Arrays.asList(0));

        DefinitionResolver resolver = new DefinitionResolver();
        DefinitionResolutionException exception = assertThrows(DefinitionResolutionException.class,
                () -> resolver.resolveConsumerDefinition(revisionHistory, supportedRevision, consumerApi));

        // Ensure that the exception has the right error message
        assertTrue(exception.getMessage().contains("is not mapped"));
    }

    /**
     * Test case: A mapping of types with incompatible (base) types is detected.
     */
    @Test
    void testIncompatibleBaseTypesInMapping() {
        // Provider revision
        ProviderApiDefinition providerApi = ProviderApiDefinition.create("test", 0);

        ProviderRecordType providerType = ProviderRecordType.createRecordType("TestType", 0, providerApi);

        ProviderField.create("testField", providerType, AtomicType.INT_32, Optionality.MANDATORY);

        // Consumer definition
        ConsumerApiDefinition consumerApi = ConsumerApiDefinition.create("test", 0);

        ConsumerRecordType consumerType = ConsumerRecordType.createRecordType("TestType", 0, consumerApi);

        ConsumerField.create("testField", consumerType, AtomicType.INT_64, Optionality.MANDATORY);

        //
        RevisionHistory revisionHistory = new RevisionHistory(providerApi);
        Set<Integer> supportedRevisions = new HashSet<>(Arrays.asList(0));
        DefinitionResolver resolver = new DefinitionResolver();

        DefinitionResolutionException exception = assertThrows(DefinitionResolutionException.class,
                () -> resolver.resolveConsumerDefinition(revisionHistory, supportedRevisions, consumerApi));

        assertTrue(exception.getMessage().contains("do not match"));
    }

    @Test
    void mapEnumMembers() {
        // Consumer API definition
        ConsumerApiDefinition consumerApi = ConsumerApiDefinition.create("test", 0);

        ConsumerEnumType consumerEnum = ConsumerEnumType.create("TestEnum", 0, consumerApi);

        ConsumerEnumMember.create("MEMBER_A", consumerEnum);
        ConsumerEnumMember.create("MEMBER_B", consumerEnum);

        consumerApi.finalizeDefinition();

        // Provider API definition
        ProviderApiDefinition providerApi = ProviderApiDefinition.create("test", 0);

        ProviderEnumType providerEnum = ProviderEnumType.create("TestEnum", 0, providerApi);

        ProviderEnumMember.create("MEMBER_A", providerEnum);
        ProviderEnumMember.create("MEMBER_B", providerEnum);

        providerApi.finalizeDefinition();

        RevisionHistory revisionHistory = new RevisionHistory(providerApi);
        Set<Integer> supportedRevisions = Collections.singleton(0);

        DefinitionResolution resolution = new DefinitionResolver().resolveConsumerDefinition(revisionHistory,
                supportedRevisions, consumerApi);

        String expected = "TestEnum -> TestEnum\n" + " MEMBER_A -> MEMBER_A\n" + " MEMBER_B -> MEMBER_B\n";

        String actual = new DefinitionResolutionPrinter().printDefinitionResolution(resolution);

        assertEquals(expected, actual);
    }

    /**
     * Test case: Map services and operations.
     */
    @Test
    void mapServicesAndOperations() {
        ConsumerApiDefinition consumerApi = ConsumerApiDefinition.create("test", 0);

        ConsumerRecordType consumerRecord = ConsumerRecordType.withInternalName("RecordType", "ConsumerRecordType", 0,
                consumerApi);

        ConsumerRecordType consumerException = ConsumerRecordType.exceptionType("ExceptionType",
                "ConsumerExceptionType", 1, consumerApi, false, Collections.emptySet());

        ConsumerOperation consumerOperation = ConsumerOperation.withInternalName("operation", "consumerOperation",
                consumerApi, consumerRecord, consumerRecord);

        consumerOperation.addThrownException(consumerException);
        consumerApi.finalizeDefinition();

        ProviderApiDefinition providerApi = ProviderApiDefinition.create("test", 0);

        ProviderRecordType providerRecord = ProviderRecordType.recordWithInternalName("RecordType", "ProviderRecordType", 0,
                providerApi);

        ProviderRecordType providerException = ProviderRecordType.exceptionWithInternalName("ExceptionType",
                "ProviderExceptionType", 1, providerApi);

        ProviderOperation providerOperation = ProviderOperation.withInternalName("operation", "providerOperation",
                providerApi, providerRecord, providerRecord);

        providerOperation.addThrownException(providerException);
        providerApi.finalizeDefinition();

        RevisionHistory revisionHistory = new RevisionHistory(providerApi);
        Set<Integer> supportedRevisions = Collections.singleton(0);

        DefinitionResolution resolution = new DefinitionResolver().resolveConsumerDefinition(revisionHistory,
                supportedRevisions, consumerApi);

        String expectedResolution = "ExceptionType(ConsumerExceptionType) -> ProviderExceptionType@revision 0\n" +
                "RecordType(ConsumerRecordType) -> ProviderRecordType@revision 0\n" +
                "operation(consumerOperation) -> operation(providerOperation)\n";

        assertEquals(expectedResolution, new DefinitionResolutionPrinter().printDefinitionResolution(resolution));
    }

    /**
     * Test case: Map list fields.
     */
    @Test
    void mapListFields() {
        // Create a consumer API with an bounded and unbounded list field
        ConsumerApiDefinition consumerApi = ConsumerApiDefinition.create("test", 0);
        ConsumerRecordType consumerRecordA = ConsumerRecordType.createRecordType("A", 0, consumerApi);
        ConsumerRecordType consumerRecordB = ConsumerRecordType.createRecordType("B", 1, consumerApi);
        ConsumerField.create("boundedListField", consumerRecordB, ListType.bounded(consumerRecordA, 10), Optionality.MANDATORY);
        ConsumerField.create("unboundedListField", consumerRecordB, ListType.unbounded(consumerRecordA), Optionality.MANDATORY);
        consumerApi.finalizeDefinition();
        
        // Create a matching provider API
        ProviderApiDefinition providerApi = ProviderApiDefinition.create("test", 0);
        ProviderRecordType providerRecordA = ProviderRecordType.createRecordType("A", 0, providerApi);
        ProviderRecordType providerRecordB = ProviderRecordType.createRecordType("B", 1, providerApi);
        ProviderField.create("boundedListField", providerRecordB, ListType.bounded(providerRecordA, 10), Optionality.MANDATORY);
        ProviderField.create("unboundedListField", providerRecordB, ListType.unbounded(providerRecordA), Optionality.MANDATORY);
        providerApi.finalizeDefinition();
        
        RevisionHistory revisionHistory = new RevisionHistory(providerApi);
        Set<Integer> supportedRevisions = Collections.singleton(0);

        DefinitionResolution resolution = new DefinitionResolver().resolveConsumerDefinition(revisionHistory,
                supportedRevisions, consumerApi);

        String expectedResolution = "A -> A@revision 0\n" +
                "B -> B@revision 0\n" +
                " boundedListField -> boundedListField@B@revision 0\n" +
                " unboundedListField -> unboundedListField@B@revision 0\n";

        assertEquals(expectedResolution, new DefinitionResolutionPrinter().printDefinitionResolution(resolution));
    }
 
    /**
     * Test case: A mapping of fields with incompatible record types is detected.
     */
    @Test
    void mapIncompatibleRecordTypes() {
        // Construct a provider API with a field of type "C".
        ProviderApiDefinition providerDefinition = ProviderApiDefinition.create("test", 0);
        
        ProviderRecordType providerTypeA = ProviderRecordType.createRecordType("A", 0, providerDefinition);
        ProviderRecordType providerTypeB = ProviderRecordType.createRecordType("B", 1, providerDefinition);
        ProviderRecordType.createRecordType("C", 2, providerDefinition);
        
        ProviderField.create("field", providerTypeA, providerTypeB, Optionality.MANDATORY);
        
        providerDefinition.finalizeDefinition();
        
        // Construct a consumer API with a field of type "B".
        ConsumerApiDefinition consumerDefinition = ConsumerApiDefinition.create("test", 0);
        
        ConsumerRecordType consumerTypeA = ConsumerRecordType.createRecordType("A", 0, consumerDefinition);
        ConsumerRecordType.createRecordType("B", 1, consumerDefinition);
        ConsumerRecordType consumerTypeC = ConsumerRecordType.createRecordType("C", 2, consumerDefinition);
        
        ConsumerField.create("field", consumerTypeA, consumerTypeC, Optionality.MANDATORY);
        
        consumerDefinition.finalizeDefinition();
        
        // Try to resolve the revisions against each other
        RevisionHistory revisionHistory = new RevisionHistory(providerDefinition);
        Set<Integer> supportedRevisions = Collections.singleton(0);
        
        DefinitionResolutionException exception = assertThrows(DefinitionResolutionException.class, 
                () -> new DefinitionResolver().resolveConsumerDefinition(revisionHistory, supportedRevisions,
                        consumerDefinition));
        
        assertTrue(exception.getMessage().contains("do not match"));
    }
        
    private DefinitionResolution runOptionalityTest(Optionality providerOptionality, Optionality consumerOptionality, Usage desiredUsage) {
        // Create a provider API definition with a type that is only used as input
        ProviderApiDefinition providerDefinition = ProviderApiDefinition.create("test", 0);
        
        ProviderRecordType providerTypeWithField = ProviderRecordType.createRecordType("A", 0, providerDefinition);
        ProviderField.create("field", providerTypeWithField, AtomicType.INT_32, providerOptionality);
        
        ProviderRecordType providerTypeNoField = ProviderRecordType.createRecordType("B", 1, providerDefinition);
        
        ProviderRecordType providerInputType;
        ProviderRecordType providerOutputType;
        
        switch (desiredUsage) {
        case INPUT:
            providerInputType = providerTypeWithField;
            providerOutputType = providerTypeNoField;
            break;
            
        case OUTPUT:
            providerInputType = providerTypeNoField;
            providerOutputType = providerTypeWithField;
            break;
            
        default:
            providerInputType = providerTypeWithField;
            providerOutputType = providerTypeWithField;
            break;
            
        }
        
        ProviderOperation.create("op", providerDefinition, providerOutputType, providerInputType);
        
        providerDefinition.finalizeDefinition();
        
        // Create a consumer API with a matching type
        ConsumerApiDefinition consumerDefinition = ConsumerApiDefinition.create("test", 0);
        
        ConsumerRecordType consumerTypeWithField = ConsumerRecordType.createRecordType("A", 0, consumerDefinition);
        
        if (consumerOptionality != null) {
            // Do not create a field if no consumer optionality is given
            ConsumerField.create("field", consumerTypeWithField, AtomicType.INT_32, consumerOptionality);
        }
        
        ConsumerRecordType consumerTypeNoField = ConsumerRecordType.createRecordType("B", 1, consumerDefinition);
        
        ConsumerRecordType consumerInputType;
        ConsumerRecordType consumerOutputType;
        
        switch (desiredUsage) {
        case INPUT:
            consumerInputType = consumerTypeWithField;
            consumerOutputType = consumerTypeNoField;
            break;
            
        case OUTPUT:
            consumerInputType = consumerTypeNoField;
            consumerOutputType = consumerTypeWithField;
            break;
            
        default:
            consumerInputType = consumerTypeWithField;
            consumerOutputType = consumerTypeWithField;
            break;
            
        }
        
        ConsumerOperation.create("op", consumerDefinition, consumerOutputType, consumerInputType);
        
        consumerDefinition.finalizeDefinition();
        
        // Try to resolve the consumer definition against the revision history
        RevisionHistory revisionHistory = new RevisionHistory(providerDefinition);
        Set<Integer> supportedRevisions = Collections.singleton(0);
        
        return new DefinitionResolver().resolveConsumerDefinition(revisionHistory, supportedRevisions, consumerDefinition);
    }
    
    private DefinitionResolution runOptionalityTestForInputOnly(Optionality providerOptionality, Optionality consumerOptionality) {
        return this.runOptionalityTest(providerOptionality, consumerOptionality, Usage.INPUT);
    }
    
    /**
     * Test case: Map a mandatory field that is used for input only in different consumer constructs.
     */
    @Test
    void mapMandatoryInputField() {
        DefinitionResolutionException exception;
        
        // The field must be mapped, so not mapping it must fail
        exception = assertThrows(DefinitionResolutionException.class, () -> this.runOptionalityTestForInputOnly(Optionality.MANDATORY, null));
        assertTrue(exception.getMessage().contains("Non-optional field") && exception.getMessage().contains("is not mapped"));
        
        // Mapping mandatory to mandatory must work
        assertNotNull(this.runOptionalityTestForInputOnly(Optionality.MANDATORY, Optionality.MANDATORY));
        // The field cannot be considered optional-for-input
        exception = assertThrows(DefinitionResolutionException.class, () -> this.runOptionalityTestForInputOnly(Optionality.MANDATORY, Optionality.OPT_IN));
        assertTrue(exception.getMessage().contains("are not compatible"));
        // Same for fully optional
        exception = assertThrows(DefinitionResolutionException.class, () -> this.runOptionalityTestForInputOnly(Optionality.MANDATORY, Optionality.OPTIONAL));
        assertTrue(exception.getMessage().contains("are not compatible"));
    }
    
    /**
     * Test case: Map an opt-in field that is used for input only in different consumer constructs.
     */
    @Test
    void mapOptInInputField() {
        // The field is opt-in, so it does not have to be mapped
        assertNotNull(this.runOptionalityTestForInputOnly(Optionality.OPT_IN, null));
        
        // The field may be considered mandatory by the consumer
        assertNotNull(this.runOptionalityTestForInputOnly(Optionality.OPT_IN, Optionality.MANDATORY));
        // The field can be considered optional-for-input
        assertNotNull(this.runOptionalityTestForInputOnly(Optionality.OPT_IN, Optionality.OPT_IN));
        // Same for fully optional
        assertNotNull(this.runOptionalityTestForInputOnly(Optionality.OPT_IN, Optionality.OPTIONAL));
    }
    
    /**
     * Test case: Map an optional field that is used for input only in different consumer constructs.
     */
    @Test
    void mapOptionalInputField() {
        // The field is optional, so it does not have to be mapped
        assertNotNull(this.runOptionalityTestForInputOnly(Optionality.OPTIONAL, null));
        
        // The field may be considered mandatory by the consumer
        assertNotNull(this.runOptionalityTestForInputOnly(Optionality.OPTIONAL, Optionality.MANDATORY));
        // The field can be considered optional-for-input
        assertNotNull(this.runOptionalityTestForInputOnly(Optionality.OPTIONAL, Optionality.OPT_IN));
        // Same for fully optional
        assertNotNull(this.runOptionalityTestForInputOnly(Optionality.OPTIONAL, Optionality.OPTIONAL));
    }
    
    private DefinitionResolution runOptionalityTestForOutputOnly(Optionality providerOptionality, Optionality consumerOptionality) {
        return this.runOptionalityTest(providerOptionality, consumerOptionality, Usage.OUTPUT);
    }
    
    /**
     * Test case: Map a mandatory field that is used for output only in different consumer constructs.
     */
    @Test
    void mapMandatoryOutputField() {
        // Output-only fields do not have to be mapped, even if they are marked as mandatory by the provider
        assertNotNull(this.runOptionalityTestForOutputOnly(Optionality.MANDATORY, null));
        
        // Mapping mandatory to mandatory must work
        assertNotNull(this.runOptionalityTestForOutputOnly(Optionality.MANDATORY, Optionality.MANDATORY));
        // The field can be considered optional-for-input by the consumer, which has no effect for output-only fields
        assertNotNull(this.runOptionalityTestForOutputOnly(Optionality.MANDATORY, Optionality.OPT_IN));
        // Same for fully optional
        assertNotNull(this.runOptionalityTestForOutputOnly(Optionality.MANDATORY, Optionality.OPTIONAL));
    }
    
    /**
     * Test case: Map an opt-in field that is used for output only in different consumer constructs.
     */
    @Test
    void mapOptInOutputField() {
        // Output-only fields do not have to be mapped
        assertNotNull(this.runOptionalityTestForOutputOnly(Optionality.OPT_IN, null));
        
        // The field may be considered mandatory by the consumer
        assertNotNull(this.runOptionalityTestForOutputOnly(Optionality.OPT_IN, Optionality.MANDATORY));
        // Same for optional-as-input
        assertNotNull(this.runOptionalityTestForOutputOnly(Optionality.OPT_IN, Optionality.OPT_IN));
        // Same for fully optional
        assertNotNull(this.runOptionalityTestForOutputOnly(Optionality.OPT_IN, Optionality.OPTIONAL));
    }
    
    /**
     * Test case: Map an optional field that is used for output only in different consumer constructs.
     */
    @Test
    void mapOptionalOutputField() {
        DefinitionResolutionException exception;
        
        // Output-only fields do not have to be mapped
        assertNotNull(this.runOptionalityTestForOutputOnly(Optionality.OPTIONAL, null));
        
        // Optional fields can only be considered optional by the consumer
        exception = assertThrows(DefinitionResolutionException.class, () -> this.runOptionalityTestForOutputOnly(Optionality.OPTIONAL, Optionality.MANDATORY));
        assertTrue(exception.getMessage().contains("are not compatible"));        
        // Same for optional-for-input
        exception = assertThrows(DefinitionResolutionException.class, () -> this.runOptionalityTestForOutputOnly(Optionality.OPTIONAL, Optionality.OPT_IN));
        assertTrue(exception.getMessage().contains("are not compatible"));
        // Optional must work
        assertNotNull(this.runOptionalityTestForOutputOnly(Optionality.OPTIONAL, Optionality.OPTIONAL));
    }
    
    private DefinitionResolution runOptionalityTestForInOut(Optionality providerOptionality, Optionality consumerOptionality) {
        return this.runOptionalityTest(providerOptionality, consumerOptionality, Usage.IN_OUT);
    }
    
    /**
     * Test case: Map a mandatory field that is used for both input and output in different consumer constructs.
     */
    @Test
    void mapMandatoryInOutField() {
        DefinitionResolutionException exception;
        
        // Mandatory fields must be mapped        
        exception = assertThrows(DefinitionResolutionException.class, () -> this.runOptionalityTestForInOut(Optionality.MANDATORY, null));
        assertTrue(exception.getMessage().contains("Non-optional field") && exception.getMessage().contains("is not mapped"));
        
        // Mapping mandatory to mandatory must work
        assertNotNull(this.runOptionalityTestForInOut(Optionality.MANDATORY, Optionality.MANDATORY));
        // No optionality is allowed for mandatory in-out fields
        exception = assertThrows(DefinitionResolutionException.class, () -> this.runOptionalityTestForInOut(Optionality.MANDATORY, Optionality.OPT_IN));
        assertTrue(exception.getMessage().contains("are not compatible"));
        // Same for fully optional
        exception = assertThrows(DefinitionResolutionException.class, () -> this.runOptionalityTestForInOut(Optionality.MANDATORY, Optionality.OPTIONAL));
        assertTrue(exception.getMessage().contains("are not compatible"));
    }
    
    /**
     * Test case: Map an opt-in field that is used for both input and output in different consumer constructs.
     */
    @Test
    void mapOptInInOutField() {
        // Opt-in fields do not have to be mapped, since the consumer does not have to provide it and is free to ignore it
        assertNotNull(this.runOptionalityTestForInOut(Optionality.OPT_IN, null));
        
        // The field may be considered mandatory by the consumer
        assertNotNull(this.runOptionalityTestForInOut(Optionality.OPT_IN, Optionality.MANDATORY));
        // Same for optional-as-input
        assertNotNull(this.runOptionalityTestForInOut(Optionality.OPT_IN, Optionality.OPT_IN));
        // Same for fully optional
        assertNotNull(this.runOptionalityTestForInOut(Optionality.OPT_IN, Optionality.OPTIONAL));
    }
    
    /**
     * Test case: Map an optional field that is used for both input and output in different consumer constructs.
     */
    @Test
    void mapOptionalInOutField() {
        DefinitionResolutionException exception;
        
        // Fully optional fields do not have to be mapped
        assertNotNull(this.runOptionalityTestForInOut(Optionality.OPTIONAL, null));
        
        // Optional fields can only be considered optional by the consumer
        exception = assertThrows(DefinitionResolutionException.class, () -> this.runOptionalityTestForInOut(Optionality.OPTIONAL, Optionality.MANDATORY));
        assertTrue(exception.getMessage().contains("are not compatible"));        
        // Same for optional-for-input
        exception = assertThrows(DefinitionResolutionException.class, () -> this.runOptionalityTestForInOut(Optionality.OPTIONAL, Optionality.OPT_IN));
        assertTrue(exception.getMessage().contains("are not compatible"));
        // Optional must work
        assertNotNull(this.runOptionalityTestForInOut(Optionality.OPTIONAL, Optionality.OPTIONAL));
    }
    
}
