package com.beyt.jdq.util.field.helper;


/**
 * Created by tdilber at 11/17/2020
 */
public class EnumFieldHelper<T extends Enum<T>> implements IFieldHelper<T> {
    private final Class<T> enumType;

    public EnumFieldHelper(Class<T> enumType) {
        this.enumType = enumType;
    }

    @Override
    public T fillRandom() {
        T[] enumConstants = enumType.getEnumConstants();
        return enumConstants[random.nextInt(enumConstants.length)];
    }

    @Override
    public T fillValue(String value) {
        T[] enumConstants = enumType.getEnumConstants();
        for (T enumConstant : enumConstants) {
            if (enumConstant.toString().equalsIgnoreCase(value)) {
                return enumConstant;
            }
        }
        throw new IllegalStateException("Enum Type: " + enumType.getSimpleName() + "  value: " + value + " not matched!");
    }

    @Override
    public String createGeneratorCode(String value) {
        T[] enumConstants = enumType.getEnumConstants();
        for (T enumConstant : enumConstants) {
            if (enumConstant.toString().equalsIgnoreCase(value)) {
                return enumType.getSimpleName() + "." + enumConstant.name();
            }
        }

        throw new IllegalStateException("Enum Type: " + enumType.getSimpleName() + "  value: " + value + " not matched!");
    }
}
