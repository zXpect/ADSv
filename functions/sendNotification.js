const axios = require('axios');

exports.handler = async (event, context) => {
  // Verificar método
  if (event.httpMethod !== 'POST') {
    return {
      statusCode: 405,
      body: JSON.stringify({ success: false, error: 'Method Not Allowed' }),
    };
  }

  // Verificar clave FCM
  const SERVER_KEY = process.env.FCM_SERVER_KEY;
  if (!SERVER_KEY) {
    console.error('FCM_SERVER_KEY no está configurada en las variables de entorno');
    return {
      statusCode: 500,
      body: JSON.stringify({
        success: false,
        error: 'FCM Server Key no está configurada'
      }),
    };
  }

  try {
    // Analizar el cuerpo de la solicitud
    const { token, title, body, data } = JSON.parse(event.body);

    // Validación de campos requeridos
    if (!token) {
      return {
        statusCode: 400,
        body: JSON.stringify({ success: false, error: 'Token FCM requerido' }),
      };
    }

    console.log(`Enviando notificación a token: ${token.substring(0, 6)}...`);

    const FCM_API = 'https://fcm.googleapis.com/fcm/send';

    const notification = {
      to: token,
      notification: {
        title: title || 'Nueva solicitud',
        body: body || 'Tienes una nueva solicitud de servicio',
        sound: 'default',
        click_action: 'FLUTTER_NOTIFICATION_CLICK'
      },
      data: data || {},
      priority: 'high',
    };

    // Realizar la solicitud a FCM usando axios
    const response = await axios.post(FCM_API, notification, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `key=${SERVER_KEY}`
      }
    });

    const result = response.data;
    console.log('Respuesta FCM:', JSON.stringify(result));

    // Verificar la respuesta de FCM
    if (result.failure > 0 || (result.success !== undefined && result.success === 0)) {
      return {
        statusCode: 200,
        body: JSON.stringify({
          success: false,
          message: 'Error enviando notificación a FCM',
          result
        }),
      };
    }

    return {
      statusCode: 200,
      body: JSON.stringify({ success: true, result }),
    };
  } catch (error) {
    console.error('Error en la función Netlify:', error);
    return {
      statusCode: 500,
      body: JSON.stringify({
        success: false,
        error: error.message || 'Error interno del servidor'
      }),
    };
  }
};