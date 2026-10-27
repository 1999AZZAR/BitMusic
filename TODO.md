# BitMusic Roadmap & Improvements

A prioritized backlog of upcoming features, UX refinements, audio pipeline enhancements, and source integrations.

---

## 1. SoundCloud Feature Expansion

- [x] **Direct URL Resolver (Track & Playlist)** ✔️
  - Paste any `soundcloud.com/<artist>/<track>`, `sets/<playlist>`, or shortlink (`on.soundcloud.com/...`) in the search bar to stream or view tracks directly.
- [x] **Native Station / Related Tracks in Autoplay** ✔️
  - Intercept SoundCloud tracks and query `/tracks/{id}/related` to queue recommendations before falling back to YouTube Music matching.
- [ ] **Public Profile Importer into Library**
  - Enter a SoundCloud username or profile URL to fetch public Likes and Sets.
  - Add single-click "Import to Library" to persist tracks as local BitMusic playlists.
- [ ] **Track Waveform & Comments Indicator**
  - Fetch SoundCloud visual waveform data (`waveform_url`) or timestamped comment counts for enhanced player sheet visuals.

---

## 2. Search & UX Polish

- [ ] **Source Filter Chips in Search**
  - **Desktop**: Add quick filter pills (`All`, `YouTube`, `SoundCloud`, `Local`) below the search bar to filter between official studio releases, bootlegs, and local files.
  - **Android / TV**: Upgrade the segmented control into a scrollable filter row supporting all active sources.
- [ ] **Title Sanitizer for Lyrics Matching**
  - Strip promotional tags, quality tags, and producer brackets (`[Free DL]`, `(Prod. by ...)`, `[Official Audio]`, `feat. ...`, `Remix 2024`) before querying LRCLIB/Musixmatch.
  - Dramatically improves lyric hit rates for non-standard SoundCloud and local music titles.
- [ ] **Audio Source Visual Badges**
  - Display small, crisp source badges (YouTube, SoundCloud, Local, etc.) in queue rows and player bottom sheet.

---

## 3. Playback Resilience & Audio Quality

- [ ] **Dual-Format Failover (HLS AAC ↔ Progressive MP3)**
  - If a SoundCloud HLS stream (`hls/aac`) experiences buffer under-runs or network drops, automatically fall back to progressive MP3 (`http/mp3`) at the current timestamp without interrupting playback.
- [ ] **Source Transition Smoothing**
  - Enhance crossfade and gapless transitions when switching between different audio containers/codecs (e.g., Opus in WebM from YouTube into AAC in MPEG-TS from SoundCloud).
- [ ] **Universal Audio Cache**
  - Extend the existing disk/memory caching engine to transparently cache non-YouTube audio streams for offline replay and bandwidth conservation.

---

## 4. Additional Audio Sources

- [ ] **Subsonic / Navidrome API (Self-Hosted)**
  - Native integration for personal music servers (Navidrome, Jellyfin, Gonic, Airsonic).
  - Fast server-side search, instant indexing of massive libraries, artist biographies, and lossless streaming over standard REST API.
- [ ] **Audius Integration**
  - Decentralized, free music streaming API offering high-quality 320kbps MP3 streams.
  - No API key constraints, ideal for electronic, hip-hop, and indie artists.
- [ ] **Bandcamp Public Stream Discovery**
  - Discover and preview public track and album streams from Bandcamp artists.

---

## 5. Platform-Specific Optimizations

- [ ] **Desktop Linux / Wayland**
  - MPRIS media key enhancements and track artwork synchronization with desktop notifications.
- [ ] **Android TV Remote / D-pad Navigation**
  - Optimize focus traversal across source filter chips and deep playlist grids.
- [ ] **macOS Native Keybinds & Media Center**
  - NowPlayingInfoCenter integration for macOS Control Center playback controls.
