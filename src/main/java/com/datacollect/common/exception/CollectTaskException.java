package com.datacollect.common.exception;

/**
 * 采集任务异常类
 * 
 * @author system
 * @since 2024-01-01
 */
public class CollectTaskException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 构造函数
     */
    public CollectTaskException() {
        super();
    }

    /**
     * 构造函数
     * 
     * @param message 错误消息
     */
    public CollectTaskException(String message) {
        super(message);
    }

    /**
     * 构造函数
     * 
     * @param message 错误消息
     * @param cause 原因
     */
    public CollectTaskException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造函数
     * 
     * @param cause 原因
     */
    public CollectTaskException(Throwable cause) {
        super(cause);
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message 错误消息
     */
    public CollectTaskException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message 错误消息
     * @param cause 原因
     */
    public CollectTaskException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 获取错误码
     * 
     * @return 错误码
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 设置错误码
     * 
     * @param errorCode 错误码
     */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}
