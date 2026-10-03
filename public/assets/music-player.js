/**
 * Visol Music Player & Smart Search Engine (JavaScript / Web / Mobile)
 * 
 * 1. Vaqtni nollash (0:00 Reset & Cache-busting)
 * 2. YouTube uslubidagi Aqlli Qidiruv (Smart Fuzzy Search, Xato yozuvlarni to'g'rilash, Naqorat/Lyrics qidiruvi)
 * 3. Real-vaqtli Autocomplete Takliflari (Suggestions dropdown)
 * 4. Barqaror Playlist renderlash (Qo'shiqlar yo'qolib qolishining oldini olish)
 */

class VisolMusicPlayer {
    constructor(options = {}) {
        this.audio = new Audio();
        this.currentTrack = null;
        this.isPlaying = false;
        this.tracksQueue = [];
        this.currentIndex = 0;
        this._isUserSeeking = false;

        // UI elementlarini bog'lash
        this.ui = {
            playBtn: options.playBtn || null,
            progressBar: options.progressBar || null,
            currentTimeEl: options.currentTimeEl || null,
            durationEl: options.durationEl || null,
            trackTitleEl: options.trackTitleEl || null,
            trackArtistEl: options.trackArtistEl || null,
            trackCoverEl: options.trackCoverEl || null
        };

        this._setupEventListeners();
    }

    _setupEventListeners() {
        this.audio.addEventListener('loadedmetadata', () => {
            this.audio.currentTime = 0;
            this._updateDurationUI();
            this._updateCurrentTimeUI(0);
        });

        this.audio.addEventListener('canplay', () => {
            if (this.audio.currentTime !== 0 && !this._isUserSeeking) {
                this.audio.currentTime = 0;
            }
        });

        this.audio.addEventListener('timeupdate', () => {
            if (this._isUserSeeking) return;
            const cur = this.audio.currentTime || 0;
            const dur = this.audio.duration || 0;
            this._updateCurrentTimeUI(cur);

            if (this.ui.progressBar && dur > 0) {
                this.ui.progressBar.value = (cur / dur) * 100;
            }
        });

        this.audio.addEventListener('play', () => {
            this.isPlaying = true;
            this._updatePlayButtonUI(true);
        });

        this.audio.addEventListener('pause', () => {
            this.isPlaying = false;
            this._updatePlayButtonUI(false);
        });

        this.audio.addEventListener('ended', () => {
            this.isPlaying = false;
            this.audio.currentTime = 0;
            this._updatePlayButtonUI(false);
            this._updateCurrentTimeUI(0);

            if (this.tracksQueue.length > this.currentIndex + 1) {
                this.playNext();
            }
        });

        this.audio.addEventListener('error', (e) => {
            console.warn('[MusicPlayer] Audio xatoligi:', e);
            this.isPlaying = false;
            this._updatePlayButtonUI(false);
        });
    }

    loadAndPlay(track, autoPlay = true) {
        if (!track || !track.audioUrl) {
            console.error('[MusicPlayer] Noto\'g\'ri trek obyekti!');
            return;
        }

        this.currentTrack = track;
        this.audio.pause();

        const separator = track.audioUrl.includes('?') ? '&' : '?';
        const cleanAudioUrl = `${track.audioUrl}${separator}_t=${Date.now()}`;

        this.audio.src = cleanAudioUrl;
        this.audio.load();
        this.audio.currentTime = 0;

        this._updateTrackInfoUI(track);
        this._updateCurrentTimeUI(0);

        if (autoPlay) {
            this.play(true);
        }
    }

    play(forceFromStart = false) {
        if (!this.audio.src) return;

        if (forceFromStart || isNaN(this.audio.currentTime) || this.audio.currentTime === this.audio.duration) {
            this.audio.currentTime = 0;
        }

        const promise = this.audio.play();
        if (promise !== undefined) {
            promise
                .then(() => {
                    this.isPlaying = true;
                    this._updatePlayButtonUI(true);
                })
                .catch((err) => {
                    this.isPlaying = false;
                    this._updatePlayButtonUI(false);
                });
        }
    }

    pause() {
        this.audio.pause();
        this.isPlaying = false;
        this._updatePlayButtonUI(false);
    }

    togglePlay() {
        if (this.isPlaying) {
            this.pause();
        } else {
            this.play();
        }
    }

