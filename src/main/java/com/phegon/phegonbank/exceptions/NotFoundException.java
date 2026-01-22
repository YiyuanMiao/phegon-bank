package com.phegon.phegonbank.exceptions;

import java.io.Serializable;

public class NotFoundException extends RuntimeException{
    public NotFoundException(String error) {
        super(error);

    }

}
