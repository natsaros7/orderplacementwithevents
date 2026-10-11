package org.poc.orderplacement.events.order.web;

import org.poc.orderplacement.events.order.OrderNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class OrderWebExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleOrderNotFound(final OrderNotFoundException exception) {
        final ModelAndView modelAndView = new ModelAndView("error/404");
        modelAndView.addObject("message", exception.getMessage());
        return modelAndView;
    }
}
