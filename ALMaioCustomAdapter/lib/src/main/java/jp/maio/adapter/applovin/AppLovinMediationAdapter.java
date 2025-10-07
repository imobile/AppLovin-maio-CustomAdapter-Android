package jp.maio.adapter.applovin;

import android.app.Activity;

import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.adapter.MaxAdViewAdapter;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
import com.applovin.mediation.adapter.MaxRewardedAdapter;
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapters.MediationAdapterBase;
import com.applovin.sdk.AppLovinSdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import jp.maio.sdk.android.v2.Version;
import jp.maio.sdk.android.v2.banner.MaioBannerListener;
import jp.maio.sdk.android.v2.banner.MaioBannerSize;
import jp.maio.sdk.android.v2.banner.MaioBannerView;
import jp.maio.sdk.android.v2.errorcode.ErrorCode;
import jp.maio.sdk.android.v2.interstitial.IInterstitialLoadCallback;
import jp.maio.sdk.android.v2.interstitial.IInterstitialShowCallback;
import jp.maio.sdk.android.v2.interstitial.Interstitial;
import jp.maio.sdk.android.v2.request.MaioRequest;
import jp.maio.sdk.android.v2.rewarddata.RewardData;
import jp.maio.sdk.android.v2.rewarded.IRewardedLoadCallback;
import jp.maio.sdk.android.v2.rewarded.IRewardedShowCallback;
import jp.maio.sdk.android.v2.rewarded.Rewarded;

