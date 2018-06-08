package com.chiaramail.chiaramailforandroid.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.net.Uri;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.MediaController;
import android.widget.VideoView;

import com.chiaramail.chiaramailforandroid.R;

public class ECSVideoViewer extends Activity {
    private static final String EXTRA_PATH = "path";
    private MediaPlayer mMediaPlayer;
    private int mVideoWidth;
    private int mVideoHeight;
    private boolean mIsVideoSizeKnown = false;
    private boolean mIsVideoReadyToBePlayed = false;    
    private SurfaceView mPreview;
    private SurfaceHolder holder;
    private String path;


    /**
     * TODO: Set the path variable to a streaming video URL or a local media
     * file path.
     */
//    private String path = "https://www.chiaramail.com/DynamicContentServer/ContentServer?email_addr=hupatest23%40gmail.com&passwd=MjAxQk5nXlg%3D&cmd=FETCH+CONTENT&parms=hupatest23%40gmail.com+1400";
//    private String path = "http://youtu.be/e5PFXhdfVT8";
    private static VideoView mVideoView;
    
    boolean readyToPlay;
    
//    private Context context;

    public static void startPlayer(Context context, Uri uri) {
        Intent i = new Intent(context, ECSVideoViewer.class);
        i.putExtra(EXTRA_PATH, uri.getEncodedPath());
        context.startActivity(i);
 //       this.context = context;
    }

    private void doCleanUp() {
        mVideoWidth = 0;
        mVideoHeight = 0;
        mIsVideoReadyToBePlayed = false;
        mIsVideoSizeKnown = false;
    }

    public void onBufferingUpdate(MediaPlayer arg0, int percent) {
    }

    public void onCompletion(MediaPlayer arg0) {
    }

    private void playVideo(String movieUri) {    	
        doCleanUp();
        try {
            // Create a new media player and set the listeners
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource("file://" + movieUri);
            mMediaPlayer.setDisplay(holder);
            mMediaPlayer.prepare();
//            mMediaPlayer.prepareAsync();
            mMediaPlayer.setOnBufferingUpdateListener(new OnBufferingUpdateListener() {
            	public void onBufferingUpdate(MediaPlayer mp, int percent) {
            		int pos = mp.getCurrentPosition();
            	}
            });
            mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {
            	public void onCompletion(MediaPlayer mp) {
            		int duration = mp.getDuration();
            	}
            });

            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared (MediaPlayer mp) {
                	mp.setLooping(false);
                    mVideoView.start();
                	mp.setOnBufferingUpdateListener(new OnBufferingUpdateListener() {
                		// show updated information about the buffering progress
            			@Override
            			public void onBufferingUpdate(MediaPlayer mp, int percent) {
//                					progress.setSecondaryProgress(percent);
            			}
                	});
                	mp.start();
                }
            });
//            mMediaPlayer.setOnVideoSizeChangedListener(this);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        } catch (Exception e) {
//            Log.e(TAG, "error: " + e.getMessage(), e);
            return;
        }
    }
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.videoview);
        mVideoView = (VideoView) findViewById(R.id.surface_view);
///        mPreview = (SurfaceView) findViewById(R.id.surface_view);
///        holder = mPreview.getHolder();
//        holder.addCallback(this);
//        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        String path = getIntent().getStringExtra(EXTRA_PATH);
///        playVideo(path);

        mVideoView.setVideoURI(Uri.parse(path));
