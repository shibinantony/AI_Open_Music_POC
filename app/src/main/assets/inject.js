/**
 * JSABMusic Shielded Core Engine v1.0.0 (JioSaavn Optimization)
 * - Complete Ad & Pro Banner Elimination
 * - True Screen-Off Background Playback (Page Visibility API Hook)
 * - Pure AMOLED Black Theme (#000000)
 * - Studio 5-Band Parametric Equalizer (Lazy-Loaded WebAudio DSP)
 * - Bi-Directional Native Android MediaSession Bridge
 */
(function () {
    if (window.__jsabShieldActive) return;
    window.__jsabShieldActive = true;

    // =========================================================================
    // 1. PAGE VISIBILITY & BACKGROUND AUDIO LOCKDOWN
    // =========================================================================
    try {
        Object.defineProperty(document, 'hidden', { get: () => false, configurable: true });
        Object.defineProperty(document, 'visibilityState', { get: () => 'visible', configurable: true });
        Object.defineProperty(document, 'webkitHidden', { get: () => false, configurable: true });
        Object.defineProperty(document, 'webkitVisibilityState', { get: () => 'visible', configurable: true });
        document.hasFocus = () => true;

        const origAddEventListener = EventTarget.prototype.addEventListener;
        EventTarget.prototype.addEventListener = function (type, listener, options) {
            if (
                type === 'visibilitychange' ||
                type === 'webkitvisibilitychange' ||
                type === 'pagehide' ||
                type === 'blur' ||
                type === 'freeze'
            ) {
                return; // Suppress background pause triggers
            }
            return origAddEventListener.call(this, type, listener, options);
        };

        window.onblur = null;
        document.onvisibilitychange = null;
    } catch (e) {
        console.error('[JSABMusic] Visibility Hook Error:', e);
    }

    // =========================================================================
    // 2. PURE AMOLED BLACK THEME & PROMO SUPPRESSION STYLES
    // =========================================================================
    const amoledStyle = document.createElement('style');
    amoledStyle.id = 'jsab-amoled-theme';
    amoledStyle.textContent = `
        /* Enforce True AMOLED Black (#000000) across JioSaavn */
        :root, html, body,
        .o-layout, .o-wrapper, .c-page,
        .c-player, #player, .o-player,
        .c-nav, .c-header, .c-sidebar,
        .c-main, .c-footer,
        [class*="theme--dark"], [class*="layout"] {
            background-color: #000000 !important;
            background: #000000 !important;
            color: #FFFFFF !important;
        }

        /* Suppress JioSaavn Ads, Pro Upgrade Modals, Banners, and Interstitials */
        .c-ad, .c-banner-ad, [id*="ad-"], [class*="ad-"],
        .c-ad-slot, .c-leaderboard, .c-banner,
        .o-modal--ad, .o-modal--pro, .o-modal--upgrade,
        [data-ad-unit], .dfp-ad, .c-subscription-prompt,
        .c-pro-banner, .c-app-download,
        .o-modal:has([href*="pro"]),
        .o-modal:has([class*="pro"]) {
            display: none !important;
            visibility: hidden !important;
            height: 0 !important;
            width: 0 !important;
            opacity: 0 !important;
            pointer-events: none !important;
        }

        /* Player bar styling */
        .c-player, #player {
            border-top: 1px solid #1A1A1A !important;
        }
    `;

    function applyAmoledTheme() {
        if (!document.getElementById('jsab-amoled-theme')) {
            (document.head || document.documentElement).appendChild(amoledStyle);
        }
    }
    applyAmoledTheme();
    document.addEventListener('DOMContentLoaded', applyAmoledTheme);

    // =========================================================================
    // 3. LOW-OVERHEAD AD SKIPPER & MODAL AUTO-DISMISSER
    // =========================================================================
    function cleanJioAds() {
        // Auto dismiss ad dialogs or trial prompts
        const closeBtns = document.querySelectorAll(
            '.c-modal__close, .o-modal__close, [aria-label="Close"], button.close, .c-btn--dismiss'
        );
        closeBtns.forEach(btn => {
            const modal = btn.closest('.o-modal, .c-modal');
            if (modal && (modal.textContent.includes('Pro') || modal.textContent.includes('Ad') || modal.textContent.includes('Upgrade'))) {
                btn.click();
            }
        });

        // Detect if audio element is playing an ad stream
        const media = document.querySelector('audio') || document.querySelector('video');
        if (media && media.src && (media.src.includes('jioads') || media.src.includes('doubleclick') || media.src.includes('ad_'))) {
            media.muted = true;
            if (!isNaN(media.duration) && media.duration > 0) {
                media.currentTime = media.duration;
            }
        }
    }

    setInterval(cleanJioAds, 1500);

    const observer = new MutationObserver(() => {
        cleanJioAds();
        applyAmoledTheme();
    });

    observer.observe(document.documentElement, {
        childList: true,
        subtree: true
    });

    // =========================================================================
    // 4. METADATA & NATIVE STATE BRIDGE
    // =========================================================================
    let lastState = {
        title: '',
        artist: '',
        album: '',
        artUrl: '',
        isPlaying: false,
        duration: 0,
        position: 0
    };

    let intentionalPause = false;

    function notifyNativeBridge() {
        if (!window.AndroidBridge) return;

        const media = document.querySelector('audio') || document.querySelector('video');
        const isPlaying = media ? !media.paused && !media.ended : false;
        const duration = media && !isNaN(media.duration) ? Math.floor(media.duration) : 0;
        const position = media && !isNaN(media.currentTime) ? Math.floor(media.currentTime) : 0;

        let title = '';
        let artist = '';
        let album = '';
        let artUrl = '';

        // Strategy 1: MediaSession
        if (navigator.mediaSession && navigator.mediaSession.metadata) {
            const meta = navigator.mediaSession.metadata;
            title = meta.title || '';
            artist = meta.artist || '';
            album = meta.album || '';
            if (meta.artwork && meta.artwork.length > 0) {
                artUrl = meta.artwork[meta.artwork.length - 1].src || '';
            }
        }

        // Strategy 2: JioSaavn DOM fallbacks
        if (!title) {
            const titleEl = document.querySelector('.c-player__title') ||
                            document.querySelector('.player-song-name') ||
                            document.querySelector('[data-qa="player-song-name"]');
            if (titleEl) title = titleEl.textContent.trim();
        }
        if (!artist) {
            const artistEl = document.querySelector('.c-player__artist') ||
                             document.querySelector('.player-artist-name') ||
                             document.querySelector('[data-qa="player-artist-name"]');
            if (artistEl) artist = artistEl.textContent.trim();
        }
        if (!artUrl) {
            const imgEl = document.querySelector('.c-player__art img') ||
                          document.querySelector('#player img') ||
                          document.querySelector('[data-qa="player-song-image"]');
            if (imgEl && imgEl.src) artUrl = imgEl.src;
        }

        if (
            lastState.title !== title ||
            lastState.artist !== artist ||
            lastState.isPlaying !== isPlaying ||
            Math.abs(lastState.position - position) >= 2 ||
            lastState.duration !== duration
        ) {
            lastState = { title, artist, album, artUrl, isPlaying, duration, position };
            try {
                window.AndroidBridge.onPlaybackStateChanged(
                    isPlaying,
                    title,
                    artist,
                    album,
                    artUrl,
                    duration,
                    position
                );
            } catch (e) {}
        }
    }

    function setupMediaListeners() {
        const media = document.querySelector('audio') || document.querySelector('video');
        if (!media) return;

        ['play', 'playing', 'timeupdate', 'ended', 'loadedmetadata', 'seeking', 'seeked'].forEach(evt => {
            media.removeEventListener(evt, notifyNativeBridge);
            media.addEventListener(evt, notifyNativeBridge);
        });

        media.addEventListener('pause', function () {
            if (!intentionalPause && lastState.isPlaying && !media.ended) {
                setTimeout(() => {
                    if (media.paused && !intentionalPause) {
                        media.play();
                    }
                }, 100);
            }
            notifyNativeBridge();
        });
    }

    setInterval(setupMediaListeners, 2000);

    // =========================================================================
    // 5. LAZY-LOADED STUDIO EQUALIZER DSP
    // =========================================================================
    let audioCtx = null;
    let sourceNode = null;
    let eqFilters = [];
    let bassBoostFilter = null;
    let preampGainNode = null;
    let isEqReady = false;

    const bandFrequencies = [60, 230, 910, 3600, 14000];

    function setupEqualizerDSP() {
        if (isEqReady) return;
        const media = document.querySelector('audio') || document.querySelector('video');
        if (!media) return;

        try {
            const AudioContextClass = window.AudioContext || window.webkitAudioContext;
            if (!AudioContextClass) return;

            audioCtx = new AudioContextClass();
            sourceNode = audioCtx.createMediaElementSource(media);

            bassBoostFilter = audioCtx.createBiquadFilter();
            bassBoostFilter.type = 'lowshelf';
            bassBoostFilter.frequency.value = 80;
            bassBoostFilter.gain.value = 0;

            eqFilters = bandFrequencies.map((freq, index) => {
                const filter = audioCtx.createBiquadFilter();
                if (index === 0) filter.type = 'lowshelf';
                else if (index === bandFrequencies.length - 1) filter.type = 'highshelf';
                else { filter.type = 'peaking'; filter.Q.value = 1.4; }
                filter.frequency.value = freq;
                filter.gain.value = 0;
                return filter;
            });

            preampGainNode = audioCtx.createGain();
            preampGainNode.gain.value = 1.0;

            let currentNode = sourceNode;
            currentNode.connect(bassBoostFilter);
            currentNode = bassBoostFilter;

            eqFilters.forEach(filter => {
                currentNode.connect(filter);
                currentNode = filter;
            });

            currentNode.connect(preampGainNode);
            preampGainNode.connect(audioCtx.destination);

            isEqReady = true;
        } catch (e) {}
    }

    // =========================================================================
    // 6. EXPOSED CONTROL INTERFACE (KOTLIN CALLABLE)
    // =========================================================================
    window.bravePlayer = {
        play: function () {
            intentionalPause = false;
            const media = document.querySelector('audio') || document.querySelector('video');
            if (audioCtx && audioCtx.state === 'suspended') audioCtx.resume();
            if (media && media.paused) {
                media.play();
            } else {
                const btn = document.querySelector('.c-player__btn--play') ||
                            document.querySelector('#play') ||
                            document.querySelector('[data-qa="play-button"]');
                if (btn) btn.click();
            }
        },
        pause: function () {
            intentionalPause = true;
            const media = document.querySelector('audio') || document.querySelector('video');
            if (media && !media.paused) {
                media.pause();
            } else {
                const btn = document.querySelector('.c-player__btn--play') ||
                            document.querySelector('#play') ||
                            document.querySelector('[data-qa="pause-button"]');
                if (btn) btn.click();
            }
        },
        togglePlay: function () {
            const media = document.querySelector('audio') || document.querySelector('video');
            if (media) {
                if (media.paused) this.play();
                else this.pause();
            }
        },
        next: function () {
            intentionalPause = false;
            const btn = document.querySelector('.c-player__btn--next') ||
                        document.querySelector('#next') ||
                        document.querySelector('[data-qa="next-button"]');
            if (btn) btn.click();
        },
        previous: function () {
            intentionalPause = false;
            const btn = document.querySelector('.c-player__btn--prev') ||
                        document.querySelector('#prev') ||
                        document.querySelector('[data-qa="previous-button"]');
            if (btn) btn.click();
        },
        seekTo: function (seconds) {
            const media = document.querySelector('audio') || document.querySelector('video');
            if (media && !isNaN(seconds)) media.currentTime = seconds;
        },
        setVolume: function (volumePercent) {
            const media = document.querySelector('audio') || document.querySelector('video');
            if (media) media.volume = Math.max(0, Math.min(1, volumePercent));
        },
        setEqualizer: function (bandGainsArray, bassBoostGain, preampGain) {
            setupEqualizerDSP();
            if (audioCtx && audioCtx.state === 'suspended') audioCtx.resume();

            if (eqFilters && eqFilters.length === 5 && Array.isArray(bandGainsArray)) {
                for (let i = 0; i < 5; i++) {
                    if (i < bandGainsArray.length) {
                        eqFilters[i].gain.value = Number(bandGainsArray[i]) || 0;
                    }
                }
            }
            if (bassBoostFilter && typeof bassBoostGain === 'number') {
                bassBoostFilter.gain.value = Number(bassBoostGain) || 0;
            }
            if (preampGainNode && typeof preampGain === 'number') {
                preampGainNode.gain.value = Number(preampGain) || 1.0;
            }
        }
    };

    notifyNativeBridge();
})();
