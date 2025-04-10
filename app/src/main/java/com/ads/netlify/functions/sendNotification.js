const fetch = require('node-fetch');

exports.handler = async (event, context) => {
  const { token, title, body, data } = JSON.parse(event.body);

  const FCM_API = 'https://fcm.googleapis.com/fcm/send';
  const SERVER_KEY = process.env.FCM_SERVER_KEY;

  const notification = {
    to: token,
    notification: {
      title: title,
      body: body,
    },
    data: data || {},
    priority: 'high',
  };

  try {
    const response = await fetch(FCM_API, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `key=${SERVER_KEY}`,
      },
      body: JSON.stringify(notification),
    });

    const result = await response.json();
    return {
      statusCode: 200,
      body: JSON.stringify({ success: true, result }),
    };
  } catch (error) {
    return {
      statusCode: 500,
      body: JSON.stringify({ success: false, error: error.message }),
    };
  }
};
