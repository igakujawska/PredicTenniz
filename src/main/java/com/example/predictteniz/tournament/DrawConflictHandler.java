package com.example.predictteniz.tournament;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class DrawConflictHandler {
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail conflict() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Turniej został zmieniony w innym oknie. Otwórz go ponownie albo zapisz kopię.");
    }
}
