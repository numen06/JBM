package jbm.framework.boot.autoconfigure.feign;

import lombok.Getter;

@Getter
public class RemoteServiceException extends RuntimeException {
    private final Integer code;
    private final String remoteUrl;

    public RemoteServiceException(Integer code, String message, String remoteUrl) {
        super(message);
        this.code = code;
        this.remoteUrl = remoteUrl;
    }

    public RemoteServiceException(int httpStatus, String message, String remoteUrl) {
        this(Integer.valueOf(httpStatus), message, remoteUrl);
    }

    public RemoteServiceException(String message, Throwable cause) {
        super(message, cause);
        this.code = null;
        this.remoteUrl = null;
    }
}
