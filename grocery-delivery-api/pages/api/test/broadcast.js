import eventBroadcaster from '../../../lib/eventBroadcaster.js';

/**
 * Test endpoint to verify Supabase Realtime broadcasting
 * GET /api/test/broadcast
 */
export default async function handler(req, res) {
  if (req.method !== 'GET') {
    return res.status(405).json({ message: 'Method not allowed' });
  }

  try {
    console.log('[TEST] Broadcasting test event to admin:orders channel');
    
    // Try to broadcast a test event
    await eventBroadcaster.broadcastToChannel('admin:orders', 'test_event', {
      message: 'This is a test broadcast',
      timestamp: new Date().toISOString()
    });

    console.log('[TEST] Test broadcast completed');

    res.status(200).json({
      success: true,
      message: 'Test broadcast sent to admin:orders channel',
      channel: 'admin:orders',
      event: 'test_event'
    });

  } catch (error) {
    console.error('[TEST] Error broadcasting test event:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to broadcast test event',
      error: error.message
    });
  }
}
