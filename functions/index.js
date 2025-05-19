/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const {onRequest} = require("firebase-functions/v2/https");
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

exports.registerProperty = functions.https.onRequest(async (req, res) => {
  const { id, ownerId, title, description, address, price, startDate, endDate, imageUrl} = req.body;

  try {
    const geoRes = await axios.get("https://dapi.kakao.com/v2/local/search/address.json", {
      headers: { Authorization: `ee2b6e2d5141747d912a9540432a7a61` },
      params: { query: address }
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
      address,
      price,
      startDate,
      endDate,
      imageUrl,
      latitude: parseFloat(latitude),
      longitude: parseFloat(longitude)
    };

    await admin.firestore().collection("Properties").add(newProperty);
    res.status(200).json({ message: "매물 등록 성공" });

  } catch (error) {
    console.error(error);
    res.status(500).json({ error: "서버 오류 발생" });
  }
});
