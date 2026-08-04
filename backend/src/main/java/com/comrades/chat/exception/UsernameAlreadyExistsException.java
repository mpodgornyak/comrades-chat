package com.comrades.chat.exception;

public class UsernameAlreadyExistsException extends RuntimeException{
    public UsernameAlreadyExistsException(String msg){
        super(msg);
    }
}
