package net.chilon.matt.teacup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.MediaStore;
import android.util.Log;

public class LastFM {
	
	public static final int PREFETCH_OK = 0;
	public static final int PREFETCH_NOCONNECTION = 1;
	public static final int PREFETCH_CANCELLED = 2;
	
	private static final int URL_TIMEOUT = 5000;
	
	private static final String API_KEY = "d6e802774ce70edfca5d501009377a53";
	private static final String API_ROOT = "http://ws.audioscrobbler.com/2.0/";
	private static final String GET_ALBUM_INFO = "method=album.getinfo";
	private static final String API_KEY_ARG = "api_key=" + API_KEY;
	private static final String ARTIST_ARG = "artist=%s";
	private static final String ALBUM_ARG = "album=%s";
	
	private static final String IMAGE_TAG = "image";
	private static final String IMAGE_SIZE = "large";
	private static final String SIZE_NAMESPACE = "";
	private static final String SIZE_ATTRIBUTE = "size";
	
	private static final String URL_TEMPLATE = API_ROOT + "?" +
	                                           GET_ALBUM_INFO + "&" +
	                                           API_KEY_ARG + "&" +
	                                           ARTIST_ARG + "&" +
	                                           ALBUM_ARG;

	private static final Bitmap.CompressFormat CACHE_TYPE = Bitmap.CompressFormat.PNG;
	private static final String CACHE_EXT = ".png";
	
	public static Bitmap getArt(Context context,
			                    Config config, 
			                    String artist, 
			                    String album,
			                    String filename) {
		Bitmap artBmp = null;
		
		if (config.getLastFMCacheStyle() != Config.LASTFM_NO_CACHE)
			artBmp = getCachedArt(config, artist, album, filename);
				
		if (artBmp == null) {
			artBmp = getWebArt(context, config, artist, album, filename);
			if (artBmp != null)
				cacheBitmap(config, artist, album, filename, artBmp);
		}
			
		return artBmp;
	}
	
