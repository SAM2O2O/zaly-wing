package com.akaxin.zaly.wing.executor.chain.handler;

/**
 * 
 * @author Sam{@link an.guoyue254@gmail.com}
 * @since 2018-04-10 16:54:31
 * @param <T>
 * @param <R>
 */
public interface IHandler<T, R> {
	public R handle(T t);
}
