package net.chilon.matt.teacup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class AlbumArtFactory {

    private static final int ART_WIDTH = 72;
    private static final int ART_HEIGHT = 72;
	
    // code below from android tutorial (more or less):
    // https://developer.android.com/training/displaying-bitmaps/load-bitmap.html

    public static Bitmap readFile(File file) {
        String path = file.getPath();
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = calculateInSampleSize(options);
        options.inJustDecodeBounds = false;
        options.inPurgeable = true;
        return BitmapFactory.decodeFile(path, options);
    }

    public static Bitmap readBytes(byte[] data) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length);
        options.inSampleSize = calculateInSampleSize(options);
        options.inJustDecodeBounds = false;
        options.inPurgeable = true;
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }
    

    public static Bitmap readUrl(String url) {
    	Bitmap artBmp = null;
    	
    	try {
    		URLConnection ucon = new URL(url).openConnection();
    		InputStream is = ucon.getInputStream();
    
    		final BitmapFactory.Options options = new BitmapFactory.Options();
    		options.inJustDecodeBounds = true;
    		BitmapFactory.decodeStream(is);
    		options.inSampleSize = calculateInSampleSize(options);
    		options.inJustDecodeBounds = false;
    		options.inPurgeable = true;
    		artBmp = BitmapFactory.decodeStream(is);
    	} catch (IOException e) {
    		// do nothing
    	}
    	
    	return artBmp;
    }
    
    

    private static int calculateInSampleSize(BitmapFactory.Options options) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > ART_HEIGHT || width > ART_WIDTH) {
            if (width > height) {
                inSampleSize = Math.round((float)height / (float)ART_WIDTH);
            } else {
                inSampleSize = Math.round((float)width / (float)ART_HEIGHT);
            }
        }

        return inSampleSize;
    }

}