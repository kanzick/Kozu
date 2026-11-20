<div align="center">

  <p>
    <a href="README.md"><img src="https://img.shields.io/badge/🇬🇧%20English-ff66aa?style=for-the-badge&labelColor=21182e" alt="English" /></a>
    <a href="README_VI.md"><img src="https://img.shields.io/badge/🇻🇳%20Ti%E1%BA%BFng%20Vi%E1%BB%87t-3fa9f5?style=for-the-badge&labelColor=21182e" alt="Tiếng Việt" /></a>
  </p>

  <p><strong>Ứng dụng nghe nhạc dành riêng cho osu! được xây dựng với Kotlin.</strong></p>
  <p>Nghe các bài hát yêu thích từ osu! mà không cần khởi chạy trò chơi gốc. Ứng dụng này hướng tới tiêu chí nhỏ gọn, nhanh chóng và mang lại trải nghiệm sử dụng dễ dàng.</p>

  <p>
    <a href="https://github.com/kanzick/Kozu/releases/latest"><strong>Tải xuống</strong></a> ·
    <a href="#tinh-nang"><strong>Tính năng</strong></a> ·
    <a href="#ket-noi-osulazer"><strong>osu!lazer</strong></a> ·
    <a href="#build-tu-ma-nguon"><strong>Build</strong></a> ·
    <a href="https://github.com/kanzick/Kozu/issues"><strong>Hỗ trợ</strong></a>
  </p>

  <p>
    <a href="https://github.com/kanzick/Kozu/actions/workflows/build.yml"><img src="https://img.shields.io/github/actions/workflow/status/kanzick/Kozu/build.yml?branch=main&style=for-the-badge&label=build&logo=githubactions&logoColor=white&labelColor=21182e" alt="Trạng thái build" /></a>
    <a href="https://github.com/kanzick/Kozu/releases"><img src="https://img.shields.io/github/v/release/kanzick/Kozu?include_prereleases&style=for-the-badge&color=ff66aa&labelColor=21182e&logo=github" alt="Bản phát hành mới nhất" /></a>
    <a href="https://github.com/kanzick/Kozu/releases"><img src="https://img.shields.io/github/downloads/kanzick/Kozu/total?style=for-the-badge&color=ff8a4c&labelColor=21182e&logo=android" alt="Lượt tải" /></a>
    <a href="https://github.com/kanzick/Kozu/stargazers"><img src="https://img.shields.io/github/stars/kanzick/Kozu?style=for-the-badge&color=3fa9f5&labelColor=21182e&logo=github" alt="GitHub stars" /></a>
    <a href="LICENSE"><img src="https://img.shields.io/github/license/kanzick/Kozu?style=for-the-badge&color=a58cff&labelColor=21182e" alt="Giấy phép GPL-3.0" /></a>
  </p>

  <p>
    <img src="https://img.shields.io/badge/Android-9%2B-3ddc84?style=flat-square&logo=android&logoColor=white" alt="Android 9 trở lên" />
    <img src="https://img.shields.io/badge/Kotlin-2.4.21-7f52ff?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" />
    <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285f4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose và Material 3" />
  </p>
</div>

---

<a id="tinh-nang"></a>
## Tính năng

