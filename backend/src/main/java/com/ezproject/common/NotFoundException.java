package com.ezproject.common;

/** Ressource introuvable : traduite en HTTP 404 sans détail technique. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String resource, Object id) {
        super(resource + " introuvable : " + id);
    }
}
