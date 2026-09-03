package com.example.engine

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

interface YouTubePlayerListener {
    fun onReady()
    fun onStateChange(state: Int) // 1 = Playing, 2 = Paused, 3 = Buffering, 0 = Ended
    fun onProgress(currentMs: Long, durationMs: Long, bufferPercent: Int)
    fun onError(errorCode: Int)
}

class YouTubeAudioBridge(
    private val context: Context,
    private val deckId: String,
    private val listener: YouTubePlayerListener
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private var isApiReady = false
    private var pendingVideoId: String? = null
    private var pendingAutoPlay: Boolean = false
    var currentVideoId: String? = null
        private set

    init {
        mainHandler.post {
            initWebView()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateWebView(ctx: Context = context): WebView {
        if (webView == null) {
            initWebView(ctx)
        }
        return webView!!
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView(ctx: Context = context) {
        if (webView != null) return
        try {
            webView = WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(1, 1)
                settings.apply {
                    javaScriptEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    domStorageEnabled = true
                    databaseEnabled = true
                    allowContentAccess = true
                    allowFileAccess = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                }
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        Log.d("YouTubeBridge", "Deck $deckId bridge page loaded")
                    }
                }
                addJavascriptInterface(BridgeInterface(), "AuraBridge")
                onResume()
                resumeTimers()
                loadDataWithBaseURL("https://www.youtube.com", HTML_TEMPLATE, "text/html", "UTF-8", null)
            }
        } catch (e: Exception) {
            Log.e("YouTubeBridge", "Error creating WebView for Deck $deckId: ${e.message}")
        }
    }

    private inner class BridgeInterface {
        @JavascriptInterface
        fun onReady() {
            mainHandler.post {
                isApiReady = true
                listener.onReady()
                pendingVideoId?.let { vid ->
                    loadVideo(vid, pendingAutoPlay)
                    pendingVideoId = null
                }
            }
        }

        @JavascriptInterface
        fun onStateChange(state: Int) {
            mainHandler.post {
                listener.onStateChange(state)
            }
        }

        @JavascriptInterface
        fun onProgress(currentSec: Float, totalSec: Float, loadedFraction: Float) {
            mainHandler.post {
                val currentMs = (currentSec * 1000f).toLong().coerceAtLeast(0L)
                val durationMs = (totalSec * 1000f).toLong().coerceAtLeast(0L)
                val bufferPercent = (loadedFraction * 100f).toInt().coerceIn(0, 100)
                listener.onProgress(currentMs, durationMs, bufferPercent)
            }
        }

        @JavascriptInterface
        fun onError(errorCode: Int) {
            mainHandler.post {
                Log.e("YouTubeBridge", "Deck $deckId YouTube error: $errorCode")
                listener.onError(errorCode)
            }
        }
    }

    fun loadVideo(videoId: String, autoPlay: Boolean = true) {
        currentVideoId = videoId
        mainHandler.post {
            if (!isApiReady || webView == null) {
                pendingVideoId = videoId
                pendingAutoPlay = autoPlay
                return@post
            }
            val js = "loadVideo('$videoId', $autoPlay);"
            webView?.evaluateJavascript(js, null)
        }
    }

    fun play() {
        mainHandler.post {
            webView?.evaluateJavascript("play();", null)
        }
    }

    fun pause() {
        mainHandler.post {
            webView?.evaluateJavascript("pause();", null)
        }
    }

    fun seekTo(positionMs: Long) {
        mainHandler.post {
            val seconds = positionMs / 1000f
            webView?.evaluateJavascript("seekTo($seconds);", null)
        }
    }

    fun setVolume(fraction: Float) {
        mainHandler.post {
            val vol = (fraction.coerceIn(0f, 1f) * 100).toInt()
            webView?.evaluateJavascript("setVolume($vol);", null)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        mainHandler.post {
            val rate = speed.coerceIn(0.5f, 2.0f)
            webView?.evaluateJavascript("setPlaybackRate($rate);", null)
        }
    }

    fun stop() {
        mainHandler.post {
            webView?.evaluateJavascript("pause();", null)
            currentVideoId = null
        }
    }

    fun release() {
        mainHandler.post {
            try {
                webView?.stopLoading()
                webView?.pauseTimers()
                webView?.loadUrl("about:blank")
                webView?.destroy()
            } catch (_: Exception) {}
            webView = null
            isApiReady = false
        }
    }

    companion object {
        private const val HTML_TEMPLATE = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
  body { background-color: #000; margin: 0; padding: 0; overflow: hidden; }
  #player { width: 100%; height: 100%; }
</style>
</head>
<body>
<div id="player"></div>
<script>
  var tag = document.createElement('script');
  tag.src = "https://www.youtube.com/iframe_api";
  var firstScriptTag = document.getElementsByTagName('script')[0];
  firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

  var player = null;
  var isReady = false;
  var pendingVideoId = null;
  var pendingAutoPlay = false;

  function onYouTubeIframeAPIReady() {
      player = new YT.Player('player', {
          height: '100%',
          width: '100%',
          playerVars: {
              'playsinline': 1,
              'controls': 0,
              'disablekb': 1,
              'fs': 0,
              'rel': 0,
              'autoplay': 1,
              'origin': 'https://www.youtube.com'
          },
          events: {
              'onReady': onPlayerReady,
              'onStateChange': onPlayerStateChange,
              'onError': onPlayerError
          }
      });
  }

  function onPlayerReady(event) {
      isReady = true;
      try {
          if (player && player.unMute) {
              player.unMute();
          }
      } catch(e) {}
      if (window.AuraBridge) window.AuraBridge.onReady();
      if (pendingVideoId) {
          loadVideo(pendingVideoId, pendingAutoPlay);
          pendingVideoId = null;
      }
  }

  function onPlayerStateChange(event) {
      // Ensure unmuted whenever state changes to playing (1)
      if (event.data === 1 && player) {
          try {
              player.unMute();
          } catch(e) {}
      }
      if (window.AuraBridge) {
          window.AuraBridge.onStateChange(event.data);
      }
  }

  function onPlayerError(event) {
      if (window.AuraBridge) {
          window.AuraBridge.onError(event.data);
      }
  }

  function loadVideo(videoId, autoPlay) {
      if (!isReady || !player) {
          pendingVideoId = videoId;
          pendingAutoPlay = autoPlay;
          return;
      }
      try {
          if (autoPlay) {
              player.loadVideoById({
                  videoId: videoId,
                  startSeconds: 0
              });
              player.unMute();

              player.playVideo();
          } else {
              player.cueVideoById(videoId);
          }
      } catch(e) {
          if (autoPlay && player.loadVideoById) {
              player.loadVideoById(videoId);
              player.unMute();

          }
      }
  }

  function play() {
      if (player && player.playVideo) {
          try {
              player.unMute();

              player.playVideo();
          } catch(e) {}
      }
  }

  function pause() {
      if (player && player.pauseVideo) {
          try {
              player.pauseVideo();
          } catch(e) {}
      }
  }

  function seekTo(seconds) {
      if (player && player.seekTo) {
          try {
              player.seekTo(seconds, true);
          } catch(e) {}
      }
  }

  function setVolume(vol) {
      if (player && player.setVolume) {
          try {
              if (vol > 0 && player.unMute) {
                  player.unMute();
              }
              player.setVolume(vol);
          } catch(e) {}
      }
  }

  function setPlaybackRate(rate) {
      if (player && player.setPlaybackRate) {
          try {
              player.setPlaybackRate(rate);
          } catch(e) {}
      }
  }

  setInterval(function() {
      if (player && isReady && window.AuraBridge) {
          try {
              var curr = player.getCurrentTime() || 0;
              var dur = player.getDuration() || 0;
              var frac = 0;
              if (player.getVideoLoadedFraction) {
                  frac = player.getVideoLoadedFraction() || 0;
              }
              window.AuraBridge.onProgress(curr, dur, frac);
          } catch(e) {}
      }
  }, 150);
</script>
</body>
</html>
"""
    }
}
