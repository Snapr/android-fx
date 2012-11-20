package nz.co.juliusspencer.android;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class JSAFileUtil {

    public final static long BYTES_PER_MB = 1048576;
    
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * read file stream
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public static ByteArrayOutputStream readFileStream(InputStream sourceStream) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		int count;
		try {
			while ((count = sourceStream.read()) != -1) byteArrayOutputStream.write(count);
			return byteArrayOutputStream;
		} finally {
			sourceStream.close();
		}
	}

}
