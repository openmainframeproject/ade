/*
 
    Copyright IBM Corp. 2010, 2016
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
package org.openmainframe.ade.utils;

/**
 * abstract class to help using lazy instances of type T
 * @param <T> the type of the lazy instantiated object
 */
public abstract class LazyObj<T> {

    /**
     * An exception to indicate that the process of creating an object failed.
     * Note that it inherits from {@link RuntimeException}, so it will be passed 
     * up the call stack unless explicitly caught. 
     */
    public static class ObjectCreationException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        /** @see {@link RuntimeException#RuntimeException()} */
        public ObjectCreationException() {
            super();
        }

        /** @see {@link RuntimeException#RuntimeException(String)} */
        public ObjectCreationException(String msg) {
            super(msg);
        }

        /** @see {@link RuntimeException#RuntimeException(Throwable)} */
        public ObjectCreationException(Throwable t) {
            super(t);
        }

        /** @see {@link RuntimeException#RuntimeException(String, Throwable)} */
        public ObjectCreationException(String msg, Throwable t) {
            super(msg, t);
        }
    }

    /**
     * the contained instance, initialized to null
     */
    private T m_lazyInst = null;

    /**
     * Used to obtain the contained object. Only on the first call to this method
     * will the contained object be created.<br/> <b>Note:</b> the thrown exception
     * inherits from {@link RuntimeException} and will be passed up the stack trace
     * unless explicitly caught!
     * @return the lazy instantiated object 
     * @throws ObjectCreationException if failed to create the object
     */
    public T get() throws ObjectCreationException {
        if (m_lazyInst == null) {
            m_lazyInst = create();
        }
        return m_lazyInst;
    }

    /**
     * Recreate the contained object.
     * @return the recreated object
     * @throws ObjectCreationException if failed to create the object
     */
    public T refresh() throws ObjectCreationException {
        m_lazyInst = create();
        return m_lazyInst;
    }

    /**
     * Creates a new instance of the object
     * @return a newly created object
     * @throws ObjectCreationException
     */
    protected abstract T create() throws ObjectCreationException;

}
