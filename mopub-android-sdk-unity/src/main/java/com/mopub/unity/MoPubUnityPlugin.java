package com.mopub.unity;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.location.Location;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;

import com.mopub.common.MediationSettings;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.mobileads.*;
import com.mopub.mobileads.MoPubConversionTracker;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;
import com.mopub.mobileads.MoPubRewardedVideoListener;
import com.mopub.mobileads.MoPubView;
import com.mopub.mobileads.VungleRewardedVideo.VungleMediationSettings;
import com.unity3d.player.UnityPlayer;

import static com.mopub.mobileads.MoPubInterstitial.InterstitialAdListener;
import static com.mopub.mobileads.MoPubView.BannerAdListener;



public class MoPubUnityPlugin implements BannerAdListener, InterstitialAdListener, MoPubRewardedVideoListener
{
	private static MoPubUnityPlugin sInstance;

	// used for testing directly in Eclipse
	public Activity mActivity;

	private static String TAG = "MoPub";
	private MoPubInterstitial mMoPubInterstitial;
	private MoPubView mMoPubView;
	private RelativeLayout mLayout;


	public static MoPubUnityPlugin instance()
	{
		if( sInstance == null )
			sInstance = new MoPubUnityPlugin();
		return sInstance;
	}


	/* ***** ***** ***** ***** ***** ***** ***** *****
	 * Private API
	 */
	private Activity getActivity()
	{
		if( mActivity != null )
			return mActivity;

		return UnityPlayer.currentActivity;
	}


	private void UnitySendMessage( String go, String m, String p )
	{
		if( mActivity != null )
		{
			Log.i( TAG, "UnitySendMessage: " + go + ", " + m + ", " + p );
		}
		else
		{
			UnityPlayer.UnitySendMessage( go, m, p );
		}
	}


	private float getScreenDensity()
	{
		DisplayMetrics metrics = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics( metrics );

		return metrics.density;
	}


	private void prepLayout( int alignment )
	{
		// create a RelativeLayout and add the ad view to it
		if( mLayout == null )
		{
			mLayout = new RelativeLayout( getActivity() );
		}
		else
		{
			// remove the layout if it has a parent
			FrameLayout parentView = (FrameLayout)mLayout.getParent();
			if( parentView != null )
				parentView.removeView( mLayout );
		}

		int gravity = 0;

		switch( alignment )
		{
			case 0:
				gravity = Gravity.TOP | Gravity.LEFT;
				break;
			case 1:
				gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
				break;
			case 2:
				gravity = Gravity.TOP | Gravity.RIGHT;
				break;
			case 3:
				gravity = Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;
				break;
			case 4:
				gravity = Gravity.BOTTOM | Gravity.LEFT;
				break;
			case 5:
				gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
				break;
			case 6:
				gravity = Gravity.BOTTOM | Gravity.RIGHT;
				break;
		}

		mLayout.setGravity( gravity );
	}


