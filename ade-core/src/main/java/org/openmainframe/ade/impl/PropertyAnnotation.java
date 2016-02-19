/*
 
    Copyright IBM Corp. 2008, 2016
    This file is part of Anomaly Detection Engine for Linux Logs (ADE).

    ADE is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ADE is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ADE.  If not, see <http://www.gnu.org/licenses/>.
 
*/
package org.openmainframe.ade.impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.reflect.TypeUtils;

public class PropertyAnnotation {


    public static interface IPropertyFactory<T> {
        public T create(Object propVal);
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Property {
        String key();

        boolean required() default true;

        Class<? extends IPropertyFactory<?>> factory() default NULL_PROPERTY_FACTORY.class;

        static final class NULL_PROPERTY_FACTORY implements IPropertyFactory<Object> {
            @Override
            public final Object create(Object propVal) {
                throw new UnsupportedOperationException("Dummy NULL class!");
            }
        }

        String help();
    }

    public static class MissingPropertyException extends Exception {
        private static final long serialVersionUID = 1L;

        public MissingPropertyException(String msg) {
            super(msg);
        }

        public MissingPropertyException(Throwable t) {
            super(t);
        }

        public MissingPropertyException(String msg, Throwable t) {
            super(msg, t);
        }

    }

    static public void setProps(Object obj, Map<String, ? extends Object> props) throws MissingPropertyException, IllegalArgumentException {
        setProps(obj, props, Pattern.compile(".*"));
    }

    static public void setProps(Object obj, Map<String, ? extends Object> props, Pattern filter) throws MissingPropertyException, IllegalArgumentException {
        setProps(obj, props, filter, true);
    }

    static public void setPropsUnsafe(Object obj, Map<String, ? extends Object> props, Pattern filter) throws MissingPropertyException, IllegalArgumentException {
        setProps(obj, props, filter, false);
    }

    static public Map<String, String> propsToMap(Properties props) {
        final Map<String, String> map = new TreeMap<String, String>();
        for (Entry<Object, Object> entry : props.entrySet()) {
            final String key = (String) entry.getKey();
            final String value = (String) entry.getValue();
            map.put(key, value);
        }

        return map;
    }

    @SuppressWarnings({ "unchecked" })
    static private void setProps(Object obj, Map<String, ? extends Object> props, Pattern filter, boolean safe) throws MissingPropertyException, IllegalArgumentException {
        final Class<?> annotatedClass = obj.getClass();
        final Set<String> keyset = new TreeSet<String>(props.keySet());

        for (Field field : annotatedClass.getDeclaredFields()) {
            final Property annos = field.getAnnotation(Property.class);
            if (annos != null) {
                // skip missing and non-required properties
                final String key = annos.key();
                if (!props.containsKey(key)) {
                    if (annos.required()) {
                        throw new MissingPropertyException("Missing property: " + key);
                    } else {
                        // no value for non-required property
                        continue;
                    }
                }

                final Class<? extends IPropertyFactory<?>> factoryClass = annos.factory();

                final Object rawVal = props.get(key);
                final Type fieldType = field.getGenericType();
                Object val = null;
                if (factoryClass != Property.NULL_PROPERTY_FACTORY.class) {
                    // check if this factory is eligible for creating this property
                    final Type factoryProductType = resolveActualTypeArgs(factoryClass, IPropertyFactory.class)[0];
                    if (!TypeUtils.isAssignable(factoryProductType, fieldType)) {
                        throw new IllegalArgumentException("The factory provided for the field: " + field.getName() + " is not compatible for creating object of type: " + fieldType);
                    }

                    Constructor<? extends IPropertyFactory<?>> constructor;
                    try {
                        constructor = factoryClass.getConstructor();
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Missing empty constructor in: " + factoryClass.getName(), e);
                    }

                    IPropertyFactory<?> factory;
                    try {
                        factory = constructor.newInstance();
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Failed instantiating: " + factoryClass.getName(), e);
                    }

                    try {
                        val = factory.create(rawVal);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Failed extractring property value: " + key, e);
                    }
                } else if (TypeUtils.isAssignable(rawVal.getClass(), fieldType)) {
                    val = rawVal;
                } else if (rawVal.getClass().equals(String.class)) {
                    final Class<?> fieldClass = field.getType();
                    final String stringVal = (String) rawVal;
                    if (fieldClass == Integer.class || fieldClass == int.class) {
                        try {
                            val = Integer.parseInt(stringVal);
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Failed parsing integer value for property: " + key, e);
                        }
                    } else if (fieldClass == Double.class || fieldClass == double.class) {
                        try {
                            val = Double.parseDouble(stringVal);
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Failed parsing double value for property: " + key, e);
                        }
                    } else if (fieldClass == Boolean.class || fieldClass == boolean.class) {
                        try {
                            val = Boolean.parseBoolean(stringVal);
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Failed parsing boolean value for property: " + key, e);
                        }
                    } else if (fieldClass == String.class) {
                        // should never have reached here, since String is assignable from String
                        val = stringVal;
                    } else if (fieldClass.isEnum()) {
                        Class<Enum> fieldEnum;
                        try {
                            fieldEnum = (Class<Enum>) fieldClass;
                        } catch (ClassCastException e) {
                            throw new IllegalArgumentException("Failed casting to Class<Enum> field class: " + fieldClass.getName(), e);
                        }
                        try {
                            val = Enum.valueOf(fieldEnum, stringVal);
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Failed parsing enum value for property: " + key + "\n\t possible values: " + Arrays.toString(fieldEnum.getEnumConstants()), e);
                        }
                    } else {
                        // try to find String constructor for field, or else throw exception
                        Constructor<?> constructor;
                        try {
                            constructor = fieldClass.getConstructor(String.class);
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Field: " + field.getName() + " of type " + fieldClass + " is not one of the known property type (Integer, Double, Boolean, String, Enum), does not have a String constructor and no custom factory is defined in the annotation!", e);
                        }
                        try {
                            val = constructor.newInstance(stringVal);
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Could not create a new instance for " + field.getName() + " using the String constructor for type: " + fieldClass, e);
                        }
                    }
                }

                if (val == null) {
                    throw new IllegalArgumentException("For the key " + key + ", we expect the value to be either assignable to " + fieldType + " or a String");
                }

                try {
                    field.setAccessible(true);
                    field.set(obj, val);
                    keyset.remove(key);
                } catch (SecurityException e) {
                    throw new SecurityException("Field " + field.getName() + " is not accesible, and could not be set as accesible (probably due to PermissionManager)", e);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Failed setting field: " + field.getName() + " with value: " + val, e);
                }
            }
        }
        if (safe && !keyset.isEmpty()) {
            throw new IllegalArgumentException("Unrecongnized arguments in the properties: " + keyset.toString());
        }
    }

