package com.quran.labs.androidquran.ui.helpers;

import android.content.Context;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.quran.labs.androidquran.common.Response;
import com.quran.labs.androidquran.util.QuranScreenInfo;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;

@Singleton
public class QuranPageWorker {
  private static final String TAG = "QuranPageWorker";

  private final Context appContext;
  private final OkHttpClient okHttpClient;

  @Inject
  QuranPageWorker(Context context, OkHttpClient okHttpClient) {
    this.appContext = context;
    this.okHttpClient = okHttpClient;
  }

  private Response downloadImage(String widthParam, int pageNumber) {
    Response response = null;
    OutOfMemoryError oom = null;

    try {
      response = QuranDisplayHelper.getQuranPage(okHttpClient, appContext, widthParam, pageNumber);
    } catch (OutOfMemoryError me){
      Crashlytics.log(Log.WARN, TAG,
          "out of memory exception loading page " + pageNumber + ", " + widthParam);
      oom = me;
    }

    if (response == null ||
        (response.getBitmap() == null &&
            response.getErrorCode() != Response.ERROR_SD_CARD_NOT_FOUND)){
      if (QuranScreenInfo.getInstance().isTablet(appContext)){
        Crashlytics.log(Log.WARN, TAG, "tablet got bitmap null, trying alternate width...");
        String param = QuranScreenInfo.getInstance().getWidthParam();
        if (param.equals(widthParam)){
          param = QuranScreenInfo.getInstance().getTabletWidthParam();
        }
        response = QuranDisplayHelper.getQuranPage(okHttpClient, appContext, param, pageNumber);
        if (response.getBitmap() == null){
          Crashlytics.log(Log.WARN, TAG,
              "bitmap still null, giving up... [" + response.getErrorCode() + "]");
        }
      }
      Crashlytics.log(Log.WARN, TAG, "got response back as null... [" +
          (response == null ? "" : response.getErrorCode()));
    }

    if ((response == null || response.getBitmap() == null) && oom != null) {
      throw oom;
    }

    response.setPageData(pageNumber);
    return response;
  }

  public Observable<Response> loadPages(final String widthParam, Integer... pages) {
    return Observable.fromArray(pages)
        .flatMap(page -> Observable.fromCallable(() -> downloadImage(widthParam, page)))
        .subscribeOn(Schedulers.io());
  }
}
