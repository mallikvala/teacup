package net.chilon.matt.teacup;

import net.chilon.matt.teacup.R;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class TeaCup extends AppWidgetProvider {
	
	public static final String BTN_JUMP_NEXT = "jump-next";
	public static final String BTN_JUMP_PREV = "jump-prev";
	public static final String BTN_PLAY_PAUSE = "play-pause";
	
	private static final String MUSIC_PLAYER = "com.android.music";
	private static final String MUSIC_SERVICE_CMD = "com.android.music.musicservicecommand";
	private static final String INTENT_COMMAND = "command";
	private static final String CMD_PLAY_PAUSE = "togglepause";
	private static final String CMD_JUMP_PREV = "previous";
	private static final String CMD_JUMP_NEXT = "next";
	
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
    	super.onDisabled(context);
    }

    @Override
    public void onEnabled(Context context) {
    	super.onEnabled(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
    	String action = intent.getAction();
    	if (action.equals(BTN_JUMP_NEXT)) {
    		onClickJumpNext(context, intent);
    	} else if (action.equals(BTN_JUMP_PREV)) {
    		onClickJumpPrev(context, intent);
    	} else if (action.equals(BTN_PLAY_PAUSE)) {
    		onClickPlayPause(context, intent);
    	}
    	super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, 
                         AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
    	
    	RemoteViews remoteViews = new RemoteViews(context.getPackageName(), 
    			                                  R.layout.teacup);
    	
    	PendingIntent playPause = makePendingIntent(context, BTN_PLAY_PAUSE);
    	PendingIntent jumpNext = makePendingIntent(context, BTN_JUMP_NEXT);
    	PendingIntent jumpPrev = makePendingIntent(context, BTN_JUMP_PREV);
    	PendingIntent launchPlayer = makeLaunchPendingIntent(context);
    	
    	remoteViews.setOnClickPendingIntent(R.id.playPauseButton, playPause);
    	remoteViews.setOnClickPendingIntent(R.id.jumpNextButton, jumpNext);
    	remoteViews.setOnClickPendingIntent(R.id.jumpPrevButton, jumpPrev);
    	remoteViews.setOnClickPendingIntent(R.id.albumArtButton, launchPlayer);
    	
    	appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
    	
   		super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
    
    private PendingIntent makePendingIntent(Context context, String command) {
    	Intent i = new Intent(context, TeaCup.class);
    	i.setAction(command);
    	return PendingIntent.getBroadcast(context, 0, i, 0);
    }
    
    private PendingIntent makeLaunchPendingIntent(Context context) {
    	Intent li = context.getPackageManager().getLaunchIntentForPackage(MUSIC_PLAYER);
    	return PendingIntent.getActivity(context, 0, li, 0);
    }
    
    private void onClickJumpNext(Context context, Intent intent) {
    	updatePlayer(context, CMD_JUMP_NEXT);
    }
    
    private void onClickJumpPrev(Context context, Intent intent) {
    	updatePlayer(context, CMD_JUMP_PREV);
    }
    
    private void onClickPlayPause(Context context, Intent intent) {
    	updatePlayer(context, CMD_PLAY_PAUSE);
    }
        
    private void updatePlayer(Context context, String command) {
    	Intent i = new Intent(MUSIC_SERVICE_CMD);
    	i.putExtra(INTENT_COMMAND, command);
    	context.sendBroadcast(i);
    }
}