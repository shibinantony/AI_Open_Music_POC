/**
 * JSABMusic Shielded Core Engine v1.0.2 (JioSaavn Optimization)
 * - Zero React DOM Crashes (Pure CSS Non-Destructive Concealment)
 * - Studio Acoustic Crossfade Engine (Smooth Fade-Out & Fade-In)
 * - Continuous Queue Auto-Advance Watchdog
 * - True Screen-Off Background Playback (Page Visibility API Hook)
 * - Studio 5-Band Parametric Equalizer (Resilient WebAudio DSP)
 * - Bi-Directional Native Android MediaSession Bridge
 */
(function () {
    if (window.__jsabShieldActive_v102) return;
    window.__jsabShieldActive_v102 = true;

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
    // 2. STORAGE QUOTA RESET (PREVENTS ARTIFICIAL STREAMING LIMITS)
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
            // Mark app install prompt as permanently dismissed in cookies
            document.cookie = "app_install_prompt_dismissed=true; path=/; max-age=31536000";
            document.cookie = "stream_limit_acknowledged=true; path=/; max-age=31536000";
        } catch (e) {}
    }
    resetPlaybackCounters();
    setInterval(resetPlaybackCounters, 5000);

    // =========================================================================
    // 3. NON-DESTRUCTIVE CSS SHIELDING (PRESERVES REACT VIRTUAL DOM)
    // =========================================================================
    const amoledStyle = document.createElement('style');
    amoledStyle.id = 'jsab-v102-theme';
    amoledStyle.textContent = `
        /* Backgrounds: Pure AMOLED Black (#000000) for Main Containers */
        body, html,
        .o-layout, .o-wrapper, .c-page,
        .c-player, #player, .o-player,
        .c-main, .c-footer {
            background-color: #000000 !important;
            background: #000000 !important;
        }

        /* Surfaces: Subtle Dark Slate (#0A0A0C) for Navbars & Headers */
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

        /* Non-Destructive Ad & App-Wall Concealment (DO NOT USE .remove()!) */
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
        .o-modal:has([href*="play.google"]),
        .o-modal:has([href*="apps.apple"]),
        .c-app-wall, .o-modal--app {
            display: none !important;
            visibility: hidden !important;
            height: 0 !important;
            width: 0 !important;
            opacity: 0 !important;
            pointer-events: none !important;
            z-index: -9999 !important;
        }

        /* Player bar styling */
        .c-player, #player {
            border-top: 1px solid #1A1A1A !important;
        }
    `;

    function applyShieldStyles() {
        if (!document.getElementById('jsab-v102-theme')) {
            (document.head || document.documentElement).appendChild(amoledStyle);
        }
    }
    applyShieldStyles();
    document.addEventListener('DOMContentLoaded', applyShieldStyles);

    // Click dismiss button safely without modifying React DOM
    function autoDismissPromos() {
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

        // Mute and fast-forward audio ad streams
        const media = document.querySelector('audio') || document.querySelector('video');
        if (media && media.src && (media.src.includes('jioads') || media.src.includes('doubleclick') || media.src.includes('ad_'))) {
            media.muted = true;
            if (!isNaN(media.duration) && media.duration > 0) {
                media.currentTime = media.duration;
            }
        }
    }

    setInterval(autoDismissPromos, 1500);

    const observer = new MutationObserver(() => {
        autoDismissPromos();
        applyShieldStyles();
    });

    observer.observe(document.documentElement, {
        childList: true,
        subtree: true
    });

    // =========================================================================
    // 4. STUDIO ACOUSTIC CROSSFADE & CONTINUOUS PLAY WATCHDOG
    // =========================================================================
    let masterVolume = 1.0;
    let isFadingOut = false;
    let transitionWatchdogTimer = null;

    function handleCrossfadeProgress() {
        const media = document.querySelector('audio') || document.querySelector('video');
        if (!media || isNaN(media.duration) || media.duration <= 6) return;

        const timeLeft = media.duration - media.currentTime;

        // Smooth Acoustic Fade-Out over final 2.5 seconds
        if (timeLeft <= 2.5 && timeLeft > 0.1 && !isFadingOut) {
            isFadingOut = true;
            let step = 0;
            const fadeInterval = setInterval(() => {
                step += 1;
                const factor = Math.max(0.15, 1.0 - (step / 10.0));
                if (media && !media.paused) {
                    media.volume = factor * masterVolume;
                }
                if (step >= 10 || media.ended) {
                    clearInterval(fadeInterval);
                }
            }, 200);
        }
    }

    function handleTrackStartFadeIn() {
        const media = document.querySelector('audio') || document.querySelector('video');
        if (!media) return;

        isFadingOut = false;
        let step = 0;
        media.volume = 0.25 * masterVolume;

        const inInterval = setInterval(() => {
            step += 1;
            const factor = Math.min(1.0, 0.25 + (step * 0.15));
            if (media) {
                media.volume = factor * masterVolume;
            }
            if (step >= 5) {
                if (media) media.volume = masterVolume;
                clearInterval(inInterval);
            }
        }, 180);
    }

    function onTrackEnded() {
        clearTimeout(transitionWatchdogTimer);
        // If JioSaavn does not advance within 900ms, force next track
        transitionWatchdogTimer = setTimeout(() => {
            const media = document.querySelector('audio') || document.querySelector('video');
            if (media && (media.paused || media.ended)) {
                window.bravePlayer && window.bravePlayer.next();
            }
        }, 900);
    }

    // =========================================================================
    // 5. METADATA & NATIVE STATE BRIDGE
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

    let boundMediaElement = null;

    function setupMediaListeners() {
        const media = document.querySelector('audio') || document.querySelector('video');
        if (!media || media === boundMediaElement) return;

        boundMediaElement = media;

        media.addEventListener('play', () => {
            intentionalPause = false;
            handleTrackStartFadeIn();
            notifyNativeBridge();
        });

        media.addEventListener('playing', () => {
            intentionalPause = false;
            notifyNativeBridge();
        });

        media.addEventListener('timeupdate', () => {
            handleCrossfadeProgress();
            notifyNativeBridge();
        });

        media.addEventListener('ended', () => {
            onTrackEnded();
            notifyNativeBridge();
        });

        media.addEventListener('pause', () => {
            notifyNativeBridge();
        });

        media.addEventListener('loadedmetadata', notifyNativeBridge);
    }

    setInterval(setupMediaListeners, 1500);

    // =========================================================================
    // 6. RESILIENT STUDIO EQUALIZER DSP (RE-ATTACHABLE ACROSS TRACKS)
    // =========================================================================
    let audioCtx = null;
    let sourceNode = null;
    let connectedMedia = null;
    let eqFilters = [];
    let bassBoostFilter = null;
    let preampGainNode = null;
    let isEqConfigured = false;

    const bandFrequencies = [60, 230, 910, 3600, 14000];

    function setupEqualizerDSP() {
        const media = document.querySelector('audio') || document.querySelector('video');
        if (!media) return;

        // If media element already connected, do not re-create source node
        if (isEqConfigured && connectedMedia === media) return;

        try {
            const AudioContextClass = window.AudioContext || window.webkitAudioContext;
            if (!AudioContextClass) return;

            if (!audioCtx) {
                audioCtx = new AudioContextClass();
            }

            if (audioCtx.state === 'suspended') {
                audioCtx.resume();
            }

            // Create media source node safely
            sourceNode = audioCtx.createMediaElementSource(media);
            connectedMedia = media;

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

            isEqConfigured = true;
        } catch (e) {
            console.warn('[JSABMusic] WebAudio DSP attachment notice:', e.message);
        }
    }

    // =========================================================================
    // 7. EXPOSED CONTROL INTERFACE
    // =========================================================================
    window.bravePlayer = {
        play: function () {
            intentionalPause = false;
            const media = document.querySelector('audio') || document.querySelector('video');
            if (audioCtx && audioCtx.state === 'suspended') audioCtx.resume();
            if (media && media.paused) {
                media.play().catch(() => {});
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
            masterVolume = Math.max(0, Math.min(1, volumePercent));
            const media = document.querySelector('audio') || document.querySelector('video');
            if (media) media.volume = masterVolume;
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