<table>
  <tr>
    <td width="50%" valign="top">
      <h3>🎼 Khám phá</h3>
      <ul>
        <li>Tìm beatmap trong kho osu! theo tên bài, nghệ sĩ hoặc mapper.</li>
        <li>Khám phá Ranked, Loved, thịnh hành, bản phát hành mới và beatmap nổi bật.</li>
        <li>Duyệt theo các chế độ chơi và thu hẹp kết quả bằng bộ lọc.</li>
        <li>Mở từng beatmap set để xem độ khó và thông tin chi tiết.</li>
      </ul>
    </td>
    <td width="50%" valign="top">
      <h3>🎧 Nghe nhạc</h3>
      <ul>
        <li>Chọn phát toàn bài qua các mirror công khai đã bật hoặc chỉ nghe bản preview.</li>
        <li>Tiếp tục phát nhạc nền với hàng chờ phát liên tục.</li>
        <li>Sử dụng shuffle, repeat, autoplay, lịch sử hàng chờ và crossfade.</li>
        <li>Tùy chỉnh âm thanh bằng equalizer, preset và điều khiển loudness.</li>
      </ul>
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <h3>🗂️ Thư viện</h3>
      <ul>
        <li>Quản lý bài hát yêu thích, playlist, nội dung đã tải và lịch sử nghe.</li>
        <li>Sắp xếp và kéo thả bài hát theo thứ tự mong muốn.</li>
        <li>Tải bài hát để nghe ngoại tuyến và sử dụng trong chế độ Game.</li>
        <li>Theo dõi xu hướng nghe, bài hát nổi bật và hoạt động trong tuần.</li>
      </ul>
    </td>
    <td width="50%" valign="top">
      <h3>🎮 Chế độ Mania</h3>
      <ul>
        <li>Chơi osu!mania cục bộ từ beatmap đã tải hoặc nhập vào.</li>
        <li>Chọn độ khó và điều chỉnh thiết lập gameplay.</li>
        <li>Lưu điểm số, thành tích cá nhân tốt nhất và lịch sử chơi.</li>
        <li>Nhập chart Mania cùng beatmap từ osu!lazer.</li>
      </ul>
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <h3>🔄 osu!lazer</h3>
      <ul>
        <li>Nhập một hoặc nhiều file <code>.osz</code>, quét thư mục hoặc chia sẻ file sang Kozu.</li>
        <li>Đọc thư viện osu!lazer từ thư mục dữ liệu đã chọn hoặc qua Shizuku trên thiết bị hỗ trợ.</li>
        <li>Chuyển các collection của osu!lazer thành playlist Kozu.</li>
        <li>Xuất beatmap đã chọn sang osu!lazer hoặc lưu và chia sẻ dưới dạng <code>.osz</code>.</li>
      </ul>
    </td>
    <td width="50%" valign="top">
      <h3>🎨 Cá nhân hóa</h3>
      <ul>
        <li>Chọn giao diện sáng, tối, theo hệ thống hoặc AMOLED.</li>
        <li>Để màu sắc trình phát thay đổi theo ảnh bìa đang phát.</li>
        <li>Tùy chỉnh kiểu player, kiểu chữ, mật độ và giao diện.</li>
        <li>Chuyển đổi giữa tiếng Anh và tiếng Việt.</li>
      </ul>
    </td>
  </tr>
</table>

Kozu còn tích hợp tin tức osu!, kết nối hồ sơ tùy chọn, nguồn âm thanh có thể cấu hình, công cụ quản lý bộ nhớ và sao lưu/khôi phục có chọn lọc.

## Nguồn âm thanh hoạt động như thế nào?

Kozu có hai chế độ phát:

- **Phát toàn bài:** thử các nguồn mirror công khai bạn đã bật và chỉ phát khi tìm được tệp âm thanh đầy đủ. Ứng dụng không âm thầm chuyển sang bản preview ngắn.
- **Chỉ nghe preview:** sử dụng nguồn preview của beatmap.

Danh sách beatmap, metadata, ảnh bìa và preview được lấy từ osu!. Các dịch vụ bên thứ ba tùy chọn có thể cung cấp chức năng phát toàn bài và tải xuống. **Kozu không lưu trữ beatmap, âm thanh hay artwork**; tình trạng khả dụng và điều khoản do từng nhà cung cấp kiểm soát, còn nội dung vẫn thuộc quyền sở hữu của tác giả.

> [!TIP]
> Không cần tài khoản osu! để duyệt catalog, nghe nhạc hoặc tải xuống. Bạn chỉ cần đăng nhập khi muốn kết nối hồ sơ osu! và đồng bộ thông tin thư viện cá nhân.

<a id="osu!lazer"></a>
## osu!lazer

Chuyển beatmap giữa Kozu và osu!lazer mà không cần tự xây dựng lại thư viện.

| Nhập vào Kozu | Xuất từ Kozu |
|---|---|
| Nhập file `.osz`, nhiều file cùng lúc hoặc cả thư mục. | Gửi beatmap đã chọn sang osu!lazer. |
| Chia sẻ trực tiếp beatmap đã xuất từ osu!lazer. | Lưu hoặc chia sẻ archive `.osz` tiêu chuẩn. |
| Đọc thư viện lazer từ thư mục dữ liệu hoặc dùng Shizuku nếu khả dụng. | Có thể kèm chart Game đã tải trong bản xuất. |
| Tùy chọn tạo playlist Kozu từ các collection của lazer. | |

