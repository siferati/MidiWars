package com.midiwars.util;

/**
 * Holds multiple custom exceptions used throughout this application.
 */
public interface MyExceptions {

    /**
     * Thrown when trying to create a new UserInterface object
     * and another UserInterface already exists.
     */
    class UIAlreadyExists extends RuntimeException {

        /**
         * Returns a new UIAlreadyExists object.
         */
        public UIAlreadyExists() {
            super();
        }
    }

}