    restart() {
        this.audio.currentTime = 0;
        this._updateCurrentTimeUI(0);
        if (this.ui.progressBar) {
            this.ui.progressBar.value = 0;
        }
        this.play(true);
    }

    seekToPercent(percentage) {
        if (this.audio.duration > 0) {
            const targetTime = (percentage / 100) * this.audio.duration;
            this.audio.currentTime = Math.max(0, Math.min(targetTime, this.audio.duration));
            this._updateCurrentTimeUI(this.audio.currentTime);
        }
    }

    playNext() {
        if (this.currentIndex + 1 < this.tracksQueue.length) {
            this.currentIndex++;
            this.loadAndPlay(this.tracksQueue[this.currentIndex], true);
        } else if (this.tracksQueue.length > 0) {
            this.currentIndex = 0;
            this.loadAndPlay(this.tracksQueue[0], true);
        }
    }

    playPrevious() {
        if (this.audio.currentTime > 3) {
            this.restart();
        } else if (this.currentIndex > 0) {
            this.currentIndex--;
            this.loadAndPlay(this.tracksQueue[this.currentIndex], true);
        } else {
            this.restart();
        }
    }

    _formatTime(seconds) {
        if (!seconds || isNaN(seconds) || seconds < 0) return '0:00';
        const mins = Math.floor(seconds / 60);
        const secs = Math.floor(seconds % 60);
        return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
    }

    _updateCurrentTimeUI(sec) {
        if (this.ui.currentTimeEl) {
            this.ui.currentTimeEl.textContent = this._formatTime(sec);
        }
    }

    _updateDurationUI() {
        if (this.ui.durationEl) {
            this.ui.durationEl.textContent = this._formatTime(this.audio.duration);
        }
    }

    _updatePlayButtonUI(isPlaying) {
        if (this.ui.playBtn) {
            this.ui.playBtn.innerHTML = isPlaying ? '⏸' : '▶';
        }
    }

    _updateTrackInfoUI(track) {
        if (this.ui.trackTitleEl) this.ui.trackTitleEl.textContent = track.title || 'Noma\'lum Tarona';
        if (this.ui.trackArtistEl) this.ui.trackArtistEl.textContent = track.artist || 'Noma\'lum Ijrochi';
        if (this.ui.trackCoverEl && track.coverUrl) this.ui.trackCoverEl.src = track.coverUrl;
    }
}

/**
 * YouTube Uslubidagi Aqlli Qidiruv Dvigateli (Smart Search & Fuzzy Match Engine)
 */
class VisolSmartSearch {
    constructor(tracksCatalog = []) {
        this.catalog = tracksCatalog;
    }

    setCatalog(tracks) {
        this.catalog = tracks || [];
    }

