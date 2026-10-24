# kozu

Nghe nhạc từ các beatmap osu! – giao diện Material 3 Expressive, tông màu hồng osu!.

## Build và phát hành APK trên GitHub

- Mỗi pull request vào `main` và mỗi lần chạy workflow **Android Build** sẽ build APK debug; tải APK ở phần **Artifacts** của lần chạy trong tab **Actions**.
- Mỗi lần cập nhật `main` hoặc chạy thủ công workflow **Release Android APK**, GitHub Actions sẽ build APK release có chữ ký và đăng lên **Releases**.
- Để phát hành, tạo keystore Android và thêm các repository secrets sau trong **Settings → Secrets and variables → Actions**:
  - `ANDROID_KEYSTORE_BASE64`: nội dung keystore đã mã hóa Base64.
  - `ANDROID_KEYSTORE_PASSWORD`: mật khẩu keystore.
  - `ANDROID_KEY_ALIAS`: alias khóa.
  - `ANDROID_KEY_PASSWORD`: mật khẩu khóa.
- Sao lưu keystore an toàn và dùng đúng keystore này cho mọi bản cập nhật. Không đưa keystore hoặc mật khẩu vào Git.

## Tính năng

- **Màu sắc**: mặc định hồng osu! (`#FF66AA`). Khi đang phát nhạc, toàn bộ app chuyển sang tông màu ảnh bìa bài hát (chuyển màu mượt); tạm dừng thì quay về hồng osu!. Bảng màu sinh bằng OKLCH (không cần thư viện ngoài), có tuỳ chọn Pure black (AMOLED).
- **Home**: lời chào theo giờ, Quick Picks (số thứ tự kiểu hit-circle), Your Mix + nút shuffle, Recently Played, thống kê nghe nhạc trong tuần, Made for you.
- **Explore**: Your Library (Liked / Most played / playlist), New Releases, Listen again, Trending (bảng xếp hạng), Loved by the community, nút "Surprise me".
- **Library**: Liked / Playlists / Songs, đổi giữa list và grid, sắp xếp, shuffle, tạo / đổi tên / xoá playlist.
- **Search**: tìm khi gõ (debounce), lịch sử tìm kiếm, duyệt theo thể loại, lọc theo Relevance / Most played / Newest.
- **Now Playing**: wavy slider, nút điều khiển "nở" theo lực nhấn, vuốt xuống để đóng, vuốt ngang để đổi bài, hàng chờ phát, thông tin codec thật của luồng âm thanh.
- **Mini player**: ảnh bìa tròn xoay + vòng tiến trình, vuốt lên để mở, vuốt ngang để đổi bài.
- **Phát nền**: `MediaSessionService` (Media3) – có thông báo điều khiển, khoá màn hình, tai nghe / Bluetooth.
- **osu! API**: nhập Client ID / Secret trong Settings để dùng toàn bộ catalog; chưa nhập thì dùng dữ liệu mẫu.

Âm thanh là bản nghe thử chính thức của osu! (`b.ppy.sh/preview/{id}.mp3`). Bài demo không có id sẽ phát file `res/raw/test.m4a`.

## Cấu trúc thư mục

```
app/src/main/kotlin/vn/kozu/
├── KozuApplication.kt, MainActivity.kt
├── di/            AppContainer (tạo & giữ các singleton)
├── domain/model/  Beatmap, Playlist, AppSettings, ListeningStats, SearchModels
├── data/
│   ├── local/       DataStore: Settings, Favorites, Playlists, History
│   ├── remote/osu/  Retrofit API, OAuth token, DTO
│   └── repository/  BeatmapRepository, DiscoverFeed, SampleBeatmaps
├── player/        PlaybackService (nền) + PlaybackController (nối với UI)
├── util/          Formatters
└── ui/
    ├── KozuApp.kt   gốc: chọn màu theme, điều hướng, overlay, sheet
    ├── theme/       OKLCH, ColorScheme, Typography, Shapes (CookieShape), Motion
    ├── components/  BeatmapArt, TrackRow, WavySlider, Bounce, Sheets, ...
    ├── navigation/  KozuTab, FloatingNavBar
    ├── home/ explore/ library/ search/ settings/   (Screen + ViewModel)
    └── player/      PlayerScreen, MiniPlayer, TransportControls, QueueSheet
```

## Lấy osu! API key

1. Vào https://osu.ppy.sh/home/account/edit#oauth → **New OAuth Application**.
2. Chép Client ID và Client Secret vào Settings → osu! API → **Save & test**.