    static public String getHelp(Object obj) {
        final StringBuilder helpString = new StringBuilder();
        final Class<?> annotatedClass = obj.getClass();
        for (Field field : annotatedClass.getDeclaredFields()) {
            final Property annos = field.getAnnotation(Property.class);
            if (annos != null) {
                helpString.append("name=" + annos.key() + ", " + "type =" + field.getClass().getSimpleName() + (annos.required() ? " required " : " ") + ": " + annos.help() + "\n");
            }
        }
        return helpString.toString();
    }


    public abstract static class PropertyFactoryByString<T> implements IPropertyFactory<T> {

        @Override
        public final T create(Object propVal) {
            if (!(propVal instanceof String)) {
                throw new IllegalArgumentException("Propery value must be of type String");
            }
            return create((String) propVal);
        }

        public abstract T create(String propStrVal);

    }

    public abstract static class ClassPropertyFactory<T> extends PropertyFactoryByString<Class<? extends T>> {
        private final Class<T> m_implementedClass;

        public ClassPropertyFactory(Class<T> implementedClass) {
            m_implementedClass = implementedClass;
        }

        @Override
        public final Class<? extends T> create(String propVal) {
            Class<?> rawClass = null;
            try {
                rawClass = Class.forName((String) propVal);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }

            if (m_implementedClass.getClass().isAssignableFrom(rawClass)) {
                throw new IllegalArgumentException(String.format("provided class: %s does not implement the interface %s: ", rawClass, m_implementedClass.getClass()));
            }
            @SuppressWarnings("unchecked")
            final Class<? extends T> res = (Class<? extends T>) rawClass;

            return res;
        }
    }

