# RentBridgeSub

RentBridgeSub는 전대인(Sublessor)과 전차인(Sublessee) 간의 전대차 계약 과정을 간편하게 지원하는 Android 앱입니다.

## 주요 기능

* **매물 등록 및 관리**: 전대인이 자신이 보유한 매물을 등록, 수정, 삭제할 수 있습니다.
* **매물 조회**: 전차인은 주변 매물을 지도 또는 리스트 형태로 조회하고, 관심 매물에 문의할 수 있습니다.
* **채팅**: 1:1 채팅 기능을 통해 전대인과 전차인이 실시간으로 대화할 수 있습니다.
* **계약서 업로드 & 검토 요청**: 전대인이 계약서 파일(PDF)을 업로드하여 전차인 및 임대인에게 검토·동의 요청을 전송합니다.
* **SMS 링크 전송**: Firebase Cloud Functions를 활용해 임대인에게 SMS로 계약 동의 링크를 자동 전송합니다.
* **계약 동의 현황 확인**: 임대인이 링크에서 동의/거부하면 앱 내에 실시간으로 계약 상태가 업데이트됩니다.
* **학생 인증**: 대학 이메일 인증을 통해 사용자가 학생임을 증명할 수 있는 기능을 제공하며, 인증된 학생에게는 🎓 뱃지를 표시합니다.

## 아키텍처

* **Front-end**: Kotlin, Android SDK, ViewBinding / DataBinding
* **Back-end**: Firebase Authentication, Cloud Firestore, Cloud Storage
* **Functions**: Firebase Cloud Functions (Node.js) — SMS 전송, 동의 기록 처리
* **이미지 로딩**: Glide, Picasso
* **지도 & 위치**: Google Maps SDK, Play Services Location

## 설치 및 실행

1. 이 리포지토리를 클론합니다:

   ```bash
   git clone https://github.com/<your-username>/RentBridgeSub.git
   ```
2. Android Studio로 프로젝트를 엽니다.
3. Firebase 프로젝트 설정 파일(`google-services.json`)을 `app/` 폴더에 추가합니다.
4. Gradle을 동기화(sync)합니다.
5. 디버그 APK를 빌드하고 설치합니다:

   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
6. 앱을 실행하고, 회원가입 후 기능을 테스트하세요.

## 환경 설정

* **Firebase 설정**: 프로젝트 Console에서 Authentication, Firestore, Storage를 활성화하세요.
* **SHA 키 등록**: Firebase 인증(reCAPTCHA)을 위해 `debug.keystore`의 SHA-1/256 지문을 등록하세요.
* **Cloud Functions 배포**:

  ```bash
  cd functions
  npm install
  firebase deploy --only functions
  ```

## 디렉토리 구조

```
/app
  ├─ src/main/java/com/example/rentbridgesub  # 앱 코드
  ├─ src/main/res                            # 레이아웃, 그림자원
  └─ google-services.json                    # Firebase 설정
/functions                                    # Cloud Functions 코드
/README.md                                    # 프로젝트 설명
```

## 라이센스

MIT License © 2025 RentBridgeSub Contributors