	private void runSafelyOnUiThread( final Runnable runner )
	{
		getActivity().runOnUiThread( new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					runner.run();
				}
				catch( Exception e )
				{
					e.printStackTrace();
				}
			}
		});
	}



	/* ***** ***** ***** ***** ***** ***** ***** *****
	 * Test Settings API
	 */
	public void addFacebookTestDeviceId( String hashedDeviceId )
	{
		try
		{
			Class<?> cls = Class.forName( "com.facebook.ads.AdSettings" );
			Method method = cls.getMethod( "addTestDevice", new Class[] { String.class } );
			method.invoke( cls, hashedDeviceId );
			Log.i( TAG, "successfully added Facebook test device: " + hashedDeviceId );
		}
		catch( ClassNotFoundException e )
		{
			Log.i( TAG, "could not find Facebook AdSettings class. Did you add the Audience Network SDK to your Android folder?" );
		}
		//AdSettings.addtestdevice
		catch( NoSuchMethodException e )
		{
			Log.i( TAG, "could not find Facebook AdSettings.addTestDevice method. Did you add the Audience Network SDK to your Android folder?" );
		}
		catch( IllegalAccessException e )
		{
			e.printStackTrace();
		}
		catch( IllegalArgumentException e )
		{
			e.printStackTrace();
		}
		catch( InvocationTargetException e )
		{
			e.printStackTrace();
		}
	}


	public void addAdMobTestDeviceId( String deviceId )
	{
		try
		{
			//com.mopub.mobileads.GooglePlayServicesBanner.testDeviceIds.add( deviceId );
			//com.mopub.mobileads.GooglePlayServicesInterstitial.testDeviceIds.add( deviceId );

			//Log.i( TAG, "successfully added AdMob test device: " + deviceId );
		}
		catch( Exception e )
		{
			e.printStackTrace();
			Log.i( TAG, "could not add AdMob test device" );
		}
	}


	public void setLocationAwareness( final String locationAwareness )
	{
		runSafelyOnUiThread( new Runnable()
		{
			public void run()
			{
				MoPub.setLocationAwareness( MoPub.LocationAwareness.valueOf( locationAwareness ) );
			}
		} );
	}



	/* ***** ***** ***** ***** ***** ***** ***** *****
	 * Banners API
	 */
	public void createBanner( final String adUnitId, final int alignment )
	{
		runSafelyOnUiThread( new Runnable()
		{
			public void run()
			{
				if( mMoPubView != null )
					return;

				mMoPubView = new MoPubView( getActivity() );
				mMoPubView.setAdUnitId( adUnitId );
				mMoPubView.setBannerAdListener( MoPubUnityPlugin.this );
				mMoPubView.loadAd();

				prepLayout( alignment );

				mLayout.addView( mMoPubView );
				getActivity().addContentView( mLayout, new LayoutParams( LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT ) );

				mLayout.setVisibility( RelativeLayout.VISIBLE );
			}
		} );
	}


	public void hideBanner( final boolean shouldHide )
	{
		if( mMoPubView == null )
			return;

		runSafelyOnUiThread( new Runnable()
		{
			public void run()
			{
				if( shouldHide )
				{
					mMoPubView.setVisibility( View.GONE );
				}
				else
				{
					mMoPubView.setVisibility( View.VISIBLE );
				}
			}
		} );
	}


	public void setBannerKeywords( final String keywords )
	{
		runSafelyOnUiThread( new Runnable()
		{
			public void run()
			{
				if( mMoPubView == null )
					return;

				mMoPubView.setKeywords( keywords );
				mMoPubView.loadAd();
			}
		} );
	}


	public void destroyBanner()
	{
		runSafelyOnUiThread( new Runnable()
		{
			public void run()
			{
				if( mMoPubView == null || mLayout == null )
					return;

				mLayout.removeAllViews();
				mLayout.setVisibility( LinearLayout.GONE );
				mMoPubView.destroy();
				mMoPubView = null;
			}
		} );
	}



	/* ***** ***** ***** ***** ***** ***** ***** *****
	 * Interstitials API
	 */
	public void requestInterstitialAd( final String adUnitId, final String keywords )
	{
		runSafelyOnUiThread( new Runnable()
		{
			public void run()
			{
				mMoPubInterstitial = new MoPubInterstitial( getActivity(), adUnitId );
				mMoPubInterstitial.setInterstitialAdListener( MoPubUnityPlugin.this );

				if( keywords != null && keywords.length() > 0 )
					mMoPubInterstitial.setKeywords( keywords );

				mMoPubInterstitial.load();
			}
		} );
	}


	public void showInterstitialAd()
	{
		runSafelyOnUiThread( new Runnable()
		{
			public void run()
			{
				mMoPubInterstitial.show();
			}
		} );
	}


	public void reportApplicationOpen()
	{
		runSafelyOnUiThread( new Runnable()
		{
			public void run()
			{
				new MoPubConversionTracker().reportAppOpen( getActivity() );
			}
		} );
	}



	/* ***** ***** ***** ***** ***** ***** ***** *****
	 * Rewarded Videos API
	 */
	private MediationSettings[] extractMediationSettingsFromJson( String json )
	{
		ArrayList<MediationSettings> settings = new ArrayList<MediationSettings>();

		try
		{
			JSONArray jsonArray = new JSONArray( json );
			for( int i = 0; i < jsonArray.length(); i++ )
			{
				JSONObject jsonObj = jsonArray.getJSONObject( i );
				String adVendor = jsonObj.getString( "adVendor" );
				Log.i( TAG, "adding MediationSettings for ad vendor: " + adVendor );

				if( adVendor.equalsIgnoreCase( "chartboost" ) )
				{
					if( jsonObj.has( "customId" ) )
					{
//						MediationSettings s = new com.mopub.mobileads.ChartboostRewardedVideo.ChartboostMediationSettings( jsonObj.getString( "customId" ) );

						try {
							Class<?> enclosingClass = Class.forName("com.mopub.mobileads.ChartboostRewardedVideo");
							Class<?> innerClass = Class.forName("com.mopub.mobileads.ChartboostRewardedVideo$ChartboostMediationSettings");
							Constructor<?> ctor = innerClass.getDeclaredConstructor(enclosingClass);

							MediationSettings s = (MediationSettings) ctor.newInstance(jsonObj.getString("customId"));

							settings.add(s);
						} catch( ClassNotFoundException e ) {
							Log.i( TAG, "could not find Chartboost ChartboostMediationSettings class. Did you add the Chartboost Network SDK to your Android folder?" );

							StringWriter errors = new StringWriter();
							e.printStackTrace(new PrintWriter(errors));
							Log.i(TAG, errors.toString());
						} catch (InstantiationException e) {
							e.printStackTrace();
						} catch (NoSuchMethodException e) {
							e.printStackTrace();
						} catch( IllegalAccessException e ) {
							e.printStackTrace();
						} catch( IllegalArgumentException e ) {
							e.printStackTrace();
						} catch( InvocationTargetException e ) {
							e.printStackTrace();
						}
					}
					else
					{
						Log.i( TAG, "No customId key found in the settings object. Aborting adding Chartboost MediationSettings" );
					}
				}
				else if( adVendor.equalsIgnoreCase( "vungle" ) )
				{
					VungleMediationSettings.Builder s = new VungleMediationSettings.Builder();

					if( jsonObj.has( "userId" ) )
						s.withUserId( jsonObj.getString( "userId" ) );

					if( jsonObj.has( "cancelDialogBody" ) )
						s.withCancelDialogBody( jsonObj.getString( "cancelDialogBody" ) );

					if( jsonObj.has( "cancelDialogCloseButton" ) )
						s.withCancelDialogCloseButton( jsonObj.getString( "cancelDialogCloseButton" ) );

					if( jsonObj.has( "cancelDialogKeepWatchingButton" ) )
						s.withCancelDialogKeepWatchingButton( jsonObj.getString( "cancelDialogKeepWatchingButton" ) );

					if( jsonObj.has( "cancelDialogTitle" ) )
						s.withCancelDialogTitle( jsonObj.getString( "cancelDialogTitle" ) );

					settings.add( s.build() );
				}
				else if( adVendor.equalsIgnoreCase( "adcolony" ) )
				{
					if( jsonObj.has( "withConfirmationDialog" ) && jsonObj.has( "withResultsDialog" ) )
					{
						boolean withConfirmationDialog = jsonObj.getBoolean( "withConfirmationDialog" );
						boolean withResultsDialog = jsonObj.getBoolean( "withResultsDialog" );

//						MediationSettings s = new com.mopub.mobileads.AdColonyRewardedVideo.AdColonyInstanceMediationSettings( withConfirmationDialog, withResultsDialog );

						try {
							Class<?> enclosingClass = Class.forName("com.mopub.mobileads.AdColonyRewardedVideo");
                            Class<?> innerClass = Class.forName("com.mopub.mobileads.AdColonyRewardedVideo$AdColonyInstanceMediationSettings");
                            Constructor<?> ctor = innerClass.getDeclaredConstructor(enclosingClass);

                            MediationSettings s = (MediationSettings) ctor.newInstance(withConfirmationDialog, withResultsDialog);

							settings.add(s);
						} catch( ClassNotFoundException e ) {
							Log.i( TAG, "could not find AdColony AdColonyInstanceMediationSettings class. Did you add the AdColony Network SDK to your Android folder?" );


                            StringWriter errors = new StringWriter();
                            e.printStackTrace(new PrintWriter(errors));
                            Log.i(TAG, errors.toString());
						} catch (InstantiationException e) {
							e.printStackTrace();
						} catch (NoSuchMethodException e) {
							e.printStackTrace();
						} catch( IllegalAccessException e ) {
							e.printStackTrace();
						} catch( IllegalArgumentException e ) {
							e.printStackTrace();
						} catch( InvocationTargetException e ) {
							e.printStackTrace();
						}
					}
				}
				else
				{
					Log.e( TAG, "adVendor not available for custom mediation settings: [" + adVendor + "]" );
				}
			}
		}
		catch( JSONException e )
		{
			e.printStackTrace();
		}

		return settings.toArray( new MediationSettings[settings.size()] );
	}



	public void initializeRewardedVideo()
	{
		runSafelyOnUiThread( new Runnable()
		{
			public void run()
			{
				MoPub.initializeRewardedVideo( getActivity() );
				MoPub.setRewardedVideoListener( MoPubUnityPlugin.this );
			}
		});
	}

    public void requestRewardedVideo(final String adUnitId, final String json,
            final String keywords, final double latitude, final double longitude) {
		runSafelyOnUiThread(new Runnable() {
			public void run() {
                Location location = new Location("");
                location.setLatitude(latitude);
                location.setLongitude(longitude);

                MoPubRewardedVideoManager.RequestParameters requestParameters =
                        new MoPubRewardedVideoManager.RequestParameters(keywords, location);

                if(json != null) {
                    MoPub.loadRewardedVideo(
                            adUnitId, requestParameters, extractMediationSettingsFromJson(json));
                } else {
                    MoPub.loadRewardedVideo(adUnitId, requestParameters);
                }
			}
		});
	}


	public void showRewardedVideo( final String adUnitId )
	{
		if( !MoPub.hasRewardedVideo( adUnitId ) )
		{
			Log.i( TAG, "no rewarded video is available at this time" );
			return;
		}

		runSafelyOnUiThread( new Runnable()
		{
			public void run()
			{
				MoPub.showRewardedVideo( adUnitId );
			}
		});
	}



	/* ***** ***** ***** ***** ***** ***** ***** *****
	 * BannerAdListener implementation
	 */
	@Override
	public void onBannerLoaded( MoPubView banner )
	{
		UnitySendMessage( "MoPubManager", "onAdLoaded", String.valueOf( banner.getAdHeight() ) );

		// re-center the ad
		int height = mMoPubView.getAdHeight();
		int width = mMoPubView.getAdWidth();
		float density = getScreenDensity();

		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)mMoPubView.getLayoutParams();
		params.width = (int)( width * density );
		params.height = (int)( height * density );

		mMoPubView.setLayoutParams( params );
	}


	@Override
	public void onBannerFailed( MoPubView banner, MoPubErrorCode errorCode )
	{
		Log.i( TAG, "onAdFailed: " + errorCode );
		UnitySendMessage( "MoPubManager", "onAdFailed", "" );
	}


	@Override
	public void onBannerClicked( MoPubView banner )
	{
		UnitySendMessage( "MoPubManager", "onAdClicked", "" );
	}


	@Override
	public void onBannerExpanded( MoPubView banner )
	{
		UnitySendMessage( "MoPubManager", "onAdExpanded", "" );
	}


	@Override
	public void onBannerCollapsed( MoPubView banner )
	{
		UnitySendMessage( "MoPubManager", "onAdCollapsed", "" );
	}




	/* ***** ***** ***** ***** ***** ***** ***** *****
	 * InterstitialAdListener implementation
	 */
	@Override
	public void onInterstitialLoaded( MoPubInterstitial interstitial )
	{
		Log.i( TAG, "onInterstitialLoaded: " + interstitial );
		UnitySendMessage( "MoPubManager", "onInterstitialLoaded", "" );
	}


	@Override
	public void onInterstitialFailed( MoPubInterstitial interstitial, MoPubErrorCode errorCode )
	{
		Log.i( TAG, "onInterstitialFailed: " + errorCode );
		UnitySendMessage( "MoPubManager", "onInterstitialFailed", errorCode.toString() );
	}


	@Override
	public void onInterstitialShown( MoPubInterstitial interstitial )
	{
		Log.i( TAG, "onInterstitialShown: " + interstitial );
		UnitySendMessage( "MoPubManager", "onInterstitialShown", "" );
	}


	@Override
	public void onInterstitialClicked( MoPubInterstitial interstitial )
	{
		Log.i( TAG, "onInterstitialClicked: " + interstitial );
		UnitySendMessage( "MoPubManager", "onInterstitialClicked", "" );
	}


	@Override
	public void onInterstitialDismissed( MoPubInterstitial interstitial )
	{
		Log.i( TAG, "onInterstitialDismissed: " + interstitial );
		UnitySendMessage( "MoPubManager", "onInterstitialDismissed", "" );
	}





	/* ***** ***** ***** ***** ***** ***** ***** *****
	 * RewardedVideoListener implementation
	 */
	@Override
	public void onRewardedVideoLoadSuccess( String adUnitId )
	{
		UnitySendMessage( "MoPubManager", "onRewardedVideoLoaded", adUnitId );
	}


	@Override
	public void onRewardedVideoLoadFailure( String adUnitId, MoPubErrorCode errorCode )
	{
		Log.i( TAG, "onRewardedVideoFailed: " + errorCode );
		UnitySendMessage( "MoPubManager", "onRewardedVideoFailed", errorCode.toString() );
	}


	@Override
	public void onRewardedVideoStarted( String adUnitId )
	{
		UnitySendMessage( "MoPubManager", "onRewardedVideoShown", adUnitId );
	}


	@Override
	public void onRewardedVideoPlaybackError( String adUnitId, MoPubErrorCode errorCode )
	{
		UnitySendMessage( "MoPubManager", "onRewardedVideoFailedToPlay", adUnitId );
	}


	@Override
	public void onRewardedVideoClosed( String adUnitId )
	{
		UnitySendMessage( "MoPubManager", "onRewardedVideoClosed", adUnitId );
	}


	@Override
	public void onRewardedVideoCompleted( Set<String> adUnitIds, MoPubReward reward )
	{
		if( adUnitIds.size() == 0 || reward == null )
		{
			Log.i( TAG, "onRewardedVideoCompleted with no adUnitId and/or reward. Bailing out." );
			return;
		}

		try
		{
			JSONObject json = new JSONObject();
			json.put( "adUnitId", adUnitIds.toArray()[0].toString() );
			json.put( "currencyType", "" );
			json.put( "amount", reward.getAmount() );

			UnitySendMessage( "MoPubManager", "onRewardedVideoReceivedReward", json.toString() );
		}
		catch( JSONException e )
		{
			e.printStackTrace();
		}
	}

}