	public static int prefetchArt(Context context, 
			                      Config config, 
			                      ProgressUpdater progress) {
		if (shouldConnect(config,  context)) {
			String projection[] = {
				MediaStore.Audio.Media.ARTIST,
				MediaStore.Audio.Media.ALBUM,
				MediaStore.Audio.Media.DATA
			};
			String selection = "";
			ContentResolver resolver = context.getContentResolver();
			Cursor result = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
					                       projection,
	                                       selection,
	                                       null,
	                                       null);
	        
			int count = result.getCount();
			int done = 0;
			result.moveToFirst();
			while (!result.isAfterLast() && !progress.getCancelled()) {
				String artist = result.getString(0);
				String album = result.getString(1);
				String filename = result.getString(2);
	    	
				getArt(context, config, artist, album, filename);
	    	
				++done;
				progress.setProgressPercent((100*done)/count);
	    	
				result.moveToNext();
			}
			
			if (progress.getCancelled())
				return PREFETCH_CANCELLED;
			else 
				return PREFETCH_OK;
		} else {
			return PREFETCH_NOCONNECTION;
		}
	}
	
	private static void cacheBitmap(Config config,
			                        String artist,
			                        String album,
			                        String filename,
			                        Bitmap artBmp) {
		try {
			String directory = getCacheDir(config, filename);
			File dir = new File(directory);
			if (!dir.exists())
				dir.mkdirs();
			String imageName = directory +
					           File.separator +
				               getCacheFileName(artist, album);
			File image = new File(imageName);
			if (!image.exists()) {
				FileOutputStream os = new FileOutputStream(image);

				artBmp.compress(CACHE_TYPE, 85, os);
				os.flush();
				os.close();
			}
		} catch (FileNotFoundException e) {
			Log.e("TeaCup", "filenotfoundexception: " + e.toString());
		} catch (IOException e) {
			Log.e("TeaCup", "ioexception: " + e.toString());
		}
	}
	
	private static Bitmap getCachedArt(Config config, 
			                           String artist, 
			                           String album,
			                           String filename) {
		String imageName = getCacheDir(config,  filename) +
				           File.separator +
				           getCacheFileName(artist, album);
		File image = new File(imageName);
		if (image.exists())
			return AlbumArtFactory.readFile(image);
		else
			return null;
	}
	
	private static String getCacheDir(Config config, String filename) {
		switch (config.getLastFMCacheStyle()) {
		case Config.LASTFM_CACHE_INDIR: return config.getLastFMDirectory();
		case Config.LASTFM_CACHE_WITHMUSIC: return new File(filename).getParent();
		default: return null;
		}
	}
	
	private static String getCacheFileName(String artist, String album) {
		String cleanArtist = artist.replaceAll("\\W+", "");
		String cleanAlbum = album.replaceAll("\\W+", "");
		
		return cleanArtist + cleanAlbum + CACHE_EXT;
	}
	
	private static Bitmap getWebArt(Context context,
			                        Config config,
			                        String artist, 
			                        String album,
			                        String filename) {
		Bitmap artBmp = null;
		
		if (shouldConnect(config, context)) {
			String artUrl = getArtUrl(artist, album);
			if (artUrl != null) {
				artBmp = AlbumArtFactory.readUrl(artUrl);
			}
		}
		
		return artBmp;
	}
	
	private static boolean shouldConnect(Config config, Context context) {
		boolean getLastFMArtWifi = config.getLastFMArtWifi();
        boolean getLastFMArtNetwork = config.getLastFMArtNetwork();
        
        ConnectivityManager cm = (ConnectivityManager)
        context.getSystemService(Context.CONNECTIVITY_SERVICE);

    	NetworkInfo wifiNetwork 
			= cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

    	if (getLastFMArtWifi &&
    		wifiNetwork != null &&  
        	wifiNetwork.isConnectedOrConnecting())
    		return true;

    	NetworkInfo mobileNetwork 
			= cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
    	
    	if (getLastFMArtNetwork &&
    	    mobileNetwork != null && 
    	    mobileNetwork.isConnectedOrConnecting())
    		return true;
    	

    	NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    	
    	if (getLastFMArtNetwork &&
		    activeNetwork != null && 
		    activeNetwork.isConnectedOrConnecting())
    		return true;
    	
    	return false;
	}
	
	private static String getArtUrl(String artist, String album) {
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(false);
			XmlPullParser xpp = factory.newPullParser();
			
			String webArtist = URLEncoder.encode(artist, "UTF-8");
			String webAlbum = URLEncoder.encode(album, "UTF-8");			
			
			URL url = new URL(String.format(URL_TEMPLATE, webArtist, webAlbum));
			URLConnection ucon = url.openConnection();
			ucon.setConnectTimeout(URL_TIMEOUT);
			ucon.setReadTimeout(URL_TIMEOUT);
			InputStream is = ucon.getInputStream();
			
			xpp.setInput(is, null);
        
			int eventType = xpp.getEventType();
        
			while (eventType != XmlPullParser.END_DOCUMENT) {
				
				if (eventType == XmlPullParser.START_TAG &&
						IMAGE_TAG.equals(xpp.getName())) {
        		
					int n = xpp.getAttributeCount();
					for (int i = 0; i < n; ++i) {
						String size = xpp.getAttributeValue(SIZE_NAMESPACE, SIZE_ATTRIBUTE);
						if (IMAGE_SIZE.equals(size)) {
							xpp.next();
							return xpp.getText();
						}
					}
				}
				xpp.next();
				eventType = xpp.getEventType();
			}
		} catch (XmlPullParserException e) {
			// do nothing
			Log.d("TeaCup", "xmlpullparserexception: " + e);
		} catch (IOException e) {
			// do nothing
			Log.d("TeaCup", "ioexception: " + e);
		}
        
        return null;
	}
}