public class AppLovinMediationAdapter
        extends MediationAdapterBase
        implements MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter
{
    private static final String ADAPTER_VERSION = "2.0.6.0.0";
    private Interstitial interstitialAd;
    private Rewarded     rewardedAd;

    // Explicit default constructor declaration
    public AppLovinMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity, final OnCompletionListener onCompletionListener)
    {
        onCompletionListener.onCompletion( InitializationStatus.DOES_NOT_APPLY, null );
    }

    @Override
    public String getSdkVersion()
    {
        return Version.Companion.getInstance().toString();
    }

    @Override
    public String getAdapterVersion()
    {
        return ADAPTER_VERSION;
    }

    @Override
    public void onDestroy()
    {
        interstitialAd = null;
        rewardedAd = null;
    }

    @Nullable
    @Override
    public Boolean shouldLoadAdsOnUiThread(final MaxAdFormat adFormat)
    {
        return true;
    }

    @Nullable
    @Override
    public Boolean shouldShowAdsOnUiThread(final MaxAdFormat adFormat)
    {
        return true;
    }

    //region MaxInterstitialAdapter

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        String zoneId = parameters.getThirdPartyAdPlacementId();
        log( "Loading interstitial ad: " + zoneId + "..." );

        MaioRequest request = new MaioRequest( zoneId, parameters.isTesting(), "" );
        interstitialAd = Interstitial.loadAd( request, getApplicationContext(), new IInterstitialLoadCallback()
        {
            @Override
            public void loaded(@NonNull final Interstitial interstitial)
            {
                log( "Interstitial ad loaded for " + zoneId );
                listener.onInterstitialAdLoaded();
            }

            @Override
            public void failed(@NonNull final Interstitial interstitial, final int errorCode)
            {
                MaxAdapterError adapterError = toMaxError( errorCode, "Unspecified" );
                log( "Interstitial ad failed to load with error (" + adapterError + ")" );
                listener.onInterstitialAdLoadFailed( adapterError );
            }
        } );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Showing interstitial ad: " + parameters.getThirdPartyAdPlacementId() );
        if ( interstitialAd == null )
        {
            log( "Unable to show interstitial - ad not ready" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Interstitial ad not ready" ) );

            return;
        }

        interstitialAd.show( getApplicationContext(), new InterstitialAdListener( listener ) );
    }

    //endregion

    //region MaxRewardedAdapter

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        String zoneId = parameters.getThirdPartyAdPlacementId();
        log( "Loading rewarded ad for " + zoneId );

        MaioRequest request = new MaioRequest( zoneId, parameters.isTesting(), "" );
        rewardedAd = Rewarded.loadAd( request, getApplicationContext(), new IRewardedLoadCallback()
        {
            @Override
            public void loaded(@NonNull final Rewarded rewarded)
            {
                log( "Rewarded ad loaded" );
                listener.onRewardedAdLoaded();
            }

            @Override
            public void failed(@NonNull final Rewarded rewarded, final int errorCode)
            {
                MaxAdapterError adapterError = toMaxError( errorCode, "Unspecified" );
                log( "Rewarded ad failed to load with error (" + adapterError + ")" );
                listener.onRewardedAdLoadFailed( adapterError );
            }
        } );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Showing rewarded ad for " + parameters.getThirdPartyAdPlacementId() );

        if ( rewardedAd == null )
        {
            log( "Unable to show rewarded ad - ad not ready" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Rewarded ad not ready" ) );

            return;
        }

        // Configure userReward from server.
        configureReward( parameters );

        rewardedAd.show( getApplicationContext(), new RewardedAdListener( listener ) );
    }

    //endregion

    //region MaxAdViewAdapter

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        String zoneId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + adFormat.getLabel() + " ad for instance ID: " + zoneId );

        if ( activity == null )
        {
            log( adFormat.getLabel() + " ad load failed: Activity is null" );
            listener.onAdViewAdLoadFailed( MaxAdapterError.MISSING_ACTIVITY );

            return;
        }

        MaioBannerView bannerAd = new MaioBannerView( activity, zoneId, toMaioBannerSize( adFormat ) );
        bannerAd.setListener( new AdViewListener( listener ) );
        bannerAd.load( parameters.isTesting() );
    }

    //endregion

    //region Helper Methods

    private MaioBannerSize toMaioBannerSize(final MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER )
        {
            return MaioBannerSize.BANNER;
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return MaioBannerSize.MEDIUM_RECTANGLE;
        }
        else
        {
            throw new IllegalArgumentException( "Invalid ad format: " + adFormat );
        }
    }

    private MaxAdapterError toMaxError(final int maioErrorCode, final String maioErrorMessage)
    {
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( ErrorCode.getMajorCode( maioErrorCode ) )
        {
            case 101:
                adapterError = MaxAdapterError.NO_CONNECTION;
                break;
            case 102:
                adapterError = MaxAdapterError.TIMEOUT;
                break;
            case 103:
            case 202:
                adapterError = MaxAdapterError.AD_NOT_READY;
                break;
            case 104:
                adapterError = MaxAdapterError.SERVER_ERROR;
                break;
            case 105:
            case 106:
            case 110:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
            case 107:
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case 108:
                adapterError = MaxAdapterError.BAD_REQUEST;
                break;
            case 109:
            case 203:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
            case 201:
                adapterError = MaxAdapterError.AD_EXPIRED;
                break;
            case 204:
                adapterError = MaxAdapterError.WEBVIEW_ERROR;
                break;
            case 205:
                adapterError = MaxAdapterError.MISSING_ACTIVITY;
                break;
        }

        return new MaxAdapterError( adapterError, maioErrorCode, maioErrorMessage );
    }

    //endregion

    //region InterstitialAdListener

    private class InterstitialAdListener
            implements IInterstitialShowCallback
    {
        private final MaxInterstitialAdapterListener listener;

        private InterstitialAdListener(final MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void opened(@NonNull final Interstitial interstitial)
        {
            log( "Interstitial ad displayed" );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void failed(@NonNull final Interstitial interstitial, final int errorCode)
        {
            MaxAdapterError adapterError = new MaxAdapterError( -4205, "Ad Display Failed", errorCode, "Unspecified" );
            log( "Interstitial ad failed to display with error (" + adapterError + ")" );
            listener.onInterstitialAdDisplayFailed( adapterError );
        }

        @Override
        public void clicked(@NonNull final Interstitial interstitial)
        {
            log( "Interstitial ad clicked" );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void closed(@NonNull final Interstitial interstitial)
        {
            log( "Interstitial ad hidden" );
            listener.onInterstitialAdHidden();
        }
    }

    //endregion

    //region RewardedAdListener

    private class RewardedAdListener
            implements IRewardedShowCallback
    {
        private final MaxRewardedAdapterListener listener;
        private       boolean                    hasGrantedReward;

        public RewardedAdListener(final MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void opened(@NonNull final Rewarded rewarded)
        {
            log( "Rewarded ad displayed" );
            listener.onRewardedAdDisplayed();
        }

        @Override
        public void failed(@NonNull final Rewarded rewarded, final int errorCode)
        {
            MaxAdapterError adapterError = new MaxAdapterError( -4205, "Ad Display Failed", errorCode, "Unspecified" );
            log( "Rewarded ad failed to display with error (" + adapterError + ")" );
            listener.onRewardedAdDisplayFailed( adapterError );
        }

        @Override
        public void clicked(@NonNull final Rewarded rewarded)
        {
            log( "Rewarded ad clicked" );
            listener.onRewardedAdClicked();
        }

        @Override
        public void rewarded(@NonNull final Rewarded rewarded, @NonNull final RewardData rewardData)
        {
            log( "Rewarded ad should grant reward" );
            hasGrantedReward = true;
        }

        @Override
        public void closed(@NonNull final Rewarded rewarded)
        {
            log( "Rewarded ad hidden" );

            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                final MaxReward reward = getReward();
                log( "Rewarded user with reward: " + reward );
                listener.onUserRewarded( reward );
            }

            listener.onRewardedAdHidden();
        }
    }

    //endregion

    //region AdViewListener

    private class AdViewListener
            implements MaioBannerListener
    {
        private final MaxAdViewAdapterListener listener;

        private AdViewListener(final MaxAdViewAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void loaded(@NonNull final MaioBannerView maioBannerView)
        {
            log( "AdView ad loaded" );
            listener.onAdViewAdLoaded( maioBannerView );
        }

        @Override
        public void failedToLoad(@NonNull final MaioBannerView maioBannerView, final int errorCode)
        {
            MaxAdapterError adapterError = toMaxError( errorCode, "Unspecified" );
            log( "AdView ad failed to load with error (" + adapterError + ")" );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void impression(@NonNull final MaioBannerView maioBannerView)
        {
            log( "AdView ad displayed" );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void clicked(@NonNull final MaioBannerView maioBannerView)
        {
            log( "AdView ad clicked" );
            listener.onAdViewAdClicked();
        }

        @Override
        public void leftApplication(@NonNull final MaioBannerView maioBannerView)
        {
            log( "AdView ad left application" );
        }

        @Override
        public void failedToShow(@NonNull final MaioBannerView maioBannerView, final int errorCode)
        {
            MaxAdapterError adapterError = toMaxError( errorCode, "Unspecified" );
            log( "AdView ad failed to display with error (" + adapterError + ")" );
            listener.onAdViewAdDisplayFailed( adapterError );
        }
    }

    //endregion

}
