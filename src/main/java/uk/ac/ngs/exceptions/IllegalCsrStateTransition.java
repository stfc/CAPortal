/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.ngs.exceptions;

/**
 * Thrown when a CSR request update requests an illegal state transition. 
 * @author David Meredith
 */
public class IllegalCsrStateTransition extends Exception {

    public IllegalCsrStateTransition(String message) {
        super(message);
    }

    public IllegalCsrStateTransition(String message, Throwable throwable) {
        super(message, throwable);
    }
}