    /**
     * Find out what are the concrete classes used in an offspring class for the generic placeholders of a base class/interface
     * 
     * @param <T> base class type
     * @param offspring class or interface subclassing or extending the base class
     * @param base class with generic arguments
     * @param actualArgs the actual type arguments passed to the offspring class (omit unless useful)
     * @return actual generic type arguments, must match the type parameters of the offspring class. If omitted, the
     * type parameters will be used instead.
     */
    @SuppressWarnings("unchecked")
    public static <T> Type[] resolveActualTypeArgs(Class<? extends T> offspring, Class<T> base, Type... actualArgs) {

        //  If actual types are omitted, the type parameters will be used instead.
        if (actualArgs.length == 0) {
            actualArgs = offspring.getTypeParameters();
        }
        // map generic parameters into the actual types
        final Map<String, Type> genericVariables = new TreeMap<String, Type>();
        for (int i = 0; i < actualArgs.length; i++) {
            final TypeVariable<?> typeVariable = (TypeVariable<?>) offspring.getTypeParameters()[i];
            genericVariables.put(typeVariable.getName(), actualArgs[i]);
        }

        // Find direct ancestors (superclass, interfaces)
        final List<Type> ancestors = new LinkedList<Type>();
        if (offspring.getGenericSuperclass() != null) {
            ancestors.add(offspring.getGenericSuperclass());
        }
        for (Type t : offspring.getGenericInterfaces()) {
            ancestors.add(t);
        }

        // Recurse into ancestors (superclass, interfaces)
        for (Type type : ancestors) {
            if (type instanceof Class<?>) {
                // ancestor is non-parameterized. Recurse only if it matches the base class.
                final Class<?> ancestorClass = (Class<?>) type;
                if (base.isAssignableFrom(ancestorClass)) {
                    final Type[] result = resolveActualTypeArgs((Class<? extends T>) ancestorClass, base);
                    if (result != null) {
                        return result;
                    }
                }
            }
            if (type instanceof ParameterizedType) {
                // ancestor is parameterized. Recurse only if the raw type matches the base class.
                final ParameterizedType parameterizedType = (ParameterizedType) type;
                final Type rawType = parameterizedType.getRawType();
                if (rawType instanceof Class<?>) {
                    final Class<?> rawTypeClass = (Class<?>) rawType;
                    if (base.isAssignableFrom(rawTypeClass)) {

                        // loop through all type arguments and replace type variables with the actually known types
                        final List<Type> resolvedTypes = new LinkedList<Type>();
                        for (Type t : parameterizedType.getActualTypeArguments()) {
                            if (t instanceof TypeVariable<?>) {
                                final Type resolvedType = genericVariables.get(((TypeVariable<?>) t).getName());
                                resolvedTypes.add(resolvedType != null ? resolvedType : t);
                            } else if (t instanceof ParameterizedType) {
                                final ParameterizedType pType = (ParameterizedType) t;
                                final Type resolvedPType = new ResolvedParameterizedType(pType, genericVariables);
                                resolvedTypes.add(resolvedPType);
                            } else {
                                resolvedTypes.add(t);
                            }
                        }

                        final Type[] result = resolveActualTypeArgs((Class<? extends T>) rawTypeClass, base, resolvedTypes.toArray(new Type[] {}));
                        if (result != null) {
                            return result;
                        }
                    }
                }
            }
        }

        // we have a result if we reached the base class.
        return offspring.equals(base) ? actualArgs : null;
    }

    private static final class ResolvedParameterizedType implements ParameterizedType {
        private final ParameterizedType m_pType;
        private final Map<String, Type> m_typeVariables;

        private ResolvedParameterizedType(ParameterizedType pType,
                Map<String, Type> typeVariables) {
            this.m_pType = pType;
            this.m_typeVariables = typeVariables;
        }

        @Override
        public Type getRawType() {
            return this.m_pType.getRawType();
        }

        @Override
        public Type getOwnerType() {
            return this.m_pType.getOwnerType();
        }

        @Override
        public Type[] getActualTypeArguments() {
            final Type[] resolvedTypes = new Type[this.m_pType.getActualTypeArguments().length];
            for (int i = 0; i < this.m_pType.getActualTypeArguments().length; i++) {
                final Type actualType = this.m_pType.getActualTypeArguments()[i];
                if (actualType instanceof WildcardType) {
                    final WildcardType actualWildcardType = (WildcardType) actualType;
                    final Type resolvedType = new WildcardType() {
                        @Override
                        public Type[] getUpperBounds() {
                            final Type[] resolvedUpperBounds = new Type[actualWildcardType.getUpperBounds().length];
                            for (int j = 0; j < actualWildcardType.getUpperBounds().length; j++) {
                                resolvedUpperBounds[j] = m_typeVariables.get(((TypeVariable<?>) actualWildcardType.getUpperBounds()[j]).getName());
                            }
                            return resolvedUpperBounds;
                        }

                        @Override
                        public Type[] getLowerBounds() {
                            final Type[] resolvedLowerBounds = new Type[actualWildcardType.getLowerBounds().length];
                            for (int j = 0; j < actualWildcardType.getLowerBounds().length; j++) {
                                resolvedLowerBounds[j] = m_typeVariables.get(((TypeVariable<?>) actualWildcardType.getLowerBounds()[j]).getName());
                            }
                            return resolvedLowerBounds;
                        }
                    };
                    resolvedTypes[i] = resolvedType;
                } else {
                    throw new UnsupportedOperationException("Currently only WildcardType is supported");
                }
            }
            return resolvedTypes;
        }
    }
}
