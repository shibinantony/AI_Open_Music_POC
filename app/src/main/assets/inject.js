/**
 * JSABMusic Shielded Core Engine v1.0.1 (JioSaavn Optimization)
 * - Complete Elimination of "Listen with no limits on the JioSaavn App" Wall
 * - Refined AMOLED Black Palette with Preserved Typography Contrast
 * - True Screen-Off Background Playback (Page Visibility API Hook)
 * - Studio 5-Band Parametric Equalizer (Lazy-Loaded WebAudio DSP)
 * - Bi-Directional Native Android MediaSession Bridge
 */
(function () {
    if (window.__jsabShieldActive_v101) return;
    window.__jsabShieldActive_v101 = true;

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
    // 2. ELIMINATE "LISTEN WITH NO LIMITS" APP-WALL & STORAGE COUNTERS
    // =========================================================================
    function resetPlaybackCounters() {
        try {
            const keysToPurge = [
                'stream_count', 'play_count', 'guest_plays',
                'anon_stream_count', 'saavn_limit', 'song_count',
                'anon_listen_limit', 'preview_limit'
            ];
            keysToPurge.forEach(k => {
                localStorage.removeItem(k);
                sessionStorage.removeItem(k);
            });
        } catch (e) {}
    }
    resetPlaybackCounters();
    setInterval(resetPlaybackCounters, 5000);

    function killAppWallsAndPromos() {
        // Target and annihilate any modal or banner mentioning the JioSaavn App
        const candidates = document.querySelectorAll(
            '.o-modal, .c-modal, .c-banner, div[class*="modal"], div[class*="popup"], div[class*="prompt"], div[class*="bottom-sheet"], div[class*="app-banner"]'
        );

        candidates.forEach(el => {
            const text = (el.textContent || '').toLowerCase();
            if (
                text.includes('listen with no limits') ||
                text.includes('jiosaavn app') ||
                text.includes('open in app') ||
                text.includes('get the app') ||
                text.includes('download app') ||
                text.includes('continue on app') ||
                text.includes('switch to app')
            ) {
                el.remove(); // Nuke from DOM
            }
        });

        // Auto-close any lingering close buttons
        const closeBtns = document.querySelectorAll(
            '.c-modal__close, .o-modal__close, [aria-label="Close"], button.close, .c-btn--dismiss'
        );
        closeBtns.forEach(btn => {
            const modal = btn.closest('.o-modal, .c-modal');
            if (modal) {
                const text = (modal.textContent || '').toLowerCase();
                if (text.includes('app') || text.includes('pro') || text.includes('ad') || text.includes('limit')) {
                    btn.click();
                }
            }
        });

        // Fast-forward any detected audio ad
        const media = document.querySelector('audio') || document.querySelector('video');
        if (media && media.src && (media.src.includes('jioads') || media.src.includes('doubleclick') || media.src.includes('ad_'))) {
            media.muted = true;
            if (!isNaN(media.duration) && media.duration > 0) {
                media.currentTime = media.duration;
            }
        }
    }

    setInterval(killAppWallsAndPromos, 1000);

    // =========================================================================
    // 3. REFINED HIGH-CONTRAST AMOLED BLACK THEME (ZERO TEXT INVERSION)
    // =========================================================================
    const amoledStyle = document.createElement('style');
    amoledStyle.id = 'jsab-refined-theme';
    amoledStyle.textContent = `
        /* Backgrounds: Pure AMOLED Black (#000000) for Main Containers */
        body, html,
        .o-layout, .o-wrapper, .c-page,
        .c-player, #player, .o-player,
        .c-main, .c-footer {
            background-color: #000000 !important;
            background: #000000 !important;
        }

        /* Surfaces: Subtle Dark Slate (#0E0E10) for Navbars & Sidebars */
        .c-nav, .c-header, .c-sidebar {
            background-color: #0A0A0C !important;
            background: #0A0A0C !important;
            border-bottom: 1px solid #1A1A1E !important;
        }

        /* Cards & Media Rows */
        .c-card, .c-list__item, .o-block {
            background-color: transparent !important;
        }

        /* Crisp Typography & Contrast Preservation */
        .c-player__title, .player-song-name, [data-qa="player-song-name"] {
            color: #FFFFFF !important;
            font-weight: 600 !important;
        }

        .c-player__artist, .player-artist-name, [data-qa="player-artist-name"] {
            color: #9E9E9E !important;
        }

        /* Hide all App-Wall and Ad elements completely */
        .c-ad, .c-banner-ad, [id*="ad-"], [class*="ad-"],
        .c-ad-slot, .c-leaderboard, .c-banner,
        .o-modal--ad, .o-modal--pro, .o-modal--upgrade,
        [data-ad-unit], .dfp-ad, .c-subscription-prompt,
        .c-pro-banner, .c-app-download, .c-app-banner,
        .c-header__app-badge, .c-btn--app,
        a[href*="play.google.com"],
        a[href*="apps.apple.com"],
        .o-modal:has([href*="pro"]),
        .o-modal:has([class*="pro"]),
        .o-modal:has([href*="play.google"]) {
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

    function applyRefinedTheme() {
        if (!document.getElementById('jsab-refined-theme')) {
            (document.head || document.documentElement).appendChild(amoledStyle);
        }
    }
    applyRefinedTheme();
    document.addEventListener('DOMContentLoaded', applyRefinedTheme);

    const observer = new MutationObserver(() => {
        killAppWallsAndPromos();
        applyRefinedTheme();
    });

    observer.observe(document.documentElement, {
        childList: true,
        subtree: true
    });

    // =========================================================================
    // 4. METADATA & NATIVE STATE BRIDGE WITH ANTI-STALL WATCHDOG
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

        // Anti-Stall Guard: If audio pauses unexpectedly (app wall or focus trigger), auto-resume!
        media.addEventListener('pause', function () {
            if (!intentionalPause && lastState.isPlaying && !media.ended) {
                setTimeout(() => {
                    if (media.paused && !intentionalPause) {
                        media.play();
                    }
                }, 150);
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
    // 6. EXPOSED CONTROL INTERFACE
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
