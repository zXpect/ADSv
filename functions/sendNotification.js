// functions/sendNotification.js
const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

// Inicializar Firebase Admin solo una vez
if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
}

exports.handler = async function(event, context) {
  // Permitir CORS para la app Android
  const headers = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type',
    'Access-Control-Allow-Methods': 'POST, OPTIONS'
  };

  // Manejar solicitudes OPTIONS para CORS pre-flight
  if (event.httpMethod === 'OPTIONS') {
    return {
      statusCode: 200,
      headers,
      body: JSON.stringify({ message: 'Preflight request successful' })
    };
  }

  // Procesar sólo solicitudes POST
  if (event.httpMethod !== 'POST') {
    return {
      statusCode: 405,
      headers,
      body: JSON.stringify({ message: 'Method not allowed' })
    };
  }

  try {
    // Parsear el cuerpo de la solicitud
    const body = JSON.parse(event.body);
    console.log('Recibida solicitud de notificación:', body);

    // Extraer datos
    const { token, title, body: messageBody, data } = body;

    if (!token) {
      return {
        statusCode: 400,
        headers,
        body: JSON.stringify({ message: 'Token no proporcionado' })
      };
    }

    // Crear mensaje para FCM
    const message = {
      token,
      notification: {
        title,
        body: messageBody
      },
      data: data || {},
      android: {
        priority: 'high',
        notification: {
          sound: 'default',
          priority: 'high',
          channelId: 'high_importance_channel'
        }
      }
    };

    // Enviar la notificación
    console.log('Enviando mensaje FCM:', message);
    const response = await admin.messaging().send(message);
    console.log('Notificación enviada correctamente:', response);

    return {
      statusCode: 200,
      headers,
      body: JSON.stringify({
        success: true,
        messageId: response
      })
    };

  } catch (error) {
    console.error('Error al enviar la notificación:', error);

    return {
      statusCode: 500,
      headers,
      body: JSON.stringify({
        success: false,
        error: error.message
      })
    };
  }
};