Khi đọc trực tiếp thư viện, hãy đóng osu!lazer trước để cơ sở dữ liệu không bị ứng dụng sử dụng. Kozu chỉ đọc một bản sao và không chỉnh sửa dữ liệu của lazer.

## Tải xuống

<a href="https://github.com/kanzick/Kozu/releases/latest"><img src="https://img.shields.io/badge/T%E1%BA%A3i%20Kozu-GitHub%20Releases-ff66aa?style=for-the-badge&logo=github&logoColor=white&labelColor=21182e" alt="Tải Kozu từ GitHub Releases" /></a>

Tải APK phù hợp với thiết bị tại [GitHub Releases](https://github.com/kanzick/Kozu/releases). Bản phát hành có các biến thể sau:

| APK | Dành cho |
|---|---|
| `kozu-universal.apk` | Lựa chọn đơn giản nhất khi bạn không chắc thiết bị dùng ABI nào. |
| `kozu-arm64-v8a.apk` | Hầu hết điện thoại và máy tính bảng Android hiện đại. |
| `kozu-armeabi-v7a.apk` | Thiết bị ARM 32-bit. |
| `kozu-x86.apk` / `kozu-x86_64.apk` | Thiết bị và trình giả lập tương thích x86. |

**Android tối thiểu:** Android 9 (API 28).

<a id="build-tu-ma-nguon"></a>
## Build từ mã nguồn

**Yêu cầu**

- Android Studio và Android SDK.
- JDK 17 trở lên.
- Thiết bị hoặc trình giả lập chạy Android 9+ để cài và thử ứng dụng.

```bash
git clone https://github.com/kanzick/Kozu.git
cd Kozu

# Build APK debug
./gradlew assembleDebug

# Hoặc cài trực tiếp lên thiết bị / trình giả lập đang kết nối
./gradlew installDebug
```

## Tài khoản, quyền riêng tư và sao lưu

- **Dùng như khách:** có thể duyệt, nghe nhạc và tải xuống mà không cần đăng nhập.
- **Đăng nhập osu! tùy chọn:** kết nối hồ sơ để hiển thị thông tin tài khoản và đồng bộ dữ liệu cá nhân.
- **Sao lưu có chọn lọc:** chọn cài đặt, bài hát yêu thích, playlist, lịch sử nghe, nhạc đã tải hoặc dữ liệu game.
- **Không đưa thông tin bí mật vào bản sao lưu:** token đăng nhập, thông tin xác thực API và mật khẩu proxy không được lưu trong file backup Kozu.

## Hỗ trợ và đóng góp

Bạn tìm thấy lỗi hoặc có ý tưởng mới? Hãy tạo [issue](https://github.com/kanzick/Kozu/issues). Mọi đóng góp và pull request đều được chào đón. Vui lòng đọc [Code of Conduct](CODE_OF_CONDUCT.md) trước khi tham gia.

## Ghi nhận và pháp lý

Kozu là dự án độc lập do cộng đồng phát triển. Ứng dụng không liên kết, không được osu! hoặc ppy xác nhận hay tài trợ.

osu! là trò chơi và hệ sinh thái beatmap nền tảng cho dự án này. Âm thanh và artwork của từng beatmap vẫn thuộc quyền sở hữu của tác giả tương ứng. Vui lòng tôn trọng điều khoản của các dịch vụ và nguồn nội dung bạn sử dụng.

## Giấy phép

Mã nguồn Kozu được phát hành theo **GNU General Public License v3.0 (GPL-3.0)**. Xem file [`LICENSE`](LICENSE) để đọc toàn văn giấy phép.

---

<div align="center">
  <strong>Đã tìm thấy bài hát yêu thích tiếp theo?</strong><br />
  <a href="https://github.com/kanzick/Kozu/stargazers">Tặng Kozu một ⭐ trên GitHub</a> ·
  <a href="https://github.com/kanzick/Kozu/releases">Tải bản mới nhất</a>
</div>
