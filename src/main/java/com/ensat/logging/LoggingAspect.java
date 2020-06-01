package com.ensat.logging;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
@Aspect
public class LoggingAspect {

    //Setup Logger
    private Logger myLogger = Logger.getLogger(getClass().getName());

    //setup pointcut declaration
    @Pointcut("execution(* com.ensat.controllers.*.*(..))")
    private void forControllerPackage() {
    }

    @Pointcut("execution(* javax.servlet.http.HttpServlet.*(..)) *)")
    private void httppointcut() {
    }

    //add @Before advice
    @Before("forControllerPackage()")
    private void before(JoinPoint theJoinPoint) {
        //Initialize http response
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getResponse();

        //get the executed methode
        String theMethod = theJoinPoint.getSignature().toShortString();

        //get the username
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        //send methode and usern to httpresponse's header
        response.setHeader("NomMethode", theMethod);
        response.setHeader("Username", auth.getName());

    }

    //add @Around advice
    @Around("httppointcut()")
    private Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {

        //Initialize http request
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();

        // duration time of methode
        long initialProcess = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long finalProcess = System.currentTimeMillis();
        long dureProcess = finalProcess - initialProcess;

        //Initialize http response
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getResponse();
        //get methode's name and username from http response's header sended from the @Before advice
        String methode = response.getHeader("NomMethode") == null ? "Indeterminate"
                : response.getHeader("NomMethode");
        String username = response.getHeader("Username") == null ? "Indeterminate"
                : response.getHeader("Username");

        //System.out.println("-------------------------------------------------------------------------------");
        myLogger.info("====>in  calling method: " + methode + " by " + username + " execution time: " + dureProcess + " Address: " + request.getRemoteAddr() + " Port: " + request.getRemotePort() + " endpoint: " + request.getRequestURI() + " Method: " + request.getMethod() + " Status: " + response.getStatus());
        //System.out.println("-------------------------------------------------------------------------------");

        return result;

    }

    //add @afterthrowing advice to log methods which throws exception
    //this advice will EXECUTE WHEN ANY EXECEPTION OCCURED
    @AfterThrowing(pointcut = "forControllerPackage() ||httppointcut()", throwing = "e")
    public void logAfterThrowing(Throwable e) {

        //Initialize http request and response
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getResponse();
        //set status to 500 for methods which throws exception
        response.setStatus(500);

        // get methode's name and username from http response's header sended from the @Before advice and which throws exception
        String methode = response.getHeader("NomMethode") == null ? "Indeterminate"
                : response.getHeader("NomMethode");
        String username = response.getHeader("Username") == null ? "Indeterminate"
                : response.getHeader("Username");

        //System.out.println("............................................................................");
        myLogger.log(Level.SEVERE, "====>in  calling method: " + methode + " by " + username + " Address: " + request.getRemoteAddr() + " Port: " + request.getRemotePort() + " endpoint: " + request.getRequestURI() + " Method: " + request.getMethod() + " Status: " + response.getStatus());
    }

}