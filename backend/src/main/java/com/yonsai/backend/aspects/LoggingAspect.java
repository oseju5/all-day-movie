package com.yonsai.backend.aspects;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

/**
 * 로깅 관련 AOP 구성
 *
 * @author : ohseju
 * @fileName : LoggingAspect
 * @since : 2026/03/13
 */

@Aspect
@Component
@Slf4j
public class LoggingAspect {

	
	@Pointcut("execution(* com.yonsai.backend.controller.*.*(..))")
    private void controllerMethods() {}
	
    /**
     * Before: 대상 “메서드”가 실행되기 전에 Advice를 실행합니다.
     *
     * @param joinPoint
     */
	@Before("controllerMethods()")
    public void logBefore(JoinPoint joinPoint) {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        log.info("[ Before ] Running: {}.{}()", className, methodName);
    }
	
    /**
     * After : 대상 “메서드”가 실행된 후에 Advice를 실행합니다.
     *
     * @param joinPoint
     */
	@After("controllerMethods()")
    public void logAfter(JoinPoint joinPoint) {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        log.info("[ After ] Completed: {}.{}()", className, methodName);
    }
	
    /**
     * AfterReturning: 대상 “메서드”가 정상적으로 실행되고 반환된 후에 Advice를 실행합니다.
     *
     * @param joinPoint
     * @param result
     */
	public void logAfterReturning(JoinPoint joinPoint, Object result) {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        log.info("[ AfterReturning ] {}.{}() | Result: {}", className, methodName, result);
    }
	
    /**
     * AfterThrowing: 대상 “메서드에서 예외가 발생”했을 때 Advice를 실행합니다.
     *
     * @param joinPoint
     * @param e
     */
	@AfterThrowing(pointcut = "controllerMethods()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        log.error("[ AfterThrowing ] {}.{}() | Exception: {}", className, methodName, e.getMessage());
    }
	
    /**
     * Around : 대상 “메서드” 실행 전, 후 또는 예외 발생 시에 Advice를 실행합니다.
     *
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("execution(* com.yonsai.backend.controller.*.*(..))")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        
        log.info("[ Around Before ] {}.{}() 시작", className, methodName);
        long start = System.currentTimeMillis();
        
        Object result = joinPoint.proceed();
        
        long executionTime = System.currentTimeMillis() - start;
        log.info("[ Around After ] {}.{}() 종료 (실행 시간: {}ms)", className, methodName, executionTime);
        
        return result;
    }
	
}
