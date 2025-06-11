/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const {onRequest} = require("firebase-functions/v2/https");
const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const logger = require("firebase-functions/logger");

// Create and deploy your first functions
// https://firebase.google.com/docs/functions/get-started

// exports.helloWorld = onRequest((request, response) => {
//   logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });



const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");

admin.initializeApp();

exports.registerProperty = functions.https.onRequest(async (req, res) =>{
  const {
  id,
  ownerId,
  title,
  description,
  addressMain,
  addressDetail,
  price,
  startDate,
  endDate,
  imageUrl,
  landlordPhone
  } = req.body;

  const fullAddress = `${addressMain} ${addressDetail}`;

  try {
    const geoRes = await axios.get("https://dapi.kakao.com/v2/local/search/address.json", {
      headers: { Authorization: `KakaoAK ee2b6e2d5141747d912a9540432a7a61` },
      params: { query: fullAddress }
    });

    if (geoRes.data.documents.length === 0) {
      return res.status(400).json({ message: "주소를 찾을 수 없습니다." });
    }

    const { y: latitude, x: longitude } = geoRes.data.documents[0];

    const newProperty = {
      id,
      ownerId,
      title,
      description,
      addressMain,
      addressDetail,
      price,
      startDate,
      endDate,
      imageUrl,
      landlordPhone,
      latitude: parseFloat(latitude),
      longitude: parseFloat(longitude)
    };

    await admin.firestore().collection("Properties").doc(id).create(newProperty);
    res.status(200).json({ message: "매물 등록 성공" });

  } catch (error) {
    console.error(error);
    res.status(500).json({ error: "서버 오류 발생" });
  }
});

exports.recordConsent = functions.https.onRequest(async (req, res) => {
  // CORS 허용
  res.set('Access-Control-Allow-Origin', '*');
  res.set('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.set('Access-Control-Allow-Headers', 'Content-Type');
  if (req.method === 'OPTIONS') {
    return res.status(204).send('');
  }
  if (req.method !== 'POST') {
    return res.status(405).send('Method Not Allowed');
  }
  const { reqId, response } = req.body || {};
  if (!reqId || !['agree','reject'].includes(response)) {
    return res.status(400).send('Invalid request');
  }
  try {
    const consentRef = admin.firestore().collection('Consents').doc(reqId);
    const snap = await consentRef.get();
    if (!snap.exists) {
      return res.status(404).send('Consent request not found');
    }
    await consentRef.update({ response, respondedAt: Date.now() });
    return res.status(200).send('OK');
  } catch (e) {
    console.error(e);
    return res.status(500).send('Server Error');
  }
});

exports.notifySublessor = onDocumentWritten(
  "Consents/{reqId}",
  async (event) => {
    const before = event.data.before.data();
    const after = event.data.after.data();

    // 처음에는 response 필드가 없었다가, 응답이 기록된 경우
    if (!before?.response && after?.response) {
      const sublessorId = after.sublessorId;
      // sublessor 의 FCM 토큰 가져오기
      const userSnap = await admin
        .firestore()
        .collection("Users")
        .doc(sublessorId)
        .get();
      const token = userSnap.get("fcmToken");
      if (!token) {
        logger.warn(`No FCM token for user ${sublessorId}`);
        return;
      }

      const payload = {
        notification: {
          title: "계약서 동의 상태 업데이트",
          body:
            after.response === "agree"
              ? "임대인이 계약서에 동의했습니다."
              : "임대인이 계약서를 거부했습니다."
        },
        data: {
          reqId: event.params.reqId,
          response: after.response
        }
      };

      try {
        await admin.messaging().sendToDevice(token, payload);
        logger.info(`Sent FCM to ${sublessorId}`);
      } catch (e) {
        logger.error("FCM send error:", e);
      }
    }
  }
);