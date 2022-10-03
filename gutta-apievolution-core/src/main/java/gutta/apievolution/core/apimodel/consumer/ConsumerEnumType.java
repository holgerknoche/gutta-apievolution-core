package gutta.apievolution.core.apimodel.consumer;

import gutta.apievolution.core.apimodel.EnumType;

/**
 * Consumer-specific implementation of an {@link EnumType}.
 */
public class ConsumerEnumType extends EnumType<ConsumerApiDefinition, ConsumerEnumType, ConsumerEnumMember>
        implements ConsumerUserDefinedType {

    /**
     * Creates a simple enum type where the public name equals the internal name.
     * @param publicName The enum type's public name
     * @param typeId The enum type's type id
     * @param owner The API definition that owns this enum type
     * @return The created enum type
     */
    public static ConsumerEnumType create(String publicName, int typeId, ConsumerApiDefinition owner) {
        return new ConsumerEnumType(publicName, null, typeId, owner);
    }
    
    /**
     * Creates a new enum type from the given data.
     *
     * @param publicName   The enum type's public name
     * @param internalName The enum type's internal name, if any. If {@code null} the
     *                     public name is assumed
     * @param typeId       The enum type's type id
     * @param owner        The API definition that owns this enum type
     */
    public ConsumerEnumType(final String publicName, final String internalName, final int typeId,
            final ConsumerApiDefinition owner) {
        super(publicName, internalName, typeId, owner);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        } else if (that instanceof ConsumerEnumType) {
            return this.stateEquals((ConsumerEnumType) that);
        } else {
            return false;
        }
    }

    boolean stateEquals(ConsumerEnumType that) {
        return super.stateEquals(that);
    }

    @Override
    public <R> R accept(ConsumerApiDefinitionElementVisitor<R> visitor) {
        return visitor.handleConsumerEnumType(this);
    }

}