    /**
     * O'zbek va lotin harflarini normalizatsiya qilish (o', g', sh, ch, x/h va xatoliklar)
     */
    static normalize(str = '') {
        return str
            .toString()
            .toLowerCase()
            .replace(/[o‘o'o`ў]/g, 'o')
            .replace(/[g‘g'g`ғ]/g, 'g')
            .replace(/sh|ш/g, 's')
            .replace(/ch|ч/g, 'c')
            .replace(/[xхҳ]/g, 'h')
            .replace(/[jж]/g, 'j')
            .replace(/[^a-z0-9\s]/g, '')
            .trim();
    }

    /**
     * Levenshtein Distance (Harflar farqini hisoblash / Xato yozilganda ham topish)
     */
    static levenshtein(a, b) {
        const matrix = [];
        for (let i = 0; i <= b.length; i++) matrix[i] = [i];
        for (let j = 0; j <= a.length; j++) matrix[0][j] = j;

        for (let i = 1; i <= b.length; i++) {
            for (let j = 1; j <= a.length; j++) {
                if (b.charAt(i - 1) === a.charAt(j - 1)) {
                    matrix[i][j] = matrix[i - 1][j - 1];
                } else {
                    matrix[i][j] = Math.min(
                        matrix[i - 1][j - 1] + 1,
                        Math.min(matrix[i][j - 1] + 1, matrix[i - 1][j] + 1)
                    );
                }
            }
        }
        return matrix[b.length][a.length];
    }

    /**
     * Aqlli qidiruv:
     * 1. Nomi (Title)
     * 2. Ijrochisi (Artist)
     * 3. Naqorat / Matni (Lyrics & Chorus)
     * 4. Kalit so'zlar (Keywords)
     * 5. Fuzzy Moslashuv (Typo tolerance)
     */
    search(query = '') {
        const rawQuery = query.trim();
        if (!rawQuery) {
            return this.catalog; // Agar bo'sh bo'lsa barcha qo'shiqlarni qaytaradi (hech qachon yo'qolmaydi)
        }

        const normQuery = VisolSmartSearch.normalize(rawQuery);
        const queryTokens = normQuery.split(/\s+/).filter(t => t.length > 0);

        const scored = this.catalog.map(track => {
            let score = 0;
            const normTitle = VisolSmartSearch.normalize(track.title || '');
            const normArtist = VisolSmartSearch.normalize(track.artist || '');
            const normLyrics = VisolSmartSearch.normalize(track.lyrics || '');
            const normGenre = VisolSmartSearch.normalize(track.genre || '');
            const normKeywords = (track.keywords || []).map(k => VisolSmartSearch.normalize(k)).join(' ');

            // 1. To'g'ridan-to'g'ri mos kelish (Eng yuqori ball)
            if (normTitle.includes(normQuery)) score += 100;
            if (normArtist.includes(normQuery)) score += 80;
            if (normLyrics.includes(normQuery)) score += 70; // Naqorat bo'yicha topildi!
            if (normKeywords.includes(normQuery)) score += 60;
            if (normGenre.includes(normQuery)) score += 40;

            // 2. Tokenlar (so'zlar) bo'yicha mos kelish
            queryTokens.forEach(token => {
                if (normTitle.includes(token)) score += 30;
                if (normArtist.includes(token)) score += 25;
                if (normLyrics.includes(token)) score += 20; // Naqoratdagi so'zlar
                if (normKeywords.includes(token)) score += 15;
            });

            // 3. Xatoliklar bilan yozilganda (Fuzzy matching)
            if (score === 0 && normQuery.length >= 3) {
                const words = `${normTitle} ${normArtist}`.split(/\s+/);
                for (let w of words) {
                    if (w.length >= 3) {
                        const dist = VisolSmartSearch.levenshtein(w, normQuery);
                        if (dist <= 2) {
                            score += Math.max(10, 30 - dist * 10);
                            break;
                        }
                    }
                }
            }

            return { track, score };
        });

        // Natijalarni saralash va filtrlash
        const filtered = scored
            .filter(item => item.score > 0)
            .sort((a, b) => b.score - a.score)
            .map(item => item.track);

        // Agar hech narsa topilmasa ham, bo'sh qoldirmay to'liq katalog qaytariladi
        return filtered.length > 0 ? filtered : this.catalog;
    }

    /**
     * Real-vaqtli Autocomplete Takliflari (YouTube Style Dropdown)
     */
    getSuggestions(query = '', limit = 5) {
        const raw = query.trim();
        if (!raw || raw.length < 1) return [];

        const normQuery = VisolSmartSearch.normalize(raw);
        const suggestions = new Set();

        for (let track of this.catalog) {
            const normTitle = VisolSmartSearch.normalize(track.title);
            const normArtist = VisolSmartSearch.normalize(track.artist);
            const normLyrics = VisolSmartSearch.normalize(track.lyrics || '');

            if (normTitle.includes(normQuery)) {
                suggestions.add({ text: track.title, type: '🎵 Qo\'shiq', track });
            }
            if (normArtist.includes(normQuery)) {
                suggestions.add({ text: track.artist, type: '👤 Ijrochi', track });
            }
            if (track.lyrics && normLyrics.includes(normQuery)) {
                // Naqoratdan mos keluvchi qismini kesib olish
                const idx = normLyrics.indexOf(normQuery);
                const start = Math.max(0, idx - 15);
                const snippet = track.lyrics.substr(start, 40) + '...';
                suggestions.add({ text: `"...${snippet}"`, type: '📝 Naqorat', track });
            }

            if (suggestions.size >= limit) break;
        }

        return Array.from(suggestions).slice(0, limit);
    }
}

// Global eksport
if (typeof window !== 'undefined') {
    window.VisolMusicPlayer = VisolMusicPlayer;
    window.VisolSmartSearch = VisolSmartSearch;
}

if (typeof module !== 'undefined' && module.exports) {
    module.exports = { VisolMusicPlayer, VisolSmartSearch };
}