//        mVideoView.setVideoPath(path);
        mVideoView.setMediaController(new MediaController(this));
        mVideoView.setOnErrorListener(new OnErrorListener() {
            public boolean onError (MediaPlayer mp, int what, int extra) {
//                mVideoView.getCurrentPosition();
//                mVideoView.getBufferPercentage();
//                mVideoView.getDuration();
/**            	 new CountDownTimer(3000, 1000) {

            	     public void onTick(long millisUntilFinished) {
            	     }

            	     public void onFinish() {
                         mVideoView.start();
            	     }
            	  }.start();**/
//                return false;
                return true;
            }
        });
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion (MediaPlayer mp) {
            }
        });
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared (MediaPlayer mp) {
            	mp.setLooping(false);
                mVideoView.start();
            	mp.setOnBufferingUpdateListener(new OnBufferingUpdateListener()
            	{
            			// show updated information about the buffering progress
            			@Override
            			public void onBufferingUpdate(MediaPlayer mp, int percent) {
//            					progress.setSecondaryProgress(percent);
                    		int pos = mp.getCurrentPosition();
            			}
            	});
            	mp.setOnSeekCompleteListener(new OnSeekCompleteListener() {
            		
             		//show current frame after changing the playback position
		        	@Override
		        		public void onSeekComplete(MediaPlayer mp) {
		        			if(mp.isPlaying()) {
		 //       				playMedia(null);
		 //       				playMedia(play);
		        			} else {
		 //       				playMedia(null);
		 //       				playMedia(play);
		 //       				playMedia(null);
		        			}
		  //      				mediaTimeElapsed.setText(countTime(vv.getCurrentPosition()));
		        		}
	        	});
	            	
            	mp.setOnCompletionListener(new OnCompletionListener() {
                	public void onCompletion(MediaPlayer mp) {
                		int duration = mp.getDuration();
                	}
                });
            	
        	readyToPlay = true;
        	int time = mVideoView.getDuration();
            int time_elapsed = mVideoView.getCurrentPosition();
            }
        });
        mVideoView.requestFocus();
/**	   	new CountDownTimer(3000, 1000) {	//Wait awhile for some data to be downloaded
		    public void onTick(long millisUntilFinished) {
		    }
		    
		    public void onFinish() {
	             mVideoView.start();
		    }
		}.start();**/
    }
    /**
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.videoview);
        mVideoView = (VideoView) findViewById(R.id.surface_view);
        String path = getIntent().getStringExtra(EXTRA_PATH);

        mVideoView.setVideoURI(Uri.parse(path));
//        mVideoView.setVideoPath(path);
        mVideoView.setMediaController(new MediaController(this));
        mVideoView.setOnErrorListener(new OnErrorListener() {
            public boolean onError (MediaPlayer mp, int what, int extra) {
                mVideoView.getCurrentPosition();
                mVideoView.getBufferPercentage();
                mVideoView.getDuration();
                return false;
//                return true;
            }
        });
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion (MediaPlayer mp) {
                mVideoView.pause();
            }
        });
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared (MediaPlayer mp) {
            	mp.setLooping(true);
//                mVideoView.start();
            	mp.setOnBufferingUpdateListener(new OnBufferingUpdateListener()
            	{
            			// show updated information about the buffering progress
            			@Override
            			public void onBufferingUpdate(MediaPlayer mp, int percent) {
//            					progress.setSecondaryProgress(percent);
            			}
            	});
            	mp.setOnSeekCompleteListener(new OnSeekCompleteListener() {
            		
             		//show current frame after changing the playback position
        	@Override
        		public void onSeekComplete(MediaPlayer mp) {
        			if(mp.isPlaying()) {
//        				playMedia(null);
//        				playMedia(play);
        			} else {
//        				playMedia(null);
//        				playMedia(play);
//        				playMedia(null);
        			}
  //      				mediaTimeElapsed.setText(countTime(vv.getCurrentPosition()));
        		}
        	});
            	
            	mp.setOnCompletionListener(null);
            	
        	readyToPlay = true;
        	int time = mVideoView.getDuration();
            int time_elapsed = mVideoView.getCurrentPosition();
            }
        });
        mVideoView.requestFocus();
        mVideoView.start();
    }
    **/
    public static void resume() {
    	mVideoView.resume();
    }
    public static void play() {
    	mVideoView.start();
    }
    public static int getCurrentPosition() {
    	return mVideoView.getCurrentPosition();
    }
    public static int getBufferPercentage() {
    	return mVideoView.getBufferPercentage();
    }
    public static int getDuration() {
    	return mVideoView.getDuration();
    }
    public static boolean isPlaying() {
    	return mVideoView.isPlaying();
    }
}
