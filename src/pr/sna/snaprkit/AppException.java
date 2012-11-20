package pr.sna.snaprkit;

public class AppException extends Exception {
	private static final long serialVersionUID = -6261133855587580168L;
	
	private boolean silent = false;

	public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable throwable) {
        super(message, throwable);
    }

	public boolean isSilent() {
		return silent;
	}

	public AppException setSilent(boolean silent) {
		this.silent = silent;
		return this;
	}
}
